package com.hexafleet.feature.lobby.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.collectAsState
import com.hexafleet.data.multiplayer.FirebaseLobbyRepository
import com.hexafleet.domain.game.multiplayer.LobbyRoom
import com.hexafleet.domain.game.ai.AiDifficulty
import com.hexafleet.domain.game.multiplayer.RoomCodeGenerator
import com.hexafleet.feature.board.model.GameCustomRules
import com.hexafleet.feature.board.settings.HexaFleetSettings
import com.hexafleet.feature.board.theme.BoardColors
import com.hexafleet.feature.board.ui.dialogs.HexaFleetExitDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetInfoDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetSettingsDialog
import com.hexafleet.feature.board.ui.dialogs.HexaFleetTopActions
import kotlinx.coroutines.delay

/**
 * HexaFleet Çok Oyunculu Eşleşme, Lobi Gözlemleme ve Yapay Zeka Operasyon Ekranı.
 * Tasarım Standartı: Beyaz Taban, Lacivert, Mor ve Kırmızı renk senkronizasyonu.
 */
@Composable
fun LobbyScreen(
    onStartOnlineGame: (roomCode: String, isHost: Boolean, turnDuration: Int, mapRadius: Int, enableHazards: Boolean, customRules: GameCustomRules) -> Unit,
    onStartAiGame: (difficulty: AiDifficulty, mapRadius: Int, enableHazards: Boolean, customRules: GameCustomRules) -> Unit,
    modifier: Modifier = Modifier,
    initialJoinCode: String? = null
) {
    val clipboardManager = LocalClipboardManager.current

    // Üst Dialog Görünürlük Durumları
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Ana Mod Seçimi: 0 -> Çevrimiçi Savaş, 1 -> Yapay Zekaya Karşı
    var mainGameModeTab by remember { mutableIntStateOf(0) }

    // Çevrimiçi Alt Sekme: 0 -> Oda Kur (Host), 1 -> Odaya Katıl (Join), 2 -> Lobi Odaları (Lobby)
    var selectedOnlineSubTab by remember(initialJoinCode) {
        mutableIntStateOf(if (!initialJoinCode.isNullOrBlank()) 1 else 0)
    }

    val publicRooms by FirebaseLobbyRepository.instance.observePublicRooms().collectAsState(
        initial = FirebaseLobbyRepository.instance.getActivePublicRooms()
    )

    // Oda Kurma Ayarları
    var generatedRoomCode by remember {
        mutableStateOf(
            RoomCodeGenerator.generateUnique(
                FirebaseLobbyRepository.instance.getActivePublicRooms().map { it.roomCode }.toSet()
            )
        )
    }
    var isPinEnabled by remember { mutableStateOf(false) }
    var hostPin by remember { mutableStateOf("") }
    var isPrivateRoom by remember { mutableStateOf(false) }
    var roomTurnDuration by remember { mutableIntStateOf(HexaFleetSettings.defaultTurnDurationSeconds) }
    var roomMapRadius by remember { mutableIntStateOf(HexaFleetSettings.defaultMapRadius) }
    var roomEnableHazards by remember { mutableStateOf(HexaFleetSettings.areTerrainHazardsEnabled) }
    var copyFeedback by remember { mutableStateOf(false) }

    // Oda Kur Özel Taktiksel Parametreleri (User Requirement 6)
    var hostMaxShields by remember(roomMapRadius) { mutableIntStateOf(if (roomMapRadius >= 7) 2 else 1) }
    var hostRadioactiveZones by remember(roomMapRadius) { mutableIntStateOf(when { roomMapRadius <= 5 -> 1; roomMapRadius == 6 -> 2; else -> 3 }) }
    var hostCoralReefs by remember(roomMapRadius) { mutableIntStateOf(when { roomMapRadius <= 5 -> 4; roomMapRadius == 6 -> 5; else -> 6 }) }
    var hostMagneticStorms by remember(roomMapRadius) { mutableIntStateOf(when { roomMapRadius <= 5 -> 4; roomMapRadius == 6 -> 6; else -> 8 }) }
    var hostMaxEmpStrikes by remember { mutableIntStateOf(1) }
    var hostMaxLineBarrages by remember { mutableIntStateOf(1) }

    // Taktiksel Parametre Bilgi Pop-Up Durumu (User Requirement 3)
    var activeTacticalInfoParam by remember { mutableStateOf<TacticalParamInfo?>(null) }

    // Lobi Gözlemleme / Bekleme Durumu (Host veya Katılımcı için)
    var isWaitingInLobby by remember { mutableStateOf(false) }
    var activeWaitingCode by remember { mutableStateOf("") }
    var isGuestConnected by remember { mutableStateOf(false) }
    var matchCountdown by remember { mutableStateOf<Int?>(null) }

    // Odaya Katılma Girdileri
    var inputCode by remember(initialJoinCode) { mutableStateOf(initialJoinCode ?: "") }
    var inputPin by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

    // Yapay Zeka Ayarları
    var selectedAiDifficulty by remember { mutableStateOf(AiDifficulty.VETERAN) }
    var aiMapRadius by remember { mutableIntStateOf(HexaFleetSettings.defaultMapRadius) }
    var aiEnableHazards by remember { mutableStateOf(HexaFleetSettings.areTerrainHazardsEnabled) }

    // Yapay Zeka Özel Taktiksel Parametreleri (User Requirement 6)
    var aiMaxShields by remember(aiMapRadius) { mutableIntStateOf(if (aiMapRadius >= 7) 2 else 1) }
    var aiRadioactiveZones by remember(aiMapRadius) { mutableIntStateOf(when { aiMapRadius <= 5 -> 1; aiMapRadius == 6 -> 2; else -> 3 }) }
    var aiCoralReefs by remember(aiMapRadius) { mutableIntStateOf(when { aiMapRadius <= 5 -> 4; aiMapRadius == 6 -> 5; else -> 6 }) }
    var aiMagneticStorms by remember(aiMapRadius) { mutableIntStateOf(when { aiMapRadius <= 5 -> 4; aiMapRadius == 6 -> 6; else -> 8 }) }
    var aiMaxEmpStrikes by remember { mutableIntStateOf(1) }
    var aiMaxLineBarrages by remember { mutableIntStateOf(1) }

    // Lobi Bekleme Odasında 2. Oyuncu bağlandığında geri sayım
    LaunchedEffect(isGuestConnected) {
        if (isGuestConnected && isWaitingInLobby) {
            matchCountdown = 3
            while (matchCountdown != null && matchCountdown!! > 0) {
                delay(1000)
                matchCountdown = matchCountdown!! - 1
            }
            // Geri sayım bitti: 2. kişi bağlandı ve oyun başlıyor!
            val hostRules = GameCustomRules(
                maxShieldDomes = hostMaxShields,
                radioactiveZonesCount = hostRadioactiveZones,
                coralReefCount = hostCoralReefs,
                magneticStormCount = hostMagneticStorms,
                maxEmpStrikes = hostMaxEmpStrikes,
                maxLineBarrages = hostMaxLineBarrages
            )
            onStartOnlineGame(activeWaitingCode, true, roomTurnDuration, roomMapRadius, roomEnableHazards, hostRules)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BoardColors.LightBackground)
    ) {
        val isTabletLayout = maxWidth >= 640.dp || maxWidth > maxHeight

        // 4. Resim: Deep Strike Arka Plan Görseli (%75 Flu / Saydam, Tüm Ekranı Kaplayan)
        Image(
            painter = painterResource(com.hexafleet.feature.lobby.R.drawable.deep_strike_lobby_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.26f),
            contentScale = ContentScale.Crop
        )

        // Çok hafif atmosferik degrade (Arka plan görselinin net görünmesini sağlar)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x180F172A),
                            Color(0x080F172A),
                            Color(0x200F172A)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTabletLayout) 20.dp else 12.dp, vertical = if (isTabletLayout) 10.dp else 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================== ÜST HEADER VE AKSİYONLAR ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(BoardColors.NavyDark, RoundedCornerShape(10.dp))
                            .border(1.5.dp, BoardColors.PurplePrimary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DS",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DEEP STRIKE",
                            color = BoardColors.NavyDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "TACTICAL NAVAL WARFARE",
                            color = BoardColors.PurplePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // Sağ Üst Info, Ayarlar, Kapatma Butonları
                HexaFleetTopActions(
                    onOpenInfo = { showInfoDialog = true },
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenExit = { showExitDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== ANA OYUN MODU SEÇİCİ ====================
            // ==================== ANA OYUN MODU SEÇİCİ (User Requirement 4: Neon Tasarım) ====================
            if (!isWaitingInLobby) {
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // TAB 0: ÇEVRİMİÇİ SAVAŞ (Neon Mor Vurgu)
                        val isOnlineSelected = mainGameModeTab == 0
                        Surface(
                            onClick = { mainGameModeTab = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isOnlineSelected) Color(0xFFF5F3FF) else Color.Transparent,
                            border = if (isOnlineSelected) {
                                androidx.compose.foundation.BorderStroke(1.8.dp, Color(0xFF8B5CF6))
                            } else null,
                            shadowElevation = if (isOnlineSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isOnlineSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF8B5CF6))
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                }
                                Image(
                                    painter = painterResource(com.hexafleet.feature.lobby.R.drawable.ic_gamepad_multiplayer),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    contentScale = ContentScale.Fit,
                                    alpha = if (isOnlineSelected) 1f else 0.65f
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ÇEVRİMİÇİ SAVAŞ",
                                    fontWeight = if (isOnlineSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.4.sp,
                                    color = if (isOnlineSelected) Color(0xFF7C3AED) else BoardColors.TextSecondary
                                )
                            }
                        }

                        // TAB 1: YAPAY ZEKAYA KARŞI (Neon Kırmızı Vurgu)
                        val isAiSelected = mainGameModeTab == 1
                        Surface(
                            onClick = { mainGameModeTab = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAiSelected) Color(0xFFFFF1F2) else Color.Transparent,
                            border = if (isAiSelected) {
                                androidx.compose.foundation.BorderStroke(1.8.dp, Color(0xFFF43F5E))
                            } else null,
                            shadowElevation = if (isAiSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isAiSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF43F5E))
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                }
                                Image(
                                    painter = painterResource(com.hexafleet.feature.lobby.R.drawable.ic_ai_robot_avatar),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "YAPAY ZEKAYA KARŞI",
                                    fontWeight = if (isAiSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.4.sp,
                                    color = if (isAiSelected) Color(0xFFE11D48) else BoardColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // ==================== MOD 1: ÇEVRİMİÇİ SAVAŞ (ONLINE) ====================
            if (mainGameModeTab == 0) {
                if (isWaitingInLobby) {
                    // LOBİ GÖZLEMLEME VE BEKLEME MODU (Requirement 2 & 3)
                    LobbyWaitingRoomView(
                        roomCode = activeWaitingCode,
                        isGuestConnected = isGuestConnected,
                        countdown = matchCountdown,
                        turnDuration = roomTurnDuration,
                        mapRadius = roomMapRadius,
                        hazardsEnabled = roomEnableHazards,
                        isPinProtected = isPinEnabled,
                        onSimulateGuestJoin = {
                            isGuestConnected = true
                        },
                        onCancelLobby = {
                            isWaitingInLobby = false
                            isGuestConnected = false
                            matchCountdown = null
                        },
                        onCopyCode = {
                            clipboardManager.setText(AnnotatedString(activeWaitingCode))
                            copyFeedback = true
                        },
                        copyFeedback = copyFeedback
                    )
                } else {
                    // ODA KUR, ODAYA KATIL VEYA LOBİ ODALARI (User Requirements 4 & 5)
                    TabRow(
                        selectedTabIndex = selectedOnlineSubTab,
                        containerColor = BoardColors.SurfaceSubtle,
                        contentColor = BoardColors.NavyDark,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedOnlineSubTab]),
                                color = BoardColors.NavyAccent
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BoardColors.BorderSubtle, RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedOnlineSubTab == 0,
                            onClick = { selectedOnlineSubTab = 0 },
                            text = {
                                Text(
                                    "ODA KUR (HOST)",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (selectedOnlineSubTab == 0) BoardColors.NavyDark else BoardColors.TextSecondary
                                )
                            }
                        )
                        Tab(
                            selected = selectedOnlineSubTab == 1,
                            onClick = { selectedOnlineSubTab = 1 },
                            text = {
                                Text(
                                    "ODAYA KATIL (JOIN)",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (selectedOnlineSubTab == 1) BoardColors.NavyDark else BoardColors.TextSecondary
                                )
                            }
                        )
                        Tab(
                            selected = selectedOnlineSubTab == 2,
                            onClick = { selectedOnlineSubTab = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "LOBİ ODALARI",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (selectedOnlineSubTab == 2) BoardColors.NavyDark else BoardColors.TextSecondary
                                    )
                                    if (publicRooms.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Surface(
                                            color = BoardColors.PurplePrimary,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${publicRooms.size}",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedOnlineSubTab) {
                        0 -> {
                            // ODA KURMA KARTI (Host Ayarları)
                            HostRoomConfigCard(
                                roomCode = generatedRoomCode,
                                isPinEnabled = isPinEnabled,
                                pin = hostPin,
                                isPrivate = isPrivateRoom,
                                turnDuration = roomTurnDuration,
                                mapRadius = roomMapRadius,
                                enableHazards = roomEnableHazards,
                                copyFeedback = copyFeedback,
                                maxShields = hostMaxShields,
                                radioactiveZones = hostRadioactiveZones,
                                coralReefs = hostCoralReefs,
                                magneticStorms = hostMagneticStorms,
                                maxEmpStrikes = hostMaxEmpStrikes,
                                maxLineBarrages = hostMaxLineBarrages,
                                isTablet = isTabletLayout,
                                onTogglePin = { isPinEnabled = it },
                                onPinChanged = { if (it.length <= 4) hostPin = it },
                                onTogglePrivate = { isPrivateRoom = it },
                                onDurationChanged = { roomTurnDuration = it },
                                onRadiusChanged = { roomMapRadius = it },
                                onHazardsChanged = { roomEnableHazards = it },
                                onMaxShieldsChanged = { hostMaxShields = it },
                                onRadioactiveZonesChanged = { hostRadioactiveZones = it },
                                onCoralReefsChanged = { hostCoralReefs = it },
                                onMagneticStormsChanged = { hostMagneticStorms = it },
                                onMaxEmpStrikesChanged = { hostMaxEmpStrikes = it },
                                onMaxLineBarragesChanged = { hostMaxLineBarrages = it },
                                onInfoClick = { activeTacticalInfoParam = getTacticalParamInfo(it) },
                                onCopyCode = {
                                    clipboardManager.setText(AnnotatedString(generatedRoomCode))
                                    copyFeedback = true
                                },
                                onCreateLobby = {
                                    val result = FirebaseLobbyRepository.instance.createRoom(
                                        hostPlayerId = "Komutan (Siz)",
                                        pin = if (isPinEnabled && hostPin.isNotBlank()) hostPin else null,
                                        turnDurationSeconds = roomTurnDuration,
                                        mapRadius = roomMapRadius,
                                        enableHazards = roomEnableHazards,
                                        isPrivate = isPrivateRoom,
                                        preferredCode = generatedRoomCode
                                    )
                                    val room = result.getOrNull()
                                    activeWaitingCode = room?.roomCode ?: generatedRoomCode
                                    isWaitingInLobby = true
                                    isGuestConnected = false
                                }
                            )
                        }
                        1 -> {
                            // ODAYA KATILMA KARTI
                            JoinRoomConfigCard(
                                inputCode = inputCode,
                                inputPin = inputPin,
                                errorMessage = joinError,
                                onCodeChanged = { inputCode = it.uppercase().take(7) },
                                onPinChanged = { if (it.length <= 4) inputPin = it },
                                onJoin = {
                                    if (!RoomCodeGenerator.isValid(inputCode)) {
                                        joinError = "Geçersiz format! Kod 'HX-XXXX' şeklinde olmalıdır."
                                    } else {
                                        val joinResult = FirebaseLobbyRepository.instance.joinRoom(
                                            roomCode = inputCode,
                                            guestPlayerId = "Misafir Komutan",
                                            inputPin = if (inputPin.isNotBlank()) inputPin else null
                                        )
                                        if (joinResult.isSuccess) {
                                            val joined = joinResult.getOrThrow()
                                            joinError = null
                                            onStartOnlineGame(
                                                joined.roomCode,
                                                false,
                                                joined.turnDurationSeconds,
                                                joined.mapRadius,
                                                joined.enableHazards,
                                                GameCustomRules.defaultForRadius(joined.mapRadius)
                                            )
                                        } else {
                                            joinError = joinResult.exceptionOrNull()?.message ?: "Odaya bağlanılamadı. Kod ve PIN kontrol edin."
                                        }
                                    }
                                }
                            )
                        }
                        2 -> {
                            // LOBİ GÖZLEMLEME & AÇIK ODALAR (LOBBY BROWSER)
                            LobbyBrowserView(
                                rooms = publicRooms,
                                onJoinRoom = { room, pin ->
                                    val joinResult = FirebaseLobbyRepository.instance.joinRoom(
                                        roomCode = room.roomCode,
                                        guestPlayerId = "Misafir Komutan",
                                        inputPin = pin
                                    )
                                    if (joinResult.isSuccess) {
                                        val joined = joinResult.getOrThrow()
                                        onStartOnlineGame(
                                            joined.roomCode,
                                            false,
                                            joined.turnDurationSeconds,
                                            joined.mapRadius,
                                            joined.enableHazards,
                                            GameCustomRules.defaultForRadius(joined.mapRadius)
                                        )
                                    }
                                },
                                onRefresh = {
                                    // Flow otomatik olarak en güncel odayı sunar
                                },
                                onSwitchToHost = {
                                    selectedOnlineSubTab = 0
                                }
                            )
                        }
                    }
                }
            } else {
                // ==================== MOD 2: YAPAY ZEKAYA KARŞI (VS AI) ====================
                VsAiConfigCard(
                    selectedDifficulty = selectedAiDifficulty,
                    mapRadius = aiMapRadius,
                    enableHazards = aiEnableHazards,
                    maxShields = aiMaxShields,
                    radioactiveZones = aiRadioactiveZones,
                    coralReefs = aiCoralReefs,
                    magneticStorms = aiMagneticStorms,
                    maxEmpStrikes = aiMaxEmpStrikes,
                    maxLineBarrages = aiMaxLineBarrages,
                    isTablet = isTabletLayout,
                    onDifficultySelected = { selectedAiDifficulty = it },
                    onRadiusChanged = { aiMapRadius = it },
                    onHazardsChanged = { aiEnableHazards = it },
                    onMaxShieldsChanged = { aiMaxShields = it },
                    onRadioactiveZonesChanged = { aiRadioactiveZones = it },
                    onCoralReefsChanged = { aiCoralReefs = it },
                    onMagneticStormsChanged = { aiMagneticStorms = it },
                    onMaxEmpStrikesChanged = { aiMaxEmpStrikes = it },
                    onMaxLineBarragesChanged = { aiMaxLineBarrages = it },
                    onInfoClick = { activeTacticalInfoParam = getTacticalParamInfo(it) },
                    onStartAiBattle = {
                        val aiRules = GameCustomRules(
                            maxShieldDomes = aiMaxShields,
                            radioactiveZonesCount = aiRadioactiveZones,
                            coralReefCount = aiCoralReefs,
                            magneticStormCount = aiMagneticStorms,
                            maxEmpStrikes = aiMaxEmpStrikes,
                            maxLineBarrages = aiMaxLineBarrages
                        )
                        onStartAiGame(selectedAiDifficulty, aiMapRadius, aiEnableHazards, aiRules)
                    }
                )
            }
        }
    }

    // Modal Dialoglar
    activeTacticalInfoParam?.let { info ->
        TacticalInfoDialog(
            info = info,
            onDismiss = { activeTacticalInfoParam = null }
        )
    }

    if (showInfoDialog) {
        HexaFleetInfoDialog(onDismissRequest = { showInfoDialog = false })
    }
    if (showSettingsDialog) {
        HexaFleetSettingsDialog(onDismissRequest = { showSettingsDialog = false })
    }
    if (showExitDialog) {
        HexaFleetExitDialog(onDismissRequest = { showExitDialog = false })
    }
}

/**
 * Lobi Gözlemleme Modu: 2. kişi bağlanana kadar oyun ASLA başlamaz!
 */
@Composable
private fun LobbyWaitingRoomView(
    roomCode: String,
    isGuestConnected: Boolean,
    countdown: Int?,
    turnDuration: Int,
    mapRadius: Int,
    hazardsEnabled: Boolean,
    isPinProtected: Boolean,
    onSimulateGuestJoin: () -> Unit,
    onCancelLobby: () -> Unit,
    onCopyCode: () -> Unit,
    copyFeedback: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BoardColors.PureWhite),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BoardColors.PurplePrimary)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Durum Rozeti
            Surface(
                color = if (isGuestConnected) BoardColors.RedLight else BoardColors.PurpleLight,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isGuestConnected) BoardColors.CrimsonRed else BoardColors.PurplePrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGuestConnected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BoardColors.CrimsonRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("2. OYUNCU BAĞLANDI - SAVAŞ BAŞLIYOR", color = BoardColors.CrimsonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = BoardColors.PurplePrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOBİ AÇIK: 2. OYUNCU BEKLENİYOR...", color = BoardColors.PurplePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("BAĞLANTI ODA KODU", color = BoardColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = roomCode,
                    color = BoardColors.NavyDark,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onCopyCode,
                    modifier = Modifier
                        .size(42.dp)
                        .background(BoardColors.PurpleLight, CircleShape)
                        .border(1.dp, BoardColors.PurplePrimary.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = BoardColors.PurplePrimary)
                }
            }

            AnimatedVisibility(visible = copyFeedback) {
                Text(
                    text = "✓ Oda kodu panoya kopyalandı!",
                    color = BoardColors.PurplePrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lobi Ayarları Özeti
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BoardColors.SurfaceSubtle, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SettingPill(label = "Süre", value = "$turnDuration sn")
                SettingPill(label = "Harita", value = "Yarıçap $mapRadius")
                SettingPill(label = "Engeller", value = if (hazardsEnabled) "Aktif" else "Kapalı")
                SettingPill(label = "PIN", value = if (isPinProtected) "Şifreli" else "Açık")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Oyuncu Yuvaları (Slots)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Oyuncu 1: Host
                Surface(
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(BoardColors.NavyAccent.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BoardColors.NavyAccent, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Komutan (Siz - Host)", color = BoardColors.NavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Filo konuşlandırıldı", color = BoardColors.TextSecondary, fontSize = 10.sp)
                            }
                        }
                        Text("HAZIR 🟢", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Oyuncu 2: Misafir (Guest)
                Surface(
                    color = if (isGuestConnected) BoardColors.SurfaceSubtle else BoardColors.PureWhite,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGuestConnected) BoardColors.CrimsonRed else BoardColors.BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isGuestConnected) BoardColors.CrimsonRed.copy(alpha = 0.15f) else BoardColors.SurfaceSubtle,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isGuestConnected) Icons.Default.Person else Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = if (isGuestConnected) BoardColors.CrimsonRed else BoardColors.TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isGuestConnected) "Rakip Komutan (Guest)" else "2. Oyuncu Bekleniyor...",
                                    color = if (isGuestConnected) BoardColors.NavyDark else BoardColors.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isGuestConnected) "Bağlantı kuruldu" else "Oda kodunu paylaşın",
                                    color = BoardColors.TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (isGuestConnected) {
                            Text("BAĞLANDI 🟢", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        } else {
                            Text("BEKLENİYOR ⏳", color = BoardColors.TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Geri Sayım Görünümü
            if (countdown != null) {
                Surface(
                    color = BoardColors.RedLight,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.CrimsonRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("MUHAREBE BAŞLIYOR!", color = BoardColors.CrimsonRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$countdown", color = BoardColors.CrimsonRed, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Geliştirici / Test için Rakip Simülasyon Butonu
            if (!isGuestConnected) {
                OutlinedButton(
                    onClick = onSimulateGuestJoin,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.PurplePrimary)
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = BoardColors.PurplePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Rakibi Bağla (Hızlı Simülasyon)", color = BoardColors.PurplePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Lobiyi İptal Et ve Kapat Butonu
            Button(
                onClick = onCancelLobby,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BoardColors.NavyPrimary)
            ) {
                Text("LOBİYİ KAPAT VE AYARLARA DÖN", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SettingPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = BoardColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = BoardColors.NavyDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

/**
 * ODA KURMA KARTI (Host Ayarları)
 * User Requirement 2: İki sütunun (Sol: Tur Süresi, Sağ: Özel Taktiksel Parametreler) başlangıcı tam eşit hizalanmıştır.
 * Üretilen Oda Kodu her iki sütunun da üstünde ince, şık bir şerit olarak yer alır.
 * User Requirement 4: Gizli / Private oda seçeneği eklenmiştir.
 */
@Composable
private fun HostRoomConfigCard(
    roomCode: String,
    isPinEnabled: Boolean,
    pin: String,
    isPrivate: Boolean = false,
    turnDuration: Int,
    mapRadius: Int,
    enableHazards: Boolean,
    copyFeedback: Boolean,
    maxShields: Int,
    radioactiveZones: Int,
    coralReefs: Int,
    magneticStorms: Int,
    maxEmpStrikes: Int = 1,
    maxLineBarrages: Int = 1,
    isTablet: Boolean = false,
    onTogglePin: (Boolean) -> Unit,
    onPinChanged: (String) -> Unit,
    onTogglePrivate: (Boolean) -> Unit,
    onDurationChanged: (Int) -> Unit,
    onRadiusChanged: (Int) -> Unit,
    onHazardsChanged: (Boolean) -> Unit,
    onMaxShieldsChanged: (Int) -> Unit,
    onRadioactiveZonesChanged: (Int) -> Unit,
    onCoralReefsChanged: (Int) -> Unit,
    onMagneticStormsChanged: (Int) -> Unit,
    onMaxEmpStrikesChanged: (Int) -> Unit = {},
    onMaxLineBarragesChanged: (Int) -> Unit = {},
    onInfoClick: (TacticalParamType) -> Unit = {},
    onCopyCode: () -> Unit,
    onCreateLobby: () -> Unit
) {
    // Zarif, hafif flu sınır animasyonları (User Requirement 3 & 4)
    val infiniteTransition = rememberInfiniteTransition(label = "HostCardBorderAnimation")
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HostFluBorderPulse"
    )
    val cyanBorder = Color(0xFF38BDF8).copy(alpha = 0.32f + 0.25f * borderPulse)
    val emeraldBorder = Color(0xFF10B981).copy(alpha = 0.30f + 0.22f * borderPulse)
    val purpleBorder = Color(0xFFA855F7).copy(alpha = 0.35f + 0.25f * borderPulse)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
    ) {
        if (isTablet) {
            // ==================== TABLET DÜZENİ: ÜST İNCE ŞERİT + 2 TAM HİZALI SÜTUN ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 1. ÜST İNCE ŞERİT: ÜRETİLEN ODA KODU (Her iki sütunun üstünü kaplar, ince ve estetik)
                Surface(
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "ÜRETİLEN ODA KODU:",
                                color = BoardColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = roomCode,
                                color = BoardColors.NavyDark,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedVisibility(visible = copyFeedback) {
                                Text("✓ Kopyalandı", color = BoardColors.PurplePrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onCopyCode,
                                modifier = Modifier.size(30.dp).background(BoardColors.PurpleLight, CircleShape)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = BoardColors.PurplePrimary, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. İKİ EŞİT SÜTUN: SOL (TUR & HARİTA + LOBİ GÜVENLİK) & SAĞ (ÖZEL PARAMETRELER + BUTON)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SOL SÜTUN (%50): KART 1 (TUR & HARİTA - Flu Mavi) ve KART 2 (LOBİ GÜVENLİK & ÇEVRE - Flu Yeşil)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // KART 1: TUR SÜRESİ VE HARİTA BÜYÜKLÜĞÜ (User Requirement 3 - Estetik Alan & Flu Mavi Animasyonlu Sınır)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, cyanBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "TEMEL HAREKAT PARAMETRELERİ",
                                    color = BoardColors.NavyDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("TUR SÜRESİ", color = BoardColors.NavyDark, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(15 to "Blitz (15 sn)", 45 to "Standart (45 sn)", 60 to "Taktik (60 sn)").forEach { (sec, label) ->
                                        val selected = turnDuration == sec
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onDurationChanged(sec) },
                                            color = if (selected) BoardColors.PurpleLight else BoardColors.PureWhite,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) BoardColors.PurplePrimary else BoardColors.BorderSubtle)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) BoardColors.PurplePrimary else BoardColors.TextPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("HARİTA BÜYÜKLÜĞÜ", color = BoardColors.NavyDark, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(5 to "Standart (91 Bölge)", 6 to "Geniş (127 Bölge)", 7 to "Devasa (169 Bölge)").forEach { (r, label) ->
                                        val selected = mapRadius == r
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onRadiusChanged(r) },
                                            color = if (selected) BoardColors.PurpleLight else BoardColors.PureWhite,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) BoardColors.PurplePrimary else BoardColors.BorderSubtle)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) BoardColors.PurplePrimary else BoardColors.TextPrimary,
                                                fontSize = 8.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // KART 2: LOBİ GÜVENLİK VE ÇEVRE DÜZENİ (User Requirement 4 - Estetik Alan & Flu Yeşil Animasyonlu Sınır)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, emeraldBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "LOBİ GÜVENLİĞİ VE ÇEVRESEL KOŞULLAR",
                                    color = BoardColors.NavyDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Mercan Resifleri ve Fırtınalar", color = BoardColors.TextPrimary, fontSize = 10.5.sp)
                                    Switch(
                                        checked = enableHazards,
                                        onCheckedChange = onHazardsChanged,
                                        colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.CoralReef)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Şifreli Özel Lobi (PIN)", color = BoardColors.TextPrimary, fontSize = 10.5.sp)
                                    Switch(
                                        checked = isPinEnabled,
                                        onCheckedChange = onTogglePin,
                                        colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.PurplePrimary)
                                    )
                                }

                                AnimatedVisibility(visible = isPinEnabled) {
                                    OutlinedTextField(
                                        value = pin,
                                        onValueChange = onPinChanged,
                                        label = { Text("4 Haneli Güvenlik PIN Kodu", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BoardColors.PurplePrimary,
                                            unfocusedBorderColor = BoardColors.BorderSubtle
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Gizli / Private Lobi", color = BoardColors.TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        Text("Lobi odaları listesinde gizle, sadece kodla girilsin", color = BoardColors.TextSecondary, fontSize = 8.5.sp)
                                    }
                                    Switch(
                                        checked = isPrivate,
                                        onCheckedChange = onTogglePrivate,
                                        colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.NavyDark)
                                    )
                                }
                            }
                        }
                    }

                    // SAĞ SÜTUN (%50): KART 3 (ÖZEL TAKTİKSEL PARAMETRELER - Flu Mor Animasyonlu Sınır)
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, purpleBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "ÖZEL TAKTİKSEL PARAMETRELER",
                                    color = BoardColors.NavyDark,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        TacticalParameterStepper(
                                            label = "Aegis Kalkanı",
                                            value = maxShields,
                                            range = 0..5,
                                            accentColor = BoardColors.PurplePrimary,
                                            onValueChange = onMaxShieldsChanged,
                                            onInfoClick = { onInfoClick(TacticalParamType.SHIELD) }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        TacticalParameterStepper(
                                            label = "Radyoaktif (2'li)",
                                            value = radioactiveZones,
                                            range = 0..5,
                                            accentColor = Color(0xFF16A34A),
                                            onValueChange = onRadioactiveZonesChanged,
                                            onInfoClick = { onInfoClick(TacticalParamType.RADIOACTIVE) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        TacticalParameterStepper(
                                            label = "Delta EMP Şoku",
                                            value = maxEmpStrikes,
                                            range = 0..3,
                                            accentColor = Color(0xFF06B6D4),
                                            onValueChange = onMaxEmpStrikesChanged,
                                            onInfoClick = { onInfoClick(TacticalParamType.EMP) }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        TacticalParameterStepper(
                                            label = "Amiral 5'li Salvo",
                                            value = maxLineBarrages,
                                            range = 0..3,
                                            accentColor = BoardColors.CrimsonRed,
                                            onValueChange = onMaxLineBarragesChanged,
                                            onInfoClick = { onInfoClick(TacticalParamType.BARRAGE) }
                                        )
                                    }
                                }

                                if (enableHazards) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TacticalParameterStepper(
                                                label = "Mercan Resifi",
                                                value = coralReefs,
                                                range = 0..10,
                                                accentColor = BoardColors.CoralReef,
                                                onValueChange = onCoralReefsChanged,
                                                onInfoClick = { onInfoClick(TacticalParamType.REEF) }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            TacticalParameterStepper(
                                                label = "Manyetik Fırtına",
                                                value = magneticStorms,
                                                range = 0..10,
                                                accentColor = BoardColors.PurpleDark,
                                                onValueChange = onMagneticStormsChanged,
                                                onInfoClick = { onInfoClick(TacticalParamType.STORM) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // EN ALTTA VE ORTALI AKSİYON BUTONU (User Requirement 1)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onCreateLobby,
                        modifier = Modifier.fillMaxWidth(0.92f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BoardColors.PurplePrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "LOBİ OLUŞTUR VE RAKİP BEKLE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            // ==================== MOBİL DÜZENİ: TEK EKRAN (KOMPAKT, SIFIR KAYDIRMA) ====================
            Column(modifier = Modifier.padding(12.dp)) {
                // Oda Kodu Satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ODA KODU", color = BoardColors.TextSecondary, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = roomCode,
                            color = BoardColors.NavyDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                    IconButton(
                        onClick = onCopyCode,
                        modifier = Modifier.size(32.dp).background(BoardColors.PurpleLight, CircleShape)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = BoardColors.PurplePrimary, modifier = Modifier.size(16.dp))
                    }
                }

                AnimatedVisibility(visible = copyFeedback) {
                    Text("✓ Kod kopyalandı", color = BoardColors.PurplePrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // KART 1: TUR SÜRESİ VE HARİTA BÜYÜKLÜĞÜ (User Requirement 3 - Estetik Alan & Flu Mavi Animasyonlu Sınır)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, cyanBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "TEMEL HAREKAT PARAMETRELERİ",
                            color = BoardColors.NavyDark,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Tur Süresi
                        Text("TUR SÜRESİ", color = BoardColors.NavyDark, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15 to "Blitz (15s)", 45 to "Standart (45s)", 60 to "Taktik (60s)").forEach { (sec, label) ->
                                val selected = turnDuration == sec
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onDurationChanged(sec) },
                                    color = if (selected) BoardColors.PurpleLight else BoardColors.SurfaceSubtle,
                                    shape = RoundedCornerShape(7.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) BoardColors.PurplePrimary else BoardColors.BorderSubtle)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) BoardColors.PurplePrimary else BoardColors.TextPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 5.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Harita Boyutu
                        Text("HARİTA BÜYÜKLÜĞÜ", color = BoardColors.NavyDark, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5 to "Standart (91)", 6 to "Geniş (127)", 7 to "Devasa (169)").forEach { (r, label) ->
                                val selected = mapRadius == r
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onRadiusChanged(r) },
                                    color = if (selected) BoardColors.PurpleLight else BoardColors.SurfaceSubtle,
                                    shape = RoundedCornerShape(7.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) BoardColors.PurplePrimary else BoardColors.BorderSubtle)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) BoardColors.PurplePrimary else BoardColors.TextPrimary,
                                        fontSize = 8.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KART 2: LOBİ GÜVENLİĞİ VE ÇEVRESEL KOŞULLAR (User Requirement 4 - Estetik Alan & Flu Yeşil Animasyonlu Sınır)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, emeraldBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "LOBİ GÜVENLİĞİ VE ÇEVRESEL KOŞULLAR",
                            color = BoardColors.NavyDark,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Tehlike & PIN Satırları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mercan & Fırtınalar", color = BoardColors.TextPrimary, fontSize = 10.sp)
                            Switch(
                                checked = enableHazards,
                                onCheckedChange = onHazardsChanged,
                                colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.CoralReef)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Şifreli PIN", color = BoardColors.TextPrimary, fontSize = 10.sp)
                            Switch(
                                checked = isPinEnabled,
                                onCheckedChange = onTogglePin,
                                colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.PurplePrimary)
                            )
                        }

                        AnimatedVisibility(visible = isPinEnabled) {
                            OutlinedTextField(
                                value = pin,
                                onValueChange = onPinChanged,
                                label = { Text("4 Haneli PIN", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BoardColors.PurplePrimary,
                                    unfocusedBorderColor = BoardColors.BorderSubtle
                                ),
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gizli / Private Lobi", color = BoardColors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Lobi listesinde gizle", color = BoardColors.TextSecondary, fontSize = 8.sp)
                            }
                            Switch(
                                checked = isPrivate,
                                onCheckedChange = onTogglePrivate,
                                colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.NavyDark)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KART 3: ÖZEL TAKTİKSEL PARAMETRELER (Flu Mor Animasyonlu Sınır)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, purpleBorder)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "ÖZEL TAKTİKSEL PARAMETRELER",
                            color = BoardColors.NavyDark,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TacticalParameterStepper(
                                    "Kalkan",
                                    maxShields,
                                    0..5,
                                    BoardColors.PurplePrimary,
                                    onMaxShieldsChanged,
                                    onInfoClick = { onInfoClick(TacticalParamType.SHIELD) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TacticalParameterStepper(
                                    "Sis (2'li)",
                                    radioactiveZones,
                                    0..5,
                                    Color(0xFF16A34A),
                                    onRadioactiveZonesChanged,
                                    onInfoClick = { onInfoClick(TacticalParamType.RADIOACTIVE) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TacticalParameterStepper(
                                    "Delta EMP",
                                    maxEmpStrikes,
                                    0..3,
                                    Color(0xFF06B6D4),
                                    onMaxEmpStrikesChanged,
                                    onInfoClick = { onInfoClick(TacticalParamType.EMP) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TacticalParameterStepper(
                                    "Amiral Salvo",
                                    maxLineBarrages,
                                    0..3,
                                    BoardColors.CrimsonRed,
                                    onMaxLineBarragesChanged,
                                    onInfoClick = { onInfoClick(TacticalParamType.BARRAGE) }
                                )
                            }
                        }

                        if (enableHazards) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    TacticalParameterStepper(
                                        "Resif",
                                        coralReefs,
                                        0..10,
                                        BoardColors.CoralReef,
                                        onCoralReefsChanged,
                                        onInfoClick = { onInfoClick(TacticalParamType.REEF) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    TacticalParameterStepper(
                                        "Fırtına",
                                        magneticStorms,
                                        0..10,
                                        BoardColors.PurpleDark,
                                        onMagneticStormsChanged,
                                        onInfoClick = { onInfoClick(TacticalParamType.STORM) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // LOBİ OLUŞTUR VE RAKİP BEKLE BUTONU (En Altta, Ortalı & Estetik)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onCreateLobby,
                        modifier = Modifier.fillMaxWidth(0.96f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BoardColors.PurplePrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "LOBİ OLUŞTUR VE RAKİP BEKLE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * LOBİ GÖZLEMLEME & AÇIK ODALARI LİSTELEME EKRANI (User Requirement 5)
 * Herkese açık kurulu odaları canlı olarak listeler.
 * Şifreli odalar kilit ikonu ile belirtilir, doğru PIN girilmeden odaya bağlanılmaz.
 */
@Composable
private fun LobbyBrowserView(
    rooms: List<LobbyRoom>,
    onJoinRoom: (room: LobbyRoom, pin: String?) -> Unit,
    onRefresh: () -> Unit,
    onSwitchToHost: () -> Unit
) {
    var selectedProtectedRoom by remember { mutableStateOf<LobbyRoom?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Başlık ve Yenileme Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AKTİF LOBİ ODALARI (CANLI)",
                        color = BoardColors.NavyDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Açık odalara tek tıkla veya PIN girerek katılabilirsiniz",
                        color = BoardColors.TextSecondary,
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(34.dp)
                        .background(BoardColors.SurfaceSubtle, CircleShape)
                        .border(1.dp, BoardColors.BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Yenile",
                        tint = BoardColors.NavyDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (rooms.isEmpty()) {
                Surface(
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = BoardColors.TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Şu anda açık lobi odası bulunmuyor.",
                            color = BoardColors.NavyDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kendi odanızı kurarak veya doğrudan oda kodu girerek oyuna başlayabilirsiniz.",
                            color = BoardColors.TextSecondary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onSwitchToHost,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BoardColors.PurplePrimary)
                        ) {
                            Text("YENİ ODA OLUŞTUR (HOST)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rooms.forEach { room ->
                        Surface(
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (room.isPinProtected) BoardColors.BorderSubtle else BoardColors.PurplePrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (room.isPinProtected) Color(0xFFFEF3C7) else BoardColors.PurpleLight,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (room.isPinProtected) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(com.hexafleet.feature.lobby.R.drawable.ic_gamepad_multiplayer),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = room.roomCode,
                                                color = BoardColors.NavyDark,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (room.isPinProtected) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (room.isPinProtected) "ŞİFRELİ" else "AÇIK",
                                                    color = if (room.isPinProtected) Color(0xFFB45309) else Color(0xFF15803D),
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        val mapDesc = when (room.mapRadius) {
                                            5 -> "Standart (91 Bölge)"
                                            6 -> "Geniş (127 Bölge)"
                                            else -> "Devasa (169 Bölge)"
                                        }
                                        Text(
                                            text = "Komutan: ${room.hostPlayerId} • ${room.turnDurationSeconds} sn • $mapDesc",
                                            color = BoardColors.TextSecondary,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        if (room.isPinProtected) {
                                            selectedProtectedRoom = room
                                            enteredPin = ""
                                            pinError = null
                                        } else {
                                            onJoinRoom(room, null)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (room.isPinProtected) BoardColors.NavyAccent else BoardColors.PurplePrimary
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (room.isPinProtected) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ŞİFRE GİR", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        } else {
                                            Text("KATIL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN Giriş Dialogu
    selectedProtectedRoom?.let { room ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedProtectedRoom = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BoardColors.PureWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ŞİFRELİ ODAYA KATIL",
                        color = BoardColors.NavyDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${room.roomCode} kodlu özel odaya katılmak için 4 haneli PIN şifresini girin.",
                        color = BoardColors.TextSecondary,
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4) enteredPin = it },
                        placeholder = { Text("4 Haneli PIN", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BoardColors.PurplePrimary,
                            unfocusedBorderColor = BoardColors.BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pinError!!,
                            color = BoardColors.CrimsonRed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedProtectedRoom = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("İPTAL", color = BoardColors.TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = {
                                if (enteredPin.length != 4) {
                                    pinError = "PIN 4 haneli olmalıdır!"
                                } else {
                                    val joinRes = FirebaseLobbyRepository.instance.joinRoom(room.roomCode, "Misafir Komutan", enteredPin)
                                    if (joinRes.isSuccess) {
                                        selectedProtectedRoom = null
                                        onJoinRoom(room, enteredPin)
                                    } else {
                                        pinError = "Hatalı PIN şifresi!"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BoardColors.PurplePrimary)
                        ) {
                            Text("ONAYLA & GİR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

/**
 * ODAYA KATILMA KARTI (Join Tab)
 */
@Composable
private fun JoinRoomConfigCard(
    inputCode: String,
    inputPin: String,
    errorMessage: String?,
    onCodeChanged: (String) -> Unit,
    onPinChanged: (String) -> Unit,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BAĞLANMAK İSTEDİĞİNİZ ODA KODUNU GİRİN", color = BoardColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputCode,
                onValueChange = onCodeChanged,
                placeholder = { Text("HX-XXXX", color = BoardColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    color = BoardColors.NavyDark
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardColors.NavyAccent,
                    unfocusedBorderColor = BoardColors.BorderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputPin,
                onValueChange = onPinChanged,
                label = { Text("PIN (Lobi şifreliyse)") },
                placeholder = { Text("4 Hane", color = BoardColors.TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardColors.PurplePrimary,
                    unfocusedBorderColor = BoardColors.BorderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = BoardColors.CrimsonRed,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onJoin,
                enabled = inputCode.length >= 7,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BoardColors.NavyAccent,
                    disabledContainerColor = BoardColors.SurfaceSubtle
                )
            ) {
                Text(
                    "DOĞRULA VE ODAYA BAĞLAN",
                    color = if (inputCode.length >= 7) Color.White else BoardColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TacticalSettingBadge(
    emoji: String,
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.09f),
        shape = RoundedCornerShape(7.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 9.sp
            )
            Text(
                text = text,
                color = color,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * YAPAY ZEKAYA KARŞI OPERASYON KARTI (3 Zorluk Seviyesi)
 */
@Composable
private fun VsAiConfigCard(
    selectedDifficulty: AiDifficulty,
    mapRadius: Int,
    enableHazards: Boolean,
    maxShields: Int,
    radioactiveZones: Int,
    coralReefs: Int,
    magneticStorms: Int,
    maxEmpStrikes: Int = 1,
    maxLineBarrages: Int = 1,
    isTablet: Boolean = false,
    onDifficultySelected: (AiDifficulty) -> Unit,
    onRadiusChanged: (Int) -> Unit,
    onHazardsChanged: (Boolean) -> Unit,
    onMaxShieldsChanged: (Int) -> Unit,
    onRadioactiveZonesChanged: (Int) -> Unit,
    onCoralReefsChanged: (Int) -> Unit,
    onMagneticStormsChanged: (Int) -> Unit,
    onMaxEmpStrikesChanged: (Int) -> Unit = {},
    onMaxLineBarragesChanged: (Int) -> Unit = {},
    onInfoClick: (TacticalParamType) -> Unit = {},
    onStartAiBattle: () -> Unit
) {
    // Zarif, hafif flu sınır animasyonları (User Requirement 3 & 4)
    val infiniteTransition = rememberInfiniteTransition(label = "VsAiCardBorderAnimation")
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VsAiFluBorderPulse"
    )
    val cyanBorder = Color(0xFF38BDF8).copy(alpha = 0.32f + 0.25f * borderPulse)
    val emeraldBorder = Color(0xFF10B981).copy(alpha = 0.30f + 0.22f * borderPulse)
    val purpleBorder = Color(0xFFA855F7).copy(alpha = 0.35f + 0.25f * borderPulse)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
    ) {
        if (isTablet) {
            // ==================== TABLET DÜZENİ: ÜST ŞERİT + 2 SÜTUN + EN ALTA ORTALI BUTON ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 1. ÜST İNCE ŞERİT: TAKTİKSEL SİMÜLASYON ŞERİDİ & DİNAMİK AYAR ETİKETLERİ (User Requirement 5: Durum Aktif kaldırıldı, dinamik etiketler eklendi)
                Surface(
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7).copy(alpha = 0.12f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(com.hexafleet.feature.lobby.R.drawable.ic_ai_robot_avatar),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            Column {
                                Text(
                                    "TAKTİKSEL HAREKÂT SİMÜLATÖRÜ",
                                    color = BoardColors.NavyDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    "6 Eksenli H-PDF Analiz Motoru",
                                    color = BoardColors.TextMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // DİNAMİK SEÇİM ETİKETLERİ (Kullanıcının yaptığı seçimlere göre anlık güncellenen estetik rozetler)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Zorluk Rozeti
                            val (diffEmoji, diffName, diffColor) = when (selectedDifficulty) {
                                AiDifficulty.RECRUIT -> Triple("🌱", "ACEMİ", Color(0xFF16A34A))
                                AiDifficulty.VETERAN -> Triple("⚡", "KIDEMLİ", Color(0xFF9333EA))
                                AiDifficulty.GRAND_ADMIRAL -> Triple("👑", "AMİRAL", BoardColors.CrimsonRed)
                            }
                            TacticalSettingBadge(emoji = diffEmoji, text = diffName, color = diffColor)

                            // 2. Harita Boyutu Rozeti
                            val mapName = when (mapRadius) {
                                5 -> "91 Petek"
                                6 -> "127 Petek"
                                else -> "169 Petek"
                            }
                            TacticalSettingBadge(emoji = "🗺️", text = mapName, color = Color(0xFF0284C7))

                            // 3. Çevre Engelleri Rozeti
                            if (enableHazards) {
                                TacticalSettingBadge(emoji = "🌊", text = "Resif & Fırtına", color = Color(0xFF0D9488))
                            }

                            // 4. Özel Taktiksel Parametreler (Kalkan ve Radyoaktif Sis)
                            TacticalSettingBadge(emoji = "🛡️", text = "${maxShields}K • ☢️ ${radioactiveZones}S", color = Color(0xFF6366F1))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SOL SÜTUN (%50): AI ZORLUK SEÇİMİ + HARİTA BOYUTU + TEHLİKE SWITCH
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YAPAY ZEKA KOMUTANI (3 SEVİYE)",
                            color = BoardColors.NavyDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        AiDifficultyCard(
                            difficulty = AiDifficulty.RECRUIT,
                            title = "1. ACEMİ (RECRUIT)",
                            badge = "KOLAY",
                            badgeColor = Color(0xFF16A34A),
                            description = "Rastgele keşif atışları yapar ve isabet aldığında düşük olasılıkla çevreyi yoklar.",
                            isSelected = selectedDifficulty == AiDifficulty.RECRUIT,
                            onClick = { onDifficultySelected(AiDifficulty.RECRUIT) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AiDifficultyCard(
                            difficulty = AiDifficulty.VETERAN,
                            title = "2. KIDEMLİ (VETERAN)",
                            badge = "ORTA",
                            badgeColor = BoardColors.PurplePrimary,
                            description = "6 eksenli Olasılık Yoğunluk Haritası (H-PDF) ve parite avlanma taktiğini uygular.",
                            isSelected = selectedDifficulty == AiDifficulty.VETERAN,
                            onClick = { onDifficultySelected(AiDifficulty.VETERAN) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AiDifficultyCard(
                            difficulty = AiDifficulty.GRAND_ADMIRAL,
                            title = "3. BÜYÜK AMİRAL (ADMIRAL)",
                            badge = "UZMAN",
                            badgeColor = BoardColors.CrimsonRed,
                            description = "Gelişmiş H-PDF + ship-fitting eleme analizi + İHA keşfi ve Aegis kalkanı kullanımı.",
                            isSelected = selectedDifficulty == AiDifficulty.GRAND_ADMIRAL,
                            onClick = { onDifficultySelected(AiDifficulty.GRAND_ADMIRAL) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // KART 1: SAVAŞ ALANI BÜYÜKLÜĞÜ (Estetik Alan & Flu Mavi Animasyonlu Sınır)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, cyanBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SAVAŞ ALANI BÜYÜKLÜĞÜ", color = BoardColors.NavyDark, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(5 to "Standart (91 Bölge)", 6 to "Geniş (127 Bölge)", 7 to "Devasa (169 Bölge)").forEach { (r, label) ->
                                        val selected = mapRadius == r
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onRadiusChanged(r) },
                                            color = if (selected) BoardColors.RedLight else BoardColors.SurfaceSubtle,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) BoardColors.CrimsonRed else BoardColors.BorderSubtle)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) BoardColors.CrimsonRed else BoardColors.TextPrimary,
                                                fontSize = 8.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // KART 2: ÇEVRESEL KOŞULLAR (Estetik Alan & Flu Yeşil Animasyonlu Sınır)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, emeraldBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mercan Resifleri ve Manyetik Fırtınalar", color = BoardColors.TextPrimary, fontSize = 11.sp)
                                Switch(
                                    checked = enableHazards,
                                    onCheckedChange = onHazardsChanged,
                                    colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.CrimsonRed)
                                )
                            }
                        }
                    }

                    // SAĞ SÜTUN (%50): ÖZEL TAKTİKSEL PARAMETRELER (Aşağıya hizalı, Çevrimiçi Savaş ile dengeli)
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, purpleBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "ÖZEL TAKTİKSEL PARAMETRELER",
                                    color = BoardColors.NavyDark,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        TacticalParameterStepper(
                                            "Aegis Kalkanı",
                                            maxShields,
                                            0..5,
                                            BoardColors.PurplePrimary,
                                            onMaxShieldsChanged,
                                            onInfoClick = { onInfoClick(TacticalParamType.SHIELD) }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        TacticalParameterStepper(
                                            "Radyoaktif (2'li)",
                                            radioactiveZones,
                                            0..5,
                                            Color(0xFF16A34A),
                                            onRadioactiveZonesChanged,
                                            onInfoClick = { onInfoClick(TacticalParamType.RADIOACTIVE) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        TacticalParameterStepper(
                                            "Delta EMP Şoku",
                                            maxEmpStrikes,
                                            0..3,
                                            Color(0xFF06B6D4),
                                            onMaxEmpStrikesChanged,
                                            onInfoClick = { onInfoClick(TacticalParamType.EMP) }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        TacticalParameterStepper(
                                            "Amiral 5'li Salvo",
                                            maxLineBarrages,
                                            0..3,
                                            BoardColors.CrimsonRed,
                                            onMaxLineBarragesChanged,
                                            onInfoClick = { onInfoClick(TacticalParamType.BARRAGE) }
                                        )
                                    }
                                }

                                if (enableHazards) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TacticalParameterStepper(
                                                "Mercan Resifi",
                                                coralReefs,
                                                0..10,
                                                BoardColors.CoralReef,
                                                onCoralReefsChanged,
                                                onInfoClick = { onInfoClick(TacticalParamType.REEF) }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            TacticalParameterStepper(
                                                "Manyetik Fırtına",
                                                magneticStorms,
                                                0..10,
                                                BoardColors.CrimsonRed,
                                                onMagneticStormsChanged,
                                                onInfoClick = { onInfoClick(TacticalParamType.STORM) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // EN ALTTA VE ORTALI AKSİYON BUTONU (User Requirement 1)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStartAiBattle,
                        modifier = Modifier.fillMaxWidth(0.92f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BoardColors.CrimsonRed),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(com.hexafleet.feature.lobby.R.drawable.ic_ai_robot_avatar),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "YAPAY ZEKAYA KARŞI SAVAŞI BAŞLAT",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            // ==================== MOBİL DÜZENİ: TEK EKRAN (KOMPAKT, SIFIR KAYDIRMA) ====================
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "YAPAY ZEKA KOMUTANI SEÇİMİ (3 SEVİYE)",
                    color = BoardColors.NavyDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(5.dp))

                // 3 Seviye Yatay Segment Seçici
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple(AiDifficulty.RECRUIT, "1. ACEMİ", Color(0xFF16A34A)),
                        Triple(AiDifficulty.VETERAN, "2. KIDEMLİ", BoardColors.PurplePrimary),
                        Triple(AiDifficulty.GRAND_ADMIRAL, "3. BÜYÜK AMİRAL", BoardColors.CrimsonRed)
                    ).forEach { (diff, label, color) ->
                        val selected = selectedDifficulty == diff
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDifficultySelected(diff) },
                            color = if (selected) color.copy(alpha = 0.12f) else BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) color else BoardColors.BorderSubtle
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (selected) color else BoardColors.TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val aiStrategyDesc = when (selectedDifficulty) {
                    AiDifficulty.RECRUIT -> "Kolay • Rastgele keşif atışları, seyrek çevre takibi."
                    AiDifficulty.VETERAN -> "Orta • 6 eksenli Olasılık Haritası (H-PDF) & parite avlanma."
                    AiDifficulty.GRAND_ADMIRAL -> "Uzman • Gelişmiş H-PDF + ship-fitting + İHA ve kalkan."
                }
                Text(
                    text = aiStrategyDesc,
                    color = BoardColors.TextSecondary,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // KART 1: SAVAŞ ALANI BÜYÜKLÜĞÜ (Estetik Alan & Flu Mavi Animasyonlu Sınır)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, cyanBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("SAVAŞ ALANI BÜYÜKLÜĞÜ", color = BoardColors.NavyDark, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5 to "Standart (91)", 6 to "Geniş (127)", 7 to "Devasa (169)").forEach { (r, label) ->
                                val selected = mapRadius == r
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onRadiusChanged(r) },
                                    color = if (selected) BoardColors.RedLight else BoardColors.SurfaceSubtle,
                                    shape = RoundedCornerShape(7.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) BoardColors.CrimsonRed else BoardColors.BorderSubtle)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) BoardColors.CrimsonRed else BoardColors.TextPrimary,
                                        fontSize = 8.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KART 2: ÇEVRESEL KOŞULLAR (Estetik Alan & Flu Yeşil Animasyonlu Sınır)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, emeraldBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mercan Resifleri ve Manyetik Fırtınalar", color = BoardColors.TextPrimary, fontSize = 10.sp)
                        Switch(
                            checked = enableHazards,
                            onCheckedChange = onHazardsChanged,
                            colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.CrimsonRed)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KART 3: ÖZEL TAKTİKSEL PARAMETRELER (Flu Mor Animasyonlu Sınır)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, purpleBorder)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "ÖZEL TAKTİKSEL PARAMETRELER",
                            color = BoardColors.NavyDark,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TacticalParameterStepper(
                                    "Kalkan",
                                    maxShields,
                                    0..5,
                                    BoardColors.PurplePrimary,
                                    onMaxShieldsChanged,
                                    onInfoClick = { onInfoClick(TacticalParamType.SHIELD) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TacticalParameterStepper(
                                    "Sis (2'li)",
                                    radioactiveZones,
                                    0..5,
                                    Color(0xFF16A34A),
                                    onRadioactiveZonesChanged,
                                    onInfoClick = { onInfoClick(TacticalParamType.RADIOACTIVE) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TacticalParameterStepper(
                                    "Delta EMP",
                                    maxEmpStrikes,
                                    0..3,
                                    Color(0xFF06B6D4),
                                    onMaxEmpStrikesChanged,
                                    onInfoClick = { onInfoClick(TacticalParamType.EMP) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TacticalParameterStepper(
                                    "Amiral Salvo",
                                    maxLineBarrages,
                                    0..3,
                                    BoardColors.CrimsonRed,
                                    onMaxLineBarragesChanged,
                                    onInfoClick = { onInfoClick(TacticalParamType.BARRAGE) }
                                )
                            }
                        }

                        if (enableHazards) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    TacticalParameterStepper(
                                        "Resif",
                                        coralReefs,
                                        0..10,
                                        BoardColors.CoralReef,
                                        onCoralReefsChanged,
                                        onInfoClick = { onInfoClick(TacticalParamType.REEF) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    TacticalParameterStepper(
                                        "Fırtına",
                                        magneticStorms,
                                        0..10,
                                        BoardColors.CrimsonRed,
                                        onMagneticStormsChanged,
                                        onInfoClick = { onInfoClick(TacticalParamType.STORM) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SAVAŞI BAŞLAT BUTONU (En Altta, Ortalı & Estetik)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStartAiBattle,
                        modifier = Modifier.fillMaxWidth(0.96f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BoardColors.CrimsonRed),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(com.hexafleet.feature.lobby.R.drawable.ic_ai_robot_avatar),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "YAPAY ZEKAYA KARŞI SAVAŞI BAŞLAT",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ayarlanabilir Sayısal Taktiksel Parametre Stepper Bileşeni [-] [değer] [+] ve Bilgi (i) Butonu.
 */
@Composable
private fun TacticalParameterStepper(
    label: String,
    value: Int,
    range: IntRange,
    accentColor: Color,
    onValueChange: (Int) -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = label,
                        color = BoardColors.NavyDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (onInfoClick != null) {
                        Surface(
                            onClick = onInfoClick,
                            color = accentColor.copy(alpha = 0.14f),
                            shape = CircleShape,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "i",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "($value / ${range.last})",
                    color = BoardColors.TextMuted,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = value > range.first) { onValueChange(value - 1) },
                color = if (value > range.first) BoardColors.SurfaceSubtle else Color(0xFFF1F5F9),
                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "−",
                        color = if (value > range.first) BoardColors.NavyDark else BoardColors.TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Surface(
                modifier = Modifier.size(width = 32.dp, height = 26.dp),
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = value.toString(),
                        color = accentColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = value < range.last) { onValueChange(value + 1) },
                color = if (value < range.last) BoardColors.SurfaceSubtle else Color(0xFFF1F5F9),
                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        color = if (value < range.last) BoardColors.NavyDark else BoardColors.TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

enum class TacticalParamType {
    SHIELD,
    RADIOACTIVE,
    REEF,
    STORM,
    EMP,
    BARRAGE
}

data class TacticalParamInfo(
    val title: String,
    val subtitle: String,
    val badge: String,
    val accentColor: Color,
    val description: String,
    val tacticalImpact: String,
    val strategicTip: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

fun getTacticalParamInfo(type: TacticalParamType): TacticalParamInfo {
    return when (type) {
        TacticalParamType.SHIELD -> TacticalParamInfo(
            title = "Aegis Kalkanı",
            subtitle = "7 Peteklik Koruma Güç Alanı",
            badge = "Savunma Modülü",
            accentColor = BoardColors.PurplePrimary,
            description = "Seçtiğiniz merkezi koordinatın etrafında 7 peteklik yüksek teknolojili bir koruma kubbesi kurar.",
            tacticalImpact = "Kubbe altındaki dost gemiler düşman roket ve torpido saldırılarını sönümler. Merkezine doğrudan 2 darbe alana kadar altındaki filoyu hasardan korur.",
            strategicTip = "Amiral gemisi gibi büyük ve kıymetli gemilerinizi kalkan kubbesi altına konuşlandırarak düşmanın ilk tur erken darbelerinden koruyabilirsiniz.",
            icon = Icons.Default.Shield
        )
        TacticalParamType.RADIOACTIVE -> TacticalParamInfo(
            title = "Radyoaktif Sis Bölgesi",
            subtitle = "İHA Tespitine Karşı Kamuflaj (2 Petek)",
            badge = "Elektronik Harp & Gizlilik",
            accentColor = Color(0xFF16A34A),
            description = "Deniz üzerinde 2 petek uzunluğunda taktiksel yeşil radyoaktif siber sis koridoru oluşturur.",
            tacticalImpact = "Bu sis hattı içindeki gemiler, düşman İHA hava keşifleri (Recon drone) tarafından taranamaz ve tespit edilemez.",
            strategicTip = "Sisi doğrudan gemilerinizin üzerine yerleştirebileceğiniz gibi, boş deniz alanlarına yerleştirerek rakibin keşif İHA'larını tuzağa düşürebilirsiniz.",
            icon = Icons.Default.Warning
        )
        TacticalParamType.REEF -> TacticalParamInfo(
            title = "Mercan Resifi",
            subtitle = "Geçilmez Doğal Deniz Resifi",
            badge = "Doğal Siper & Engel",
            accentColor = BoardColors.CoralReef,
            description = "Deniz tabanından yükselen renkli ve sarp mercan resifleridir. Geçit vermeyen doğal su altı kayalık bariyeri oluşturur.",
            tacticalImpact = "Ne dost ne düşman gemileri bu peteklere konuşlandırılamaz. Düşmanın düz hat torpido ve su üstü atışları bu mercan resiflerine çarparak engellenir.",
            strategicTip = "Mercan resiflerinin arkasını doğal bir siper olarak kullanarak filonuzu düşman torpido hatlarından saklayabilirsiniz.",
            icon = Icons.Default.Landscape
        )
        TacticalParamType.STORM -> TacticalParamInfo(
            title = "Manyetik Fırtına Bölgesi",
            subtitle = "Gizli Sismik EMP Anomalisi",
            badge = "Radar Bozucu & Zırh",
            accentColor = BoardColors.PurpleDark,
            description = "Yüksek manyetik ve sismik iyon yoğunluğuna sahip mor anomalilerdir.",
            tacticalImpact = "SADECE sizin haritanızda görünür; düşman fırtınanın nerede olduğunu asla göremez! Fırtına içine isabet eden düşman atışlarının hasarı emilir ve radar parazitlenir.",
            strategicTip = "Fırtına bölgelerini gizli bir koruma hattı gibi kullanarak gemilerinizin omurgasını fırtına içine konuşlandırabilirsiniz.",
            icon = Icons.Default.ElectricBolt
        )
        TacticalParamType.EMP -> TacticalParamInfo(
            title = "Delta EMP İyon Şoku",
            subtitle = "Kalkan Çökertme & 1 CP Silme (0 CP)",
            badge = "Delta Kruvazörü Özel",
            accentColor = Color(0xFF06B6D4),
            description = "Delta Taarruz Kruvazörü hayattayken kullanılabilen 7 peteklik yüksek güçlü elektromanyetik darbedir.",
            tacticalImpact = "Hedef sektördeki düşman Aegis Kalkanı'nı anında çökertir. Eğer sektörde düşman gemisi varsa, rakibin bir sonraki turundaki Komut Puanı (CP) 3 yerine 2'ye düşer.",
            strategicTip = "Düşman kalkan açtığında doğrudan kalkanı delmek veya kritik turlarda rakibin hamle sayısını kısıtlamak için kullanın.",
            icon = Icons.Default.ElectricBolt
        )
        TacticalParamType.BARRAGE -> TacticalParamInfo(
            title = "Amiral Ağır Bombardımanı",
            subtitle = "5 Peteklik Doğrusal Süpürme (3 CP)",
            badge = "Amiral Zırhlısı Özel",
            accentColor = BoardColors.CrimsonRed,
            description = "Amiral Zırhlısı hayattayken kullanılabilen, altıgen eksen boyunca ardışık 5 peteği aynı anda vuran devasa top salvosudur.",
            tacticalImpact = "Havadan bombardıman yapıldığı için mercan resifleri mermileri durduramaz. Geniş deniz koridorlarında saklanan gemileri tek hamlede tarayıp vurur.",
            strategicTip = "Düşmanın uzun gemilerini (Amiral/İHA) veya resif arkasına gizlenmiş filoları süpürmek için maçın dönüm noktalarında ateşleyin.",
            icon = Icons.Default.AutoAwesome
        )
    }
}

@Composable
private fun TacticalInfoDialog(
    info: TacticalParamInfo,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BoardColors.PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, info.accentColor.copy(alpha = 0.6f)),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth(0.94f).padding(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Başlık Satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(info.accentColor.copy(alpha = 0.12f))
                                .border(1.2.dp, info.accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = info.icon,
                                contentDescription = null,
                                tint = info.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = info.title,
                                color = BoardColors.NavyDark,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = info.subtitle,
                                color = BoardColors.TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).background(BoardColors.SurfaceSubtle, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = BoardColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rozet
                Surface(
                    color = info.accentColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, info.accentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "● ${info.badge}",
                        color = info.accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Taktiksel Açıklama Kutusu
                Surface(
                    color = BoardColors.SurfaceSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ℹ", color = info.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = info.description,
                                color = BoardColors.NavyDark,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⚔", color = info.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = info.tacticalImpact,
                                color = BoardColors.TextPrimary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.5.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💡", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = info.strategicTip,
                                color = Color(0xFFB45309),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = info.accentColor)
                ) {
                    Text(
                        text = "ANLADIM",
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

@Composable
private fun AiDifficultyCard(
    difficulty: AiDifficulty,
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) badgeColor.copy(alpha = 0.08f) else BoardColors.SurfaceSubtle,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) badgeColor else BoardColors.BorderSubtle
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isSelected) badgeColor else BoardColors.NavyDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = BoardColors.TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}
