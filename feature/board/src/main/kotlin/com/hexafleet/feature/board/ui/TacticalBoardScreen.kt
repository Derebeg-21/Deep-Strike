package com.hexafleet.feature.board.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.hexafleet.feature.board.model.RadioactiveZone
import com.hexafleet.feature.board.model.ShieldDome
import com.hexafleet.feature.board.model.ShipDisplayModel
import com.hexafleet.feature.board.model.TacticalGameMode
import com.hexafleet.feature.board.model.TerrainType
import com.hexafleet.feature.board.model.TileHighlightState
import com.hexafleet.feature.board.theme.BoardColors
import com.hexafleet.feature.board.ui.dialogs.HexaFleetExitDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetInfoDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetSettingsDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetTopActions
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
    val soundManager = remember { HexaFleetSoundManager() }
    val scope = rememberCoroutineScope()

    // Üst Sağ Dialog Durumları
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCombatLogDialog by remember { mutableStateOf(false) }
    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    // Android Donanım/Sistem Geri Tuşu Yakalama (User Requirement 6)
    BackHandler(enabled = true) {
        showExitConfirmationDialog = true
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    // İki Haritalı Oyun Durumu
    var battleState by remember(mapRadius, enableHazards, customRules) {
        mutableStateOf(createInitialDualBattleState(mapRadius, enableHazards, customRules))
    }

    var isAiTurnInProgress by remember { mutableStateOf(false) }
    val aiKnownTargets = remember { mutableListOf<HexCoordinate>() }

    // Tur Süresi ve Geri Sayım Parametreleri (User Requirement 3)
    val effectiveTurnDuration = when (gameMode) {
        is TacticalGameMode.Online -> gameMode.turnDurationSeconds
        is TacticalGameMode.VsAi -> gameMode.turnDurationSeconds
    }.coerceAtLeast(15)

    var remainingTurnSeconds by remember(battleState.turnNumber, battleState.isPlayerTurn, battleState.phase) {
        mutableIntStateOf(effectiveTurnDuration)
    }

    // ==================== YAPAY ZEKA / RAKİP TAARRUZ MOTORU ====================
    fun triggerEnemyTurn() {
        if (battleState.winner != null || isAiTurnInProgress) return

        scope.launch {
            isAiTurnInProgress = true
            battleState = battleState.copy(
                isPlayerTurn = false,
                lastBannerMessage = "⚠️ DÜŞMAN DONANMASI SALDIRI HESAPLIYOR..."
            )
            delay(700)

            val difficulty = when (gameMode) {
                is TacticalGameMode.VsAi -> gameMode.difficulty
                is TacticalGameMode.Online -> AiDifficulty.VETERAN
            }

            var enemyCp = battleState.maxCommandPoints // 3 CP (Hiçbiri boşta bırakılmayacak!)
            val currentTurnAttacks = mutableListOf<HexCoordinate>()
            var currentTurnDroneCenter: HexCoordinate? = null

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

            // 2. ADIM: YAPAY ZEKA İHA KEŞFİ (Hedef bilinmiyorsa ve CP >= 3 ise - User Requirement 3)
            val radMistCoords = battleState.friendlyRadioactiveZones.flatMap { it.parts }.toSet()
            val shouldDeployDrone = enemyCp >= 3 &&
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

                // Gemi tespiti (Radyoaktif Yeşil Sis ve Manyetik Fırtına İHA'yı ENGELLER!)
                val activeFriendlyParts = battleState.friendlyShips.filter { !it.isSunk }.flatMap { it.parts }.toSet()
                var detectedCount = 0
                for (c in scanArea) {
                    val isCloaked = c in radMistCoords || battleState.friendlyTerrainMap[c] == TerrainType.MAGNETIC_STORM
                    if (!isCloaked && c in activeFriendlyParts && c !in battleState.enemyShotsOnPlayer.keys) {
                        if (c !in aiKnownTargets) {
                            aiKnownTargets.add(c)
                            detectedCount++
                        }
                    }
                }

                val droneMsg = if (detectedCount > 0) {
                    "📡 DÜŞMAN İHA KEŞFİ: Düşman İHA'sı (${droneCenter.q}, ${droneCenter.r}) bölgesinde filomuza ait $detectedCount sinyal tespit etti!"
                } else {
                    "📡 DÜŞMAN İHA KEŞFİ: Düşman İHA'sı (${droneCenter.q}, ${droneCenter.r}) çevresini taradı, sinyal alamadı."
                }

                val logEntry = CombatLogEntry(
                    turnNumber = battleState.turnNumber,
                    actor = "DÜŞMAN",
                    actionType = "📡 İHA",
                    coordinate = droneCenter,
                    resultTitle = if (detectedCount > 0) "TESPİT: $detectedCount" else "TEMİZ",
                    detailMessage = droneMsg + if (shieldHitOccurred) " (Dost kalkan dalgadan etkilendi!)" else ""
                )

                battleState = battleState.copy(
                    friendlyShieldDome = updatedPlayerDomes.firstOrNull(),
                    friendlyShieldDomes = updatedPlayerDomes,
                    lastBannerMessage = droneMsg,
                    battleLog = listOf(droneMsg) + battleState.battleLog,
                    structuredLogs = listOf(logEntry) + battleState.structuredLogs
                )
                soundManager.playRadarPing()
                delay(500)
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
                battleState = battleState.copy(
                    turnNumber = battleState.turnNumber + 1,
                    commandPoints = battleState.maxCommandPoints,
                    isPlayerTurn = true,
                    lastEnemyAttacks = currentTurnAttacks,
                    lastEnemyDroneCenter = currentTurnDroneCenter,
                    lastBannerMessage = "🎯 SIRA SİZDE (TUR ${battleState.turnNumber + 1}): Düşman radarına dokunup ateş edin!"
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
            val bannerMsg = "⏱️ SÜRE DOLDU: 3 rastgele hedefe acil durum salvosu atıldı, sıra devredildi."

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

    // Tur Süresi Geri Sayım Döngüsü
    LaunchedEffect(battleState.turnNumber, battleState.isPlayerTurn, battleState.phase, isAiTurnInProgress, battleState.winner) {
        if (battleState.phase == BattlePhase.COMBAT && battleState.isPlayerTurn && !isAiTurnInProgress && battleState.winner == null) {
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
                lastBannerMessage = "⚠️ (${target.q}, ${target.r}) koordinatına zaten ateş ettiniz! Başka bir petek seçin."
            )
            return
        }
        if (target in battleState.sharedReefCoords || battleState.enemyTerrainMap[target] == TerrainType.CORAL_REEF) {
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ Mercan resiflerine ateş edilemez! Açık deniz peteği seçin."
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

            logMsg = "🛡️ SÖNÜMLENDİ: (${target.q}, ${target.r}) peteğinde düşmanın Aegis Kalkanı mermiyi absorbe etti!"
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
                    logMsg = "🔥 DÜŞMAN GEMİSİ BATIRILDI! ${hitEnemy.name} (${hitEnemy.parts.size}B) tamamen imha edildi!"
                    structuredEntry = CombatLogEntry(
                        turnNumber = battleState.turnNumber,
                        actor = "SİZ",
                        actionType = "💥 GEMİ BATIRILDI",
                        coordinate = target,
                        resultTitle = "GEMİ BATTI",
                        detailMessage = logMsg
                    )
                } else {
                    newPlayerShots[target] = TileHighlightState.HIT
                    logMsg = "🎯 TAM İSABET! (${target.q}, ${target.r}) koordinatında düşman gemisi vuruldu!"
                    structuredEntry = CombatLogEntry(
                        turnNumber = battleState.turnNumber,
                        actor = "SİZ",
                        actionType = "🎯 ATEŞ",
                        coordinate = target,
                        resultTitle = "TAM İSABET",
                        detailMessage = logMsg
                    )
                }
            } else {
                soundManager.playWaterSplash()
                newPlayerShots[target] = TileHighlightState.MISS
                logMsg = "💧 KARAVANA: (${target.q}, ${target.r}) koordinatında su sıçradı, hedef boş."
                structuredEntry = CombatLogEntry(
                    turnNumber = battleState.turnNumber,
                    actor = "SİZ",
                    actionType = "🎯 ATEŞ",
                    coordinate = target,
                    resultTitle = "KARAVANA",
                    detailMessage = logMsg
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
        // Oyuncu fırtına ve sis olduğunu bilmez, düşmanda gizlidir.
        val enemyRadCoords = battleState.enemyRadioactiveZones.flatMap { it.parts }.toSet()
        val activeEnemyParts = battleState.enemyShips.filter { !it.isSunk }.flatMap { it.parts }.toSet()
        val newDroneMap = battleState.droneScannedHexes.toMutableMap()
        var detectedCount = 0

        for (c in scanArea) {
            val isEnemyStorm = battleState.enemyTerrainMap[c] == TerrainType.MAGNETIC_STORM
            val isEnemyRad = c in enemyRadCoords
            if (c !in battleState.playerShotsOnEnemy.keys) {
                if (!isEnemyStorm && !isEnemyRad && c in activeEnemyParts) {
                    newDroneMap[c] = TileHighlightState.RECON_DETECTED
                    detectedCount++
                } else {
                    newDroneMap[c] = TileHighlightState.RECON_CLEAR
                }
            }
        }

        val remainingCp = (battleState.commandPoints - 3).coerceAtLeast(0)
        val baseMsg = if (detectedCount > 0) {
            "📡 İHA KEŞFİ: (${center.q}, ${center.r}) çevresinde $detectedCount ADET DÜŞMAN BÖLMESİ TESPİT EDİLDİ!"
        } else {
            "📡 İHA KEŞFİ: (${center.q}, ${center.r}) bölgesi temiz, sinyal yok."
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
            lastBannerMessage = finalMsg,
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
                lastBannerMessage = "⚠️ Bu maçta kalkan kullanımı kural gereği devre dışıdır (Maks: 0)."
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
            lastBannerMessage = msg,
            battleLog = listOf(msg) + battleState.battleLog,
            structuredLogs = listOf(structuredEntry) + battleState.structuredLogs
        )

        if (remainingCp == 0) {
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
                    lastBannerMessage = "☢️ ${radAtHex.name} seçildi. Sürükleyerek taşıyın veya dokunarak döndürün."
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
                    lastBannerMessage = "⚓ ${shipAtHex.name} (${shipAtHex.length} Petek) seçildi. Sürükleyerek taşıyın veya dokunarak döndürün."
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
                    lastBannerMessage = "☢️ ${currentRad.name} (${clickedHex.q}, ${clickedHex.r}) koordinatına konuşlandırıldı!"
                )
            } else {
                haptics.playButtonClick()
                battleState = battleState.copy(
                    lastBannerMessage = "⚠️ Radyoaktif sis harita sınırları dışına veya mercan resifine taşamaz!"
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
                lastBannerMessage = "✅ ${currentShip.name} (${clickedHex.q}, ${clickedHex.r}) koordinatına konuşlandırıldı!"
            )
        } else {
            haptics.playButtonClick()
            battleState = battleState.copy(
                lastBannerMessage = "⚠️ ${currentShip.name} buraya sığmıyor veya başka bir gemi/resif ile çakışıyor!"
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
            lastBannerMessage = "🎲 Filo ve radyoaktif sis bölgeleri yeniden konuşlandırıldı!"
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
                remainingTurnSeconds = remainingTurnSeconds
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
                                lastBannerMessage = "⚔️ MUHAREBE BAŞLADI! Düşman Hedef Haritasında bir peteğe dokunup ateş edin!"
                            )
                        }
                    )
                } else {
                    DualBoardCombatWorkspace(
                        battleState = battleState,
                        isWideLayout = isWideLayout,
                        isAiThinking = isAiTurnInProgress,
                        onEnemyHexClicked = { clickedHex ->
                            haptics.playSonarTick()
                            soundManager.playRadarPing()
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
                                    lastBannerMessage = "🎯 HEDEF KİLİTLENDİ: (${clickedHex.q}, ${clickedHex.r}) — Ateş etmek için butona veya peteğe tekrar dokunun!"
                                )
                            }
                        },
                        onFriendlyHexClicked = { clickedHex ->
                            haptics.playSonarTick()
                            battleState = battleState.copy(
                                selectedShieldCoord = clickedHex,
                                lastBannerMessage = "🛡️ DOST SEKTÖR SEÇİLDİ: (${clickedHex.q}, ${clickedHex.r}) — Aegis Kalkanı kurmak için 'KALKAN (2 CP)' butonuna dokunun!"
                            )
                        },
                        onFire = {
                            battleState.selectedAttackCoord?.let { onPlayerFireAt(it) }
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
    remainingTurnSeconds: Int = 45,
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
                // Ergonomik Geri / Çıkış Butonu (Material 3 Touch Target)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(BoardColors.SurfaceSubtle, CircleShape)
                        .border(1.2.dp, BoardColors.BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Lobiye Dön",
                        tint = BoardColors.NavyDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // ÜST SATIR: BRANDING + MOD / TUR ROZETLERİ
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DEEP STRIKE",
                            color = BoardColors.NavyDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "•",
                            color = BoardColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (battleState.phase == BattlePhase.DEPLOYMENT) {
                            Surface(
                                color = BoardColors.PurpleLight,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, BoardColors.PurplePrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "⚓ FİLO KONUŞLANDIRMA",
                                    color = BoardColors.PurplePrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            // Tur Rozeti
                            Surface(
                                color = BoardColors.NavyDark,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "TUR ${battleState.turnNumber}",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Mod / Rakip Rozeti
                            val (badgeLabel, badgeBg, badgeTextColor) = when (gameMode) {
                                is TacticalGameMode.Online -> Triple("ODA: ${gameMode.roomCode}", BoardColors.PurpleLight, BoardColors.PurplePrimary)
                                is TacticalGameMode.VsAi -> when (gameMode.difficulty) {
                                    AiDifficulty.RECRUIT -> Triple("VS ACEMİ AI", Color(0xFFDCFCE7), Color(0xFF16A34A))
                                    AiDifficulty.VETERAN -> Triple("VS KIDEMLİ AI", BoardColors.PurpleLight, BoardColors.PurplePrimary)
                                    AiDifficulty.GRAND_ADMIRAL -> Triple("VS BÜYÜK AMİRAL AI", Color(0xFFFEE2E2), BoardColors.CrimsonRed)
                                }
                            }
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, badgeTextColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = badgeTextColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // ALT SATIR: KOMUT PUANI + GERİ SAYIM ZAMANLAYICISI
                    if (battleState.phase == BattlePhase.COMBAT) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isAiThinking) "DÜŞMAN SIRASI" else "KOMUT PUANI:",
                                color = if (isAiThinking) BoardColors.CrimsonRed else BoardColors.TextSecondary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            // Taktiksel CP Enerji Hücreleri
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                for (i in 0 until battleState.maxCommandPoints) {
                                    val active = i < battleState.commandPoints
                                    Box(
                                        modifier = Modifier
                                            .size(width = 16.dp, height = 7.dp)
                                            .clip(RoundedCornerShape(3.5.dp))
                                            .background(if (active) BoardColors.PurplePrimary else BoardColors.CommandPointEmpty)
                                            .border(0.6.dp, if (active) BoardColors.PurplePrimary else BoardColors.BorderSubtle, RoundedCornerShape(3.5.dp))
                                    )
                                }
                            }

                            Text(
                                text = "${battleState.commandPoints}/${battleState.maxCommandPoints} CP",
                                color = BoardColors.PurplePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )

                            // Tur Süresi Göstergesi (Canlı Geri Sayım Kapsülü)
                            if (battleState.isPlayerTurn && !isAiThinking) {
                                val isWarning = remainingTurnSeconds <= 10
                                Surface(
                                    color = if (isWarning) Color(0xFFFEE2E2) else BoardColors.PurpleLight,
                                    shape = RoundedCornerShape(7.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isWarning) BoardColors.CrimsonRed else BoardColors.PurplePrimary.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = "⏱️",
                                            fontSize = 9.sp
                                        )
                                        Text(
                                            text = "${remainingTurnSeconds}s",
                                            color = if (isWarning) BoardColors.CrimsonRed else BoardColors.PurplePrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Gemilerinizi konuşlandırın ve yönlerini belirleyin",
                            color = BoardColors.TextSecondary,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
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
    remainingTurnSeconds: Int = 45
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
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, containerBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SOL DURUM VE SIRA ROZETİ (Cyber Status Badge)
            if (isEnemyTurn) {
                Surface(
                    color = BoardColors.CrimsonRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "DÜŞMAN SIRASI",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else {
                Surface(
                    color = Color(0xFF15803D), // Taktik Canlı Yeşil
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF86EFAC))
                        )
                        Text(
                            text = "SIRA SİZDE (TUR $turnNumber | ⏱️ ${remainingTurnSeconds}s)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Temizlenmiş mesaj metni (Tekrarlayan 'SIRA SİZDE' ibarelerini temizleyip doğrudan taktiksel yönlendirmeyi verir)
            val cleanedMessage = message
                .replace(Regex("^🎯?\\s*S[İI]RA\\s+S[İI]ZDE[^:]*:\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^⚡?\\s*DÜŞMAN\\s+SIRASI[^:]*:\\s*", RegexOption.IGNORE_CASE), "")

            Text(
                text = cleanedMessage,
                color = if (isEnemyTurn) Color(0xFF991B1B) else BoardColors.NavyDark,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    onStartCombat: () -> Unit
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
                title = "⚓ DOST FİLO HARİTASI (YERLEŞTİRME)",
                subtitle = "Gemi veya sis sürükleyin veya dokunarak yerleştirin",
                badgeText = "${battleState.friendlyShips.size}/4 GEMİ",
                badgeColor = BoardColors.NavyAccent,
                borderColor = BoardColors.NavyAccent,
                modifier = Modifier.weight(0.55f).fillMaxWidth()
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
                modifier = Modifier.weight(0.45f).fillMaxWidth()
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
                text = "DONANMA TERSANESİ (GEMİ SEÇ & DÖNDÜR)",
                color = BoardColors.NavyDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Bir gemi veya radyoaktif sis seçin, yönünü belirleyin ve haritaya dokunun.",
                color = BoardColors.TextSecondary,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(10.dp))

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
                            .padding(vertical = 3.5.dp)
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                                Column {
                                    Text(
                                        text = "${ship.name} (${ship.length} Petek)",
                                        color = if (isSelected) Color(0xFF854D0E) else BoardColors.NavyDark,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Yön: ${directionLabel(ship.directionIndex, isTriangle = ship.isTriangleShape)}",
                                        color = if (isSelected) Color(0xFFA16207) else BoardColors.TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            // Gemi boyutu kutucukları
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

                // Radyoaktif Sis Bölgesi Seçim Kartları (User Requirement 4)
                if (battleState.friendlyRadioactiveZones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "RADYOAKTİF SİS BÖLGELERİ (İHA KAMUFLAJI - 2 PETEK)",
                        color = Color(0xFF15803D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "☢️ ${radZone.name} (2 Petek)",
                                        color = if (isSelected) Color(0xFF14532D) else BoardColors.NavyDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Yön: ${directionLabel(radZone.directionIndex)}",
                                        color = BoardColors.TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
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

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Aşağıda nesne olduğunu belirten flu animasyonlu "AŞAĞI KAYDIR" butonu (User Requirement 6: Turkuaz & Sarı tonları, Flu cam)
                if (scrollState.canScrollForward) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo((scrollState.value + 160).coerceAtMost(scrollState.maxValue))
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        color = Color(0xC0083344), // Flu derin turkuaz cam gövde
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, Color(0xFF22D3EE).copy(alpha = 0.85f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Aşağı Kaydır",
                                tint = Color(0xFF22D3EE), // Canlı turkuaz neon ok
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "AŞAĞI KAYDIR",
                                color = Color(0xFFFDE047), // Okunabilir, yüksek kontrastlı siber sarı metin
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Yukarıda nesne olduğunu belirten flu animasyonlu "YUKARI KAYDIR" butonu (User Requirement 6: Turkuaz & Sarı tonları, Flu cam)
                if (scrollState.canScrollBackward) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo((scrollState.value - 160).coerceAtLeast(0))
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp),
                        color = Color(0xC0083344), // Flu derin turkuaz cam gövde
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, Color(0xFF22D3EE).copy(alpha = 0.85f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Yukarı Kaydır",
                                tint = Color(0xFF22D3EE), // Canlı turkuaz neon ok
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "YUKARI KAYDIR",
                                color = Color(0xFFFDE047), // Okunabilir, yüksek kontrastlı siber sarı metin
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. SABİT ALT AKSİYONLAR: Yön Döndür & Rastgele Diz Butonları (User Requirement 2: Estetik Taktiksel İkonlar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRotate,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BoardColors.PurplePrimary)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(BoardColors.PurpleLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = BoardColors.PurplePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "YÖN DÖNDÜR",
                        color = BoardColors.PurplePrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = onRandomize,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BoardColors.NavyAccent)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = BoardColors.NavyAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "RASTGELE DİZ",
                        color = BoardColors.NavyAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Savaşı Başlat Butonu
            Button(
                onClick = onStartCombat,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BoardColors.CrimsonRed)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FİLOYU ONAYLA VE SAVAŞA BAŞLA",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
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
                    HexBoardCanvas(
                        state = enemyAttackBoardState,
                        modifier = Modifier.fillMaxSize(),
                        onHexClicked = onEnemyHexClicked
                    )
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ÜST HARİTA: DÜŞMAN HEDEF RADARI (SALDIRI SAHASI)
            BoardContainerCard(
                title = "🎯 DÜŞMAN HEDEF RADARI",
                subtitle = "Hedef seçmek ve vurmak için peteğe dokunun",
                badgeText = "DÜŞMAN: $aliveEnemyShips/4",
                badgeColor = BoardColors.CrimsonRed,
                borderColor = BoardColors.CrimsonRed.copy(alpha = 0.65f),
                isAttackBoard = true,
                footerContent = {
                    FleetMiniStatusRow(
                        label = "DÜŞMAN:",
                        ships = battleState.enemyShips,
                        activeColor = BoardColors.CrimsonRed
                    )
                },
                modifier = Modifier.weight(0.46f).fillMaxWidth()
            ) {
                HexBoardCanvas(
                    state = enemyAttackBoardState,
                    modifier = Modifier.fillMaxSize(),
                    onHexClicked = onEnemyHexClicked
                )
            }

            // ORTA KOMUTA KONSOLU
            CombatActionConsole(
                selectedCoord = battleState.selectedAttackCoord,
                commandPoints = battleState.commandPoints,
                isPlayerTurn = battleState.isPlayerTurn && !isAiThinking,
                onFire = onFire,
                onDroneScan = onDroneScan,
                onShield = onShield,
                onEndTurn = onEndTurn,
                onOpenLog = onOpenCombatLog
            )

            // ALT HARİTA: DOST FİLO HARİTASI (SENİN GEMİLERİN)
            BoardContainerCard(
                title = "🛡️ DOST FİLO HARİTASI",
                subtitle = "Gemilerinizin durumu, kalkan ve düşman atışları",
                badgeText = "DOST: $aliveFriendlyShips/4",
                badgeColor = BoardColors.NavyAccent,
                borderColor = BoardColors.NavyAccent.copy(alpha = 0.65f),
                isAttackBoard = false,
                footerContent = {
                    FleetMiniStatusRow(
                        label = "FİLOMUZ:",
                        ships = battleState.friendlyShips,
                        activeColor = BoardColors.NavyAccent
                    )
                },
                modifier = Modifier.weight(0.40f).fillMaxWidth()
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
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Sol Renk Çizgisi Vurgusu
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isAttackBoard) BoardColors.CrimsonRed else BoardColors.NavyAccent)
                    )

                    // İkon Rozeti
                    Box(
                        modifier = Modifier
                            .size(28.dp)
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
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = BoardColors.NavyDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            color = BoardColors.TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Sağ Durum Rozeti (Cyber-Tactical Status Badge - User Requirement 6)
                Surface(
                    color = if (isAttackBoard) Color(0xFFFFF1F2) else Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, badgeColor.copy(alpha = 0.65f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Orta Harita Alanı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp),
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
                        .padding(horizontal = 10.dp, vertical = 5.dp)
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
    onFire: () -> Unit,
    onDroneScan: () -> Unit,
    onShield: () -> Unit,
    onEndTurn: () -> Unit,
    onOpenLog: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BoardColors.PureWhite,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seçili Koordinat Rozeti
            Surface(
                color = BoardColors.SurfaceSubtle,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NİŞANGAH",
                        color = BoardColors.TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (selectedCoord != null) "(${selectedCoord.q}, ${selectedCoord.r})" else "SEÇİN",
                        color = if (selectedCoord != null) BoardColors.CrimsonRed else BoardColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 1. ATEŞ ET (1 CP)
            Button(
                onClick = onFire,
                enabled = isPlayerTurn && selectedCoord != null && commandPoints >= 1,
                modifier = Modifier.weight(1.3f).height(40.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BoardColors.CrimsonRed,
                    disabledContainerColor = BoardColors.SurfaceSubtle
                )
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isPlayerTurn && selectedCoord != null && commandPoints >= 1) Color.White else BoardColors.TextMuted
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ATEŞ ET (1 CP)",
                    color = if (isPlayerTurn && selectedCoord != null && commandPoints >= 1) Color.White else BoardColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 2. İHA KEŞFİ (3 CP - User Requirement 3)
            Button(
                onClick = onDroneScan,
                enabled = isPlayerTurn && selectedCoord != null && commandPoints >= 3,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BoardColors.PurplePrimary,
                    disabledContainerColor = BoardColors.SurfaceSubtle
                )
            ) {
                Text(
                    text = "İHA (3 CP)",
                    color = if (isPlayerTurn && selectedCoord != null && commandPoints >= 3) Color.White else BoardColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 3. AEGIS KALKANI (2 CP)
            Button(
                onClick = onShield,
                enabled = isPlayerTurn && commandPoints >= 2,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BoardColors.NavyAccent,
                    disabledContainerColor = BoardColors.SurfaceSubtle
                )
            ) {
                Text(
                    text = "KALKAN (2 CP)",
                    color = if (isPlayerTurn && commandPoints >= 2) Color.White else BoardColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 4. TURU BİTİR
            OutlinedButton(
                onClick = onEndTurn,
                enabled = isPlayerTurn,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(9.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.NavyDark)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Turu Bitir", tint = BoardColors.NavyDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "TUR BİTİR",
                    color = BoardColors.NavyDark,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 5. MUHAREBE GÜNLÜĞÜ BUTONU (User Requirement 5)
            IconButton(
                onClick = onOpenLog,
                modifier = Modifier
                    .size(40.dp)
                    .background(BoardColors.SurfaceSubtle, RoundedCornerShape(9.dp))
                    .border(1.dp, BoardColors.BorderSubtle, RoundedCornerShape(9.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = "Muharebe Günlüğü",
                    tint = BoardColors.NavyDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Zafer ve Bozgun Sonuç Penceresi
 */
@Composable
private fun VictoryDefeatDialog(
    isWin: Boolean,
    turnNumber: Int,
    friendlyRemaining: Int,
    enemyRemaining: Int,
    onPlayAgain: () -> Unit,
    onBackToLobby: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BoardColors.PureWhite),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (isWin) BoardColors.PurplePrimary else BoardColors.CrimsonRed
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (isWin) BoardColors.PurpleLight else BoardColors.RedLight,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWin) Icons.Default.EmojiEvents else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isWin) BoardColors.PurplePrimary else BoardColors.CrimsonRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isWin) "DENİZ ZAFERİ!" else "FİLO KAYBEDİLDİ!",
                    color = if (isWin) BoardColors.NavyDark else BoardColors.CrimsonRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isWin)
                        "Tebrikler Komutan! Düşman donanmasındaki tüm gemileri batırarak harekât sahasının hakimiyetini ele geçirdiniz."
                    else
                        "Düşman donanması tüm filonuzu batırdı. Yeni bir taktiksel dizilimle rövanşı alın!",
                    color = BoardColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOPLAM TUR", fontSize = 9.sp, color = BoardColors.TextSecondary, fontFamily = FontFamily.Monospace)
                            Text("$turnNumber", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BoardColors.NavyDark, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SAĞLAM GEMİMİZ", fontSize = 9.sp, color = BoardColors.TextSecondary, fontFamily = FontFamily.Monospace)
                            Text("$friendlyRemaining/4", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BoardColors.NavyAccent, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DÜŞMAN GEMİSİ", fontSize = 9.sp, color = BoardColors.TextSecondary, fontFamily = FontFamily.Monospace)
                            Text("$enemyRemaining/4", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BoardColors.CrimsonRed, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToLobby,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("LOBİYE DÖN", color = BoardColors.NavyDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWin) BoardColors.PurplePrimary else BoardColors.CrimsonRed
                        )
                    ) {
                        Text("YENİ MAÇ", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
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

                        // Alt Kaydırma İpucu (User Requirement 4)
                        if (logScrollState.canScrollForward) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                BoardColors.PureWhite.copy(alpha = 0.95f),
                                                BoardColors.PureWhite
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    color = BoardColors.NavyAccent.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.NavyAccent.copy(alpha = 0.35f)),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = BoardColors.NavyAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "AŞAĞI KAYDIRIN",
                                            color = BoardColors.NavyAccent,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = BoardColors.NavyAccent,
                                            modifier = Modifier.size(13.dp)
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
