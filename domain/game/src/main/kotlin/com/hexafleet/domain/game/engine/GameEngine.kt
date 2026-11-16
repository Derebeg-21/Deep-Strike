package com.hexafleet.domain.game.engine

import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.core.hexmath.HexLineOfSight
import com.hexafleet.domain.game.model.BoardTile
import com.hexafleet.domain.game.model.GameActionResult
import com.hexafleet.domain.game.model.PlayerId
import com.hexafleet.domain.game.model.Ship
import com.hexafleet.domain.game.model.ShipClass
import com.hexafleet.domain.game.model.ShipSegment
import com.hexafleet.domain.game.model.Terrain
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * Oyunun tüm tahta, filo, sıra ve sis durumunu tutan değişmez (immutable) veri modeli.
 */
@Serializable
data class GameState(
    val radius: Int = 6,
    val tiles: Map<HexCoordinate, BoardTile> = emptyMap(),
    val ships: Map<PlayerId, List<Ship>> = emptyMap(),
    val activePlayer: PlayerId = PlayerId.BLUE,
    val turnNumber: Int = 1,
    val commandPoints: Map<PlayerId, Int> = mapOf(PlayerId.BLUE to 3, PlayerId.RED to 3),
    val maxCommandPoints: Int = 4,
    val winner: PlayerId? = null,
    val revealedFog: Map<PlayerId, Set<HexCoordinate>> = mapOf(PlayerId.BLUE to emptySet(), PlayerId.RED to emptySet()),
    val battleLog: List<String> = emptyList()
) {
    fun getPlayerShips(playerId: PlayerId): List<Ship> = ships[playerId] ?: emptyList()
    fun getOpponent(playerId: PlayerId): PlayerId = if (playerId == PlayerId.BLUE) PlayerId.RED else PlayerId.BLUE
    fun getCp(playerId: PlayerId): Int = commandPoints[playerId] ?: 0
}

/**
 * HexaFleet Sıra Tabanlı Oyun Kural ve Mantık Motoru.
 */
object GameEngine {

    /**
     * Standart Ana Batarya Atışı (Maliyet: 1 CP)
     */
    fun fire(state: GameState, target: HexCoordinate): Pair<GameState, GameActionResult.Fire> {
        val player = state.activePlayer
        val currentCp = state.getCp(player)
        if (currentCp < 1) {
            return Pair(state, GameActionResult.Fire.Miss)
        }

        val opponent = state.getOpponent(player)
        val opponentShips = state.getPlayerShips(opponent)

        // Hedeflenen petekte düşman gemisi var mı?
        val targetShip = opponentShips.firstOrNull { it.containsCoordinate(target) && !it.isSunk }

        val newCp = state.commandPoints.toMutableMap()
        newCp[player] = currentCp - 1

        val newRevealed = state.revealedFog.toMutableMap()
        newRevealed[player] = (state.revealedFog[player] ?: emptySet()) + target

        val updatedTiles = state.tiles.toMutableMap()
        val currentTile = updatedTiles[target] ?: BoardTile(coordinate = target)
        updatedTiles[target] = currentTile.copy(isAttacked = true)

        if (targetShip != null) {
            // Durum 1: Gemi Kalkanlı ise kalkan darbeyi emer!
            if (targetShip.isShielded) {
                val updatedShip = targetShip.copy(isShielded = false)
                val newOpponentShips = opponentShips.map { if (it.id == targetShip.id) updatedShip else it }
                val newShips = state.ships.toMutableMap()
                newShips[opponent] = newOpponentShips

                val log = "KALKAN SÖNÜMLEDİ: ${targetShip.shipClass.displayName} kalkanı darbeyi absorbe etti!"
                val nextState = state.copy(
                    commandPoints = newCp,
                    ships = newShips,
                    tiles = updatedTiles,
                    revealedFog = newRevealed,
                    battleLog = listOf(log) + state.battleLog
                )
                return Pair(nextState, GameActionResult.Fire.ShieldAbsorbed(targetShip.id))
            }

            // Durum 2: Doğrudan Gövde İsabeti
            val damagedShip = targetShip.damageSegmentAt(target)
            val isSunk = damagedShip.isSunk
            val newOpponentShips = opponentShips.map { if (it.id == targetShip.id) damagedShip else it }
            val newShips = state.ships.toMutableMap()
            newShips[opponent] = newOpponentShips

            // Tüm düşman filosu battı mı? Zafer kontrolü!
            val hasWon = newOpponentShips.all { it.isSunk }

            val log = if (isSunk) {
                "DÜŞMAN GEMİSİ BATTI! ${damagedShip.shipClass.displayName} imha edildi!"
            } else {
                "İSABET: ${damagedShip.shipClass.displayName} hasar aldı! (${target.q}, ${target.r})"
            }

            val nextState = state.copy(
                commandPoints = newCp,
                ships = newShips,
                tiles = updatedTiles,
                revealedFog = newRevealed,
                winner = if (hasWon) player else null,
                battleLog = listOf(log) + state.battleLog
            )
            return Pair(nextState, GameActionResult.Fire.Hit(targetShip.id, isSunk))
        }

        // Mayın kontrolü
        if (currentTile.hasMine) {
            updatedTiles[target] = currentTile.copy(hasMine = false, isAttacked = true)
            val log = "MAYIN PATLAMASI! (${target.q}, ${target.r}) peteğindeki mayın infilak etti."
            val nextState = state.copy(
                commandPoints = newCp,
                tiles = updatedTiles,
                revealedFog = newRevealed,
                battleLog = listOf(log) + state.battleLog
            )
            return Pair(nextState, GameActionResult.Fire.MineExploded(target))
        }

        // Karavana
        val log = "KARAVANA: Mermi suya düştü. (${target.q}, ${target.r})"
        val nextState = state.copy(
            commandPoints = newCp,
            tiles = updatedTiles,
            revealedFog = newRevealed,
            battleLog = listOf(log) + state.battleLog
        )
        return Pair(nextState, GameActionResult.Fire.Miss)
    }

    /**
     * Taktiksel Manevra: Gemiyi 1 petek doğrusal kaydırır (Maliyet: 1 CP)
     */
    fun moveShip(state: GameState, shipId: String, directionIndex: Int): Pair<GameState, GameActionResult.Move> {
        val player = state.activePlayer
        val currentCp = state.getCp(player)
        if (currentCp < 1) {
            return Pair(state, GameActionResult.Move.Failed("Yetersiz Komut Puanı (1 CP gerekli)."))
        }

        val ship = state.getPlayerShips(player).firstOrNull { it.id == shipId }
            ?: return Pair(state, GameActionResult.Move.Failed("Gemi bulunamadı."))

        if (ship.isSunk) {
            return Pair(state, GameActionResult.Move.Failed("Batmış gemi hareket edemez."))
        }

        // Yeni koordinatları hesapla
        val dir = HexCoordinate.DIRECTIONS[Math.floorMod(directionIndex, 6)]
        val newSegments = ship.segments.map { it.copy(coordinate = it.coordinate + dir) }
        val newCoords = newSegments.map { it.coordinate }

        // Sınır ve Engel Kontrolleri
        for (coord in newCoords) {
            if (coord.distanceTo(HexCoordinate.ZERO) > state.radius) {
                return Pair(state, GameActionResult.Move.Failed("Harita sınırları dışına çıkılamaz."))
            }
            val tileTerrain = state.tiles[coord]?.terrain ?: Terrain.Water
            if (tileTerrain is Terrain.CoralReef) {
                return Pair(state, GameActionResult.Move.Failed("Mercan resiflerine girilemez."))
            }
            if (tileTerrain is Terrain.Trench && !ship.shipClass.canSubmerge) {
                return Pair(state, GameActionResult.Move.Failed("Derin hendeğe yalnızca denizaltılar girebilir."))
            }
        }

        // Başka gemilerle çarpışma kontrolü
        val allOtherShips = state.ships.values.flatten().filter { it.id != shipId && !it.isSunk }
        for (coord in newCoords) {
            if (allOtherShips.any { it.containsCoordinate(coord) }) {
                return Pair(state, GameActionResult.Move.Failed("Başka bir gemiyle çarpışma rotası!"))
            }
        }

        val updatedShip = ship.copy(segments = newSegments)
        val newPlayerShips = state.getPlayerShips(player).map { if (it.id == shipId) updatedShip else it }
        val newShips = state.ships.toMutableMap()
        newShips[player] = newPlayerShips

        val newCp = state.commandPoints.toMutableMap()
        newCp[player] = currentCp - 1

        val log = "MANEVRA: ${ship.shipClass.displayName} 1 petek kaydırıldı."
        val nextState = state.copy(
            commandPoints = newCp,
            ships = newShips,
            battleLog = listOf(log) + state.battleLog
        )
        return Pair(nextState, GameActionResult.Move.Success(shipId, newCoords))
    }

    /**
     * Gemi Rotasyonu: Belirtilen parça etrafında 60° pivot dönüş (Maliyet: 1 CP)
     */
    fun rotateShip(
        state: GameState,
        shipId: String,
        pivotIndex: Int,
        steps60: Int
    ): Pair<GameState, GameActionResult.Rotate> {
        val player = state.activePlayer
        val currentCp = state.getCp(player)
        if (currentCp < 1) {
            return Pair(state, GameActionResult.Rotate.Failed("Yetersiz Komut Puanı (1 CP gerekli)."))
        }

        val ship = state.getPlayerShips(player).firstOrNull { it.id == shipId }
            ?: return Pair(state, GameActionResult.Rotate.Failed("Gemi bulunamadı."))

        if (ship.isSunk) {
            return Pair(state, GameActionResult.Rotate.Failed("Batmış gemi dönemez."))
        }

        val pivotCoord = ship.segments.getOrNull(pivotIndex)?.coordinate
            ?: return Pair(state, GameActionResult.Rotate.Failed("Geçersiz pivot parçası."))

        val newSegments = ship.segments.map { segment ->
            segment.copy(coordinate = segment.coordinate.rotate60(center = pivotCoord, steps = steps60))
        }
        val newCoords = newSegments.map { it.coordinate }

        // Sınır & Engel denetimi
        for (coord in newCoords) {
            if (coord.distanceTo(HexCoordinate.ZERO) > state.radius) {
                return Pair(state, GameActionResult.Rotate.Failed("Dönüş harita dışına taşıyor."))
            }
            if (state.tiles[coord]?.terrain is Terrain.CoralReef) {
                return Pair(state, GameActionResult.Rotate.Failed("Resif üzerine dönülemez."))
            }
        }

        // Çarpışma denetimi
        val allOtherShips = state.ships.values.flatten().filter { it.id != shipId && !it.isSunk }
        for (coord in newCoords) {
            if (allOtherShips.any { it.containsCoordinate(coord) }) {
                return Pair(state, GameActionResult.Rotate.Failed("Dönüş rotasında başka bir gemi var."))
            }
        }

        val newOrientation = Math.floorMod(ship.orientation + steps60, 6)
        val updatedShip = ship.copy(segments = newSegments, orientation = newOrientation)
        val newPlayerShips = state.getPlayerShips(player).map { if (it.id == shipId) updatedShip else it }
        val newShips = state.ships.toMutableMap()
        newShips[player] = newPlayerShips

        val newCp = state.commandPoints.toMutableMap()
        newCp[player] = currentCp - 1

        val log = "ROTASYON: ${ship.shipClass.displayName} 60° yön değiştirdi."
        val nextState = state.copy(
            commandPoints = newCp,
            ships = newShips,
            battleLog = listOf(log) + state.battleLog
        )
        return Pair(nextState, GameActionResult.Rotate.Success(shipId, newCoords))
    }

    /**
     * Aktif Gemi Yeteneği Kullanımı
     */
    fun useAbility(
        state: GameState,
        shipId: String,
        target: HexCoordinate
    ): Pair<GameState, GameActionResult.Ability> {
        val player = state.activePlayer
        val ship = state.getPlayerShips(player).firstOrNull { it.id == shipId }
            ?: return Pair(state, GameActionResult.Ability.Failed("Gemi bulunamadı."))

        if (!ship.hasAbilityReady()) {
            return Pair(state, GameActionResult.Ability.Failed("Yetenek hazır değil (Bekleme süresi veya EMP kilidi)."))
        }

        val cpCost = ship.shipClass.abilityCpCost
        val currentCp = state.getCp(player)
        if (currentCp < cpCost) {
            return Pair(state, GameActionResult.Ability.Failed("Yetersiz Komut Puanı ($cpCost CP gerekli)."))
        }

        val newCp = state.commandPoints.toMutableMap()
        newCp[player] = currentCp - cpCost

        val updatedShip = ship.copy(abilityCooldown = ship.shipClass.cooldownRounds)
        val newPlayerShips = state.getPlayerShips(player).map { if (it.id == shipId) updatedShip else it }.toMutableList()
        val newShips = state.ships.toMutableMap()
        newShips[player] = newPlayerShips

        return when (ship.shipClass) {
            ShipClass.DRONE_CARRIER -> {
                // 7 Peteklik İHA Taraması (Merkez + 1. Halka)
                val scoutedArea = (target.ring(1) + target).filter { coord ->
                    coord.distanceTo(HexCoordinate.ZERO) <= state.radius
                }.toSet()

                // Manyetik fırtınalar İHA taramasını engeller
                val clearScouted = scoutedArea.filter { coord ->
                    state.tiles[coord]?.terrain !is Terrain.MagneticStorm
                }.toSet()

                val newRevealed = state.revealedFog.toMutableMap()
                newRevealed[player] = (state.revealedFog[player] ?: emptySet()) + clearScouted

                val log = "İHA KEŞFİ: (${target.q}, ${target.r}) merkezli alan tarandı."
                val nextState = state.copy(
                    commandPoints = newCp,
                    ships = newShips,
                    revealedFog = newRevealed,
                    battleLog = listOf(log) + state.battleLog
                )
                Pair(nextState, GameActionResult.Ability.DroneScouted(target, clearScouted))
            }

            ShipClass.EMP_CRUISER -> {
                // EMP Şoku: Hedef ve komşularındaki düşman gemilerinin yetenek ve kalkanlarını 2 tur kilitler
                val empArea = (target.ring(1) + target).toSet()
                val opponent = state.getOpponent(player)
                val opponentShips = state.getPlayerShips(opponent)

                val affectedIds = mutableListOf<String>()
                val updatedOpponentShips = opponentShips.map { enemyShip ->
                    if (enemyShip.segments.any { it.coordinate in empArea }) {
                        affectedIds.add(enemyShip.id)
                        enemyShip.copy(isShielded = false, empLockTurnsRemaining = 2)
                    } else {
                        enemyShip
                    }
                }
                newShips[opponent] = updatedOpponentShips

                val log = "EMP ŞOKU: ${affectedIds.size} düşman gemisinin sistemleri kilitlendi!"
                val nextState = state.copy(
                    commandPoints = newCp,
                    ships = newShips,
                    battleLog = listOf(log) + state.battleLog
                )
                Pair(nextState, GameActionResult.Ability.EmpApplied(affectedIds))
            }

            ShipClass.AEGIS_DESTROYER -> {
                // Nano-Kalkan: Hedef dost gemiye 1 turluk koruma kalkanı örter
                val targetFriendly = newPlayerShips.firstOrNull { it.containsCoordinate(target) }
                if (targetFriendly == null) {
                    return Pair(state, GameActionResult.Ability.Failed("Kalkan yalnızca dost gemilere uygulanabilir."))
                }

                val shielded = targetFriendly.copy(isShielded = true)
                val finalFriendlyList = newPlayerShips.map { if (it.id == targetFriendly.id) shielded else it }
                newShips[player] = finalFriendlyList

                val log = "AEGIS KALKANI: ${targetFriendly.shipClass.displayName} kalkanla korundu!"
                val nextState = state.copy(
                    commandPoints = newCp,
                    ships = newShips,
                    battleLog = listOf(log) + state.battleLog
                )
                Pair(nextState, GameActionResult.Ability.ShieldActivated(targetFriendly.id))
            }

            ShipClass.STEALTH_SUBMARINE -> {
                // Vektörel Çizgisel Torpido
                val head = ship.segments.first().coordinate
                val raycast = HexLineOfSight.raycast(
                    start = head,
                    directionIndex = ship.orientation,
                    maxDistance = state.radius * 2,
                    isObstacle = { coord ->
                        state.tiles[coord]?.terrain is Terrain.CoralReef ||
                                state.ships.values.flatten().any { it.containsCoordinate(coord) && it.id != shipId }
                    }
                )

                val hitCoord = raycast.hitCoordinate
                if (hitCoord != null) {
                    val hitShip = state.ships.values.flatten().firstOrNull { it.containsCoordinate(hitCoord) }
                    if (hitShip != null) {
                        val damagedShip = hitShip.damageSegmentAt(hitCoord)
                        val isSunk = damagedShip.isSunk
                        val shipOwner = damagedShip.owner
                        val updatedOwnerShips = state.getPlayerShips(shipOwner).map {
                            if (it.id == damagedShip.id) damagedShip else it
                        }
                        newShips[shipOwner] = updatedOwnerShips

                        val log = "TORPİDO VURDU! ${hitShip.shipClass.displayName} torpidoyla vuruldu!"
                        val nextState = state.copy(
                            commandPoints = newCp,
                            ships = newShips,
                            battleLog = listOf(log) + state.battleLog
                        )
                        return Pair(nextState, GameActionResult.Ability.TorpedoImpact(hitCoord, hitShip.id, isSunk))
                    }
                }

                val log = "TORPİDO ISKALADI: Torpido hedefe ulaşamadan patladı."
                val nextState = state.copy(
                    commandPoints = newCp,
                    ships = newShips,
                    battleLog = listOf(log) + state.battleLog
                )
                Pair(nextState, GameActionResult.Ability.TorpedoImpact(head, null, false))
            }

            ShipClass.CORVETTE_MINELAYER -> {
                // Akıllı Mayın Bırakma
                val updatedTiles = state.tiles.toMutableMap()
                val currentTile = updatedTiles[target] ?: BoardTile(coordinate = target)
                updatedTiles[target] = currentTile.copy(hasMine = true)

                val log = "MAYIN YERLEŞTİRİLDİ: (${target.q}, ${target.r}) sularına su altı mayını bırakıldı."
                val nextState = state.copy(
                    commandPoints = newCp,
                    ships = newShips,
                    tiles = updatedTiles,
                    battleLog = listOf(log) + state.battleLog
                )
                Pair(nextState, GameActionResult.Ability.TrapDeployed(target, isMine = true))
            }

            ShipClass.DREADNOUGHT -> {
                // Dreadnought salvo
                val log = "AĞIR BOMBARDIMAN: Hedef bölge bombalandı."
                val nextState = state.copy(
                    commandPoints = newCp,
                    ships = newShips,
                    battleLog = listOf(log) + state.battleLog
                )
                Pair(nextState, GameActionResult.Ability.DroneScouted(target, emptySet()))
            }
        }
    }

    /**
     * Tur Sonu: Okyanus akıntılarını işletir, bekleme sürelerini düşürür ve CP yeniler.
     */
    fun endTurn(state: GameState): GameState {
        val nextPlayer = state.getOpponent(state.activePlayer)
        val isNewRound = nextPlayer == PlayerId.BLUE

        // 1. Akıntı Sürüklenmesi (Termal Akıntılardaki gemileri 0 CP ücretsiz 1 petek kaydırır)
        val updatedShips = state.ships.mapValues { (_, shipList) ->
            shipList.map { ship ->
                var currentShip = ship
                val currentTile = state.tiles[ship.segments.first().coordinate]
                if (currentTile?.terrain is Terrain.Current && !ship.isSunk) {
                    val driftDir = (currentTile.terrain as Terrain.Current).directionIndex
                    val dir = HexCoordinate.DIRECTIONS[Math.floorMod(driftDir, 6)]
                    val driftedSegments = ship.segments.map { it.copy(coordinate = it.coordinate + dir) }
                    // Taşma yoksa sürükle
                    if (driftedSegments.all { it.coordinate.distanceTo(HexCoordinate.ZERO) <= state.radius }) {
                        currentShip = ship.copy(segments = driftedSegments)
                    }
                }
                currentShip
            }
        }

        // 2. Cooldown ve EMP Kilidi Azaltımı
        val cooledShips = updatedShips.mapValues { (owner, shipList) ->
            if (owner == nextPlayer) {
                shipList.map { ship ->
                    ship.copy(
                        abilityCooldown = max(0, ship.abilityCooldown - 1),
                        empLockTurnsRemaining = max(0, ship.empLockTurnsRemaining - 1)
                    )
                }
            } else {
                shipList
            }
        }

        // 3. Komut Puanı (CP) Yenilenmesi: +3 CP (Maksimum 4)
        val newCp = state.commandPoints.toMutableMap()
        val currentNextPlayerCp = state.getCp(nextPlayer)
        newCp[nextPlayer] = min(state.maxCommandPoints, currentNextPlayerCp + 3)

        val nextTurnNumber = if (isNewRound) state.turnNumber + 1 else state.turnNumber
        val log = "SIRA DEVRİ: Tur $nextTurnNumber başladı. Aktif Filo: $nextPlayer."

        return state.copy(
            activePlayer = nextPlayer,
            turnNumber = nextTurnNumber,
            ships = cooledShips,
            commandPoints = newCp,
            battleLog = listOf(log) + state.battleLog
        )
    }
}

