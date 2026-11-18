package com.hexafleet.feature.board.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.hexafleet.feature.board.R
import com.hexafleet.feature.board.visuals.ShipVisuals
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.domain.game.ai.AiDifficulty
import com.hexafleet.feature.board.audio.HexaFleetSoundManager
import com.hexafleet.feature.board.canvas.HexBoardCanvas
import com.hexafleet.feature.board.haptics.HexaFleetHapticFeedback
import com.hexafleet.feature.board.model.BattlePhase
import com.hexafleet.feature.board.model.CombatLogEntry
import com.hexafleet.feature.board.model.DualBattleUiState
import com.hexafleet.feature.board.model.GameCustomRules
import com.hexafleet.feature.board.model.RadialActionType
import com.hexafleet.feature.board.model.RadioactiveZone
import com.hexafleet.feature.board.model.ShieldDome
import com.hexafleet.feature.board.model.ShipDisplayModel
import com.hexafleet.feature.board.model.SpecialActionMode
import com.hexafleet.feature.board.model.TacticalGameMode
import com.hexafleet.feature.board.model.TerrainType
import com.hexafleet.feature.board.model.TileHighlightState
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import com.hexafleet.feature.board.theme.BoardColors
import com.hexafleet.feature.board.ui.dialogs.DeployInfoIconButton
import com.hexafleet.feature.board.ui.dialogs.DeployableObjectInfo
import com.hexafleet.feature.board.ui.dialogs.DeployableObjectInfoDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetExitDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetInfoDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetSettingsDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetTopActions
import com.hexafleet.feature.board.ui.dialogs.getDeployableObjectAccent
import com.hexafleet.feature.board.ui.dialogs.getDeployableObjectInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * İki Haritalı (Dost Filo Haritası + Düşman Hedef Radarı) Profesyonel Altıgen Amiral Battı Ekranı.
 * Ekran boyutuna göre otomatik olarak Sağ-Sol (Geniş/Yatay) veya Alt-Üst (Dikey/Mobil) yerleşime geçer.
 * Önce Gemi Yerleştirme (Konuşlandırma) evresi, ardından Sıra Tabanlı Savaş evresi sunar.
 */
@Composable
fun TacticalBoardScreen(
    gameMode: TacticalGameMode = TacticalGameMode.VsAi(AiDifficulty.VETERAN),
    mapRadius: Int = 5,
    enableHazards: Boolean = true,
    customRules: GameCustomRules = GameCustomRules.defaultForRadius(mapRadius),
    onBackToLobby: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = remember(context) { HexaFleetHapticFeedback(context) }
    val soundManager = remember(context) { HexaFleetSoundManager(context) }
    val scope = rememberCoroutineScope()

    // Üst Sağ Dialog Durumları
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCombatLogDialog by remember { mutableStateOf(false) }
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    var selectedDeployableObjectInfo by remember { mutableStateOf<DeployableObjectInfo?>(null) }

    // Android Donanım/Sistem Geri Tuşu Yakalama (User Requirement 6)
    BackHandler(enabled = true) {
        showExitConfirmationDialog = true
    }

    DisposableEffect(Unit) {
        onDispose {
            HexaFleetSoundManager.stopMusic()
            soundManager.release()
        }
    }

    // İki Haritalı Oyun Durumu
    var battleState by remember(mapRadius, enableHazards, customRules) {
        mutableStateOf(createInitialDualBattleState(mapRadius, enableHazards, customRules))
    }

    var isAiTurnInProgress by remember { mutableStateOf(false) }
    val aiKnownTargets = remember { mutableListOf<HexCoordinate>() }

    // Tur Süresi ve Geri Sayım Parametreleri (Çevrimiçi için min 15sn, Yapay Zekaya karşı 0 = süresiz)
    val effectiveTurnDuration = when (gameMode) {
        is TacticalGameMode.Online -> gameMode.turnDurationSeconds.coerceAtLeast(15)
        is TacticalGameMode.VsAi -> gameMode.turnDurationSeconds
    }
    val isTurnTimerActive = effectiveTurnDuration > 0

    var remainingTurnSeconds by remember(battleState.turnNumber, battleState.isPlayerTurn, battleState.phase) {
        mutableIntStateOf(if (isTurnTimerActive) effectiveTurnDuration else 0)
    }

    // ==================== YAPAY ZEKA / RAKİP TAARRUZ MOTORU ====================
    fun triggerEnemyTurn() {
        if (battleState.winner != null || isAiTurnInProgress) return

        scope.launch {
            isAiTurnInProgress = true
            battleState = battleState.copy(
                isPlayerTurn = false,
                lastBannerMessage = "⚠️ Düşman saldırı hesaplıyor..."
            )
            delay(700)

            val difficulty = when (gameMode) {
                is TacticalGameMode.VsAi -> gameMode.difficulty
                is TacticalGameMode.Online -> AiDifficulty.VETERAN
            }

            val enemyPenalty = battleState.enemyPendingCpPenalty
            var enemyCp = (battleState.maxCommandPoints - enemyPenalty).coerceAtLeast(1)
            var enemyRemainingEmp = battleState.enemyRemainingEmpStrikes
            var pendingPlayerCpLoss = battleState.playerPendingCpPenalty
            val currentTurnAttacks = mutableListOf<HexCoordinate>()
            var currentTurnDroneCenter: HexCoordinate? = null

            // 0. ADIM: YAPAY ZEKA DELTA EMP ŞOKU (Oyuncunun kalkanı varsa çökertir)
            val isEnemyDeltaAlive = battleState.enemyShips.any { (it.id.contains("delta") || it.name.contains("Delta")) && !it.isSunk }
            if (isEnemyDeltaAlive && enemyRemainingEmp > 0 && battleState.friendlyShieldDomes.isNotEmpty()) {
                val targetDome = battleState.friendlyShieldDomes.first()
                val targetCenter = targetDome.center
                val sector = (targetCenter.ring(1) + targetCenter).toSet()

                val updatedPlayerDomes = battleState.friendlyShieldDomes.filter { it != targetDome }
                enemyRemainingEmp--

                val activeFriendlyParts = battleState.friendlyShips.filter { !it.isSunk }.flatMap { it.parts }.toSet()
                val friendlyHit = sector.any { it in activeFriendlyParts }
                if (friendlyHit) {
                    pendingPlayerCpLoss = 1
                }

                val empMsg = if (friendlyHit) "⚡ DÜŞMAN EMP: Kalkan çöktü, -1 CP!" else "⚡ DÜŞMAN EMP: Kalkan kubbemiz çöktü!"

                val empLog = CombatLogEntry(
                    turnNumber = battleState.turnNumber,
                    actor = "DÜŞMAN",
                    actionType = "⚡ EMP ŞOKU",
                    coordinate = targetCenter,
                    resultTitle = "KALKAN ÇÖKTÜ",
                    detailMessage = empMsg
                )

                battleState = battleState.copy(
                    friendlyShieldDome = updatedPlayerDomes.firstOrNull(),
                    friendlyShieldDomes = updatedPlayerDomes,
                    enemyRemainingEmpStrikes = enemyRemainingEmp,
                    playerPendingCpPenalty = pendingPlayerCpLoss,
                    lastBannerMessage = empMsg,
                    battleLog = listOf(empMsg) + battleState.battleLog,
                    structuredLogs = listOf(empLog) + battleState.structuredLogs
                )
                soundManager.playEmpPulse()
                delay(600)
            }

            // 1. ADIM: YÜKSEK SEVİYE YAPAY ZEKA KALKAN KULLANIMI (VETERAN / GRAND ADMIRAL)
            val maxEnemyShields = battleState.customRules.maxShieldDomes
            val damagedEnemyShip = battleState.enemyShips.firstOrNull { !it.isSunk && it.hitParts.isNotEmpty() }
            val isDamagedShipShielded = battleState.enemyShieldDomes.any { dome ->
                damagedEnemyShip?.parts?.any { it in dome.protectedCoords } == true
            }

            val shouldDeployShield = enemyCp >= 2 &&
                maxEnemyShields > 0 &&
                battleState.enemyShieldDomes.size < maxEnemyShields &&
                damagedEnemyShip != null &&
                !isDamagedShipShielded &&
                (difficulty == AiDifficulty.GRAND_ADMIRAL || (difficulty == AiDifficulty.VETERAN && Random.nextFloat() < 0.65f))

            if (shouldDeployShield) {
                val shieldCenter = damagedEnemyShip!!.hitParts.firstOrNull() ?: damagedEnemyShip.parts.first()
                val protectedArea = (shieldCenter.ring(1) + shieldCenter).filter {
                    it.distanceTo(HexCoordinate.ZERO) <= battleState.radius
                }.toSet()

                val newDome = ShieldDome(center = shieldCenter, centerHp = 2, protectedCoords = protectedArea)
                val updatedEnemyDomes = battleState.enemyShieldDomes + newDome
                enemyCp -= 2

                val shieldMsg = "🛡️ DÜŞMAN AEGIS KALKANI: Düşman filosu savunma kubbesi kurdu!"
                val logEntry = CombatLogEntry(
                    turnNumber = battleState.turnNumber,
                    actor = "DÜŞMAN",
                    actionType = "🛡️ KALKAN",
                    coordinate = HexCoordinate.ZERO, // Düşman kalkan koordinatı gizlidir, logda asla gösterilmez
                    resultTitle = "KALKAN KURULDU",
                    detailMessage = "Düşman amiralliği filosunun üzerine 7 peteklik Aegis Savunma Kubbesi konuşlandırdı."
                )

                battleState = battleState.copy(
                    enemyShieldDome = updatedEnemyDomes.firstOrNull(),
                    enemyShieldDomes = updatedEnemyDomes,
                    lastBannerMessage = shieldMsg,
                    battleLog = listOf(shieldMsg) + battleState.battleLog,
                    structuredLogs = listOf(logEntry) + battleState.structuredLogs
                )
                soundManager.playShieldAbsorb()
                delay(500)
            }

            // 2. ADIM: YAPAY ZEKA İHA KEŞFİ (Taşıyıcı hayattaysa, hedef bilinmiyorsa ve CP >= 3 ise)
            val isEnemyCarrierAlive = battleState.enemyShips.any { (it.id.contains("carrier") || it.name.contains("İHA")) && !it.isSunk }
            val radMistCoords = battleState.friendlyRadioactiveZones.flatMap { it.parts }.toSet()
            val shouldDeployDrone = isEnemyCarrierAlive && enemyCp >= 3 &&
                aiKnownTargets.isEmpty() &&
                when (difficulty) {
                    AiDifficulty.RECRUIT -> Random.nextFloat() < 0.25f
                    AiDifficulty.VETERAN -> Random.nextFloat() < 0.45f
                    AiDifficulty.GRAND_ADMIRAL -> Random.nextFloat() < 0.65f
                }

            if (shouldDeployDrone) {
                val allCoords = HexCoordinate.ZERO.area(battleState.radius)
                val unscannedCandidates = allCoords.filter {
                    it !in battleState.enemyShotsOnPlayer.keys &&
                        it !in battleState.sharedReefCoords &&
                        it.distanceTo(HexCoordinate.ZERO) <= (battleState.radius - 1)
                }
                val droneCenter = if (unscannedCandidates.isNotEmpty()) unscannedCandidates.random() else HexCoordinate.ZERO
                currentTurnDroneCenter = droneCenter
                enemyCp -= 3

                val scanArea = (droneCenter.ring(1) + droneCenter).filter {
                    it.distanceTo(HexCoordinate.ZERO) <= battleState.radius
                }

                // Düşman İHA'sı oyuncu kalkanına çarparsa:
                var updatedPlayerDomes = battleState.friendlyShieldDomes.toMutableList()
                var shieldHitOccurred = false
                for (dome in battleState.friendlyShieldDomes) {
                    if (dome.center in scanArea) {
                        updatedPlayerDomes.remove(dome)
                        if (dome.centerHp > 1) {
                            updatedPlayerDomes.add(dome.copy(centerHp = 1))
                        }
                        shieldHitOccurred = true
                    } else if (scanArea.any { it in dome.protectedCoords }) {
                        updatedPlayerDomes.remove(dome)
                        shieldHitOccurred = true
                    }
                }

                // Gemi tespiti (Radyoaktif Yeşil Sis, Manyetik Fırtına ve Korvet %50 Pasif Stealth İHA'yı ENGELLER!)
                val activeFriendlyParts = battleState.friendlyShips.filter { !it.isSunk }.flatMap { it.parts }.toSet()
                var detectedCount = 0
                var playerCorvetteStealthTriggered = false
                for (c in scanArea) {
                    val isPlayerCorvettePart = battleState.friendlyShips.any {
                        (it.id.contains("corvette") || it.name.contains("Korvet")) && !it.isSunk && c in it.parts
                    }
                    // Pasif Özellik: Korvet %50 ihtimalle radara yakalanmaz!
                    val isCorvetteStealth = isPlayerCorvettePart && Random.nextFloat() < 0.5f
                    if (isCorvetteStealth) {
                        playerCorvetteStealthTriggered = true
                    }

                    val isCloaked = c in radMistCoords ||
                        battleState.friendlyTerrainMap[c] == TerrainType.MAGNETIC_STORM ||
                        isCorvetteStealth

                    if (!isCloaked && c in activeFriendlyParts && c !in battleState.enemyShotsOnPlayer.keys) {
                        if (c !in aiKnownTargets) {
                            aiKnownTargets.add(c)
                            detectedCount++
                        }
                    }
                }

                var droneMsg = if (detectedCount > 0) {
                    "📡 DÜŞMAN İHA KEŞFİ: Düşman İHA'sı (${droneCenter.q}, ${droneCenter.r}) bölgesinde filomuza ait $detectedCount sinyal tespit etti!"
                } else {
                    "📡 DÜŞMAN İHA KEŞFİ: Düşman İHA'sı (${droneCenter.q}, ${droneCenter.r}) çevresini taradı, sinyal alamadı."
                }
                if (playerCorvetteStealthTriggered) {
                    droneMsg += " ⚡ Hayalet Korvetimiz (%50 şans) radar sinyalini soğurarak gizlendi!"
                }

                val logEntry = CombatLogEntry(
                    turnNumber = battleState.turnNumber,
                    actor = "DÜŞMAN",
                    actionType = "📡 İHA",
                    coordinate = droneCenter,
                    resultTitle = if (detectedCount > 0) "TESPİT: $detectedCount" else "TEMİZ",
                    detailMessage = droneMsg + if (shieldHitOccurred) " (Dost kalkan dalgadan etkilendi!)" else ""
                )

                val scanSet = scanArea.toSet()
                val updatedScans = battleState.enemyDroneScans + listOf(scanSet)
                battleState = battleState.copy(
                    friendlyShieldDome = updatedPlayerDomes.firstOrNull(),
                    friendlyShieldDomes = updatedPlayerDomes,
                    enemyDroneScans = updatedScans,
                    lastEnemyDroneCenter = droneCenter,
                    lastBannerMessage = droneMsg,
                    battleLog = listOf(droneMsg) + battleState.battleLog,
                    structuredLogs = listOf(logEntry) + battleState.structuredLogs
                )
                soundManager.playRadarPing()
                delay(500)
            }

            // 2.5 ADIM: YAPAY ZEKA AMİRAL AĞIR BOMBARDIMANI (5'Lİ HAT SALVOSU - 3 CP)
            val isEnemyFlagshipAlive = battleState.enemyShips.any { (it.id.contains("flagship") || it.name.contains("Amiral")) && !it.isSunk }
            var enemyRemainingBarrage = battleState.enemyRemainingLineBarrages
            val shouldDeployBarrage = isEnemyFlagshipAlive && enemyRemainingBarrage > 0 && enemyCp >= 3 &&
                (battleState.turnNumber >= 2 || aiKnownTargets.isNotEmpty()) &&
                when (difficulty) {
                    AiDifficulty.RECRUIT -> Random.nextFloat() < 0.25f
                    AiDifficulty.VETERAN -> Random.nextFloat() < 0.55f
                    AiDifficulty.GRAND_ADMIRAL -> Random.nextFloat() < 0.80f
                }

            if (shouldDeployBarrage) {
                val centerCoord = if (aiKnownTargets.isNotEmpty()) {
                    aiKnownTargets.random()
                } else {
                    val all = HexCoordinate.ZERO.area(battleState.radius).filter {
                        it.distanceTo(HexCoordinate.ZERO) <= (battleState.radius - 2)
                    }
                    if (all.isNotEmpty()) all.random() else HexCoordinate.ZERO
                }
                val dirIndex = Random.nextInt(3)
                val dir = HexCoordinate.DIRECTIONS[dirIndex]
                val barrageCoords = listOf(
                    centerCoord - dir * 2,
                    centerCoord - dir,
                    centerCoord,
                    centerCoord + dir,
                    centerCoord + dir * 2
                ).filter { it.distanceTo(HexCoordinate.ZERO) <= battleState.radius }

                enemyRemainingBarrage--
                enemyCp -= 3

                var updatedFriendlyShips = battleState.friendlyShips
                var updatedFriendlyDomes = battleState.friendlyShieldDomes
                val newAbsorbed = battleState.friendlyShieldAbsorbedCoords.toMutableSet()
                val newEnemyShots = battleState.enemyShotsOnPlayer.toMutableMap()
                var hitCount = 0

                soundManager.playCannonFire()
                haptics.playButtonClick()

                for (target in barrageCoords) {
                    currentTurnAttacks.add(target)
                    val hitDome = updatedFriendlyDomes.firstOrNull { target in it.protectedCoords }
                    val hitShip = updatedFriendlyShips.firstOrNull { !it.isSunk && target in it.parts }

                    if (hitDome != null) {
                        newAbsorbed.add(target)
                        updatedFriendlyDomes = updatedFriendlyDomes.filter { it != hitDome }
                    } else if (hitShip != null) {
                        hitCount++
                        val updatedHitParts = hitShip.hitParts + target
                        val willSink = updatedHitParts.size >= hitShip.parts.size
                        updatedFriendlyShips = updatedFriendlyShips.map {
                            if (it.id == hitShip.id) it.copy(hitParts = updatedHitParts) else it
                        }
                        if (willSink) {
                            for (p in hitShip.parts) {
                                newEnemyShots[p] = TileHighlightState.SUNK_SHIP
                                aiKnownTargets.remove(p)
                            }
                        } else {
                            newEnemyShots[target] = TileHighlightState.HIT
                            if (target !in aiKnownTargets) {
                                aiKnownTargets.add(target)
                            }
                        }
                    } else {
                        newEnemyShots[target] = TileHighlightState.MISS
                    }
                }

                val barrageMsg = "💥 DÜŞMAN AMİRAL BOMBARDIMANI: Hat boyunca ${barrageCoords.size} petek bombalandı! ($hitCount İsabet)"
                val barrageLog = CombatLogEntry(
                    turnNumber = battleState.turnNumber,
                    actor = "DÜŞMAN",
                    actionType = "💥 BOMBARDIMAN",
                    coordinate = centerCoord,
                    resultTitle = if (hitCount > 0) "BOMBARDIMAN: $hitCount İSABET" else "BOMBARDIMAN",
                    detailMessage = barrageMsg
                )

                battleState = battleState.copy(
                    friendlyShips = updatedFriendlyShips,
                    friendlyShieldDome = updatedFriendlyDomes.firstOrNull(),
                    friendlyShieldDomes = updatedFriendlyDomes,
                    friendlyShieldAbsorbedCoords = newAbsorbed,
                    enemyShotsOnPlayer = newEnemyShots,
                    enemyRemainingLineBarrages = enemyRemainingBarrage,
                    lastBannerMessage = barrageMsg,
                    battleLog = listOf(barrageMsg) + battleState.battleLog,
                    structuredLogs = listOf(barrageLog) + battleState.structuredLogs
                )
                soundManager.playHitExplosion()
                delay(600)
            }

            // 3. ADIM: TOP ATIŞLARI (Kalan tüm CP harcanana kadar: 1 CP başına 1 atış!)
            while (enemyCp > 0 && battleState.winner == null) {
                val allCoords = HexCoordinate.ZERO.area(battleState.radius)
                val availableTargets = allCoords.filter { coord ->
                    coord !in battleState.enemyShotsOnPlayer.keys &&
                        coord !in battleState.sharedReefCoords &&
                        battleState.friendlyTerrainMap[coord] != TerrainType.CORAL_REEF
                }

                if (availableTargets.isEmpty()) break

                // Hedef Seçimi:
                // 1. İHA ile tespit edilen veya avlanan aktif hedefler
                // 2. Akıllı taktiksel hedefleme
                val target = when {
                    aiKnownTargets.any { it in availableTargets } -> {
                        val t = aiKnownTargets.first { it in availableTargets }
                        aiKnownTargets.remove(t)
                        t
                    }
                    else -> selectAiTargetOnPlayerBoard(battleState, availableTargets, difficulty)
                }

                currentTurnAttacks.add(target)
                enemyCp -= 1

                soundManager.playCannonFire()
                haptics.playButtonClick()

                // Kalkan kontrolü (Çoklu kalkan desteği):
                val hitDome = battleState.friendlyShieldDomes.firstOrNull { target in it.protectedCoords }
                val hitShip = battleState.friendlyShips.firstOrNull { !it.isSunk && target in it.parts }
                val newEnemyShots = battleState.enemyShotsOnPlayer.toMutableMap()
                var updatedFriendlyShips = battleState.friendlyShips
                var updatedFriendlyDomes = battleState.friendlyShieldDomes
                val newAbsorbed = battleState.friendlyShieldAbsorbedCoords.toMutableSet()
                val logMessage: String
                val structuredEntry: CombatLogEntry

                if (hitDome != null) {
                    soundManager.playShieldAbsorb()
                    haptics.playShieldHum()
                    newAbsorbed.add(target)
                    updatedFriendlyDomes = updatedFriendlyDomes.filter { it != hitDome }

                    logMessage = "🛡️ AEGIS KALKANI SAVUNMASI: (${target.q}, ${target.r}) koordinatına gelen düşman atışı sönümlendi! Filomuz hasar almadı."
                    structuredEntry = CombatLogEntry(
                        turnNumber = battleState.turnNumber,
                        actor = "DÜŞMAN",
                        actionType = "🛡️ SÖNÜMLENDİ",
                        coordinate = target,
                        resultTitle = "KALKAN SÖNÜMLEDİ",
                        detailMessage = "Düşman mermisi Aegis Kalkanı tarafından havada sönümlendi. Düşman altta gemi olup olmadığını göremedi."
                    )
                } else if (hitShip != null) {
                    haptics.playHitImpact()
                    soundManager.playHitExplosion()
                    val updatedHitParts = hitShip.hitParts + target
                    val willSink = updatedHitParts.size >= hitShip.parts.size

                    updatedFriendlyShips = battleState.friendlyShips.map {
                        if (it.id == hitShip.id) it.copy(hitParts = updatedHitParts) else it
                    }

                    if (willSink) {
                        haptics.playShipSunkDrama()
                        for (p in hitShip.parts) {
                            newEnemyShots[p] = TileHighlightState.SUNK_SHIP
                            aiKnownTargets.remove(p)
                        }
                        logMessage = "💥 GEMİMİZ BATTI! ${hitShip.name} düşman ateşiyle sulara gömüldü!"
                        structuredEntry = CombatLogEntry(
                            turnNumber = battleState.turnNumber,
                            actor = "DÜŞMAN",
                            actionType = "💥 GEMİ BATIRILDI",
                            coordinate = target,
                            resultTitle = "GEMİ BATTI",
                            detailMessage = logMessage
                        )
                    } else {
                        newEnemyShots[target] = TileHighlightState.HIT
                        // Vurulan geminin diğer parçalarını öncelikli av listesine ekle!
                        val remainingParts = hitShip.parts - updatedHitParts
                        for (rp in remainingParts) {
                            if (rp !in aiKnownTargets) aiKnownTargets.add(rp)
                        }

                        logMessage = "🚨 İSABET ALDIK: (${target.q}, ${target.r}) noktasında ${hitShip.name} vuruldu!"
                        structuredEntry = CombatLogEntry(
                            turnNumber = battleState.turnNumber,
                            actor = "DÜŞMAN",
                            actionType = "🎯 ATEŞ",
                            coordinate = target,
                            resultTitle = "TAM İSABET",
                            detailMessage = logMessage
                        )
                    }
                } else {
                    soundManager.playWaterSplash()
                    newEnemyShots[target] = TileHighlightState.MISS
                    logMessage = "💧 DÜŞMAN ISKALADI: (${target.q}, ${target.r}) koordinatına düşen mermi suya gömüldü."
                    structuredEntry = CombatLogEntry(
                        turnNumber = battleState.turnNumber,
                        actor = "DÜŞMAN",
                        actionType = "🎯 ATEŞ",
                        coordinate = target,
                        resultTitle = "KARAVANA",
                        detailMessage = logMessage
                    )
                }

                val allFriendlySunk = updatedFriendlyShips.all { it.isSunk }

                battleState = battleState.copy(
                    friendlyShips = updatedFriendlyShips,
                    enemyShotsOnPlayer = newEnemyShots,
                    friendlyShieldDome = updatedFriendlyDomes.firstOrNull(),
                    friendlyShieldDomes = updatedFriendlyDomes,
                    friendlyShieldAbsorbedCoords = newAbsorbed,
                    winner = if (allFriendlySunk) "ENEMY" else null,
                    lastBannerMessage = logMessage,
                    battleLog = listOf(logMessage) + battleState.battleLog,
                    structuredLogs = listOf(structuredEntry) + battleState.structuredLogs
                )

                if (enemyCp > 0 && !allFriendlySunk) {
                    delay(550)
                }
            }

            if (battleState.winner == null) {
                delay(400)
                val penalty = battleState.playerPendingCpPenalty
                val startingCp = (battleState.maxCommandPoints - penalty).coerceAtLeast(1)
                val turnMsg = if (penalty > 0) {
                    "⚡ EMP ETKİSİ: Bu tur $startingCp CP kullanılabilir"
                } else {
                    "🎯 Düşman radarına dokunup ateş edin"
                }
                battleState = battleState.copy(
                    turnNumber = battleState.turnNumber + 1,
                    commandPoints = startingCp,
                    playerPendingCpPenalty = 0,
                    isPlayerTurn = true,
                    lastEnemyAttacks = currentTurnAttacks,
                    lastEnemyDroneCenter = currentTurnDroneCenter ?: battleState.lastEnemyDroneCenter,
                    lastBannerMessage = turnMsg
                )
            }

            isAiTurnInProgress = false
        }
    }

    // ==================== TUR SÜRESİ SONU OTOMATİK SALVO FONKSİYONU (Requirement 3) ====================
    fun executeTimeoutEmergencyBarrage() {
        if (battleState.phase != BattlePhase.COMBAT || !battleState.isPlayerTurn || isAiTurnInProgress || battleState.winner != null) return

        val allCoords = HexCoordinate.ZERO.area(battleState.radius)
        val availableTargets = allCoords.filter { coord ->
            coord !in battleState.playerShotsOnEnemy.keys &&
                coord !in battleState.sharedReefCoords &&
                battleState.enemyTerrainMap[coord] != TerrainType.CORAL_REEF
        }.shuffled().take(3)

        if (availableTargets.isEmpty()) {
            triggerEnemyTurn()
            return
        }

        scope.launch {
            var currentShots = battleState.playerShotsOnEnemy.toMutableMap()
            var currentEnemyShips = battleState.enemyShips
            var currentEnemyDomes = battleState.enemyShieldDomes
            var currentAbsorbed = battleState.enemyShieldAbsorbedCoords.toMutableSet()
            val textLogs = mutableListOf<String>()
            val newStructuredLogs = mutableListOf<CombatLogEntry>()

            for (target in availableTargets) {
                soundManager.playCannonFire()
                haptics.playButtonClick()

                val hitDome = currentEnemyDomes.firstOrNull { target in it.protectedCoords }
                if (hitDome != null) {
                    soundManager.playShieldAbsorb()
                    haptics.playShieldHum()
                    currentAbsorbed.add(target)
                    currentEnemyDomes = currentEnemyDomes.filter { it != hitDome }

                    val msg = "⏱️ SÜRE DOLDU SALVOSU: (${target.q}, ${target.r}) mermisi düşman kalkanı tarafından havada sönümlendi!"
                    textLogs.add(msg)
                    newStructuredLogs.add(
                        CombatLogEntry(
                            turnNumber = battleState.turnNumber,
                            actor = "SİZ",
                            actionType = "⏱️ SÜRE DOLDU",
                            coordinate = target,
                            resultTitle = "KALKAN SÖNÜMLEDİ",
                            detailMessage = msg
                        )
                    )
                } else {
                    val hitEnemy = currentEnemyShips.firstOrNull { !it.isSunk && target in it.parts }
                    if (hitEnemy != null) {
                        haptics.playHitImpact()
                        soundManager.playHitExplosion()
                        val newHits = hitEnemy.hitParts + target
                        val isSunk = newHits.size >= hitEnemy.parts.size
                        currentEnemyShips = currentEnemyShips.map {
                            if (it.id == hitEnemy.id) it.copy(hitParts = newHits) else it
                        }
                        if (isSunk) {
                            haptics.playShipSunkDrama()
                            for (p in hitEnemy.parts) {
                                currentShots[p] = TileHighlightState.SUNK_SHIP
                            }
                            val msg = "⏱️ SÜRE DOLDU SALVOSU: Düşman gemisi ${hitEnemy.name} batırıldı!"
                            textLogs.add(msg)
                            newStructuredLogs.add(
                                CombatLogEntry(
                                    turnNumber = battleState.turnNumber,
                                    actor = "SİZ",
                                    actionType = "💥 GEMİ BATTI",
                                    coordinate = target,
                                    resultTitle = "GEMİ BATTI",
                                    detailMessage = msg
                                )
                            )
                        } else {
                            currentShots[target] = TileHighlightState.HIT
                            val msg = "⏱️ SÜRE DOLDU SALVOSU: (${target.q}, ${target.r}) hedefinde düşman gemisi vuruldu!"
                            textLogs.add(msg)
                            newStructuredLogs.add(
                                CombatLogEntry(
                                    turnNumber = battleState.turnNumber,
                                    actor = "SİZ",
                                    actionType = "🎯 İSABET",
                                    coordinate = target,
                                    resultTitle = "TAM İSABET",
                                    detailMessage = msg
                                )
                            )
                        }
                    } else {
                        soundManager.playWaterSplash()
                        currentShots[target] = TileHighlightState.MISS
                        val msg = "⏱️ SÜRE DOLDU SALVOSU: (${target.q}, ${target.r}) karavana."
                        textLogs.add(msg)
                        newStructuredLogs.add(
                            CombatLogEntry(
                                turnNumber = battleState.turnNumber,
                                actor = "SİZ",
                                actionType = "💧 KARAVANA",
                                coordinate = target,
                                resultTitle = "KARAVANA",
                                detailMessage = msg
                            )
                        )
                    }
                }
                delay(280L)
            }

            val allEnemySunk = currentEnemyShips.all { it.isSunk }
            val bannerMsg = "⏱️ SÜRE DOLDU: Acil durum salvosu atıldı"

            battleState = battleState.copy(
                playerShotsOnEnemy = currentShots,
                enemyShips = currentEnemyShips,
                enemyShieldDome = currentEnemyDomes.firstOrNull(),
                enemyShieldDomes = currentEnemyDomes,
                enemyShieldAbsorbedCoords = currentAbsorbed,
                commandPoints = 0,
                winner = if (allEnemySunk) "PLAYER" else null,
                lastBannerMessage = bannerMsg,
                battleLog = textLogs + battleState.battleLog,
                structuredLogs = newStructuredLogs + battleState.structuredLogs
            )

            if (!allEnemySunk) {
                delay(350L)
                triggerEnemyTurn()
            }
        }
    }

    // Tur Süresi Geri Sayım Döngüsü (Yapay zekaya karşı süresizdir)
    LaunchedEffect(battleState.turnNumber, battleState.isPlayerTurn, battleState.phase, isAiTurnInProgress, battleState.winner) {
        if (isTurnTimerActive && battleState.phase == BattlePhase.COMBAT && battleState.isPlayerTurn && !isAiTurnInProgress && battleState.winner == null) {
            remainingTurnSeconds = effectiveTurnDuration
            while (remainingTurnSeconds > 0 && battleState.isPlayerTurn && battleState.winner == null && !isAiTurnInProgress) {
                delay(1000L)
                remainingTurnSeconds -= 1
            }
            if (remainingTurnSeconds <= 0 && battleState.isPlayerTurn && battleState.winner == null && !isAiTurnInProgress) {
                executeTimeoutEmergencyBarrage()
            }
        }
    }

    // ==================== OYUNCU SALDIRI VE YETENEK FONKSİYONLARI ====================
    fun onPlayerFireAt(target: HexCoordinate) {
        if (battleState.phase != BattlePhase.COMBAT || !battleState.isPlayerTurn || isAiTurnInProgress) return
        if (battleState.commandPoints < 1) return
        if (target in battleState.playerShotsOnEnemy.keys) {
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ (${target.q}, ${target.r}) vurulmuş! Başka petek seçin"
            )
            return
        }
        if (target in battleState.sharedReefCoords || battleState.enemyTerrainMap[target] == TerrainType.CORAL_REEF) {
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ Resif vurulamaz! Açık deniz seçin"
            )
            return
        }

        soundManager.playCannonFire()
        haptics.playButtonClick()

        // 1. Düşman Kalkan Kontrolü (Çoklu kalkan desteği):
        val hitDome = battleState.enemyShieldDomes.firstOrNull { target in it.protectedCoords }
        val isEnemyShieldHit = hitDome != null

        val newPlayerShots = battleState.playerShotsOnEnemy.toMutableMap()
        var updatedEnemyShips = battleState.enemyShips
        val newEnemyShieldAbsorbed = battleState.enemyShieldAbsorbedCoords.toMutableSet()
        var updatedEnemyDomes = battleState.enemyShieldDomes
        val logMsg: String
        val structuredEntry: CombatLogEntry

        if (hitDome != null) {
            soundManager.playShieldAbsorb()
            haptics.playShieldHum()
            newEnemyShieldAbsorbed.add(target)
            updatedEnemyDomes = updatedEnemyDomes.filter { it != hitDome } // Kalkan mermiyi absorbe edip çöker

            logMsg = "🛡️ KALKAN: (${target.q}, ${target.r}) sönümlendi"
            structuredEntry = CombatLogEntry(
                turnNumber = battleState.turnNumber,
                actor = "SİZ",
                actionType = "🛡️ SÖNÜMLENDİ",
                coordinate = target,
                resultTitle = "KALKAN SÖNÜMLEDİ",
                detailMessage = "Atışınız düşman kalkan kubbesi tarafından havada yok edildi. Hedefte gemi olup olmadığı bilinmiyor."
            )
        } else {
            val hitEnemy = battleState.enemyShips.firstOrNull { !it.isSunk && target in it.parts }
            if (hitEnemy != null) {
                haptics.playHitImpact()
                soundManager.playHitExplosion()
                val newHits = hitEnemy.hitParts + target
                val isNowSunk = newHits.size >= hitEnemy.parts.size

                updatedEnemyShips = battleState.enemyShips.map {
                    if (it.id == hitEnemy.id) it.copy(hitParts = newHits) else it
                }

                if (isNowSunk) {
                    haptics.playShipSunkDrama()
                    for (p in hitEnemy.parts) {
                        newPlayerShots[p] = TileHighlightState.SUNK_SHIP
                    }
                    logMsg = "🔥 DÜŞMAN BATTI! ${hitEnemy.name} imha edildi"
                    structuredEntry = CombatLogEntry(
                        turnNumber = battleState.turnNumber,
                        actor = "SİZ",
                        actionType = "💥 GEMİ BATIRILDI",
                        coordinate = target,
                        resultTitle = "GEMİ BATTI",
                        detailMessage = "Düşman gemisi ${hitEnemy.name} (${hitEnemy.parts.size} petek) batırıldı!"
                    )
                } else {
                    newPlayerShots[target] = TileHighlightState.HIT
                    logMsg = "🎯 TAM İSABET! (${target.q}, ${target.r}) düşman vuruldu"
                    structuredEntry = CombatLogEntry(
                        turnNumber = battleState.turnNumber,
                        actor = "SİZ",
                        actionType = "🎯 ATEŞ",
                        coordinate = target,
                        resultTitle = "TAM İSABET",
                        detailMessage = "(${target.q}, ${target.r}) koordinatında düşman gemisi isabet aldı!"
                    )
                }
            } else {
                soundManager.playWaterSplash()
                newPlayerShots[target] = TileHighlightState.MISS
                logMsg = "💧 KARAVANA: (${target.q}, ${target.r}) hedef boş deniz"
                structuredEntry = CombatLogEntry(
                    turnNumber = battleState.turnNumber,
                    actor = "SİZ",
                    actionType = "🎯 ATEŞ",
                    coordinate = target,
                    resultTitle = "KARAVANA",
                    detailMessage = "(${target.q}, ${target.r}) koordinatında su sıçradı, hedef boş."
                )
            }
        }

        val allEnemySunk = updatedEnemyShips.all { it.isSunk }
        val remainingCp = (battleState.commandPoints - 1).coerceAtLeast(0)

        battleState = battleState.copy(
            enemyShips = updatedEnemyShips,
            playerShotsOnEnemy = newPlayerShots,
            enemyShieldDome = updatedEnemyDomes.firstOrNull(),
            enemyShieldDomes = updatedEnemyDomes,
            enemyShieldAbsorbedCoords = newEnemyShieldAbsorbed,
            commandPoints = remainingCp,
            winner = if (allEnemySunk) "PLAYER" else null,
            lastBannerMessage = logMsg,
            battleLog = listOf(logMsg) + battleState.battleLog,
            structuredLogs = listOf(structuredEntry) + battleState.structuredLogs
        )

        if (remainingCp == 0 && !allEnemySunk) {
            triggerEnemyTurn()
        }
    }

    fun onPlayerDroneScan(center: HexCoordinate) {
        if (battleState.phase != BattlePhase.COMBAT || !battleState.isPlayerTurn || isAiTurnInProgress) return
        val isCarrierAlive = battleState.friendlyShips.any { (it.id.contains("carrier") || it.name.contains("İHA")) && !it.isSunk }
        if (!isCarrierAlive) {
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ İHA Taşıyıcı battı, keşif kullanılamaz"
            )
            return
        }
        if (battleState.commandPoints < 3) return

        haptics.playSonarTick()
        soundManager.playRadarPing()

        val scanArea = (center.ring(1) + center).filter {
            it.distanceTo(HexCoordinate.ZERO) <= battleState.radius
        }

        // 1. Düşman Kalkan Etkileşimi (Merkeze İHA gelirse 1 canı kalır, komşuya gelirse çöker)
        var updatedEnemyDomes = battleState.enemyShieldDomes.toMutableList()
        var shieldScanMsg: String? = null
        for (dome in battleState.enemyShieldDomes) {
            if (dome.center in scanArea) {
                updatedEnemyDomes.remove(dome)
                if (dome.centerHp > 1) {
                    updatedEnemyDomes.add(dome.copy(centerHp = 1))
                    shieldScanMsg = "🛡️ Düşman kalkan merkezine temas edildi (1 canı kaldı)!"
                } else {
                    shieldScanMsg = "🛡️ Düşman kalkan merkezi aşırı yüklendi ve kalkan çöktü!"
                }
            } else if (scanArea.any { it in dome.protectedCoords }) {
                updatedEnemyDomes.remove(dome)
                shieldScanMsg = "🛡️ Düşman kalkan çeperi deşildi ve kalkan çöktü!"
            }
        }

        // 2. Gemi Tespiti:
        // Manyetik Fırtına VE Radyoaktif Sis alanındaki gemiler İHA ile TESPİT EDİLEMEZ (RECON_CLEAR)!
        // Pasif Özellik: Düşman Hayalet Korveti (%50 şans) radara yakalanmaz!
        val enemyRadCoords = battleState.enemyRadioactiveZones.flatMap { it.parts }.toSet()
        val activeEnemyParts = battleState.enemyShips.filter { !it.isSunk }.flatMap { it.parts }.toSet()
        val newDroneMap = battleState.droneScannedHexes.toMutableMap()
        var detectedCount = 0
        var enemyCorvetteEvaded = false

        for (c in scanArea) {
            val isEnemyStorm = battleState.enemyTerrainMap[c] == TerrainType.MAGNETIC_STORM
            val isEnemyRad = c in enemyRadCoords
            val isEnemyCorvettePart = battleState.enemyShips.any {
                (it.id.contains("corvette") || it.name.contains("Korvet")) && !it.isSunk && c in it.parts
            }
            val isCorvetteStealth = isEnemyCorvettePart && Random.nextFloat() < 0.5f
            if (isCorvetteStealth) {
                enemyCorvetteEvaded = true
            }

            if (c !in battleState.playerShotsOnEnemy.keys) {
                if (!isEnemyStorm && !isEnemyRad && !isCorvetteStealth && c in activeEnemyParts) {
                    newDroneMap[c] = TileHighlightState.RECON_DETECTED
                    detectedCount++
                } else {
                    newDroneMap[c] = TileHighlightState.RECON_CLEAR
                }
            }
        }

        val remainingCp = (battleState.commandPoints - 3).coerceAtLeast(0)
        val bannerMsg = if (detectedCount > 0) {
            "📡 İHA KEŞFİ: (${center.q}, ${center.r}) $detectedCount temas!"
        } else {
            "📡 İHA KEŞFİ: (${center.q}, ${center.r}) bölge temiz"
        }
        var baseMsg = if (detectedCount > 0) {
            "📡 İHA KEŞFİ: (${center.q}, ${center.r}) çevresinde $detectedCount ADET DÜŞMAN BÖLMESİ TESPİT EDİLDİ!"
        } else {
            "📡 İHA KEŞFİ: (${center.q}, ${center.r}) bölgesi temiz, sinyal yok."
        }
        if (enemyCorvetteEvaded) {
            baseMsg += " (Radar paraziti: Hayalet Korvet gizlenmiş olabilir!)"
        }
        val finalMsg = if (shieldScanMsg != null) "$baseMsg $shieldScanMsg" else baseMsg

        val structuredEntry = CombatLogEntry(
            turnNumber = battleState.turnNumber,
            actor = "SİZ",
            actionType = "📡 İHA",
            coordinate = center,
            resultTitle = if (detectedCount > 0) "TESPİT: $detectedCount" else "TEMİZ",
            detailMessage = finalMsg
        )

        battleState = battleState.copy(
            droneScannedHexes = newDroneMap,
            enemyShieldDome = updatedEnemyDomes.firstOrNull(),
            enemyShieldDomes = updatedEnemyDomes,
            commandPoints = remainingCp,
            lastBannerMessage = bannerMsg,
            battleLog = listOf(finalMsg) + battleState.battleLog,
            structuredLogs = listOf(structuredEntry) + battleState.structuredLogs
        )

        if (remainingCp == 0) {
            triggerEnemyTurn()
        }
    }

    fun onPlayerActivateShield() {
        if (battleState.phase != BattlePhase.COMBAT || !battleState.isPlayerTurn || isAiTurnInProgress) return
        if (battleState.commandPoints < 2) return

        val maxShields = battleState.customRules.maxShieldDomes
        if (maxShields <= 0) {
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ Bu maçta kalkan devre dışıdır"
            )
            return
        }

        val center = battleState.selectedShieldCoord
            ?: battleState.friendlyShips.firstOrNull { it.hitParts.isNotEmpty() && !it.isSunk }?.hitParts?.firstOrNull()
            ?: battleState.friendlyShips.firstOrNull { !it.isSunk }?.parts?.firstOrNull()
            ?: HexCoordinate.ZERO
        val protectedArea = (center.ring(1) + center).filter {
            it.distanceTo(HexCoordinate.ZERO) <= battleState.radius
        }.toSet()

        haptics.playShieldHum()
        soundManager.playEmpPulse()

        val newShieldDome = ShieldDome(
            center = center,
            centerHp = 2,
            protectedCoords = protectedArea
        )

        val currentDomes = battleState.friendlyShieldDomes
        val trimmedDomes = if (currentDomes.size >= maxShields) {
            currentDomes.drop(currentDomes.size - maxShields + 1)
        } else {
            currentDomes
        }
        val updatedDomes = trimmedDomes + newShieldDome

        val remainingCp = (battleState.commandPoints - 2).coerceAtLeast(0)
        val bannerMsg = "🛡️ KALKAN AKTİF: (${center.q}, ${center.r}) 7 petek korumada (${updatedDomes.size}/$maxShields)"
        val msg = "🛡️ AEGIS KALKANI AKTİF: (${center.q}, ${center.r}) merkezli 7 peteklik koruma kubbesi konuşlandırıldı! (Aktif: ${updatedDomes.size}/$maxShields)"

        val structuredEntry = CombatLogEntry(
            turnNumber = battleState.turnNumber,
            actor = "SİZ",
            actionType = "🛡️ KALKAN",
            coordinate = center,
            resultTitle = "7 PETEK KORUNDU",
            detailMessage = msg
        )

        battleState = battleState.copy(
            friendlyShieldDome = updatedDomes.firstOrNull(),
            friendlyShieldDomes = updatedDomes,
            friendlyShieldAbsorbedCoords = emptySet(),
            commandPoints = remainingCp,
            selectedShieldCoord = null,
            lastBannerMessage = bannerMsg,
            battleLog = listOf(msg) + battleState.battleLog,
            structuredLogs = listOf(structuredEntry) + battleState.structuredLogs
        )

        if (remainingCp == 0) {
            triggerEnemyTurn()
        }
    }

    // ==================== GEMİYE ÖZEL YENİLİKÇİ OPERASYONLAR (DELTA EMP & AMİRAL BOMBARDIMANI) ====================
    fun onToggleEmp() {
        if (battleState.phase != BattlePhase.COMBAT || !battleState.isPlayerTurn || isAiTurnInProgress) return
        val isDeltaAlive = battleState.friendlyShips.any { (it.id.contains("delta") || it.name.contains("Delta")) && !it.isSunk }
        if (!isDeltaAlive) {
            battleState = battleState.copy(
                activeSpecialMode = SpecialActionMode.NONE,
                lastBannerMessage = "⚠️ Delta Taarruz Kruvazörü batırıldı! EMP Şoku kullanılamaz."
            )
            return
        }
        if (battleState.playerRemainingEmpStrikes <= 0) {
            battleState = battleState.copy(
                activeSpecialMode = SpecialActionMode.NONE,
                lastBannerMessage = "⚠️ Bu maçta kullanabileceğiniz EMP Şoku hakkınız kalmadı!"
            )
            return
        }

        val nextMode = if (battleState.activeSpecialMode == SpecialActionMode.EMP_STRIKE) SpecialActionMode.NONE else SpecialActionMode.EMP_STRIKE
        haptics.playButtonClick()
        battleState = battleState.copy(
            activeSpecialMode = nextMode,
            lastBannerMessage = if (nextMode == SpecialActionMode.EMP_STRIKE) {
                "⚡ DELTA EMP İYON ŞOKU (0 CP): Düşman radarına dokunarak 7 peteklik EMP şoku gönderin!"
            } else {
                "Standart taarruz moduna dönüldü."
            }
        )
    }

    fun onToggleBarrage() {
        if (battleState.phase != BattlePhase.COMBAT || !battleState.isPlayerTurn || isAiTurnInProgress) return
        val isFlagshipAlive = battleState.friendlyShips.any { (it.id.contains("flagship") || it.name.contains("Amiral")) && !it.isSunk }
        if (!isFlagshipAlive) {
            battleState = battleState.copy(
                activeSpecialMode = SpecialActionMode.NONE,
                lastBannerMessage = "⚠️ Amiral Zırhlısı battı, bombardıman kullanılamaz"
            )
            return
        }
        if (battleState.playerRemainingLineBarrages <= 0) {
            battleState = battleState.copy(
                activeSpecialMode = SpecialActionMode.NONE,
                lastBannerMessage = "⚠️ Bombardıman hakkınız kalmadı"
            )
            return
        }
        if (battleState.commandPoints < 3) {
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ Bombardıman için 3 CP gereklidir"
            )
            return
        }

        val nextMode = if (battleState.activeSpecialMode == SpecialActionMode.LINE_BARRAGE) SpecialActionMode.NONE else SpecialActionMode.LINE_BARRAGE
        haptics.playButtonClick()
        battleState = battleState.copy(
            activeSpecialMode = nextMode,
            lastBannerMessage = if (nextMode == SpecialActionMode.LINE_BARRAGE) {
                "💥 BOMBARDIMAN (3 CP): Hatta dokunup ateş edin"
            } else {
                "Standart taarruz moduna dönüldü."
            }
        )
    }

    fun onCycleBarrageDir() {
        val nextDir = (battleState.lineBarrageDirectionIndex + 1) % 3
        haptics.playButtonClick()
        battleState = battleState.copy(
            lineBarrageDirectionIndex = nextDir,
            lastBannerMessage = "↻ Yön: ${when (nextDir) { 0 -> "Yatay (──)"; 1 -> "KD-GB (╱)"; else -> "KB-GD (╲)" }}"
        )
    }

    fun onPlayerFireEmp(targetHex: HexCoordinate) {
        if (battleState.phase != BattlePhase.COMBAT || !battleState.isPlayerTurn || isAiTurnInProgress) return
        val isDeltaAlive = battleState.friendlyShips.any { (it.id.contains("delta") || it.name.contains("Delta")) && !it.isSunk }
        if (!isDeltaAlive) {
            battleState = battleState.copy(
                activeSpecialMode = SpecialActionMode.NONE,
                lastBannerMessage = "⚠️ Delta Kruvazör battı, EMP kullanılamaz"
            )
            return
        }
        if (battleState.playerRemainingEmpStrikes <= 0) {
            battleState = battleState.copy(
                activeSpecialMode = SpecialActionMode.NONE,
                lastBannerMessage = "⚠️ EMP Şoku hakkınız kalmadı"
            )
            return
        }

        haptics.playShieldHum()
        soundManager.playEmpPulse()

        val sector = (targetHex.ring(1) + targetHex).filter { it.distanceTo(HexCoordinate.ZERO) <= battleState.radius }.toSet()

        var updatedDomes = battleState.enemyShieldDomes.toMutableList()
        var shieldDestroyed = false
        for (dome in battleState.enemyShieldDomes) {
            if (dome.center in sector || sector.any { it in dome.protectedCoords }) {
                updatedDomes.remove(dome)
                shieldDestroyed = true
            }
        }

        val activeEnemyParts = battleState.enemyShips.filter { !it.isSunk }.flatMap { it.parts }.toSet()
        val enemyShipsHit = sector.any { it in activeEnemyParts }
        val enemyCpPenalty = if (enemyShipsHit) 1 else 0

        val remainingEmp = (battleState.playerRemainingEmpStrikes - 1).coerceAtLeast(0)

        val detailMsg = buildString {
            append("⚡ DELTA EMP İYON ŞOKU (${targetHex.q}, ${targetHex.r}): 7 peteklik sektör tarandı. ")
            if (shieldDestroyed) append("Düşman kalkanı aşırı yüklenerek ÇÖKTÜ! ")
            if (enemyShipsHit) append("Düşman gemisi EMP şoku aldı! Rakibin sonraki turundaki Komut Puanı (CP) 2'ye düşürüldü!")
            else append("Sektörde düşman gemisi sinyali yakalanmadı.")
        }

        val logEntry = CombatLogEntry(
            turnNumber = battleState.turnNumber,
            actor = "SİZ",
            actionType = "⚡ EMP ŞOKU",
            coordinate = targetHex,
            resultTitle = if (enemyShipsHit) "ELEKTRONİK FELÇ (-1 CP)" else if (shieldDestroyed) "KALKAN ÇÖKTÜ" else "EMP ATILDI",
            detailMessage = detailMsg
        )

        val bannerMsg = if (shieldDestroyed) "⚡ EMP: Düşman kalkanı çöktü!" else if (enemyShipsHit) "⚡ EMP: Gemi felç edildi (-1 CP)" else "⚡ EMP: Sektör tarandı, temas yok"

        battleState = battleState.copy(
            playerRemainingEmpStrikes = remainingEmp,
            enemyPendingCpPenalty = enemyCpPenalty,
            enemyShieldDome = updatedDomes.firstOrNull(),
            enemyShieldDomes = updatedDomes,
            activeSpecialMode = SpecialActionMode.NONE,
            lastBannerMessage = bannerMsg,
            battleLog = listOf(detailMsg) + battleState.battleLog,
            structuredLogs = listOf(logEntry) + battleState.structuredLogs
        )
    }

    fun onPlayerFireLineBarrage(targetHex: HexCoordinate) {
        if (battleState.phase != BattlePhase.COMBAT || !battleState.isPlayerTurn || isAiTurnInProgress) return
        val isFlagshipAlive = battleState.friendlyShips.any { (it.id.contains("flagship") || it.name.contains("Amiral")) && !it.isSunk }
        if (!isFlagshipAlive) {
            battleState = battleState.copy(
                activeSpecialMode = SpecialActionMode.NONE,
                lastBannerMessage = "⚠️ Amiral Zırhlısı battı, bombardıman kullanılamaz"
            )
            return
        }
        if (battleState.playerRemainingLineBarrages <= 0) {
            battleState = battleState.copy(
                activeSpecialMode = SpecialActionMode.NONE,
                lastBannerMessage = "⚠️ Bombardıman hakkınız kalmadı"
            )
            return
        }
        if (battleState.commandPoints < 3) {
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ Bombardıman için 3 CP gereklidir"
            )
            return
        }

        haptics.playHitImpact()
        soundManager.playCannonFire()

        val dir = HexCoordinate.DIRECTIONS[battleState.lineBarrageDirectionIndex % 3]
        val lineCoords = listOf(
            targetHex - dir * 2,
            targetHex - dir,
            targetHex,
            targetHex + dir,
            targetHex + dir * 2
        ).filter { it.distanceTo(HexCoordinate.ZERO) <= battleState.radius }

        var hitCount = 0
        var missCount = 0
        val updatedPlayerShots = battleState.playerShotsOnEnemy.toMutableMap()
        var updatedEnemyShips = battleState.enemyShips.toMutableList()
        var updatedEnemyDomes = battleState.enemyShieldDomes.toMutableList()
        val newlySunkShips = mutableListOf<String>()

        for (c in lineCoords) {
            // Kalkan kontrolü
            val hitDome = updatedEnemyDomes.firstOrNull { c in it.protectedCoords }
            if (hitDome != null) {
                updatedEnemyDomes.remove(hitDome)
                continue
            }

            // Gemi kontrolü (Havadan bombardıman olduğu için resifler mermiyi engellemez!)
            val hitShip = updatedEnemyShips.firstOrNull { it.parts.contains(c) && !it.isSunk }
            if (hitShip != null) {
                val updatedHitParts = hitShip.hitParts + c
                val isSunkNow = updatedHitParts.size >= hitShip.parts.size
                updatedPlayerShots[c] = if (isSunkNow) TileHighlightState.SUNK_SHIP else TileHighlightState.HIT
                val newShip = hitShip.copy(hitParts = updatedHitParts)
                updatedEnemyShips = updatedEnemyShips.map { if (it.id == hitShip.id) newShip else it }.toMutableList()
                hitCount++
                if (isSunkNow) {
                    newlySunkShips.add(newShip.name)
                }
            } else {
                if (c !in updatedPlayerShots) {
                    updatedPlayerShots[c] = TileHighlightState.MISS
                    missCount++
                }
            }
        }

        val remainingBarrage = (battleState.playerRemainingLineBarrages - 1).coerceAtLeast(0)
        val remainingCp = (battleState.commandPoints - 3).coerceAtLeast(0)

        val detailMsg = buildString {
            append("💥 AMİRAL 5'Lİ BOMBARDIMANI: Hat boyunca ${lineCoords.size} petek vuruldu! ")
            append("($hitCount İSABET, $missCount KARAVANA)")
            if (newlySunkShips.isNotEmpty()) {
                append(" BATIRILAN: ${newlySunkShips.joinToString(", ")}!")
            }
        }

        val logEntry = CombatLogEntry(
            turnNumber = battleState.turnNumber,
            actor = "SİZ",
            actionType = "💥 BOMBARDIMAN",
            coordinate = targetHex,
            resultTitle = if (hitCount > 0) "$hitCount İSABET!" else "BOŞ ATIŞ",
            detailMessage = detailMsg
        )

        val allSunk = updatedEnemyShips.all { it.isSunk }
        val winner = if (allSunk) "PLAYER" else null
        val bannerMsg = if (winner != null) {
            "🏆 ZAFER! Düşman filosu imha edildi"
        } else if (newlySunkShips.isNotEmpty()) {
            "💥 BOMBARDIMAN: ${newlySunkShips.first()} battı!"
        } else if (hitCount > 0) {
            "💥 BOMBARDIMAN: $hitCount isabet!"
        } else {
            "💥 BOMBARDIMAN: İsabet yok, karavana"
        }

        battleState = battleState.copy(
            playerShotsOnEnemy = updatedPlayerShots,
            enemyShips = updatedEnemyShips,
            enemyShieldDome = updatedEnemyDomes.firstOrNull(),
            enemyShieldDomes = updatedEnemyDomes,
            playerRemainingLineBarrages = remainingBarrage,
            commandPoints = remainingCp,
            activeSpecialMode = SpecialActionMode.NONE,
            winner = winner,
            lastBannerMessage = bannerMsg,
            battleLog = listOf(detailMsg) + battleState.battleLog,
            structuredLogs = listOf(logEntry) + battleState.structuredLogs
        )

        if (winner == null && remainingCp == 0) {
            triggerEnemyTurn()
        }
    }

    // ==================== GEMİ VE RADYOAKTİF BÖLGE YERLEŞTİRME (DEPLOYMENT) ====================
    fun onRotateSelectedShip(clockwise: Boolean = true) {
        haptics.playButtonClick()

        if (battleState.selectedDeploymentCategory == "RADIOACTIVE") {
            val step = if (clockwise) 1 else 2
            val nextDir = (battleState.deploymentDirectionIndex + step) % 3
            val currentIdx = battleState.selectedDeploymentRadioactiveIndex
            val currentRad = battleState.friendlyRadioactiveZones.getOrNull(currentIdx)
            if (currentRad != null && currentRad.parts.isNotEmpty()) {
                val bow = currentRad.parts.first()
                val rotatedParts = buildShipCoordinates(bow, 2, nextDir)
                val isValid = rotatedParts.all {
                    it.distanceTo(HexCoordinate.ZERO) <= battleState.radius &&
                        battleState.friendlyTerrainMap[it] != TerrainType.CORAL_REEF
                }
                if (isValid) {
                    soundManager.playRadarPing()
                    val updatedZones = battleState.friendlyRadioactiveZones.mapIndexed { idx, zone ->
                        if (idx == currentIdx) zone.copy(parts = rotatedParts, directionIndex = nextDir)
                        else zone
                    }
                    battleState = battleState.copy(
                        friendlyRadioactiveZones = updatedZones,
                        deploymentDirectionIndex = nextDir,
                        lastBannerMessage = "↻ ${currentRad.name} ekseni döndürüldü: ${directionLabel(nextDir)}"
                    )
                    return
                }
            }
            battleState = battleState.copy(
                deploymentDirectionIndex = nextDir,
                lastBannerMessage = "↻ Radyoaktif yönü: ${directionLabel(nextDir)}."
            )
            return
        }

        val currentShip = battleState.friendlyShips.firstOrNull { it.id == battleState.selectedDeploymentShipId }
        val isTri = currentShip?.isTriangleShape == true
        val maxDirs = if (isTri) 6 else 3
        val step = if (clockwise) 1 else (maxDirs - 1)
        val nextDir = (battleState.deploymentDirectionIndex + step) % maxDirs

        if (currentShip != null && currentShip.parts.isNotEmpty()) {
            val bow = currentShip.parts.first()
            val rotatedParts = buildShipCoordinates(bow, currentShip.length, nextDir, isTriangle = isTri)
            if (isValidShipPlacement(
                    parts = rotatedParts,
                    shipId = currentShip.id,
                    allShips = battleState.friendlyShips,
                    terrainMap = battleState.friendlyTerrainMap,
                    radius = battleState.radius
                )
            ) {
                soundManager.playRadarPing()
                val updated = battleState.friendlyShips.map {
                    if (it.id == currentShip.id) it.copy(parts = rotatedParts, directionIndex = nextDir) else it
                }
                battleState = battleState.copy(
                    friendlyShips = updated,
                    deploymentDirectionIndex = nextDir,
                    lastBannerMessage = "↻ ${currentShip.name} ekseni döndürüldü: ${directionLabel(nextDir, isTriangle = isTri)}"
                )
                return
            }
        }

        battleState = battleState.copy(
            deploymentDirectionIndex = nextDir,
            lastBannerMessage = "↻ Yerleştirme yönü: ${directionLabel(nextDir, isTriangle = isTri)}. Haritada bir peteğe dokunun."
        )
    }

    fun onDeploymentHexClicked(clickedHex: HexCoordinate) {
        haptics.playSonarTick()

        // 1. Tıklanan petekte bir radyoaktif sis var mı?
        val radAtHex = battleState.friendlyRadioactiveZones.firstOrNull { clickedHex in it.parts }
        if (radAtHex != null) {
            val radIdx = battleState.friendlyRadioactiveZones.indexOf(radAtHex)
            if (battleState.selectedDeploymentCategory == "RADIOACTIVE" && battleState.selectedDeploymentRadioactiveIndex == radIdx) {
                // Seçili sisin üzerine tekrar tıklandıysa: ELLE DÖNDÜR
                onRotateSelectedShip(clockwise = true)
            } else {
                battleState = battleState.copy(
                    selectedDeploymentCategory = "RADIOACTIVE",
                    selectedDeploymentRadioactiveIndex = radIdx,
                    deploymentDirectionIndex = radAtHex.directionIndex,
                    lastBannerMessage = "☢️ ${radAtHex.name}: Taşıyın veya dokunup döndürün"
                )
            }
            return
        }

        // 2. Tıklanan petekte bir dost gemi var mı?
        val shipAtHex = battleState.friendlyShips.firstOrNull { clickedHex in it.parts }
        if (shipAtHex != null) {
            if (battleState.selectedDeploymentCategory == "SHIP" && shipAtHex.id == battleState.selectedDeploymentShipId) {
                // Seçili geminin üzerine tekrar tıklandıysa: ELLE DÖNDÜR (Cycle direction)
                onRotateSelectedShip(clockwise = true)
            } else {
                battleState = battleState.copy(
                    selectedDeploymentCategory = "SHIP",
                    selectedDeploymentShipId = shipAtHex.id,
                    deploymentDirectionIndex = shipAtHex.directionIndex,
                    lastBannerMessage = "⚓ ${shipAtHex.name}: Taşıyın veya dokunup döndürün"
                )
            }
            return
        }

        // 3. Boş peteğe bırakma / taşıma
        if (battleState.selectedDeploymentCategory == "RADIOACTIVE") {
            val currentIdx = battleState.selectedDeploymentRadioactiveIndex
            val currentRad = battleState.friendlyRadioactiveZones.getOrNull(currentIdx) ?: return
            val newParts = buildShipCoordinates(clickedHex, 2, battleState.deploymentDirectionIndex)

            val isValid = newParts.all {
                it.distanceTo(HexCoordinate.ZERO) <= battleState.radius &&
                    battleState.friendlyTerrainMap[it] != TerrainType.CORAL_REEF
            }

            if (isValid) {
                soundManager.playRadarPing()
                val updatedZones = battleState.friendlyRadioactiveZones.mapIndexed { idx, zone ->
                    if (idx == currentIdx) zone.copy(parts = newParts, directionIndex = battleState.deploymentDirectionIndex)
                    else zone
                }
                battleState = battleState.copy(
                    friendlyRadioactiveZones = updatedZones,
                    lastBannerMessage = "☢️ ${currentRad.name} (${clickedHex.q}, ${clickedHex.r}) yerleştirildi"
                )
            } else {
                haptics.playButtonClick()
                battleState = battleState.copy(
                    lastBannerMessage = "⚠️ Sis harita dışına veya resiflere taşamaz!"
                )
            }
            return
        }

        // Gemi taşıma
        val currentShip = battleState.friendlyShips.firstOrNull { it.id == battleState.selectedDeploymentShipId } ?: return
        val newParts = buildShipCoordinates(clickedHex, currentShip.length, battleState.deploymentDirectionIndex, isTriangle = currentShip.isTriangleShape)

        if (isValidShipPlacement(
                parts = newParts,
                shipId = currentShip.id,
                allShips = battleState.friendlyShips,
                terrainMap = battleState.friendlyTerrainMap,
                radius = battleState.radius
            )
        ) {
            soundManager.playRadarPing()
            val updatedShips = battleState.friendlyShips.map {
                if (it.id == currentShip.id) {
                    it.copy(parts = newParts, directionIndex = battleState.deploymentDirectionIndex)
                } else it
            }
            battleState = battleState.copy(
                friendlyShips = updatedShips,
                lastBannerMessage = "✅ ${currentShip.name} (${clickedHex.q}, ${clickedHex.r}) yerleştirildi"
            )
        } else {
            haptics.playButtonClick()
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ Buraya sığmıyor veya çakışıyor!"
            )
        }
    }

    fun onRandomizeFleet() {
        haptics.playButtonClick()
        soundManager.playRadarPing()
        val randomizedFriendly = generateRandomFleet(
            isEnemy = false,
            radius = battleState.radius,
            terrainMap = battleState.friendlyTerrainMap
        )
        val randomizedRad = generateRandomRadioactiveZones(
            radius = battleState.radius,
            count = battleState.customRules.radioactiveZonesCount,
            terrainMap = battleState.friendlyTerrainMap
        )
        battleState = battleState.copy(
            friendlyShips = randomizedFriendly,
            friendlyRadioactiveZones = randomizedRad,
            lastBannerMessage = "🎲 Filo ve sisler yeniden dağıtıldı"
        )
    }

    // ==================== EKRAN BOYUTUNA GÖRE UYARLANABİLİR (SAĞ-SOL / ALT-ÜST) ARAYÜZ ====================
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BoardColors.LightBackground)
    ) {
        val isWideLayout = maxWidth >= 640.dp || maxWidth > maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // 1. ÜST KOMUTA ÇUBUĞU (Top Command Header)
            CompactBattleHeader(
                battleState = battleState,
                gameMode = gameMode,
                isAiThinking = isAiTurnInProgress,
                remainingTurnSeconds = remainingTurnSeconds,
                onBack = { showExitConfirmationDialog = true },
                onOpenInfo = { showInfoDialog = true },
                onOpenSettings = { showSettingsDialog = true },
                onOpenExit = { showExitDialog = true }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. CANLI DURUM & BİLDİRİM BANDI (Status Banner)
            TacticalBannerStrip(
                message = battleState.lastBannerMessage,
                isAlert = battleState.lastBannerMessage.contains("⚠️") ||
                    battleState.lastBannerMessage.contains("🚨") ||
                    battleState.lastBannerMessage.contains("💥") ||
                    battleState.lastBannerMessage.contains("⏱️"),
                isAiThinking = isAiTurnInProgress,
                isPlayerTurn = battleState.isPlayerTurn,
                turnNumber = battleState.turnNumber,
                remainingTurnSeconds = remainingTurnSeconds,
                phase = battleState.phase
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. ANA GÖVDE: GEMİ YERLEŞTİRME VEYA İKİ HARİTALI MUHAREBE
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (battleState.phase == BattlePhase.DEPLOYMENT) {
                    FleetDeploymentWorkspace(
                        battleState = battleState,
                        isWideLayout = isWideLayout,
                        onSelectShip = { shipId ->
                            val s = battleState.friendlyShips.firstOrNull { it.id == shipId }
                            battleState = battleState.copy(
                                selectedDeploymentCategory = "SHIP",
                                selectedDeploymentShipId = shipId,
                                deploymentDirectionIndex = s?.directionIndex ?: battleState.deploymentDirectionIndex
                            )
                        },
                        onSelectRadioactiveZone = { radIdx ->
                            val rz = battleState.friendlyRadioactiveZones.getOrNull(radIdx)
                            battleState = battleState.copy(
                                selectedDeploymentCategory = "RADIOACTIVE",
                                selectedDeploymentRadioactiveIndex = radIdx,
                                deploymentDirectionIndex = rz?.directionIndex ?: battleState.deploymentDirectionIndex
                            )
                        },
                        onHexClicked = ::onDeploymentHexClicked,
                        onRotate = ::onRotateSelectedShip,
                        onRotateBiDirectional = ::onRotateSelectedShip,
                        onRandomize = ::onRandomizeFleet,
                        onStartCombat = {
                            haptics.playButtonClick()
                            soundManager.playCannonFire()
                            battleState = battleState.copy(
                                phase = BattlePhase.COMBAT,
                                lastBannerMessage = "⚔️ MUHAREBE BAŞLADI! Hedefe dokunup ateş edin"
                            )
                        },
                        onShowDeployableInfo = { selectedDeployableObjectInfo = it }
                    )
                } else {
                    DualBoardCombatWorkspace(
                        battleState = battleState,
                        isWideLayout = isWideLayout,
                        isAiThinking = isAiTurnInProgress,
                        onEnemyHexClicked = { clickedHex ->
                            haptics.playSonarTick()
                            soundManager.playRadarPing()
                            when (battleState.activeSpecialMode) {
                                SpecialActionMode.EMP_STRIKE -> {
                                    if (battleState.selectedAttackCoord == clickedHex) {
                                        onPlayerFireEmp(clickedHex)
                                    } else {
                                        battleState = battleState.copy(
                                            selectedAttackCoord = clickedHex,
                                            selectedTargetCoord = clickedHex,
                                            lastBannerMessage = "⚡ EMP HEDEFİ: (${clickedHex.q}, ${clickedHex.r}) — Vurmak için dokunun"
                                        )
                                    }
                                }
                                SpecialActionMode.LINE_BARRAGE -> {
                                    if (battleState.selectedAttackCoord == clickedHex) {
                                        onPlayerFireLineBarrage(clickedHex)
                                    } else {
                                        battleState = battleState.copy(
                                            selectedAttackCoord = clickedHex,
                                            selectedTargetCoord = clickedHex,
                                            lastBannerMessage = "💥 BOMBARDIMAN: (${clickedHex.q}, ${clickedHex.r}) — Vurmak için dokunun"
                                        )
                                    }
                                }
                                else -> {
                                    if (battleState.selectedAttackCoord == clickedHex &&
                                        clickedHex !in battleState.playerShotsOnEnemy.keys
                                    ) {
                                        // Aynı peteğe ikinci kez dokunulduğunda hızlı ateş et!
                                        onPlayerFireAt(clickedHex)
                                    } else {
                                        battleState = battleState.copy(
                                            selectedAttackCoord = clickedHex,
                                            selectedTargetCoord = clickedHex,
                                            selectedShieldCoord = null,
                                            lastBannerMessage = "🎯 HEDEF: (${clickedHex.q}, ${clickedHex.r}) — Ateş için dokunun"
                                        )
                                    }
                                }
                            }
                        },
                        onFriendlyHexClicked = { clickedHex ->
                            haptics.playSonarTick()
                            battleState = battleState.copy(
                                selectedShieldCoord = clickedHex,
                                lastBannerMessage = "🛡️ KALKAN: (${clickedHex.q}, ${clickedHex.r}) — Kurmak için butona dokunun"
                            )
                        },
                        onRadialAction = { coord, actionType ->
                            when (actionType) {
                                RadialActionType.FIRE -> onPlayerFireAt(coord)
                                RadialActionType.DRONE -> {
                                    val isCarrierAlive = battleState.friendlyShips.any { it.id.contains("carrier") && !it.isSunk }
                                    if (!isCarrierAlive) {
                                        haptics.playButtonClick()
                                        battleState = battleState.copy(
                                            lastBannerMessage = "⚠️ İHA Taşıyıcı battı, keşif devre dışı!"
                                        )
                                    } else {
                                        onPlayerDroneScan(coord)
                                    }
                                }
                                RadialActionType.SHIELD -> {
                                    battleState = battleState.copy(selectedShieldCoord = coord)
                                    onPlayerActivateShield()
                                }
                            }
                        },
                        onToggleEmp = ::onToggleEmp,
                        onToggleBarrage = ::onToggleBarrage,
                        onCycleBarrageDir = ::onCycleBarrageDir,
                        onFire = {
                            battleState.selectedAttackCoord?.let { coord ->
                                when (battleState.activeSpecialMode) {
                                    SpecialActionMode.EMP_STRIKE -> onPlayerFireEmp(coord)
                                    SpecialActionMode.LINE_BARRAGE -> onPlayerFireLineBarrage(coord)
                                    else -> onPlayerFireAt(coord)
                                }
                            }
                        },
                        onDroneScan = {
                            battleState.selectedAttackCoord?.let { onPlayerDroneScan(it) }
                        },
                        onShield = ::onPlayerActivateShield,
                        onEndTurn = {
                            if (!isAiTurnInProgress && battleState.isPlayerTurn) {
                                triggerEnemyTurn()
                            }
                        },
                        onOpenCombatLog = {
                            showCombatLogDialog = true
                        }
                    )
                }
            }
        }

        // ==================== ZAFER / BOZGUN SONUÇ DİYALOĞU ====================
        if (battleState.winner != null) {
            val isWin = battleState.winner == "PLAYER"
            VictoryDefeatDialog(
                isWin = isWin,
                gameMode = gameMode,
                turnNumber = battleState.turnNumber,
                friendlyRemaining = battleState.friendlyShips.count { !it.isSunk },
                enemyRemaining = battleState.enemyShips.count { !it.isSunk },
                onPlayAgain = {
                    battleState = createInitialDualBattleState(mapRadius, enableHazards, customRules)
                },
                onBackToLobby = onBackToLobby
            )
        }
    }

    // Sağ Üst Modal Pencereler
    if (showInfoDialog) HexaFleetInfoDialog(onDismissRequest = { showInfoDialog = false })
    if (showSettingsDialog) HexaFleetSettingsDialog(onDismissRequest = { showSettingsDialog = false })
    if (showExitDialog) HexaFleetExitDialog(onDismissRequest = { showExitDialog = false })
    if (selectedDeployableObjectInfo != null) {
        DeployableObjectInfoDialog(
            info = selectedDeployableObjectInfo!!,
            onDismissRequest = { selectedDeployableObjectInfo = null }
        )
    }
    if (showCombatLogDialog) {
        CombatLogHistoryDialog(
            structuredLogs = battleState.structuredLogs,
            turnNumber = battleState.turnNumber,
            onDismiss = { showCombatLogDialog = false }
        )
    }

    // Geri Tuşu Taktiksel Çıkış Onay Pop-Up'ı (User Requirement 6)
    if (showExitConfirmationDialog) {
        Dialog(onDismissRequest = { showExitConfirmationDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(20.dp),
                color = BoardColors.PureWhite,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.50f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFFFEE2E2), CircleShape)
                            .border(1.2.dp, Color(0xFFFCA5A5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = BoardColors.CrimsonRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "HAREKATI SONLANDIR",
                        color = BoardColors.NavyDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Mevcut filo konuşlandırmasından veya devam eden muharebeden ayrılmak istediğinize emin misiniz? Devam eden taktiksel dizilim ve oyun ilerlemesi sonlandırılacaktır.",
                        color = BoardColors.TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExitConfirmationDialog = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, BoardColors.BorderSubtle)
                        ) {
                            Text(
                                text = "İPTAL",
                                color = BoardColors.TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = {
                                showExitConfirmationDialog = false
                                onBackToLobby()
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BoardColors.CrimsonRed)
                        ) {
                            Text(
                                text = "ÇIKIŞ YAP",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Üst Komuta Çubuğu: Geri, Aşama/Tur Bilgisi, Komut Puanı (CP) ve Sağ Üst Aksiyonlar (Info, Ayarlar, Çıkış).
 */
@Composable
private fun CompactBattleHeader(
    battleState: DualBattleUiState,
    gameMode: TacticalGameMode,
    isAiThinking: Boolean,
    remainingTurnSeconds: Int = 0,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BoardColors.PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, BoardColors.BorderSubtle),
        shadowElevation = 2.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Ergonomik Geri / Çıkış Butonu (Kompakt & Estetik)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BoardColors.SurfaceSubtle)
                        .border(1.dp, BoardColors.BorderSubtle, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Lobiye Dön",
                        tint = BoardColors.NavyDark,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val isTabletHeader = LocalConfiguration.current.screenWidthDp >= 600

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    // ÜST SATIR: BRANDING + TUR + MOD/RAKİP + SÜRE/SÜRESİZ ROZETLERİ (Hepsi Tek Satırda)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isTabletHeader) 4.5.dp else 3.dp)
                    ) {
                        Text(
                            text = "DEEP STRIKE",
                            color = BoardColors.NavyDark,
                            fontSize = if (isTabletHeader) 11.5.sp else 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = if (isTabletHeader) 0.5.sp else 0.sp,
                            maxLines = 1,
                            softWrap = false
                        )

                        Text(
                            text = "•",
                            color = BoardColors.TextMuted,
                            fontSize = if (isTabletHeader) 9.sp else 7.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (battleState.phase == BattlePhase.DEPLOYMENT) {
                            Surface(
                                color = BoardColors.PurpleLight,
                                shape = RoundedCornerShape(5.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, BoardColors.PurplePrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "⚓ YERLEŞTİRME",
                                    color = BoardColors.PurplePrimary,
                                    fontSize = if (isTabletHeader) 8.5.sp else 7.2.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            // Tur Rozeti
                            Surface(
                                color = BoardColors.NavyDark,
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = "TUR ${battleState.turnNumber}",
                                    color = Color.White,
                                    fontSize = if (isTabletHeader) 8.5.sp else 7.2.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            // Mod / Rakip Rozeti
                            val (badgeLabel, badgeBg, badgeTextColor) = when (gameMode) {
                                is TacticalGameMode.Online -> Triple("ODA: ${gameMode.roomCode}", BoardColors.PurpleLight, BoardColors.PurplePrimary)
                                is TacticalGameMode.VsAi -> when (gameMode.difficulty) {
                                    AiDifficulty.RECRUIT -> Triple("VS ACEMİ", Color(0xFFDCFCE7), Color(0xFF16A34A))
                                    AiDifficulty.VETERAN -> Triple("VS KIDEMLİ", BoardColors.PurpleLight, BoardColors.PurplePrimary)
                                    AiDifficulty.GRAND_ADMIRAL -> Triple("VS AMİRAL", Color(0xFFFEE2E2), BoardColors.CrimsonRed)
                                }
                            }
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(5.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, badgeTextColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = badgeTextColor,
                                    fontSize = if (isTabletHeader) 8.5.sp else 7.2.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            // Tur Süresi / Süresiz Rozeti (Üst satırda Tur ve Mod rozetleri ile yan yana)
                            if (battleState.isPlayerTurn && !isAiThinking) {
                                if (remainingTurnSeconds > 0) {
                                    val isWarning = remainingTurnSeconds <= 10
                                    Surface(
                                        color = if (isWarning) Color(0xFFFEE2E2) else BoardColors.PurpleLight,
                                        shape = RoundedCornerShape(5.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.8.dp,
                                            if (isWarning) BoardColors.CrimsonRed else BoardColors.PurplePrimary.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text(
                                            text = "⏱️ ${remainingTurnSeconds}s",
                                            color = if (isWarning) BoardColors.CrimsonRed else BoardColors.PurplePrimary,
                                            fontSize = if (isTabletHeader) 8.5.sp else 7.2.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(5.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.8.dp,
                                            Color(0xFF86EFAC)
                                        )
                                    ) {
                                        Text(
                                            text = "⏱️ SÜRESİZ",
                                            color = Color(0xFF15803D),
                                            fontSize = if (isTabletHeader) 8.5.sp else 7.2.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ALT SATIR: KOMUT PUANI (Sadece CP ve Enerji Hücreleri - Bağımsız ve Estetik)
                    if (battleState.phase == BattlePhase.COMBAT) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isTabletHeader) 6.dp else 4.dp)
                        ) {
                            Text(
                                text = if (isAiThinking) "DÜŞMAN SIRASI" else if (isTabletHeader) "KOMUT PUANI:" else "KOMUT:",
                                color = if (isAiThinking) BoardColors.CrimsonRed else BoardColors.TextSecondary,
                                fontSize = if (isTabletHeader) 9.sp else 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false
                            )

                            // Taktiksel CP Enerji Hücreleri
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until battleState.maxCommandPoints) {
                                    val active = i < battleState.commandPoints
                                    Box(
                                        modifier = Modifier
                                            .size(
                                                width = if (isTabletHeader) 16.dp else 12.dp,
                                                height = if (isTabletHeader) 7.dp else 5.5.dp
                                            )
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (active) BoardColors.PurplePrimary else BoardColors.CommandPointEmpty)
                                            .border(0.6.dp, if (active) BoardColors.PurplePrimary else BoardColors.BorderSubtle, RoundedCornerShape(3.dp))
                                    )
                                }
                            }

                            Text(
                                text = "${battleState.commandPoints}/${battleState.maxCommandPoints} CP",
                                color = BoardColors.PurplePrimary,
                                fontSize = if (isTabletHeader) 9.5.sp else 8.2.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Text(
                            text = "Filoyu ve sisleri yerleştirin",
                            color = BoardColors.TextSecondary,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            HexaFleetTopActions(
                onOpenInfo = onOpenInfo,
                onOpenSettings = onOpenSettings,
                onOpenExit = onOpenExit
            )
        }
    }
}

/**
 * Canlı Muharebe Bildirim Şeridi (User Requirement 3)
 * Sıranın kimde olduğunu net, büyük ve estetik bir taktik rozetle gösterir.
 */
@Composable
private fun TacticalBannerStrip(
    message: String,
    isAlert: Boolean,
    isAiThinking: Boolean,
    isPlayerTurn: Boolean = true,
    turnNumber: Int = 1,
    remainingTurnSeconds: Int = 0,
    phase: BattlePhase = BattlePhase.COMBAT
) {
    val isEnemyTurn = isAiThinking || !isPlayerTurn
    val containerBg = when {
        isEnemyTurn -> Color(0xFFFFF1F2)
        isAlert -> Color(0xFFFEF2F2)
        else -> Color(0xFFF0FDF4)
    }
    val containerBorder = when {
        isEnemyTurn -> Color(0xFFFDA4AF)
        isAlert -> Color(0xFFFCA5A5)
        else -> Color(0xFF86EFAC)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerBg,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, containerBorder),
        shadowElevation = 1.dp
    ) {
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600

        Row(
            modifier = Modifier.padding(horizontal = if (isTablet) 8.dp else 6.dp, vertical = if (isTablet) 5.dp else 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 6.dp else 4.5.dp)
        ) {
            // SOL DURUM VE SIRA ROZETİ (Cyber Status Badge - Kompakt & Tek Satır)
            if (phase == BattlePhase.DEPLOYMENT) {
                Surface(
                    color = BoardColors.PurplePrimary,
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (isTablet) 6.dp else 4.5.dp, vertical = if (isTablet) 3.dp else 1.8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isTablet) 6.dp else 4.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD8B4FE))
                        )
                        Text(
                            text = "DİZİLİM",
                            color = Color.White,
                            fontSize = if (isTablet) 8.5.sp else 7.2.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else if (isEnemyTurn) {
                Surface(
                    color = BoardColors.CrimsonRed,
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (isTablet) 6.dp else 4.5.dp, vertical = if (isTablet) 3.dp else 1.8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(if (isTablet) 10.dp else 8.dp),
                            color = Color.White,
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = "DÜŞMAN SIRASI",
                            color = Color.White,
                            fontSize = if (isTablet) 8.5.sp else 7.2.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else {
                Surface(
                    color = Color(0xFF15803D), // Taktik Canlı Yeşil
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (isTablet) 6.dp else 4.5.dp, vertical = if (isTablet) 3.dp else 1.8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isTablet) 6.dp else 4.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF86EFAC))
                        )
                        Text(
                            text = if (remainingTurnSeconds > 0) {
                                "SIRA SİZDE (${remainingTurnSeconds}s)"
                            } else {
                                "SIRA SİZDE"
                            },
                            color = Color.White,
                            fontSize = if (isTablet) 8.5.sp else 7.2.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Temizlenmiş mesaj metni (Tekrarlayan 'SIRA SİZDE' ibarelerini temizleyip doğrudan taktiksel yönlendirmeyi verir)
            val cleanedMessage = message
                .replace(Regex("^🎯?\\s*S[İI]RA\\s+S[İI]ZDE[^:]*:\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^⚡?\\s*DÜŞMAN\\s+SIRASI[^:]*:\\s*", RegexOption.IGNORE_CASE), "")

            val bannerFontSize = when {
                isTablet -> 9.5.sp
                cleanedMessage.length > 50 -> 6.8.sp
                cleanedMessage.length > 38 -> 7.2.sp
                cleanedMessage.length > 28 -> 7.8.sp
                else -> 8.5.sp
            }

            Text(
                text = cleanedMessage,
                color = if (isEnemyTurn) Color(0xFF991B1B) else BoardColors.NavyDark,
                fontSize = bannerFontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =========================================================================================
// EVRE 1: GEMİ YERLEŞTİRME (FİLO KONUŞLANDIRMA) ÇALIŞMA ALANI
// =========================================================================================
@Composable
private fun FleetDeploymentWorkspace(
    battleState: DualBattleUiState,
    isWideLayout: Boolean,
    onSelectShip: (String) -> Unit,
    onSelectRadioactiveZone: (Int) -> Unit,
    onHexClicked: (HexCoordinate) -> Unit,
    onRotate: () -> Unit,
    onRotateBiDirectional: ((Boolean) -> Unit)? = null,
    onRandomize: () -> Unit,
    onStartCombat: () -> Unit,
    onShowDeployableInfo: (DeployableObjectInfo) -> Unit = {}
) {
    var activeDragHex by remember { mutableStateOf<HexCoordinate?>(null) }

    val isRadioactiveCat = battleState.selectedDeploymentCategory == "RADIOACTIVE"
    val currentShip = battleState.friendlyShips.firstOrNull { it.id == battleState.selectedDeploymentShipId }
    val currentRad = battleState.friendlyRadioactiveZones.getOrNull(battleState.selectedDeploymentRadioactiveIndex)

    val (previewParts, isPreviewValid) = remember(
        activeDragHex,
        battleState.selectedDeploymentCategory,
        battleState.selectedDeploymentShipId,
        battleState.selectedDeploymentRadioactiveIndex,
        battleState.deploymentDirectionIndex,
        battleState.friendlyShips,
        battleState.friendlyTerrainMap
    ) {
        if (isRadioactiveCat) {
            val anchor = activeDragHex ?: currentRad?.parts?.firstOrNull()
            if (anchor != null) {
                val parts = buildShipCoordinates(anchor, 2, battleState.deploymentDirectionIndex)
                val valid = parts.all {
                    it.distanceTo(HexCoordinate.ZERO) <= battleState.radius &&
                        battleState.friendlyTerrainMap[it] != TerrainType.CORAL_REEF
                }
                parts to valid
            } else {
                emptyList<HexCoordinate>() to true
            }
        } else {
            if (currentShip != null) {
                val anchor = activeDragHex ?: currentShip.parts.firstOrNull()
                if (anchor != null) {
                    val parts = buildShipCoordinates(
                        anchor,
                        currentShip.length,
                        battleState.deploymentDirectionIndex,
                        isTriangle = currentShip.isTriangleShape
                    )
                    val valid = isValidShipPlacement(
                        parts = parts,
                        shipId = currentShip.id,
                        allShips = battleState.friendlyShips,
                        terrainMap = battleState.friendlyTerrainMap,
                        radius = battleState.radius
                    )
                    parts to valid
                } else {
                    emptyList<HexCoordinate>() to true
                }
            } else {
                emptyList<HexCoordinate>() to true
            }
        }
    }

    val friendlyBoardState = battleState.toFriendlyBoardState().copy(
        previewShipParts = previewParts,
        isPreviewValid = isPreviewValid,
        previewCategory = battleState.selectedDeploymentCategory
    )

    if (isWideLayout) {
        // SAĞ - SOL DÜZENİ (Geniş Ekran / Yatay)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sol %58: Dost Filo Haritası
            BoardContainerCard(
                title = "⚓ DOST FİLO HARİTASI (KONUŞLANDIRMA SAHASI)",
                subtitle = "Seçili gemi veya radyoaktif sisi sürükleyerek taşıyın, yönünü döndürün",
                badgeText = "${battleState.friendlyShips.size}/4 GEMİ + ${battleState.friendlyRadioactiveZones.size} SİS",
                badgeColor = BoardColors.NavyAccent,
                borderColor = BoardColors.NavyAccent,
                modifier = Modifier.weight(0.58f).fillMaxHeight()
            ) {
                HexBoardCanvas(
                    state = friendlyBoardState,
                    modifier = Modifier.fillMaxSize(),
                    onHexClicked = onHexClicked,
                    onRotateShip = onRotateBiDirectional,
                    onDragHover = { hoveredHex -> activeDragHex = hoveredHex },
                    onDragRelease = { finalHex ->
                        activeDragHex = null
                        onHexClicked(finalHex)
                    }
                )
            }

            // Sağ %42: Filo Tersanesi ve Kontroller
            DeploymentControlPanel(
                battleState = battleState,
                onSelectShip = onSelectShip,
                onSelectRadioactiveZone = onSelectRadioactiveZone,
                onRotate = onRotate,
                onRandomize = onRandomize,
                onStartCombat = onStartCombat,
                onShowInfo = onShowDeployableInfo,
                modifier = Modifier.weight(0.42f).fillMaxHeight()
            )
        }
    } else {
        // ALT - ÜST DÜZENİ (Dikey / Mobil)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BoardContainerCard(
                title = "⚓ DOST FİLO HARİTASI",
                subtitle = "Gemi veya sisleri yerleştirin",
                badgeText = "${battleState.friendlyShips.size}/4 GEMİ",
                badgeColor = BoardColors.NavyAccent,
                borderColor = BoardColors.NavyAccent,
                modifier = Modifier.weight(0.52f).fillMaxWidth()
            ) {
                HexBoardCanvas(
                    state = friendlyBoardState,
                    modifier = Modifier.fillMaxSize(),
                    onHexClicked = onHexClicked,
                    onRotateShip = onRotateBiDirectional,
                    onDragHover = { hoveredHex -> activeDragHex = hoveredHex },
                    onDragRelease = { finalHex ->
                        activeDragHex = null
                        onHexClicked(finalHex)
                    }
                )
            }

            DeploymentControlPanel(
                battleState = battleState,
                onSelectShip = onSelectShip,
                onSelectRadioactiveZone = onSelectRadioactiveZone,
                onRotate = onRotate,
                onRandomize = onRandomize,
                onStartCombat = onStartCombat,
                onShowInfo = onShowDeployableInfo,
                modifier = Modifier.weight(0.48f).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DeploymentControlPanel(
    battleState: DualBattleUiState,
    onSelectShip: (String) -> Unit,
    onSelectRadioactiveZone: (Int) -> Unit,
    onRotate: () -> Unit,
    onRandomize: () -> Unit,
    onStartCombat: () -> Unit,
    onShowInfo: (DeployableObjectInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BoardColors.PureWhite),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Text(
                text = "DONANMA TERSANESİ",
                color = BoardColors.NavyDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "Gemi veya radyoaktif sis seçip haritaya yerleştirin",
                color = BoardColors.TextSecondary,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. KAYDIRILABİLİR LİSTE ALANI & ANİMASYONLU FLU KAYDIR BUTONLARI (User Requirement 2)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 2.dp)
                ) {

                Spacer(modifier = Modifier.height(6.dp))

                // Gemi Listesi (1. Korvet, 2. Delta, 3. İHA G., 4. Amiral)
                val sortedDeploymentShips = remember(battleState.friendlyShips) {
                    battleState.friendlyShips.sortedBy { ShipVisuals.getCanonicalShipOrder(it.name) }
                }
                for (ship in sortedDeploymentShips) {
                    val isSelected = battleState.selectedDeploymentCategory == "SHIP" && ship.id == battleState.selectedDeploymentShipId
                    val shipDrawable = ShipVisuals.getShipDrawableRes(ship.name, isEnemy = false)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelectShip(ship.id) },
                        color = if (isSelected) Color(0xFFFEF9C3) else BoardColors.PureWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.8.dp else 1.dp,
                            if (isSelected) Color(0xFFEAB308) else BoardColors.BorderSubtle
                        ),
                        shadowElevation = if (isSelected) 2.dp else 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Gemi Önizleme Görseli
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 26.dp)
                                        .background(if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.5f) else BoardColors.SurfaceSubtle, RoundedCornerShape(6.dp))
                                        .border(0.8.dp, if (isSelected) Color(0xFFEAB308) else BoardColors.BorderSubtle, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(shipDrawable),
                                        contentDescription = ship.name,
                                        modifier = Modifier.fillMaxSize().padding(2.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                val displayShipName = remember(ship.name) {
                                    if (ship.name.contains("Delta", ignoreCase = true)) "Delta Taarruz"
                                    else ship.name
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$displayShipName (${ship.length} Petek)",
                                        color = if (isSelected) Color(0xFF854D0E) else BoardColors.NavyDark,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Yön: ${directionLabel(ship.directionIndex, isTriangle = ship.isTriangleShape)}",
                                        color = if (isSelected) Color(0xFFA16207) else BoardColors.TextSecondary,
                                        fontSize = 8.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Sağ Bölüm: Profesyonel Estetik (i) Butonu + Gemi Boyutu Kutucukları
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DeployInfoIconButton(
                                    onClick = { onShowInfo(getDeployableObjectInfo(ship.name)) },
                                    accentColor = getDeployableObjectAccent(ship.name),
                                    contentDescription = "${ship.name} Bilgisi"
                                )

                                // Gemi boyutu kutucukları (Delta Kruvazörü için 2. Resimdeki üçgen piramit form, diğerleri doğrusal)
                                val isDeltaTriangle = ship.isTriangleShape || ship.name.contains("Delta", ignoreCase = true)
                                if (isDeltaTriangle) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 9.5.dp, height = 5.dp)
                                                .clip(RoundedCornerShape(1.8.dp))
                                                .background(if (isSelected) Color(0xFFEAB308) else BoardColors.NavyAccent)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(2) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 9.5.dp, height = 5.dp)
                                                        .clip(RoundedCornerShape(1.8.dp))
                                                        .background(if (isSelected) Color(0xFFEAB308) else BoardColors.NavyAccent)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        repeat(ship.length) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 11.dp, height = 7.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (isSelected) Color(0xFFEAB308) else BoardColors.NavyAccent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Radyoaktif Sis Bölgesi Seçim Kartları (User Requirement 4)
                if (battleState.friendlyRadioactiveZones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RADYOAKTİF SİS BÖLGELERİ (İHA KAMUFLAJI - 2 PETEK)",
                            color = Color(0xFF15803D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        DeployInfoIconButton(
                            onClick = { onShowInfo(getDeployableObjectInfo("radioactive")) },
                            accentColor = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp),
                            contentDescription = "Radyoaktif Sis Bilgisi"
                        )
                    }
                    Text(
                        text = "İHA'ların tespit edemediği yeşil sis bölgesi. Gemilerinizin üzerine veya boş sulara konuşlandırın.",
                        color = BoardColors.TextSecondary,
                        fontSize = 8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    for ((idx, radZone) in battleState.friendlyRadioactiveZones.withIndex()) {
                        val isSelected = battleState.selectedDeploymentCategory == "RADIOACTIVE" &&
                            battleState.selectedDeploymentRadioactiveIndex == idx
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { onSelectRadioactiveZone(idx) },
                            color = if (isSelected) Color(0xFFDCFCE7) else BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) Color(0xFF16A34A) else BoardColors.BorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Radyoaktif Tehlike Önizleme Görseli (Ekteki 2. resim taktik ikonu)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 46.dp, height = 26.dp)
                                            .background(if (isSelected) Color(0xFFBBF7D0).copy(alpha = 0.6f) else BoardColors.SurfaceSubtle, RoundedCornerShape(6.dp))
                                            .border(0.8.dp, if (isSelected) Color(0xFF16A34A) else BoardColors.BorderSubtle, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.hazard_radioactive),
                                            contentDescription = radZone.name,
                                            modifier = Modifier.size(22.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${radZone.name} (2 Petek)",
                                            color = if (isSelected) Color(0xFF14532D) else BoardColors.NavyDark,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Yön: ${directionLabel(radZone.directionIndex)}",
                                            color = if (isSelected) Color(0xFF15803D) else BoardColors.TextSecondary,
                                            fontSize = 8.5.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DeployInfoIconButton(
                                        onClick = { onShowInfo(getDeployableObjectInfo("radioactive")) },
                                        accentColor = Color(0xFF16A34A),
                                        contentDescription = "Radyoaktif Bölge Bilgisi"
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(2) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 14.dp, height = 8.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFF22C55E))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Aşağıda nesne olduğunu belirten kompakt flu animasyonlu "AŞAĞI KAYDIR" butonu
                if (scrollState.canScrollForward) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo((scrollState.value + 160).coerceAtMost(scrollState.maxValue))
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp),
                        color = Color(0xD8082F49), // Zarif flu derin turkuaz cam gövde
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF22D3EE).copy(alpha = 0.75f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Aşağı Kaydır",
                                tint = Color(0xFF22D3EE), // Canlı turkuaz neon ok
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "AŞAĞI KAYDIR",
                                color = Color(0xFFFDE047), // Okunabilir, yüksek kontrastlı siber sarı metin
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }

                // Yukarıda nesne olduğunu belirten kompakt flu animasyonlu "YUKARI KAYDIR" butonu
                if (scrollState.canScrollBackward) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo((scrollState.value - 160).coerceAtLeast(0))
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp),
                        color = Color(0xD8082F49), // Zarif flu derin turkuaz cam gövde
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF22D3EE).copy(alpha = 0.75f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Yukarı Kaydır",
                                tint = Color(0xFF22D3EE), // Canlı turkuaz neon ok
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "YUKARI KAYDIR",
                                color = Color(0xFFFDE047), // Okunabilir, yüksek kontrastlı siber sarı metin
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. SABİT ALT AKSİYONLAR: Yön Döndür & Rastgele Diz Butonları (User Requirement 2: Estetik Taktiksel İkonlar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRotate,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BoardColors.PurplePrimary)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(BoardColors.PurpleLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = BoardColors.PurplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "YÖN DÖNDÜR",
                        color = BoardColors.PurplePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = onRandomize,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BoardColors.NavyAccent)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = BoardColors.NavyAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "RASTGELE DİZ",
                        color = BoardColors.NavyAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Savaşı Başlat Butonu
            Button(
                onClick = onStartCombat,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BoardColors.CrimsonRed)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FİLOYU ONAYLA VE BAŞLA",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// =========================================================================================
// EVRE 2: İKİ HARİTALI AMİRAL BATTI SAVAŞ ALANI (SAĞ-SOL VEYA ALT-ÜST)
// =========================================================================================
@Composable
private fun DualBoardCombatWorkspace(
    battleState: DualBattleUiState,
    isWideLayout: Boolean,
    isAiThinking: Boolean,
    onEnemyHexClicked: (HexCoordinate) -> Unit,
    onFriendlyHexClicked: (HexCoordinate) -> Unit,
    onRadialAction: (HexCoordinate, RadialActionType) -> Unit,
    onToggleEmp: () -> Unit,
    onToggleBarrage: () -> Unit,
    onCycleBarrageDir: () -> Unit,
    onFire: () -> Unit,
    onDroneScan: () -> Unit,
    onShield: () -> Unit,
    onEndTurn: () -> Unit,
    onOpenCombatLog: () -> Unit
) {
    val enemyAttackBoardState = battleState.toEnemyAttackBoardState(isRadarActive = true)
    val friendlyDefenseBoardState = battleState.toFriendlyBoardState()

    val aliveEnemyShips = battleState.enemyShips.count { !it.isSunk }
    val aliveFriendlyShips = battleState.friendlyShips.count { !it.isSunk }
    val isCarrierAlive = battleState.friendlyShips.any { it.id.contains("carrier") && !it.isSunk }
    val isDeltaAlive = battleState.friendlyShips.any { it.id.contains("delta") && !it.isSunk }
    val isFlagshipAlive = battleState.friendlyShips.any { it.id.contains("flagship") && !it.isSunk }

    if (isWideLayout) {
        // ==================== GENİŞ EKRAN / YATAY: SAĞ - SOL ÇİFT HARİTA ====================
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // SOL HARİTA: DÜŞMAN HEDEF RADARI (SALDIRI SAHASI)
                BoardContainerCard(
                    title = "🎯 DÜŞMAN HEDEF RADARI",
                    subtitle = "Ateş veya İHA keşfi için düşman peteğine dokunun",
                    badgeText = "HEDEF: $aliveEnemyShips/4 GEMİ",
                    badgeColor = BoardColors.CrimsonRed,
                    borderColor = BoardColors.CrimsonRed.copy(alpha = 0.65f),
                    isAttackBoard = true,
                    footerContent = {
                        FleetMiniStatusRow(
                            label = "DÜŞMAN FİLOSU:",
                            ships = battleState.enemyShips,
                            activeColor = BoardColors.CrimsonRed
                        )
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HexBoardCanvas(
                            state = enemyAttackBoardState,
                            modifier = Modifier.fillMaxSize(),
                            onHexClicked = onEnemyHexClicked,
                            onRadialAction = onRadialAction
                        )
                        TacticalSpecialOpsOverlay(
                            battleState = battleState,
                            isDeltaAlive = isDeltaAlive,
                            isFlagshipAlive = isFlagshipAlive,
                            isAiThinking = isAiThinking,
                            onToggleEmp = onToggleEmp,
                            onToggleBarrage = onToggleBarrage,
                            onCycleBarrageDir = onCycleBarrageDir,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }
                }

                // SAĞ HARİTA: DOST FİLO HARİTASI (SAVUNMA SAHASI)
                BoardContainerCard(
                    title = "🛡️ DOST FİLO HARİTASI",
                    subtitle = "Savunma kubbesi, filo dizilimi ve gelen saldırılar",
                    badgeText = "SAĞLAM: $aliveFriendlyShips/4 GEMİ",
                    badgeColor = BoardColors.NavyAccent,
                    borderColor = BoardColors.NavyAccent.copy(alpha = 0.65f),
                    isAttackBoard = false,
                    footerContent = {
                        FleetMiniStatusRow(
                            label = "DOST FİLO:",
                            ships = battleState.friendlyShips,
                            activeColor = BoardColors.NavyAccent
                        )
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    HexBoardCanvas(
                        state = friendlyDefenseBoardState,
                        modifier = Modifier.fillMaxSize(),
                        onHexClicked = onFriendlyHexClicked
                    )
                }
            }

            // ALT KOMUTA KONSOLU (Tactical Action Bar)
            CombatActionConsole(
                selectedCoord = battleState.selectedAttackCoord,
                commandPoints = battleState.commandPoints,
                isPlayerTurn = battleState.isPlayerTurn && !isAiThinking,
                isCarrierAlive = isCarrierAlive,
                onFire = onFire,
                onDroneScan = onDroneScan,
                onShield = onShield,
                onEndTurn = onEndTurn,
                onOpenLog = onOpenCombatLog
            )
        }
    } else {
        // ==================== DİKEY / MOBİL EKRAN: ALT - ÜST ÇİFT HARİTA ====================
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ÜST HARİTA: DÜŞMAN HEDEF RADARI (SALDIRI SAHASI - GENİŞLETİLMİŞ MOBİL ALAN)
            BoardContainerCard(
                title = "🎯 DÜŞMAN HEDEF RADARI",
                subtitle = "Vurmak için peteğe dokunun",
                badgeText = "DÜŞMAN: $aliveEnemyShips/4",
                badgeColor = BoardColors.CrimsonRed,
                borderColor = BoardColors.CrimsonRed.copy(alpha = 0.65f),
                isAttackBoard = true,
                isCompactCard = false,
                footerContent = {
                    FleetMiniStatusRow(
                        label = "DÜŞMAN:",
                        ships = battleState.enemyShips,
                        activeColor = BoardColors.CrimsonRed
                    )
                },
                modifier = Modifier.weight(0.58f).fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val hasSpecialCards = battleState.customRules.maxEmpStrikes > 0 || battleState.customRules.maxLineBarrages > 0
                    HexBoardCanvas(
                        state = enemyAttackBoardState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (hasSpecialCards) 18.dp else 0.dp),
                        onHexClicked = onEnemyHexClicked,
                        onRadialAction = onRadialAction
                    )
                    TacticalSpecialOpsOverlay(
                        battleState = battleState,
                        isDeltaAlive = isDeltaAlive,
                        isFlagshipAlive = isFlagshipAlive,
                        isAiThinking = isAiThinking,
                        onToggleEmp = onToggleEmp,
                        onToggleBarrage = onToggleBarrage,
                        onCycleBarrageDir = onCycleBarrageDir,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }

            // ORTA KOMUTA KONSOLU
            CombatActionConsole(
                selectedCoord = battleState.selectedAttackCoord,
                commandPoints = battleState.commandPoints,
                isPlayerTurn = battleState.isPlayerTurn && !isAiThinking,
                isCarrierAlive = isCarrierAlive,
                onFire = onFire,
                onDroneScan = onDroneScan,
                onShield = onShield,
                onEndTurn = onEndTurn,
                onOpenLog = onOpenCombatLog
            )

            // ALT HARİTA: DOST FİLO HARİTASI (KOMPAKT SAVUNMA İZLEME SAHASI)
            BoardContainerCard(
                title = "🛡️ DOST FİLO HARİTASI",
                subtitle = "Filo durumu ve savunma kalkanı",
                badgeText = "DOST: $aliveFriendlyShips/4",
                badgeColor = BoardColors.NavyAccent,
                borderColor = BoardColors.NavyAccent.copy(alpha = 0.65f),
                isAttackBoard = false,
                isCompactCard = true,
                footerContent = {
                    FleetMiniStatusRow(
                        label = "FİLOMUZ:",
                        ships = battleState.friendlyShips,
                        activeColor = BoardColors.NavyAccent
                    )
                },
                modifier = Modifier.weight(0.30f).fillMaxWidth()
            ) {
                HexBoardCanvas(
                    state = friendlyDefenseBoardState,
                    modifier = Modifier.fillMaxSize(),
                    onHexClicked = onFriendlyHexClicked
                )
            }
        }
    }
}

/**
 * Harita Kartı Çerçevesi (Başlık, Rozet, Otomatik Ölçeklenen Harita Alanı ve Alt Filo Durumu)
 * User Requirement 6: Yüksek Kaliteli Taktiksel Deniz Komuta Başlık Tasarımı
 */
@Composable
private fun BoardContainerCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    borderColor: Color,
    isAttackBoard: Boolean = false,
    isCompactCard: Boolean = false,
    modifier: Modifier = Modifier,
    footerContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BoardColors.PureWhite),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Üst Başlık Şeridi - Modern Taktiksel Fırçalanmış Gradient
            val headerGradient = if (isAttackBoard) {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFFF1F2),
                        Color(0xFFFFFFFF),
                        Color(0xFFFEE2E2)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFF1F5F9),
                        Color(0xFFFFFFFF),
                        Color(0xFFEFF6FF)
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(horizontal = if (isCompactCard) 10.dp else 12.dp, vertical = if (isCompactCard) 3.5.dp else 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactCard) 6.dp else 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Sol Renk Çizgisi Vurgusu
                    Box(
                        modifier = Modifier
                            .width(if (isCompactCard) 3.dp else 3.5.dp)
                            .height(if (isCompactCard) 20.dp else 28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isAttackBoard) BoardColors.CrimsonRed else BoardColors.NavyAccent)
                    )

                    // İkon Rozeti
                    Box(
                        modifier = Modifier
                            .size(if (isCompactCard) 22.dp else 28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAttackBoard) BoardColors.CrimsonRed.copy(alpha = 0.12f)
                                else BoardColors.NavyAccent.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAttackBoard) Icons.Default.Radar else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isAttackBoard) BoardColors.CrimsonRed else BoardColors.NavyAccent,
                            modifier = Modifier.size(if (isCompactCard) 12.dp else 16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = BoardColors.NavyDark,
                            fontSize = if (isCompactCard) 8.5.sp else 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = subtitle,
                            color = BoardColors.TextSecondary,
                            fontSize = if (isCompactCard) 7.sp else 8.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Sağ Durum Rozeti (Cyber-Tactical Status Badge - User Requirement 6)
                Surface(
                    color = if (isAttackBoard) Color(0xFFFFF1F2) else Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.65f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (isCompactCard) 6.dp else 9.dp, vertical = if (isCompactCard) 2.dp else 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isCompactCard) 4.dp else 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompactCard) 5.dp else 7.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = if (isCompactCard) 8.5.sp else 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = if (isCompactCard) 0.2.sp else 0.5.sp
                        )
                    }
                }
            }

            // Orta Harita Alanı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(if (isCompactCard) 2.dp else 4.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }

            // Alt Filo Sağlık Özeti Şeridi
            if (footerContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BoardColors.SurfaceSubtle)
                        .padding(horizontal = if (isCompactCard) 8.dp else 10.dp, vertical = if (isCompactCard) 3.dp else 5.dp)
                ) {
                    footerContent()
                }
            }
        }
    }
}

private fun shortShipName(shipName: String): String = ShipVisuals.getCanonicalShipName(shipName)

/**
 * Harita Altında Gemi Sağlık Çubuklarını ve Donanma İkonlarını Gösteren Estetik, Ortalanmış, Hasar İkonlu Taktik Şerit.
 * Sıralama: 1. Korvet, 2. Delta, 3. İHA G., 4. Amiral
 */
@Composable
private fun FleetMiniStatusRow(
    label: String,
    ships: List<ShipDisplayModel>,
    activeColor: Color
) {
    val sortedShips = remember(ships) {
        ships.sortedBy { ShipVisuals.getCanonicalShipOrder(it.name) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = BoardColors.SurfaceSubtle.copy(alpha = 0.95f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = BoardColors.NavyDark,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                for (ship in sortedShips) {
                    val sName = shortShipName(ship.name)
                    val isSunk = ship.isSunk
                    val drawableRes = ShipVisuals.getShipDrawableRes(ship.name, ship.isEnemy)

                    Surface(
                        color = if (isSunk) Color(0xFFFEE2E2).copy(alpha = 0.75f) else BoardColors.PureWhite,
                        shape = RoundedCornerShape(7.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            if (isSunk) Color(0xFFFCA5A5) else activeColor.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                        ) {
                            // Gemi Minyatür İkonu
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(drawableRes),
                                    contentDescription = sName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(if (isSunk) 0.35f else 1.0f),
                                    contentScale = ContentScale.Fit
                                )
                                if (isSunk) {
                                    Text(
                                        text = "✕",
                                        color = BoardColors.CrimsonRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = sName,
                                    color = if (isSunk) Color(0xFF991B1B) else BoardColors.NavyDark,
                                    fontSize = 7.5.sp,
                                    fontWeight = if (isSunk) FontWeight.Normal else FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 9.sp
                                )

                                if (ship.isTriangleShape && ship.parts.size == 3) {
                                    // Üçgen Delta Gövdesi Görünümü: 1 Önde (Üst), 2 Arkada (Alt)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        ShipPartBox(ship, 0, activeColor, isDelta = true)
                                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                            ShipPartBox(ship, 1, activeColor, isDelta = true)
                                            ShipPartBox(ship, 2, activeColor, isDelta = true)
                                        }
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (i in 0 until ship.parts.size) {
                                            ShipPartBox(ship, i, activeColor, isDelta = false)
                                        }
                                    }
                                }
                            }

                            if (isSunk) {
                                Text(
                                    text = "💥",
                                    fontSize = 7.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShipPartBox(
    ship: ShipDisplayModel,
    partIndex: Int,
    activeColor: Color,
    isDelta: Boolean = false
) {
    val partCoord = ship.parts.getOrNull(partIndex)
    val isDamaged = if (partCoord != null) partCoord in ship.hitParts else partIndex >= ship.remainingHp
    val boxWidth = if (isDelta) 7.5.dp else 8.5.dp
    val boxHeight = if (isDelta) 5.dp else 6.5.dp

    if (isDamaged) {
        // Hasar İkonlu Kutu (Flu / Canlı Kırmızı ve ✕ İkonu)
        Box(
            modifier = Modifier
                .size(width = boxWidth, height = boxHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFFEF2F2))
                .border(0.8.dp, BoardColors.CrimsonRed, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = BoardColors.CrimsonRed,
                fontSize = if (isDelta) 4.5.sp else 5.5.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = if (isDelta) 5.sp else 6.sp
            )
        }
    } else {
        // Sağlam Gemi Bölmesi
        Box(
            modifier = Modifier
                .size(width = boxWidth, height = boxHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(if (ship.isSunk) Color(0xFF94A3B8) else activeColor)
        )
    }
}

/**
 * Alt / Orta Taktiksel Atış ve Yetenek Konsolu (User Requirement 5: Muharebe Günlüğü Butonu Eklendi)
 */
@Composable
private fun CombatActionConsole(
    selectedCoord: HexCoordinate?,
    commandPoints: Int,
    isPlayerTurn: Boolean,
    isCarrierAlive: Boolean = true,
    onFire: () -> Unit,
    onDroneScan: () -> Unit,
    onShield: () -> Unit,
    onEndTurn: () -> Unit,
    onOpenLog: () -> Unit
) {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val btnHeight = if (isTablet) 42.dp else 38.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BoardColors.PureWhite,
        shape = RoundedCornerShape(if (isTablet) 14.dp else 11.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 10.dp else 5.dp, vertical = if (isTablet) 7.dp else 5.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 7.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seçili Koordinat Rozeti (Kompakt & Tek Satır Güvenli)
            Surface(
                color = BoardColors.SurfaceSubtle,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                modifier = Modifier.height(btnHeight)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = if (isTablet) 8.dp else 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isTablet) "NİŞANGAH" else "HEDEF",
                        color = BoardColors.TextSecondary,
                        fontSize = if (isTablet) 7.5.sp else 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = if (selectedCoord != null) "(${selectedCoord.q},${selectedCoord.r})" else "SEÇİN",
                        color = if (selectedCoord != null) BoardColors.CrimsonRed else BoardColors.TextMuted,
                        fontSize = if (isTablet) 10.sp else 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // 1. ATEŞ ET (1 CP)
            val canFire = isPlayerTurn && selectedCoord != null && commandPoints >= 1
            Button(
                onClick = onFire,
                enabled = canFire,
                modifier = Modifier.weight(1.08f).height(btnHeight),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 1.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BoardColors.CrimsonRed,
                    disabledContainerColor = BoardColors.SurfaceSubtle
                )
            ) {
                if (isTablet) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (canFire) Color.White else BoardColors.TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ATEŞ ET (1 CP)",
                        color = if (canFire) Color.White else BoardColors.TextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎯 ATEŞ ET",
                            color = if (canFire) Color.White else BoardColors.TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 9.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "1 CP",
                            color = if (canFire) Color.White.copy(alpha = 0.90f) else BoardColors.TextMuted,
                            fontSize = 6.8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 7.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 2. İHA KEŞFİ (3 CP)
            val canDrone = isPlayerTurn && selectedCoord != null && commandPoints >= 3 && isCarrierAlive
            Button(
                onClick = onDroneScan,
                enabled = canDrone,
                modifier = Modifier.weight(1f).height(btnHeight),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 1.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BoardColors.PurplePrimary,
                    disabledContainerColor = BoardColors.SurfaceSubtle
                )
            ) {
                if (isTablet) {
                    Text(
                        text = if (isCarrierAlive) "📡 İHA (3 CP)" else "İHA (KİLİTLİ)",
                        color = if (canDrone) Color.White else BoardColors.TextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📡 İHA KEŞFİ",
                            color = if (canDrone) Color.White else BoardColors.TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 9.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = if (isCarrierAlive) "3 CP" else "KİLİTLİ",
                            color = if (canDrone) Color.White.copy(alpha = 0.90f) else BoardColors.TextMuted,
                            fontSize = 6.8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 7.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 3. AEGIS KALKANI (2 CP)
            val canShield = isPlayerTurn && commandPoints >= 2
            Button(
                onClick = onShield,
                enabled = canShield,
                modifier = Modifier.weight(1f).height(btnHeight),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 1.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BoardColors.NavyAccent,
                    disabledContainerColor = BoardColors.SurfaceSubtle
                )
            ) {
                if (isTablet) {
                    Text(
                        text = "🛡️ KALKAN (2 CP)",
                        color = if (canShield) Color.White else BoardColors.TextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🛡️ KALKAN",
                            color = if (canShield) Color.White else BoardColors.TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 9.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "2 CP",
                            color = if (canShield) Color.White.copy(alpha = 0.90f) else BoardColors.TextMuted,
                            fontSize = 6.8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 7.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 4. TURU BİTİR
            OutlinedButton(
                onClick = onEndTurn,
                enabled = isPlayerTurn,
                modifier = Modifier.height(btnHeight),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = if (isTablet) 8.dp else 5.dp, vertical = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.NavyDark)
            ) {
                if (isTablet) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Turu Bitir", tint = BoardColors.NavyDark, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "TUR BİTİR",
                        color = BoardColors.NavyDark,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⏭ TUR",
                            color = BoardColors.NavyDark,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 8.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "BİTİR",
                            color = BoardColors.NavyDark,
                            fontSize = 6.8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 7.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 5. MUHAREBE GÜNLÜĞÜ BUTONU (Özel Box Mimarisi - Asla 48dp zorlaması yapmaz)
            Box(
                modifier = Modifier
                    .size(if (isTablet) 42.dp else 34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BoardColors.SurfaceSubtle)
                    .border(1.dp, BoardColors.BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenLog),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = "Muharebe Günlüğü",
                    tint = BoardColors.NavyDark,
                    modifier = Modifier.size(if (isTablet) 19.dp else 16.dp)
                )
            }
        }
    }
}

/**
 * Düşman Hedef Radarı sol üst ve sağ üst köşelerinde yer alan Kompakt Taktiksel Özel Yetenek Kartları.
 * Küçük telefon ve tablet ekranlarının tamamında peteklerin üzerine asla gelmeyecek şekilde küçültülerek optimize edilmiştir.
 */
@Composable
private fun TacticalSpecialOpsOverlay(
    battleState: DualBattleUiState,
    isDeltaAlive: Boolean,
    isFlagshipAlive: Boolean,
    isAiThinking: Boolean,
    onToggleEmp: () -> Unit,
    onToggleBarrage: () -> Unit,
    onCycleBarrageDir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showEmp = battleState.customRules.maxEmpStrikes > 0
    val showBarrage = battleState.customRules.maxLineBarrages > 0

    if (!showEmp && !showBarrage) return

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val isEnemyDeltaAlive = battleState.enemyShips.none { (it.id.contains("delta") || it.name.contains("Delta")) && it.isSunk }
    val isEnemyFlagshipAlive = battleState.enemyShips.none { (it.id.contains("flagship") || it.name.contains("Amiral")) && it.isSunk }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. SOL ÜST KÖŞE: DELTA KRUVAZÖRÜ: İYON ŞOKU / EMP (0 CP)
        if (showEmp) {
            val isEmpActive = battleState.activeSpecialMode == SpecialActionMode.EMP_STRIKE
            val canUseEmp = isDeltaAlive && battleState.playerRemainingEmpStrikes > 0 && battleState.isPlayerTurn && !isAiThinking
            val empBg = if (isEmpActive) BoardColors.PurpleLight else BoardColors.PureWhite.copy(alpha = 0.94f)
            val empBorderColor = if (isEmpActive) Color(0xFF06B6D4) else BoardColors.BorderSubtle

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = if (isTablet) 6.dp else 2.dp, top = if (isTablet) 4.dp else 1.dp)
                    .clip(RoundedCornerShape(if (isTablet) 8.dp else 6.dp))
                    .clickable(enabled = canUseEmp || isEmpActive) { onToggleEmp() },
                color = empBg,
                shape = RoundedCornerShape(if (isTablet) 8.dp else 6.dp),
                border = androidx.compose.foundation.BorderStroke(if (isEmpActive) 1.5.dp else 0.8.dp, empBorderColor),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 6.dp else 4.dp,
                        vertical = if (isTablet) 3.5.dp else 2.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 5.dp else 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isTablet) 17.dp else 13.dp)
                            .background(
                                color = if (canUseEmp || isEmpActive) Color(0xFF06B6D4).copy(alpha = 0.15f) else BoardColors.SurfaceSubtle,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 0.7.dp,
                                color = if (canUseEmp || isEmpActive) Color(0xFF06B6D4).copy(alpha = 0.45f) else BoardColors.BorderSubtle,
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "EMP",
                            tint = if (canUseEmp || isEmpActive) Color(0xFF06B6D4) else BoardColors.TextMuted,
                            modifier = Modifier.size(if (isTablet) 10.dp else 8.5.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "İYON ŞOKU",
                                fontSize = if (isTablet) 8.sp else 6.5.sp,
                                lineHeight = if (isTablet) 9.sp else 7.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (canUseEmp || isEmpActive) BoardColors.NavyDark else BoardColors.TextMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = " (0 CP)",
                                fontSize = if (isTablet) 7.sp else 5.8.sp,
                                lineHeight = if (isTablet) 8.sp else 6.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canUseEmp || isEmpActive) Color(0xFF06B6D4) else BoardColors.TextMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        val playerEmpText = if (!isDeltaAlive) {
                            "Siz: Battı"
                        } else {
                            "Siz: ${battleState.playerRemainingEmpStrikes}/${battleState.customRules.maxEmpStrikes} Hak"
                        }
                        Text(
                            text = playerEmpText,
                            fontSize = if (isTablet) 6.8.sp else 5.6.sp,
                            lineHeight = if (isTablet) 7.5.sp else 6.2.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isDeltaAlive) BoardColors.CrimsonRed else BoardColors.TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )

                        val enemyEmpText = when {
                            !isEnemyDeltaAlive -> "Rakip: Battı"
                            battleState.enemyRemainingEmpStrikes == 0 -> "Rakip: 0/${battleState.customRules.maxEmpStrikes}"
                            else -> "Rakip: ${battleState.enemyRemainingEmpStrikes}/${battleState.customRules.maxEmpStrikes} Hak"
                        }
                        val enemyEmpColor = when {
                            !isEnemyDeltaAlive -> BoardColors.CrimsonRed
                            battleState.enemyRemainingEmpStrikes == 0 -> Color(0xFFDC2626)
                            else -> Color(0xFF15803D)
                        }
                        Text(
                            text = enemyEmpText,
                            fontSize = if (isTablet) 6.8.sp else 5.6.sp,
                            lineHeight = if (isTablet) 7.5.sp else 6.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = enemyEmpColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 2. SAĞ ÜST KÖŞE: AMİRAL ZIRHLISI: 5'Lİ AĞIR BOMBARDIMAN (3 CP)
        if (showBarrage) {
            val isBarrageActive = battleState.activeSpecialMode == SpecialActionMode.LINE_BARRAGE
            val canUseBarrage = isFlagshipAlive && battleState.playerRemainingLineBarrages > 0 &&
                battleState.commandPoints >= 3 && battleState.isPlayerTurn && !isAiThinking
            val barrageBg = if (isBarrageActive) BoardColors.RedLight else BoardColors.PureWhite.copy(alpha = 0.94f)
            val barrageBorderColor = if (isBarrageActive) BoardColors.CrimsonRed else BoardColors.BorderSubtle

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = if (isTablet) 6.dp else 2.dp, top = if (isTablet) 4.dp else 1.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (isTablet) 8.dp else 6.dp))
                        .clickable(enabled = canUseBarrage || isBarrageActive) { onToggleBarrage() },
                    color = barrageBg,
                    shape = RoundedCornerShape(if (isTablet) 8.dp else 6.dp),
                    border = androidx.compose.foundation.BorderStroke(if (isBarrageActive) 1.5.dp else 0.8.dp, barrageBorderColor),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (isTablet) 6.dp else 4.dp,
                            vertical = if (isTablet) 3.5.dp else 2.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 5.dp else 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isTablet) 17.dp else 13.dp)
                                .background(
                                    color = if (canUseBarrage || isBarrageActive) BoardColors.CrimsonRed.copy(alpha = 0.12f) else BoardColors.SurfaceSubtle,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 0.7.dp,
                                    color = if (canUseBarrage || isBarrageActive) BoardColors.CrimsonRed.copy(alpha = 0.45f) else BoardColors.BorderSubtle,
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Line Barrage",
                                tint = if (canUseBarrage || isBarrageActive) BoardColors.CrimsonRed else BoardColors.TextMuted,
                                modifier = Modifier.size(if (isTablet) 10.dp else 8.5.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BOMBARDIMAN",
                                    fontSize = if (isTablet) 8.sp else 6.5.sp,
                                    lineHeight = if (isTablet) 9.sp else 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (canUseBarrage || isBarrageActive) BoardColors.NavyDark else BoardColors.TextMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = " (3 CP)",
                                    fontSize = if (isTablet) 7.sp else 5.8.sp,
                                    lineHeight = if (isTablet) 8.sp else 6.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canUseBarrage || isBarrageActive) BoardColors.CrimsonRed else BoardColors.TextMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            val playerBarrageText = if (!isFlagshipAlive) {
                                "Siz: Battı"
                            } else {
                                "Siz: ${battleState.playerRemainingLineBarrages}/${battleState.customRules.maxLineBarrages} Hak"
                            }
                            Text(
                                text = playerBarrageText,
                                fontSize = if (isTablet) 6.8.sp else 5.6.sp,
                                lineHeight = if (isTablet) 7.5.sp else 6.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!isFlagshipAlive) BoardColors.CrimsonRed else BoardColors.TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )

                            val enemyBarrageText = when {
                                !isEnemyFlagshipAlive -> "Rakip: Battı"
                                battleState.enemyRemainingLineBarrages == 0 -> "Rakip: 0/${battleState.customRules.maxLineBarrages}"
                                else -> "Rakip: ${battleState.enemyRemainingLineBarrages}/${battleState.customRules.maxLineBarrages} Hak"
                            }
                            val enemyBarrageColor = when {
                                !isEnemyFlagshipAlive -> BoardColors.CrimsonRed
                                battleState.enemyRemainingLineBarrages == 0 -> Color(0xFFDC2626)
                                else -> Color(0xFF15803D)
                            }
                            Text(
                                text = enemyBarrageText,
                                fontSize = if (isTablet) 6.8.sp else 5.6.sp,
                                lineHeight = if (isTablet) 7.5.sp else 6.2.sp,
                                fontWeight = FontWeight.Bold,
                                color = enemyBarrageColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Bombardıman aktifken hat doğrultusu değiştirici düğme (Hemen altında sağa hizalı)
                if (showBarrage && battleState.activeSpecialMode == SpecialActionMode.LINE_BARRAGE) {
                    val dirLabel = when (battleState.lineBarrageDirectionIndex % 3) {
                        0 -> "── Yatay"
                        1 -> "╱ Çapraz R"
                        else -> "╲ Çapraz L"
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .clickable { onCycleBarrageDir() },
                        color = BoardColors.PureWhite,
                        shape = RoundedCornerShape(5.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, BoardColors.CrimsonRed),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "YÖN: $dirLabel 🔄",
                            color = BoardColors.CrimsonRed,
                            fontSize = if (isTablet) 7.5.sp else 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class CelebrationParticle(
    val xFraction: Float,
    val initialYFraction: Float,
    val speedY: Float,
    val driftX: Float,
    val wobbleFreq: Float,
    val wobbleAmp: Float,
    val sizeDp: Float,
    val color: Color,
    val shapeType: Int, // 0: Ribbon, 1: Circle/Spark, 2: Diamond/Star
    val rotationSpeed: Float,
    val phaseOffset: Float
)

/**
 * Zafer ve Bozgun Sonuç Penceresi (Tam Ekran Konfeti / Kıvılcım Efektli & Müzik Entegreli Material 3 Tasarım)
 */
@Composable
private fun VictoryDefeatDialog(
    isWin: Boolean,
    gameMode: TacticalGameMode,
    turnNumber: Int,
    friendlyRemaining: Int,
    enemyRemaining: Int,
    onPlayAgain: () -> Unit,
    onBackToLobby: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Pencere açıldığında zafer veya bozgun müziğini 1 kez çal ve kazanılan rozetleri kaydet
    DisposableEffect(isWin) {
        if (isWin) {
            com.hexafleet.feature.board.ui.dialogs.HexaFleetAchievements.recordVictory(
                context = context,
                gameMode = gameMode,
                turnNumber = turnNumber,
                friendlyRemaining = friendlyRemaining
            )
            HexaFleetSoundManager.playVictoryMusic(context)
        } else {
            HexaFleetSoundManager.playDefeatMusic(context)
        }
        onDispose {
            HexaFleetSoundManager.stopMusic()
        }
    }

    // Sonsuz konfeti / kıvılcım ve madalyon nabız animasyonu
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "VictoryDefeatTransition")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 5200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "ConfettiProgress"
    )
    val medalPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "MedalPulse"
    )

    val particles = remember(isWin) {
        val winPalette = listOf(
            Color(0xFFF59E0B), // Altın
            Color(0xFFFACC15), // Parlak Sarı
            Color(0xFF8B5CF6), // Kraliyet Moru
            Color(0xFF06B6D4), // Siber Camgöbeği
            Color(0xFF10B981), // Zümrüt Yeşili
            Color(0xFFF43F5E), // Mercan Pembesi
            Color(0xFF3B82F6)  // Deniz Mavisi
        )
        val defeatPalette = listOf(
            Color(0xFFEF4444), // Kızıl Kor
            Color(0xFFF97316), // Turuncu Kıvılcım
            Color(0xFF991B1B), // Koyu Kırmızı
            Color(0xFF64748B), // Duman Grisi
            Color(0xFFF59E0B)  // Kehribar Kıvılcım
        )
        val palette = if (isWin) winPalette else defeatPalette
        val rng = kotlin.random.Random(if (isWin) 2026 else 9999)
        List(if (isWin) 95 else 50) { idx ->
            CelebrationParticle(
                xFraction = rng.nextFloat(),
                initialYFraction = rng.nextFloat(),
                speedY = if (isWin) (0.35f + rng.nextFloat() * 0.65f) else (0.20f + rng.nextFloat() * 0.40f),
                driftX = (rng.nextFloat() - 0.5f) * 0.18f,
                wobbleFreq = 2f + rng.nextFloat() * 4f,
                wobbleAmp = 0.015f + rng.nextFloat() * 0.035f,
                sizeDp = if (isWin) (6f + rng.nextFloat() * 7f) else (3.5f + rng.nextFloat() * 5f),
                color = palette[idx % palette.size],
                shapeType = idx % 3,
                rotationSpeed = (180f + rng.nextFloat() * 360f) * if (idx % 2 == 0) 1f else -1f,
                phaseOffset = rng.nextFloat()
            )
        }
    }

    val primaryAccent = if (isWin) BoardColors.PurplePrimary else BoardColors.CrimsonRed
    val secondaryAccent = if (isWin) Color(0xFFD97706) else Color(0xFF991B1B)

    val (rankBadgeText, rankBadgeBg, rankBadgeColor) = if (isWin) {
        when {
            friendlyRemaining >= 4 -> Triple("⭐ KUSURSUZ AMİRAL ZAFERİ (S+)", Color(0xFFFEF3C7), Color(0xFFB45309))
            friendlyRemaining == 3 -> Triple("🎖️ ÜSTÜN TAKTİK ZAFERİ (A+)", Color(0xFFF3E8FF), BoardColors.PurplePrimary)
            friendlyRemaining == 2 -> Triple("⚓ DENİZ HAKİMİYETİ ZAFERİ (A)", Color(0xFFE0F2FE), Color(0xFF0369A1))
            else -> Triple("🔥 ÇETİN MUHAREBE ZAFERİ (B+)", Color(0xFFDCFCE7), Color(0xFF15803D))
        }
    } else {
        Triple("⚠️ KRİTİK BOZGUN • YENİDEN KONUŞLANIN", Color(0xFFFFE4E6), BoardColors.CrimsonRed)
    }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 1. TAM EKRAN KONFETİ (ZAFER) VEYA KOR KIVILCIM & SONAR HALKA (BOZGUN) EFEKT KATMANI
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val w = size.width
                val h = size.height
                val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)

                // Merkezden yayılan kutlama / radar şok dalgası halkaları
                val ringRadius1 = ((animProgress * 1.4f) % 1f) * (w.coerceAtLeast(h) * 0.55f)
                val ringAlpha1 = (1f - ((animProgress * 1.4f) % 1f)).coerceIn(0f, 1f) * 0.25f
                drawCircle(
                    color = (if (isWin) Color(0xFFF59E0B) else BoardColors.CrimsonRed).copy(alpha = ringAlpha1),
                    radius = ringRadius1,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )

                val ringPhase2 = (animProgress + 0.5f) % 1f
                val ringRadius2 = ringPhase2 * (w.coerceAtLeast(h) * 0.50f)
                val ringAlpha2 = (1f - ringPhase2).coerceIn(0f, 1f) * 0.20f
                drawCircle(
                    color = primaryAccent.copy(alpha = ringAlpha2),
                    radius = ringRadius2,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )

                // Konfeti ve Kıvılcım Parçacıkları
                particles.forEach { p ->
                    val t = (animProgress * p.speedY + p.phaseOffset) % 1f
                    val yNorm = if (isWin) {
                        // Yukarıdan aşağıya süzülen konfetiler
                        (p.initialYFraction + t) % 1f
                    } else {
                        // Aşağıdan yukarıya yükselen kor kıvılcımlar
                        1f - ((p.initialYFraction + t) % 1f)
                    }
                    val wobble = kotlin.math.sin((t * p.wobbleFreq * 2f * Math.PI).toFloat()) * p.wobbleAmp
                    val xNorm = ((p.xFraction + p.driftX * t + wobble) % 1f + 1f) % 1f

                    val px = xNorm * w
                    val py = yNorm * h
                    val pSize = p.sizeDp.dp.toPx()
                    val alpha = if (isWin) 0.90f else (0.35f + 0.50f * (1f - t))

                    val angle = (t * p.rotationSpeed) % 360f
                    val tumbleScale = kotlin.math.abs(kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat().coerceAtLeast(0.25f)

                    when (p.shapeType) {
                        0 -> {
                            // 3B Dönen Konfeti Şeridi
                            drawRect(
                                color = p.color.copy(alpha = alpha),
                                topLeft = androidx.compose.ui.geometry.Offset(px - (pSize * tumbleScale) / 2f, py - pSize * 0.35f),
                                size = androidx.compose.ui.geometry.Size(pSize * tumbleScale, pSize * 0.7f)
                            )
                        }
                        1 -> {
                            // Parlak Küre / Kıvılcım
                            drawCircle(
                                color = p.color.copy(alpha = alpha),
                                radius = pSize * 0.42f,
                                center = androidx.compose.ui.geometry.Offset(px, py)
                            )
                        }
                        else -> {
                            // Elmas / Yıldız Parçacığı
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(px, py - pSize * 0.55f)
                                lineTo(px + pSize * 0.45f * tumbleScale, py)
                                lineTo(px, py + pSize * 0.55f)
                                lineTo(px - pSize * 0.45f * tumbleScale, py)
                                close()
                            }
                            drawPath(path = path, color = p.color.copy(alpha = alpha))
                        }
                    }
                }
            }

            // 2. ANA SONUÇ KARTI (Her ekran boyutuna duyarlı, homojen beyaz zeminli)
            Surface(
                modifier = Modifier
                    .widthIn(max = if (isTablet) 460.dp else 380.dp)
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = BoardColors.PureWhite,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = primaryAccent
                ),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isTablet) 24.dp else 18.dp,
                            vertical = if (isTablet) 22.dp else 18.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ÜST DERECELENDİRME KAPSÜLÜ
                    Surface(
                        color = rankBadgeBg,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, rankBadgeColor.copy(alpha = 0.35f)),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = rankBadgeText,
                            color = rankBadgeColor,
                            fontSize = if (isTablet) 9.5.sp else 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isTablet) 14.dp else 10.dp))

                    // ANİMASYONLU KUPA / ALARM MADALYONU
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(if (isTablet) 82.dp else 70.dp)
                    ) {
                        // Dış Hale
                        Box(
                            modifier = Modifier
                                .size((if (isTablet) 78.dp else 66.dp) * medalPulse)
                                .clip(CircleShape)
                                .background(
                                    if (isWin) Color(0xFFFEF3C7) else Color(0xFFFFE4E6)
                                )
                                .border(
                                    1.5.dp,
                                    if (isWin) Color(0xFFF59E0B).copy(alpha = 0.55f) else BoardColors.CrimsonRed.copy(alpha = 0.45f),
                                    CircleShape
                                )
                        )
                        // İç Madalyon
                        Box(
                            modifier = Modifier
                                .size(if (isTablet) 60.dp else 52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (isWin) {
                                            listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                        } else {
                                            listOf( Color(0xFFE11D48), Color(0xFF991B1B))
                                        }
                                    )
                                )
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isWin) Icons.Default.EmojiEvents else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isWin) Color(0xFFFDE047) else Color.White,
                                modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)
                            )
                        }
                    }

                    if (isWin) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "★   ★   ★",
                            color = Color(0xFFF59E0B),
                            fontSize = if (isTablet) 14.sp else 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isTablet) 8.dp else 6.dp))

                    // ANA BAŞLIK
                    Text(
                        text = if (isWin) "DENİZ ZAFERİ!" else "FİLO KAYBEDİLDİ!",
                        color = if (isWin) BoardColors.NavyDark else BoardColors.CrimsonRed,
                        fontSize = if (isTablet) 22.sp else 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // AÇIKLAMA METNİ
                    Text(
                        text = if (isWin)
                            "Tebrikler Komutan! Düşman donanmasındaki tüm gemileri batırarak harekât sahasının mutlak hakimiyetini ele geçirdiniz."
                        else
                            "Düşman donanması tüm filonuzu batırdı. Yeni bir taktiksel dizilim ve stratejiyle rövanşı alın!",
                        color = BoardColors.TextSecondary,
                        fontSize = if (isTablet) 12.sp else 11.sp,
                        lineHeight = if (isTablet) 17.sp else 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))

                    // 3 EŞİT SÜTUNLU TAKTİKSEL İSTATİSTİK KARTLARI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Toplam Tur
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "TOPLAM TUR",
                                    fontSize = if (isTablet) 8.5.sp else 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BoardColors.TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "$turnNumber",
                                    fontSize = if (isTablet) 18.sp else 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BoardColors.NavyDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // 2. Sağlam Gemimiz
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "SAĞLAM GEMİ",
                                    fontSize = if (isTablet) 8.5.sp else 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BoardColors.TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "$friendlyRemaining / 4",
                                    fontSize = if (isTablet) 18.sp else 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0284C7),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // 3. Düşman Gemisi
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "DÜŞMAN GEMİ",
                                    fontSize = if (isTablet) 8.5.sp else 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BoardColors.TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "$enemyRemaining / 4",
                                    fontSize = if (isTablet) 18.sp else 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BoardColors.CrimsonRed,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isTablet) 18.dp else 14.dp))

                    // ALT EYLEM BUTONLARI (Eşit Genişlik, Tek Satır, İkonlu)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                HexaFleetSoundManager.stopMusic()
                                onBackToLobby()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isTablet) 46.dp else 42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, BoardColors.BorderSubtle),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = BoardColors.NavyDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LOBİYE DÖN",
                                color = BoardColors.NavyDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isTablet) 11.5.sp else 10.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                HexaFleetSoundManager.stopMusic()
                                onPlayAgain()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isTablet) 46.dp else 42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YENİ MAÇ",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isTablet) 11.5.sp else 10.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Muharebe Günlüğü (Hamle Geçmişi) Modalı
 * User Requirement 5: Yapılan tüm atışlar, keşifler ve kalkan hamleleri detaylıca listelenir.
 */
@Composable
private fun CombatLogHistoryDialog(
    structuredLogs: List<CombatLogEntry>,
    turnNumber: Int,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filteredLogs = remember(structuredLogs, selectedFilter) {
        when (selectedFilter) {
            "FRIENDLY" -> structuredLogs.filter { it.isFriendly }
            "ENEMY" -> structuredLogs.filter { !it.isFriendly }
            else -> structuredLogs
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = BoardColors.PureWhite),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BoardColors.NavyDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Üst Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BoardColors.NavyAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = BoardColors.NavyDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MUHAREBE GÜNLÜĞÜ",
                                color = BoardColors.NavyDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Tur $turnNumber • Toplam ${structuredLogs.size} Taktiksel Olay",
                                color = BoardColors.TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = BoardColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filtreleme Sekmeleri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "ALL" to "TÜMÜ (${structuredLogs.size})",
                        "FRIENDLY" to "BİZİM (${structuredLogs.count { it.isFriendly }})",
                        "ENEMY" to "DÜŞMAN (${structuredLogs.count { !it.isFriendly }})"
                    )
                    for ((key, label) in tabs) {
                        val isSelected = selectedFilter == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = key },
                            color = if (isSelected) BoardColors.NavyDark else BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BoardColors.NavyDark else BoardColors.BorderSubtle
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else BoardColors.TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Log Listesi
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz bu kategoride kayıtlı muharebe hamlesi bulunmuyor.",
                            color = BoardColors.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val logScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(logScrollState)
                                .padding(bottom = 26.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (entry in filteredLogs) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (entry.isFriendly) Color(0xFFF8FAFC) else Color(0xFFFFF7F7),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (entry.isFriendly) BoardColors.NavyAccent.copy(alpha = 0.3f)
                                        else BoardColors.CrimsonRed.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    color = if (entry.isFriendly) BoardColors.NavyAccent else BoardColors.CrimsonRed,
                                                    shape = RoundedCornerShape(5.dp)
                                                ) {
                                                    Text(
                                                        text = entry.actor,
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Text(
                                                    text = "TUR ${entry.turnNumber}",
                                                    color = BoardColors.TextMuted,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )

                                                Text(
                                                    text = entry.displayCoordinate,
                                                    color = BoardColors.TextSecondary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Surface(
                                                color = when {
                                                    entry.resultTitle.contains("BATTI") || entry.resultTitle.contains("İMHA") -> BoardColors.CrimsonRed.copy(alpha = 0.15f)
                                                    entry.resultTitle.contains("İSABET") -> Color(0xFFFEF3C7)
                                                    entry.resultTitle.contains("KALKAN") || entry.resultTitle.contains("SÖNÜM") -> BoardColors.PurpleLight
                                                    else -> BoardColors.SurfaceSubtle
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = entry.resultTitle,
                                                    color = when {
                                                        entry.resultTitle.contains("BATTI") || entry.resultTitle.contains("İMHA") -> BoardColors.CrimsonRed
                                                        entry.resultTitle.contains("İSABET") -> Color(0xFFB45309)
                                                        entry.resultTitle.contains("KALKAN") || entry.resultTitle.contains("SÖNÜM") -> BoardColors.PurpleDark
                                                        else -> BoardColors.TextSecondary
                                                    },
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = entry.detailMessage,
                                            color = BoardColors.NavyDark,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Alt Kaydırma İpucu (Kompakt ve zarif)
                        if (logScrollState.canScrollForward) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                BoardColors.PureWhite.copy(alpha = 0.90f),
                                                BoardColors.PureWhite
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    color = BoardColors.NavyAccent.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, BoardColors.NavyAccent.copy(alpha = 0.35f)),
                                    modifier = Modifier.padding(bottom = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = BoardColors.NavyAccent,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "AŞAĞI KAYDIR",
                                            color = BoardColors.NavyAccent,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BoardColors.NavyDark)
                ) {
                    Text(
                        text = "KAPAT",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// =========================================================================================
// YARDIMCI KURULUM, GEMİ DİZİLİM VE AKILLI YAPAY ZEKA HEDEFLEME FONKSİYONLARI
// =========================================================================================

private val HEX_PRIMARY_AXES = listOf(
    HexCoordinate.DIRECTIONS[0], // 0: Doğu-Batı (──)
    HexCoordinate.DIRECTIONS[1], // 1: Kuzeydoğu-Güneybatı (╱)
    HexCoordinate.DIRECTIONS[2]  // 2: Kuzeybatı-Güneydoğu (╲)
)

private fun directionLabel(dirIndex: Int, isTriangle: Boolean = false): String =
    if (isTriangle) {
        when (Math.floorMod(dirIndex, 6)) {
            0 -> "Delta Doğu (►)"
            1 -> "Delta Kuzeydoğu (◥)"
            2 -> "Delta Kuzeybatı (◤)"
            3 -> "Delta Batı (◄)"
            4 -> "Delta Güneybatı (◣)"
            5 -> "Delta Güneydoğu (◢)"
            else -> "Delta Üçgen"
        }
    } else {
        when (Math.floorMod(dirIndex, 3)) {
            0 -> "Doğu-Batı (──)"
            1 -> "KD-GB Çapraz (╱)"
            else -> "KB-GD Çapraz (╲)"
        }
    }

private fun buildShipCoordinates(
    bow: HexCoordinate,
    length: Int,
    directionIndex: Int,
    isTriangle: Boolean = false
): List<HexCoordinate> {
    if (isTriangle) {
        val d = Math.floorMod(directionIndex, 6)
        val dir1 = HexCoordinate.DIRECTIONS[d]
        val dir2 = HexCoordinate.DIRECTIONS[(d + 1) % 6]
        return listOf(bow, bow + dir1, bow + dir2)
    }
    val dir = HEX_PRIMARY_AXES[Math.floorMod(directionIndex, 3)]
    return (0 until length).map { step -> bow + (dir * step) }
}

private fun isValidShipPlacement(
    parts: List<HexCoordinate>,
    shipId: String,
    allShips: List<ShipDisplayModel>,
    terrainMap: Map<HexCoordinate, TerrainType>,
    radius: Int
): Boolean {
    val occupiedByOthers = allShips.filter { it.id != shipId }.flatMap { it.parts }.toSet()
    return parts.all { coord ->
        coord.distanceTo(HexCoordinate.ZERO) <= radius &&
            coord !in occupiedByOthers &&
            terrainMap[coord] != TerrainType.CORAL_REEF
    }
}

private data class ShipSpec(
    val id: String,
    val name: String,
    val length: Int,
    val isTriangle: Boolean = false
)

private fun generateRandomFleet(
    isEnemy: Boolean,
    radius: Int,
    terrainMap: Map<HexCoordinate, TerrainType>
): List<ShipDisplayModel> {
    val suffix = if (isEnemy) "enemy" else "friendly"
    val specs = listOf(
        ShipSpec("corvette_$suffix", if (isEnemy) "Düşman Korveti" else "Hayalet Korvet", 2, isTriangle = false),
        ShipSpec("delta_$suffix", if (isEnemy) "Düşman Delta Kruvazörü" else "Delta Taarruz Kruvazörü", 3, isTriangle = true),
        ShipSpec("carrier_$suffix", if (isEnemy) "Düşman İHA Gemisi" else "İHA Gemisi", 3, isTriangle = false),
        ShipSpec("flagship_$suffix", if (isEnemy) "Düşman Amiral Gemisi" else "Amiral Gemisi", 4, isTriangle = false)
    )

    val allCoords = HexCoordinate.ZERO.area(radius)
    val placedShips = mutableListOf<ShipDisplayModel>()

    for (spec in specs) {
        var placed: ShipDisplayModel? = null
        val shuffledCoords = allCoords.shuffled()
        for (bow in shuffledCoords) {
            val dirs = if (spec.isTriangle) (0..5).toList().shuffled() else listOf(0, 1, 2).shuffled()
            for (d in dirs) {
                val candidateParts = buildShipCoordinates(bow, spec.length, d, isTriangle = spec.isTriangle)
                if (isValidShipPlacement(candidateParts, spec.id, placedShips, terrainMap, radius)) {
                    placed = ShipDisplayModel(
                        id = spec.id,
                        name = spec.name,
                        length = spec.length,
                        parts = candidateParts,
                        isEnemy = isEnemy,
                        isTriangleShape = spec.isTriangle,
                        directionIndex = d
                    )
                    break
                }
            }
            if (placed != null) break
        }
        if (placed != null) {
            placedShips.add(placed)
        }
    }
    return placedShips
}

/**
 * 3 Seviyeli Yapay Zeka (Recruit, Veteran, Grand Admiral) için Dost Harita üzerinde hedef seçici.
 */
private fun selectAiTargetOnPlayerBoard(
    state: DualBattleUiState,
    availableTargets: List<HexCoordinate>,
    difficulty: AiDifficulty
): HexCoordinate {
    // Henüz batmamış ama vurulmuş dost gemi parçaları (Aktif İsabetler)
    val activeHitCoords = state.friendlyShips
        .filter { !it.isSunk && it.hitParts.isNotEmpty() }
        .flatMap { it.hitParts }

    when (difficulty) {
        AiDifficulty.RECRUIT -> {
            // Acemi: %35 ihtimalle vurulan parçanın komşusunu yoklar, %65 rastgele atar
            val neighbors = activeHitCoords.flatMap { it.allNeighbors() }.filter { it in availableTargets }
            return if (neighbors.isNotEmpty() && Random.nextFloat() < 0.35f) {
                neighbors.random()
            } else {
                availableTargets.random()
            }
        }

        AiDifficulty.VETERAN -> {
            // Kıdemli Taktikçi: Aktif isabet varsa 6 komşuyu veya doğrultuyu takip eder; yoksa parite taraması yapar
            if (activeHitCoords.isNotEmpty()) {
                val neighbors = activeHitCoords.flatMap { it.allNeighbors() }.filter { it in availableTargets }
                if (neighbors.isNotEmpty()) return neighbors.random()
            }
            val parityTargets = availableTargets.filter { (it.q + it.r) % 2 == 0 }
            return if (parityTargets.isNotEmpty()) parityTargets.random() else availableTargets.random()
        }

        AiDifficulty.GRAND_ADMIRAL -> {
            // Büyük Amiral: 3 eksenli Olasılık Yoğunluk Haritası (H-PDF) ve Doğrultu Kilitlenmesi
            val remainingLengths = state.friendlyShips.filter { !it.isSunk }.map { it.length }
            val blocked = state.enemyShotsOnPlayer.filter { it.value != TileHighlightState.HIT }.keys +
                state.sharedReefCoords

            val scores = mutableMapOf<HexCoordinate, Int>()
            for (c in availableTargets) scores[c] = 0

            val allCoords = HexCoordinate.ZERO.area(state.radius)
            for (len in remainingLengths) {
                for (start in allCoords) {
                    for (dir in HEX_PRIMARY_AXES) {
                        val segs = (0 until len).map { step -> start + (dir * step) }
                        val valid = segs.all {
                            it.distanceTo(HexCoordinate.ZERO) <= state.radius && it !in blocked
                        }
                        if (valid) {
                            val hitCount = segs.count { it in activeHitCoords }
                            val weight = when {
                                hitCount >= 2 -> 80 // Aynı eksende 2 isabet varsa o hattın uçlarına kilitlen!
                                hitCount == 1 -> 20
                                else -> 1
                            }
                            for (s in segs) {
                                if (s in scores) {
                                    scores[s] = (scores[s] ?: 0) + weight
                                }
                            }
                        }
                    }
                }
            }

            val maxScore = scores.values.maxOrNull() ?: 0
            val bestCandidates = scores.filter { it.value == maxScore }.keys.toList()
            return if (bestCandidates.isNotEmpty()) bestCandidates.random() else availableTargets.random()
        }
    }
}

/**
 * Rastgele Ortak Mercan Resifleri Üreticisi (User Requirement 4)
 * İki oyuncu için de aynı konumlarda oluşur. Gemiler yerleştirilemez.
 */
private fun generateSharedReefs(radius: Int, count: Int): Set<HexCoordinate> {
    val candidates = HexCoordinate.ZERO.area(radius).filter {
        val dist = it.distanceTo(HexCoordinate.ZERO)
        dist in 1..(radius - 1)
    }.shuffled()

    val chosen = mutableSetOf<HexCoordinate>()
    for (coord in candidates) {
        if (chosen.none { it.distanceTo(coord) < 2 }) {
            chosen.add(coord)
            if (chosen.size >= count) break
        }
    }
    return chosen
}

/**
 * Rastgele Manyetik Fırtına Üreticisi (User Requirement 1)
 * Taraflar için ayrı ayrı bağımsız üretilir. Düşman tarafındaki fırtınalar oyuncu saldırı radarında gizlidir.
 */
private fun generateMagneticStorms(radius: Int, count: Int, excludeCoords: Set<HexCoordinate>): Set<HexCoordinate> {
    val candidates = HexCoordinate.ZERO.area(radius).filter {
        it !in excludeCoords && it.distanceTo(HexCoordinate.ZERO) in 1..radius
    }.shuffled()

    val chosen = mutableSetOf<HexCoordinate>()
    for (coord in candidates) {
        if (chosen.none { it.distanceTo(coord) < 2 }) {
            chosen.add(coord)
            if (chosen.size >= count) break
        }
    }
    return chosen
}

/**
 * Rastgele 2 Peteklik Radyoaktif Sis Bölgeleri Üreticisi (User Requirement 4)
 * İHA tespitlerini engelleyen kamuflaj bölgeleri.
 */
private fun generateRandomRadioactiveZones(
    radius: Int,
    count: Int,
    terrainMap: Map<HexCoordinate, TerrainType>
): List<RadioactiveZone> {
    if (count <= 0) return emptyList()
    val allCoords = HexCoordinate.ZERO.area(radius)
    val zones = mutableListOf<RadioactiveZone>()

    for (i in 0 until count) {
        var placed: RadioactiveZone? = null
        val candidates = allCoords.shuffled()
        for (bow in candidates) {
            val dirs = listOf(0, 1, 2).shuffled()
            for (d in dirs) {
                val parts = buildShipCoordinates(bow, 2, d)
                val valid = parts.all {
                    it.distanceTo(HexCoordinate.ZERO) <= radius &&
                        terrainMap[it] != TerrainType.CORAL_REEF
                }
                if (valid) {
                    placed = RadioactiveZone(
                        id = "radioactive_$i",
                        name = "Radyoaktif Bölge ${i + 1}",
                        parts = parts,
                        directionIndex = d
                    )
                    break
                }
            }
            if (placed != null) break
        }
        if (placed != null) zones.add(placed)
    }
    return zones
}

/**
 * İki Haritalı Başlangıç Durumu Üreticisi
 * User Requirement 4: 2'li Radyoaktif Sis Bölgeleri (R=5: 1, R=6: 2, R=7: 3 veya özel kural)
 * User Requirement 5: Kalkan limiti (Standart: 1, Devasa: 2 veya özel kural)
 * User Requirement 6: Oyun başında ayarlanabilir taktiksel parametreler (Kalkan 0-5, Radyoaktif 0-5, Resif 0-10, Fırtına 0-10)
 */
private fun createInitialDualBattleState(
    radius: Int,
    enableHazards: Boolean,
    customRules: GameCustomRules = GameCustomRules.defaultForRadius(radius)
): DualBattleUiState {
    val allCoords = HexCoordinate.ZERO.area(radius)

    val reefCount = if (enableHazards) customRules.coralReefCount else 0
    val stormCount = if (enableHazards) customRules.magneticStormCount else 0
    val radCount = customRules.radioactiveZonesCount

    val sharedReefs = if (reefCount > 0) generateSharedReefs(radius, reefCount) else emptySet()
    val friendlyStorms = if (stormCount > 0) generateMagneticStorms(radius, stormCount, sharedReefs) else emptySet()
    val enemyStorms = if (stormCount > 0) generateMagneticStorms(radius, stormCount, sharedReefs) else emptySet()

    val friendlyTerrainMap = allCoords.associateWith { c ->
        when (c) {
            in sharedReefs -> TerrainType.CORAL_REEF
            in friendlyStorms -> TerrainType.MAGNETIC_STORM
            else -> TerrainType.WATER
        }
    }

    val enemyTerrainMap = allCoords.associateWith { c ->
        when (c) {
            in sharedReefs -> TerrainType.CORAL_REEF
            in enemyStorms -> TerrainType.MAGNETIC_STORM
            else -> TerrainType.WATER
        }
    }

    val friendlyShips = generateRandomFleet(isEnemy = false, radius = radius, terrainMap = friendlyTerrainMap)
    val enemyShips = generateRandomFleet(isEnemy = true, radius = radius, terrainMap = enemyTerrainMap)

    val friendlyRad = generateRandomRadioactiveZones(radius, radCount, friendlyTerrainMap)
    val enemyRad = generateRandomRadioactiveZones(radius, radCount, enemyTerrainMap)

    return DualBattleUiState(
        radius = radius,
        phase = BattlePhase.DEPLOYMENT,
        customRules = customRules,
        friendlyTerrainMap = friendlyTerrainMap,
        enemyTerrainMap = enemyTerrainMap,
        sharedReefCoords = sharedReefs,
        friendlyRadioactiveZones = friendlyRad,
        enemyRadioactiveZones = enemyRad,
        friendlyShips = friendlyShips,
        enemyShips = enemyShips,
        selectedDeploymentShipId = friendlyShips.firstOrNull()?.id ?: "corvette_friendly",
        selectedAttackCoord = null,
        selectedTargetCoord = null,
        selectedShieldCoord = null,
        commandPoints = 3,
        maxCommandPoints = 3,
        structuredLogs = emptyList()
    )
}
