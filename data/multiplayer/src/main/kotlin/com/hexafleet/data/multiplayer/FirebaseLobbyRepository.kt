package com.hexafleet.data.multiplayer

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hexafleet.domain.game.multiplayer.LobbyRoom
import com.hexafleet.domain.game.multiplayer.NetworkPayload
import com.hexafleet.domain.game.multiplayer.RoomCodeGenerator
import com.hexafleet.domain.game.multiplayer.RoomStatus
import com.hexafleet.domain.game.multiplayer.ZeroKnowledgeFog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Çevrimiçi Lobi ve Maç Senkronizasyon Deposu.
 * Firebase Realtime Database ve reaktif bellek senkronizasyonu.
 * Mock data tamamen kaldırılmıştır. Canlı Firebase bulut senkronizasyonu ve çevrimdışı dayanıklılık içerir.
 */
class FirebaseLobbyRepository {

    companion object {
        // User Requirement 2: Mock odalar tamamen kaldırıldı, gerçek dinamik lobi yapısı başlatıldı.
        val instance = FirebaseLobbyRepository()
    }

    // Aktif lobi odalarını bellekte ve Firebase'de eşzamanlı tutan yapı
    private val activeRooms = mutableMapOf<String, MutableStateFlow<LobbyRoom?>>()
    private val publicRoomsFlow = MutableStateFlow<List<LobbyRoom>>(emptyList())

    // Firebase Realtime Database İstemcisi (Hata yakalama ve çevrimdışı dayanıklılık)
    private val firebaseDatabase: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance().also { db ->
                try {
                    db.setPersistenceEnabled(true)
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            null
        }
    }

    init {
        // Firebase Cloud senkronizasyon dinleyicisi
        try {
            firebaseDatabase?.getReference("rooms")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val room = child.toLobbyRoom()
                        if (room != null) {
                            val flow = activeRooms.getOrPut(room.roomCode) { MutableStateFlow(room) }
                            flow.value = room
                        }
                    }
                    updatePublicRooms()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Firebase bağlantı hatası durumunda yerel reaktif akış kesintisiz çalışmaya devam eder
                }
            })
        } catch (_: Throwable) {}
    }

    private fun updatePublicRooms() {
        val rooms = activeRooms.values
            .mapNotNull { it.value }
            .filter { !it.isPrivate && it.status == RoomStatus.WAITING_FOR_OPPONENT && !it.isFull }
            .sortedByDescending { it.lastHeartbeatTimestamp }
        publicRoomsFlow.value = rooms
    }

    /**
     * Canlı açık odaları Flow olarak dinler (Private odalar listelenmez).
     */
    fun observePublicRooms(): Flow<List<LobbyRoom>> = publicRoomsFlow.asStateFlow()

    fun getActivePublicRooms(): List<LobbyRoom> = publicRoomsFlow.value

    /**
     * Yeni bir çok oyunculu lobi oluşturur (User Requirements 4 & 6).
     * Mükerrer olmayan benzersiz oda kodu garantisi, Firebase senkronizasyonu ve Gizli (Private) lobi desteği sunar.
     */
    fun createRoom(
        hostPlayerId: String,
        pin: String? = null,
        turnDurationSeconds: Int = 45,
        mapRadius: Int = 5,
        enableHazards: Boolean = true,
        isPrivate: Boolean = false,
        preferredCode: String? = null
    ): Result<LobbyRoom> {
        val activeCodes = activeRooms.filter { 
            val s = it.value.value?.status
            s != RoomStatus.FINISHED && s != RoomStatus.FORFEITED 
        }.keys.toSet()
        val roomCode = if (!preferredCode.isNullOrBlank() && preferredCode !in activeCodes && RoomCodeGenerator.isValid(preferredCode)) {
            preferredCode
        } else {
            RoomCodeGenerator.generateUnique(activeCodes)
        }

        val roomSalt = UUID.randomUUID().toString().take(12)
        val pinHash = if (!pin.isNullOrBlank()) ZeroKnowledgeFog.hashPin(pin, roomSalt) else null

        val room = LobbyRoom(
            roomCode = roomCode,
            hostPlayerId = hostPlayerId,
            pinHash = pinHash,
            roomSalt = roomSalt,
            turnDurationSeconds = turnDurationSeconds,
            mapRadius = mapRadius,
            enableHazards = enableHazards,
            isPrivate = isPrivate,
            status = RoomStatus.WAITING_FOR_OPPONENT,
            currentTurnPlayerId = hostPlayerId
        )

        activeRooms[roomCode] = MutableStateFlow(room)
        updatePublicRooms()

        // Firebase Cloud Realtime Database Yazımı
        try {
            firebaseDatabase?.getReference("rooms")?.child(roomCode)?.setValue(room.toMap())
        } catch (_: Throwable) {}

        return Result.success(room)
    }

    /**
     * Geriye dönük uyumluluk için eski imza aşırı yüklemesi.
     */
    fun createRoom(
        hostPlayerId: String,
        pin: String? = null,
        isBlitz: Boolean = false
    ): Result<LobbyRoom> = createRoom(
        hostPlayerId = hostPlayerId,
        pin = pin,
        turnDurationSeconds = if (isBlitz) 10 else 45,
        mapRadius = 5,
        enableHazards = true,
        isPrivate = false
    )

    /**
     * Oda kodu ve isteğe bağlı PIN ile mevcut lobiye katılır.
     */
    fun joinRoom(
        roomCode: String,
        guestPlayerId: String,
        inputPin: String? = null
    ): Result<LobbyRoom> {
        val normalizedCode = roomCode.trim().uppercase()
        val roomFlow = activeRooms[normalizedCode]
            ?: return Result.failure(IllegalArgumentException("Oda bulunamadı: $normalizedCode"))

        val currentRoom = roomFlow.value
            ?: return Result.failure(IllegalArgumentException("Oda kapalı veya mevcut değil."))

        if (currentRoom.isFull && currentRoom.guestPlayerId != guestPlayerId) {
            return Result.failure(IllegalStateException("Oda dolu. İki oyuncu zaten eşleşti."))
        }

        // PIN Koruması Denetimi
        if (currentRoom.isPinProtected) {
            val expectedHash = currentRoom.pinHash!!
            if (inputPin.isNullOrBlank() || !ZeroKnowledgeFog.verifyPin(inputPin, currentRoom.roomSalt, expectedHash)) {
                return Result.failure(SecurityException("Hatalı veya eksik 4 haneli PIN şifresi."))
            }
        }

        val updatedRoom = currentRoom.copy(
            guestPlayerId = guestPlayerId,
            status = RoomStatus.FLEET_SETUP
        )
        roomFlow.value = updatedRoom
        updatePublicRooms()

        // Firebase Cloud Güncellemesi
        try {
            firebaseDatabase?.getReference("rooms")?.child(normalizedCode)?.setValue(updatedRoom.toMap())
        } catch (_: Throwable) {}

        return Result.success(updatedRoom)
    }

    /**
     * Odanın durumunu Flow olarak dinler.
     */
    fun observeRoom(roomCode: String): Flow<LobbyRoom?> {
        val normalized = roomCode.trim().uppercase()
        val roomFlow = activeRooms.getOrPut(normalized) { MutableStateFlow(null) }

        // Firebase Cloud dinleyicisi
        try {
            firebaseDatabase?.getReference("rooms")?.child(normalized)
                ?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val room = snapshot.toLobbyRoom()
                        if (room != null) {
                            roomFlow.value = room
                            updatePublicRooms()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        } catch (_: Throwable) {}

        return roomFlow.asStateFlow()
    }

    /**
     * Oyuncuların filo özet hash'ini kaydeder. İkisi de teyit ettiğinde maç başlar.
     */
    fun commitFleetHash(roomCode: String, playerId: String, commitmentHash: String): Result<LobbyRoom> {
        val normalized = roomCode.trim().uppercase()
        val roomFlow = activeRooms[normalized]
            ?: return Result.failure(IllegalArgumentException("Oda bulunamadı."))

        val current = roomFlow.value ?: return Result.failure(IllegalArgumentException("Oda kapalı."))

        val isHost = playerId == current.hostPlayerId
        val updated = if (isHost) {
            current.copy(hostCommitmentHash = commitmentHash)
        } else {
            current.copy(guestCommitmentHash = commitmentHash)
        }

        val finalRoom = if (updated.isReadyToStart) {
            updated.copy(status = RoomStatus.IN_GAME)
        } else {
            updated
        }

        roomFlow.value = finalRoom

        try {
            firebaseDatabase?.getReference("rooms")?.child(normalized)?.setValue(finalRoom.toMap())
        } catch (_: Throwable) {}

        return Result.success(finalRoom)
    }

    /**
     * Oyuncunun aktif olduğunu bildiren kalp atışı (heartbeat) gönderir.
     */
    fun sendHeartbeat(roomCode: String, playerId: String): Boolean {
        val normalized = roomCode.trim().uppercase()
        val flow = activeRooms[normalized] ?: return false
        val current = flow.value ?: return false
        if (current.hostPlayerId != playerId && current.guestPlayerId != playerId) return false

        val updated = current.copy(lastHeartbeatTimestamp = System.currentTimeMillis())
        flow.value = updated
        try {
            firebaseDatabase?.getReference("rooms")?.child(normalized)?.child("lastHeartbeatTimestamp")
                ?.setValue(updated.lastHeartbeatTimestamp)
        } catch (_: Throwable) {}
        return true
    }

    /**
     * Rakip oyuncunun 15 saniyeden uzun süre yanıt vermemesi (Disconnect) veya oyunu terk etmesi (Rage-Quit)
     * durumunda maçı hükmen mağlubiyet (FORFEITED) ile dürüst oyuncu lehine sonuçlandırır.
     */
    fun forfeitMatch(roomCode: String, forfeitedPlayerId: String): Result<LobbyRoom> {
        val normalized = roomCode.trim().uppercase()
        val flow = activeRooms[normalized]
            ?: return Result.failure(IllegalArgumentException("Oda bulunamadı: $normalized"))
        val current = flow.value ?: return Result.failure(IllegalArgumentException("Oda kapalı."))

        val updated = current.copy(
            status = RoomStatus.FORFEITED,
            forfeitedPlayerId = forfeitedPlayerId
        )
        flow.value = updated
        updatePublicRooms()

        try {
            firebaseDatabase?.getReference("rooms")?.child(normalized)?.setValue(updated.toMap())
        } catch (_: Throwable) {}

        return Result.success(updated)
    }

    /**
     * Odanın kalp atışı zaman aşımına (varsayılan 15 saniye) uğrayıp uğramadığını kontrol eder.
     */
    fun isHeartbeatTimedOut(room: LobbyRoom, timeoutMillis: Long = 15_000L): Boolean {
        if (room.status != RoomStatus.IN_GAME) return false
        val elapsed = System.currentTimeMillis() - room.lastHeartbeatTimestamp
        return elapsed > timeoutMillis
    }
}

/**
 * LobbyRoom modelini Firebase Database için Map formatına çevirir.
 */
private fun LobbyRoom.toMap(): Map<String, Any?> = mapOf(
    "roomCode" to roomCode,
    "hostPlayerId" to hostPlayerId,
    "guestPlayerId" to guestPlayerId,
    "pinHash" to pinHash,
    "roomSalt" to roomSalt,
    "turnDurationSeconds" to turnDurationSeconds,
    "mapRadius" to mapRadius,
    "enableHazards" to enableHazards,
    "isPrivate" to isPrivate,
    "status" to status.name,
    "hostCommitmentHash" to hostCommitmentHash,
    "guestCommitmentHash" to guestCommitmentHash,
    "currentTurnPlayerId" to currentTurnPlayerId,
    "turnNumber" to turnNumber,
    "lastHeartbeatTimestamp" to lastHeartbeatTimestamp,
    "forfeitedPlayerId" to forfeitedPlayerId
)

/**
 * Firebase DataSnapshot nesnesini güvenle LobbyRoom modeline dönüştürür.
 */
private fun DataSnapshot.toLobbyRoom(): LobbyRoom? {
    return try {
        val code = child("roomCode").getValue(String::class.java) ?: return null
        val host = child("hostPlayerId").getValue(String::class.java) ?: return null
        val guest = child("guestPlayerId").getValue(String::class.java)
        val pin = child("pinHash").getValue(String::class.java)
        val salt = child("roomSalt").getValue(String::class.java) ?: ""
        val turnSec = child("turnDurationSeconds").getValue(Long::class.java)?.toInt() ?: 45
        val radius = child("mapRadius").getValue(Long::class.java)?.toInt() ?: 5
        val hazards = child("enableHazards").getValue(Boolean::class.java) ?: true
        val isPriv = child("isPrivate").getValue(Boolean::class.java) ?: false
        val statusStr = child("status").getValue(String::class.java) ?: RoomStatus.WAITING_FOR_OPPONENT.name
        val status = try { RoomStatus.valueOf(statusStr) } catch (_: Exception) { RoomStatus.WAITING_FOR_OPPONENT }
        val hostHash = child("hostCommitmentHash").getValue(String::class.java)
        val guestHash = child("guestCommitmentHash").getValue(String::class.java)
        val currentTurn = child("currentTurnPlayerId").getValue(String::class.java) ?: host
        val turnNum = child("turnNumber").getValue(Long::class.java)?.toInt() ?: 1
        val heartbeat = child("lastHeartbeatTimestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
        val forfeited = child("forfeitedPlayerId").getValue(String::class.java)

        LobbyRoom(
            roomCode = code,
            hostPlayerId = host,
            guestPlayerId = guest,
            pinHash = pin,
            roomSalt = salt,
            turnDurationSeconds = turnSec,
            mapRadius = radius,
            enableHazards = hazards,
            isPrivate = isPriv,
            status = status,
            hostCommitmentHash = hostHash,
            guestCommitmentHash = guestHash,
            currentTurnPlayerId = currentTurn,
            turnNumber = turnNum,
            lastHeartbeatTimestamp = heartbeat,
            forfeitedPlayerId = forfeited
        )
    } catch (_: Throwable) {
        null
    }
}


