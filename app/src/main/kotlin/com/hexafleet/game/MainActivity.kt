package com.hexafleet.game

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hexafleet.domain.game.ai.AiDifficulty
import com.hexafleet.feature.board.model.GameCustomRules
import com.hexafleet.feature.board.model.TacticalGameMode
import com.hexafleet.feature.board.theme.BoardColors
import com.hexafleet.feature.board.ui.TacticalBoardScreen
import com.hexafleet.feature.lobby.ui.LobbyScreen

enum class GameScreen {
    LOBBY,
    BATTLE_BOARD
}

class MainActivity : ComponentActivity() {

    private var deepLinkRoomCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        extractRoomCodeFromIntent(intent)

        setContent {
            HexaFleetStandardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BoardColors.LightBackground
                ) {
                    var currentScreen by remember { mutableStateOf(GameScreen.LOBBY) }
                    var activeGameMode by remember {
                        mutableStateOf<TacticalGameMode>(TacticalGameMode.VsAi(AiDifficulty.VETERAN))
                    }
                    var activeMapRadius by remember { mutableIntStateOf(5) }
                    var activeHazardsEnabled by remember { mutableStateOf(true) }
                    var activeCustomRules by remember { mutableStateOf(GameCustomRules.defaultForRadius(5)) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            GameScreen.LOBBY -> {
                                LobbyScreen(
                                    initialJoinCode = deepLinkRoomCode,
                                    onStartOnlineGame = { code, isHost, turnDuration, radius, hazards, rules ->
                                        activeGameMode = TacticalGameMode.Online(
                                            roomCode = code,
                                            isHost = isHost,
                                            turnDurationSeconds = turnDuration
                                        )
                                        activeMapRadius = radius
                                        activeHazardsEnabled = hazards
                                        activeCustomRules = rules
                                        currentScreen = GameScreen.BATTLE_BOARD
                                    },
                                    onStartAiGame = { difficulty, radius, hazards, rules ->
                                        activeGameMode = TacticalGameMode.VsAi(difficulty)
                                        activeMapRadius = radius
                                        activeHazardsEnabled = hazards
                                        activeCustomRules = rules
                                        currentScreen = GameScreen.BATTLE_BOARD
                                    }
                                )
                            }
                            GameScreen.BATTLE_BOARD -> {
                                TacticalBoardScreen(
                                    gameMode = activeGameMode,
                                    mapRadius = activeMapRadius,
                                    enableHazards = activeHazardsEnabled,
                                    customRules = activeCustomRules,
                                    onBackToLobby = {
                                        currentScreen = GameScreen.LOBBY
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractRoomCodeFromIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun extractRoomCodeFromIntent(incomingIntent: Intent?) {
        val data: Uri? = incomingIntent?.data
        if (data != null && (data.scheme == "deepstrike" || data.scheme == "hexafleet") && data.host == "lobby") {
            // deepstrike://lobby/HX-7482 or hexafleet://lobby/HX-7482
            val pathSegment = data.lastPathSegment
            val queryCode = data.getQueryParameter("code")
            val code = queryCode ?: pathSegment
            if (!code.isNullOrBlank()) {
                deepLinkRoomCode = code.uppercase()
            }
        }
    }
}

/**
 * Deep Strike Standart Arayüz Teması: Modern Taktik Beyaz Taban, Lacivert, Mor ve Kırmızı Senkronizasyonu.
 */
@Composable
fun DeepStrikeStandardTheme(content: @Composable () -> Unit) = HexaFleetStandardTheme(content)

@Composable
fun HexaFleetStandardTheme(content: @Composable () -> Unit) {
    val standardLightScheme = lightColorScheme(
        primary = BoardColors.NavyAccent,
        secondary = BoardColors.PurplePrimary,
        tertiary = BoardColors.CrimsonRed,
        background = BoardColors.LightBackground,
        surface = BoardColors.PureWhite,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = BoardColors.NavyDark,
        onSurface = BoardColors.NavyDark
    )

    MaterialTheme(
        colorScheme = standardLightScheme,
        content = content
    )
}
