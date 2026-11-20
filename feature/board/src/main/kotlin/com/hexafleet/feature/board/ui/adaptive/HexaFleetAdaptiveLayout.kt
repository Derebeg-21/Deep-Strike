package com.hexafleet.feature.board.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Android Resmi Window Size Classes Standardına Göre Genişlik Sınıfları
 */
enum class WindowWidthClass {
    COMPACT,  // < 600.dp (Standart Dikey Telefonlar)
    MEDIUM,   // 600.dp .. 839.dp (Katlanabilir İç Ekranlar, Küçük Tabletler, Yatay Telefonlar)
    EXPANDED  // >= 840.dp (Büyük Tabletler, Masaüstü, Geniş Yatay Ekranlar)
}

/**
 * Android Resmi Window Size Classes Standardına Göre Yükseklik Sınıfları
 */
enum class WindowHeightClass {
    COMPACT,  // < 480.dp (Yatay Telefonlar - Dikey Alan Çok Kısıtlı!)
    MEDIUM,   // 480.dp .. 899.dp (Dikey Telefonlar, Yatay Tabletler, Katlanabilir Cihazlar)
    EXPANDED  // >= 900.dp (Dikey Büyük Tabletler)
}

/**
 * Genişlik + Yükseklik Matrisini Birlikte Değerlendiren Cihaz Duruş (Posture) Standardı.
 * Yatay tutulan telefonların (ör. 840x380 dp) yanlışlıkla "Tablet" sanılmasını kesin olarak engeller.
 */
enum class DevicePosture {
    COMPACT_PORTRAIT,   // Normal/Dar Dikey Telefon (Genişlik < 600dp, Yükseklik >= 480dp)
    COMPACT_LANDSCAPE,  // Yatay Telefon (Yükseklik < 480dp) — KRİTİK KOKPİT / YAN RAY MODU!
    FOLDABLE_SQUARE,    // Katlanabilir İç Ekran veya Küçük Tablet (Genişlik 600..839dp, Yükseklik >= 480dp)
    EXPANDED_TABLET     // Büyük Tablet / Masaüstü (Genişlik >= 840dp, Yükseklik >= 480dp)
}

/**
 * Tüm ekran tiplerinde (Dikey Telefon, Yatay Telefon, Foldable, Tablet) arayüz kararlarını
 * tek merkezden yöneten adaptif yapılandırma modeli.
 */
@Immutable
data class HexaFleetLayoutInfo(
    val posture: DevicePosture,
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isTabletOrExpanded: Boolean,
    val isCompactPortrait: Boolean,
    val isCompactLandscape: Boolean,
    val isFoldableSquare: Boolean,
    val isExpandedTablet: Boolean,
    val useMasterDetailLobby: Boolean,
    val usePipMiniRadarCombat: Boolean,
    val useSideRailCockpitCombat: Boolean,
    val useDualBoardSideBySide: Boolean,
    val showTacticalIntelSidebar: Boolean,
    val maxContentWidth: Dp,
    val containerPadding: PaddingValues
)

fun calculateHexaFleetLayoutInfo(width: Dp, height: Dp): HexaFleetLayoutInfo {
    val widthClass = when {
        width < 600.dp -> WindowWidthClass.COMPACT
        width < 840.dp -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }
    val heightClass = when {
        height < 480.dp -> WindowHeightClass.COMPACT
        height < 900.dp -> WindowHeightClass.MEDIUM
        else -> WindowHeightClass.EXPANDED
    }

    val posture = when {
        // Yükseklik 480dp altındaysa genişlik ne olursa olsun bu bir YATAY TELEFONDUR!
        heightClass == WindowHeightClass.COMPACT -> DevicePosture.COMPACT_LANDSCAPE
        widthClass == WindowWidthClass.COMPACT -> DevicePosture.COMPACT_PORTRAIT
        widthClass == WindowWidthClass.MEDIUM -> DevicePosture.FOLDABLE_SQUARE
        else -> DevicePosture.EXPANDED_TABLET
    }

    val isTabletOrExpanded = posture == DevicePosture.FOLDABLE_SQUARE || posture == DevicePosture.EXPANDED_TABLET
    val isCompactPortrait = posture == DevicePosture.COMPACT_PORTRAIT
    val isCompactLandscape = posture == DevicePosture.COMPACT_LANDSCAPE
    val isFoldableSquare = posture == DevicePosture.FOLDABLE_SQUARE
    val isExpandedTablet = posture == DevicePosture.EXPANDED_TABLET

    return HexaFleetLayoutInfo(
        posture = posture,
        widthClass = widthClass,
        heightClass = heightClass,
        screenWidth = width,
        screenHeight = height,
        isTabletOrExpanded = isTabletOrExpanded,
        isCompactPortrait = isCompactPortrait,
        isCompactLandscape = isCompactLandscape,
        isFoldableSquare = isFoldableSquare,
        isExpandedTablet = isExpandedTablet,
        useMasterDetailLobby = width >= 640.dp,
        usePipMiniRadarCombat = isCompactPortrait,
        useSideRailCockpitCombat = isCompactLandscape,
        useDualBoardSideBySide = isTabletOrExpanded || isCompactLandscape,
        showTacticalIntelSidebar = isExpandedTablet && width >= 920.dp,
        maxContentWidth = 960.dp,
        containerPadding = when (posture) {
            DevicePosture.EXPANDED_TABLET -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            DevicePosture.FOLDABLE_SQUARE -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            DevicePosture.COMPACT_LANDSCAPE -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            DevicePosture.COMPACT_PORTRAIT -> PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        }
    )
}

val LocalHexaFleetLayout = staticCompositionLocalOf<HexaFleetLayoutInfo?> { null }

@Composable
fun rememberHexaFleetLayoutInfo(maxWidth: Dp, maxHeight: Dp): HexaFleetLayoutInfo {
    return remember(maxWidth, maxHeight) {
        calculateHexaFleetLayoutInfo(maxWidth, maxHeight)
    }
}

/**
 * Herhangi bir Composable veya Dialog içerisinden mevcut ekranın DevicePosture / LayoutInfo
 * bilgisine güvenle erişmeyi sağlar.
 */
@Composable
fun currentHexaFleetLayoutInfo(): HexaFleetLayoutInfo {
    val provided = LocalHexaFleetLayout.current
    if (provided != null) return provided
    val config = LocalConfiguration.current
    return remember(config.screenWidthDp, config.screenHeightDp) {
        calculateHexaFleetLayoutInfo(config.screenWidthDp.dp, config.screenHeightDp.dp)
    }
}

/**
 * Kök bileşende ekran boyutunu dinleyerek tüm alt ağaca [LocalHexaFleetLayout] sağlar.
 */
@Composable
fun ProvideHexaFleetLayout(
    modifier: Modifier = Modifier,
    content: @Composable (HexaFleetLayoutInfo) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutInfo = rememberHexaFleetLayoutInfo(maxWidth, maxHeight)
        CompositionLocalProvider(LocalHexaFleetLayout provides layoutInfo) {
            content(layoutInfo)
        }
    }
}

/**
 * Kamera deliği (Cutout / Notch) ve sistem güvenli çizim alanlarını hesaplayan yardımcı.
 */
@Composable
fun rememberSafeBoardInsetsPadding(): PaddingValues {
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
    return remember(cutout, safeDrawing) {
        safeDrawing
    }
}
