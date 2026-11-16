package com.hexafleet.feature.board.canvas

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.core.hexmath.HexLayout
import com.hexafleet.feature.board.model.FogVisibility
import com.hexafleet.feature.board.model.HexBoardUiState
import com.hexafleet.feature.board.model.HexTileUiModel
import com.hexafleet.feature.board.model.ShieldDome
import com.hexafleet.feature.board.model.TerrainType
import com.hexafleet.feature.board.model.TileHighlightState
import com.hexafleet.feature.board.theme.BoardColors
import com.hexafleet.feature.board.visuals.ShipVisuals
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Ekran boyutuna (Sağ-Sol veya Alt-Üst) otomatik ölçeklenen (Auto-Fit),
 * sıfır koordinat sapmalı, estetik flu 7 peteklik Kalkan Kubbesi (Aegis Shield Dome),
 * gizli düşman manyetik fırtınaları ve gerçekçi zırhlı gemi gövdesi efektlerine sahip
 * Altıgen Amiral Battı Render Motoru.
 */
@Composable
fun HexBoardCanvas(
    state: HexBoardUiState,
    modifier: Modifier = Modifier,
    onHexClicked: (HexCoordinate) -> Unit,
    onRotateShip: ((clockwise: Boolean) -> Unit)? = null,
    onDragHover: ((HexCoordinate) -> Unit)? = null,
    onDragRelease: ((HexCoordinate) -> Unit)? = null
) {
    // Radar ve Nabız (Pulse) Animasyonları
    val infiniteTransition = rememberInfiniteTransition(label = "HexBoardAnimations")
    val context = LocalContext.current
    val shipBitmaps = remember(context) { ShipVisuals.loadShipBitmaps(context) }

    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ReticlePulse"
    )

    val arrowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ArrowPhase"
    )

    // Manyetik Fırtına Mor Sis Dalgalanması Animasyonu (Yavaşlatılmış, Zarif ve Akıcı)
    val stormWaveAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "StormWaveDrift"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(state.radius, onRotateShip, state.previewShipParts, onDragHover, onDragRelease) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val hexSize = calculateOptimalHexSize(w, h, state.radius)
                    val layout = HexLayout(size = hexSize)

                    val startWorldX = down.position.x - (w / 2f)
                    val startWorldY = down.position.y - (h / 2f)

                    // 1. Seçili gemi üstündeki 2 yönlü kırmızı döndürme butonlarına dokunma kontrolü
                    if (onRotateShip != null && state.previewShipParts.isNotEmpty()) {
                        val hudCenter = calculateShipHudCenter(state.previewShipParts, layout, hexSize, h)
                        val btnOffset = hexSize * 0.90f
                        val ccwCenter = Offset(hudCenter.x - btnOffset, hudCenter.y)
                        val cwCenter = Offset(hudCenter.x + btnOffset, hudCenter.y)
                        val hitRadius = (hexSize * 0.48f).coerceAtLeast(36f)

                        val dCcw = hypot(startWorldX - ccwCenter.x, startWorldY - ccwCenter.y)
                        val dCw = hypot(startWorldX - cwCenter.x, startWorldY - cwCenter.y)

                        if (dCcw <= hitRadius) {
                            onRotateShip(false) // Saatin tersine / Sola döndür
                            down.consume()
                            return@awaitEachGesture
                        }
                        if (dCw <= hitRadius) {
                            onRotateShip(true) // Saat yönünde / Sağa döndür
                            down.consume()
                            return@awaitEachGesture
                        }
                    }

                    val downHex = layout.pixelToHex(startWorldX, startWorldY)
                    var isDrag = false
                    var currentHex = downHex
                    val touchSlop = 14f

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            // Parmak kaldırıldı (Release / Drop)
                            if (isDrag && onDragRelease != null) {
                                if (currentHex.distanceTo(HexCoordinate.ZERO) <= state.radius) {
                                    onDragRelease(currentHex)
                                }
                            } else {
                                if (downHex.distanceTo(HexCoordinate.ZERO) <= state.radius) {
                                    onHexClicked(downHex)
                                }
                            }
                            change.consume()
                            break
                        }

                        val dist = hypot(change.position.x - down.position.x, change.position.y - down.position.y)
                        if (dist > touchSlop) {
                            isDrag = true
                            val worldX = change.position.x - (w / 2f)
                            val worldY = change.position.y - (h / 2f)
                            val hoverHex = layout.pixelToHex(worldX, worldY)
                            if (hoverHex != currentHex) {
                                currentHex = hoverHex
                                if (hoverHex.distanceTo(HexCoordinate.ZERO) <= state.radius) {
                                    onDragHover?.invoke(hoverHex)
                                }
                            }
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val hexSize = calculateOptimalHexSize(size.width, size.height, state.radius)
        val cache = HexPathCache(hexSize)

        // 0. KATMAN: Arka Plan Taktiksel Okyanus Dalgası İllüstrasyonu (User Requirement 1)
        // Statik (animasyonsuz) ve kesinlikle üst başlık alanına taşmayacak şekilde tahta sınırları içine kırpılmış.
        val waveBmp = shipBitmaps["board_ocean_waves"]
        if (waveBmp != null) {
            clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
                val bmpW = waveBmp.width.toFloat()
                val bmpH = waveBmp.height.toFloat()
                val scale = max(size.width / bmpW, size.height / bmpH)
                val scaledW = bmpW * scale
                val scaledH = bmpH * scale
                val offsetX = (size.width - scaledW) / 2f
                val offsetY = (size.height - scaledH) / 2f

                drawImage(
                    image = waveBmp,
                    dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                    dstSize = IntSize(scaledW.roundToInt(), scaledH.roundToInt()),
                    alpha = 0.30f
                )
            }
        }

        translate(left = centerX, top = centerY) {
            // 1. KATMAN: Altıgen Deniz Izgarası, Çevre Unsurları ve Sis Durumu
            drawHexGridAndTerrain(
                state = state,
                cache = cache,
                mountainBitmap = shipBitmaps["terrain_mountain"],
                radioactiveBitmap = shipBitmaps["hazard_radioactive"],
                stormBitmap = shipBitmaps["hazard_storm"],
                pulseAlpha = pulseAlpha,
                stormWaveAngle = stormWaveAngle
            )

            // 2. KATMAN: Zırhlı Savaş Gemisi Gövdeleri (Dost Filo veya Batırılmış Düşman Gemileri)
            drawArmoredShips(state, cache, shipBitmaps, pulseAlpha)

            // 2.5 KATMAN: Gemi Altındaki Radyoaktif Sis ve Manyetik Fırtına Flu Gösterimi (User Requirement 2 & 4)
            if (!state.isAttackBoard) {
                drawSubmergedShipHazardOverlays(
                    state = state,
                    cache = cache,
                    radioactiveBitmap = shipBitmaps["hazard_radioactive"],
                    stormBitmap = shipBitmaps["hazard_storm"],
                    pulseAlpha = pulseAlpha,
                    stormWaveAngle = stormWaveAngle
                )
            }

            // 3. KATMAN: SADECE DOST HARİTADA: Flu Estetik Kalkan Kubbeleri (Aegis Shield Domes)
            if (!state.isAttackBoard) {
                val domes = if (state.shieldDomes.isNotEmpty()) state.shieldDomes else listOfNotNull(state.shieldDome)
                for (dome in domes) {
                    drawShieldDome(dome, cache, pulseAlpha)
                }
            }

            // 4. KATMAN: Gemi Yerleştirme Önizlemesi (Varsa)
            if (state.previewShipParts.isNotEmpty()) {
                drawShipPlacementPreview(state, cache, pulseAlpha, shipBitmaps)
            }

            // 4.5 KATMAN: Seçili Gemi Üzerinde Flu Taktiksel Çift Yönlü Döndürme HUD'ı (User Requirement 1)
            if (onRotateShip != null && state.previewShipParts.isNotEmpty()) {
                val hudCenter = calculateShipHudCenter(state.previewShipParts, cache.layout, hexSize, size.height)
                drawShipRotationHud(
                    hudCenter = hudCenter,
                    hexSize = hexSize,
                    pulseAlpha = pulseAlpha,
                    arrowPhase = arrowPhase
                )
            }

            // 5. KATMAN: Radar Süpürme Işını (Saldırı Haritasında)
            if (state.isRadarSweepActive) {
                val maxRadarRadius = (state.radius + 0.6f) * hexSize * 1.72f
                drawRadarSweep(maxRadarRadius, radarAngle)
            }

            // 6. KATMAN: Patlamalar (HIT), Su Sıçramaları (MISS), İHA Keşifleri, Kalkan Sönümlemeleri ve Sabit Hedefleme İmleci
            drawCombatMarkersAndReticle(state, cache, pulseAlpha)
        }
    }
}

/**
 * Verilen Canvas genişlik ve yüksekliğine göre altıgen ızgaranın taşmadan
 * tam ortalı ve maksimum büyüklükte görünmesini sağlayan ideal piksel yarıçapını hesaplar.
 */
private fun calculateOptimalHexSize(width: Float, height: Float, radius: Int): Float {
    val hexSpanWidth = (radius * 2 + 1.35f) * 1.73205f // sqrt(3) * (2R + 1.35)
    val hexSpanHeight = (radius * 3f + 2.35f)          // 1.5 * 2R + 2.35
    val maxByWidth = (width * 0.94f) / hexSpanWidth
    val maxByHeight = (height * 0.94f) / hexSpanHeight
    return min(maxByWidth, maxByHeight).coerceAtLeast(8f)
}

/**
 * Altıgen zemin peteklerini, savaş sisini ve çevre engellerini (Mercan Resifi, Manyetik Fırtına) çizer.
 * DÜŞMANIN MANYETİK FIRTINASI ASLA ÇİZİLMEZ! (Oyuncu düşman fırtınalarını göremez).
 */
private fun DrawScope.drawHexGridAndTerrain(
    state: HexBoardUiState,
    cache: HexPathCache,
    mountainBitmap: ImageBitmap? = null,
    radioactiveBitmap: ImageBitmap? = null,
    stormBitmap: ImageBitmap? = null,
    pulseAlpha: Float = 1f,
    stormWaveAngle: Float = 0f
) {
    val allCoords = HexCoordinate.ZERO.area(state.radius)

    for (coord in allCoords) {
        val tile = state.tiles[coord] ?: HexTileUiModel(coordinate = coord)
        val pixelCenter = cache.layout.hexToPixel(coord)

        translate(left = pixelCenter.x, top = pixelCenter.y) {
            // Zemin rengi: Saldırı radarı ile Dost filo haritası arasında net görsel ayrım
            val baseWaterColor = if (state.isAttackBoard) {
                if (tile.visibility == FogVisibility.VISIBLE) Color(0xFFE0E7FF) else Color(0xFFDBEAFE)
            } else {
                Color(0xFFE2E8F0)
            }

            // Düşman haritasında Manyetik Fırtına normal su gibi görünür (GİZLİDİR)
            val showStorm = !state.isAttackBoard && tile.terrain == TerrainType.MAGNETIC_STORM
            val isRadioactive = !state.isAttackBoard && coord in state.radioactiveCoords

            val fillColor = when {
                tile.highlight == TileHighlightState.HIT -> Color(0xFFFEE2E2)
                tile.highlight == TileHighlightState.SUNK_SHIP -> Color(0xFFFECACA)
                tile.highlight == TileHighlightState.SHIELD_ABSORBED -> Color(0xFFE0F2FE)
                tile.highlight == TileHighlightState.RECON_DETECTED -> Color(0xFFEDE9FE)
                tile.highlight == TileHighlightState.RECON_CLEAR -> Color(0xFFF3E8FF)
                isRadioactive -> Color(0xFFDCFCE7) // Taktik yeşil sis taban rengi
                tile.terrain == TerrainType.CORAL_REEF -> Color(0xFFFFEDD5)
                showStorm -> Color(0xFFEDE9FE)
                tile.terrain == TerrainType.OCEAN_CURRENT_E || tile.terrain == TerrainType.OCEAN_CURRENT_W -> Color(0xFFE0F2FE)
                else -> baseWaterColor
            }

            drawPath(path = cache.baseHexPath, color = fillColor)

            // Altıgen Çerçeve Çizgisi
            val strokeColor = when {
                tile.highlight == TileHighlightState.HIT || tile.highlight == TileHighlightState.SUNK_SHIP ->
                    BoardColors.CrimsonRed.copy(alpha = 0.65f)
                tile.highlight == TileHighlightState.SHIELD_ABSORBED ->
                    BoardColors.ShieldColor.copy(alpha = 0.8f)
                tile.highlight == TileHighlightState.RECON_DETECTED ->
                    BoardColors.PurplePrimary
                isRadioactive ->
                    Color(0xFF22C55E).copy(alpha = 0.85f)
                state.isAttackBoard ->
                    Color(0xFF94A3B8)
                else ->
                    Color(0xFF64748B)
            }

            drawPath(
                path = cache.baseHexPath,
                color = strokeColor,
                style = Stroke(width = 1.6f)
            )

            // Çevre Engeli İkonları
            // 1. Dağ / Ada Görseli (User Requirement 1: Ekteki 1. resim profesyonelce yerleştirildi)
            if (tile.terrain == TerrainType.CORAL_REEF) {
                if (mountainBitmap != null) {
                    val mw = cache.hexSize * 1.82f
                    val mh = mw * (mountainBitmap.height.toFloat() / mountainBitmap.width.toFloat())
                    drawImage(
                        image = mountainBitmap,
                        dstOffset = IntOffset((-mw / 2f).roundToInt(), (-mh / 2f).roundToInt()),
                        dstSize = IntSize(mw.roundToInt(), mh.roundToInt())
                    )
                } else {
                    drawPath(
                        path = cache.reefPath,
                        color = BoardColors.CoralReef.copy(alpha = 0.25f)
                    )
                    drawPath(
                        path = cache.reefPath,
                        color = BoardColors.CoralReef,
                        style = Stroke(width = 2f, join = StrokeJoin.Round)
                    )
                }
            }

            // 2. Manyetik Fırtına (Mor sis, %20 artırılmış fluluk ve yavaşlatılmış dalgalanma animasyonlu)
            if (showStorm) {
                if (stormBitmap != null) {
                    val waveSin = sin(Math.toRadians(stormWaveAngle.toDouble())).toFloat()
                    val waveCos = cos(Math.toRadians(stormWaveAngle.toDouble())).toFloat()
                    val stormSize = cache.hexSize * (1.64f + 0.06f * waveSin)
                    val stormAlpha = (0.60f + 0.05f * waveSin).coerceIn(0.48f, 0.70f)
                    val driftDeg = waveCos * 5f
                    val driftOffset = Offset(waveSin * 2.2f, waveCos * 1.5f)

                    // Flu mor siber aura (Yumuşak ve transparan)
                    drawCircle(
                        color = BoardColors.PurplePrimary.copy(alpha = 0.14f + 0.05f * waveSin),
                        radius = cache.hexSize * 0.48f,
                        center = Offset.Zero
                    )
                    rotate(degrees = driftDeg, pivot = Offset.Zero) {
                        drawImage(
                            image = stormBitmap,
                            dstOffset = IntOffset(
                                (driftOffset.x - stormSize / 2f).roundToInt(),
                                (driftOffset.y - stormSize / 2f).roundToInt()
                            ),
                            dstSize = IntSize(stormSize.roundToInt(), stormSize.roundToInt()),
                            alpha = stormAlpha
                        )
                    }
                } else {
                    drawCircle(
                        color = BoardColors.MagneticStorm.copy(alpha = 0.55f),
                        radius = cache.hexSize * 0.42f,
                        center = Offset.Zero,
                        style = Stroke(width = 1.8f)
                    )
                    drawCircle(
                        color = BoardColors.MagneticStorm.copy(alpha = 0.25f),
                        radius = cache.hexSize * 0.22f,
                        center = Offset.Zero
                    )
                }
            }

            // 3. Radyoaktif Sis Bölgesi (%25 daha flu ve yumuşak neon yeşil animasyonlu)
            if (isRadioactive) {
                if (radioactiveBitmap != null) {
                    val radSize = cache.hexSize * 1.55f
                    val radAlpha = (0.48f + 0.16f * pulseAlpha).coerceIn(0.25f, 0.68f)

                    // Flu neon yeşil puls aurası (Yumuşak ve estetik)
                    drawCircle(
                        color = Color(0xFF22C55E).copy(alpha = 0.16f * pulseAlpha),
                        radius = cache.hexSize * 0.50f,
                        center = Offset.Zero
                    )
                    drawImage(
                        image = radioactiveBitmap,
                        dstOffset = IntOffset((-radSize / 2f).roundToInt(), (-radSize / 2f).roundToInt()),
                        dstSize = IntSize(radSize.roundToInt(), radSize.roundToInt()),
                        alpha = radAlpha
                    )
                } else {
                    drawCircle(
                        color = Color(0xFF22C55E).copy(alpha = 0.35f),
                        radius = cache.hexSize * 0.52f,
                        center = Offset.Zero
                    )
                    drawCircle(
                        color = Color(0xFF16A34A).copy(alpha = 0.55f),
                        radius = cache.hexSize * 0.30f,
                        center = Offset.Zero,
                        style = Stroke(width = 1.8f)
                    )
                }
            }

            // Akıntı okları
            if (tile.terrain == TerrainType.OCEAN_CURRENT_E) {
                drawPath(
                    path = cache.currentChevronPath,
                    color = BoardColors.OceanCurrent,
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                )
            } else if (tile.terrain == TerrainType.OCEAN_CURRENT_W) {
                rotate(degrees = 180f, pivot = Offset.Zero) {
                    drawPath(
                        path = cache.currentChevronPath,
                        color = BoardColors.OceanCurrent,
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * 7 Peteklik Heksagonal Aegis Savunma Kubbesi (Flu, Estetik, Siber Kalkan Halesi).
 * SADECE DOST HARİTADA çizilir!
 */
private fun DrawScope.drawShieldDome(
    shieldDome: ShieldDome,
    cache: HexPathCache,
    pulseAlpha: Float
) {
    val centerPoint = cache.layout.hexToPixel(shieldDome.center)

    // 1. 7 Peteğin her birine yumuşak flu kalkan halesi ve kenarlık çiz
    for (coord in shieldDome.protectedCoords) {
        val p = cache.layout.hexToPixel(coord)
        val isCenter = coord == shieldDome.center

        translate(left = p.x, top = p.y) {
            // Flu saydam koruma dolgusu
            drawPath(
                path = cache.baseHexPath,
                color = Color(0xFF2563EB).copy(alpha = if (isCenter) 0.28f * pulseAlpha else 0.16f * pulseAlpha)
            )

            // Heksagonal enerji teli
            drawPath(
                path = cache.baseHexPath,
                color = Color(0xFF38BDF8).copy(alpha = 0.55f * pulseAlpha),
                style = Stroke(width = if (isCenter) 2.4f else 1.4f, join = StrokeJoin.Round)
            )
        }
    }

    // 2. Kalkan Merkezinde Estetik Kalkan Jeneratör Çekirdeği ve Can Barları (centerHp)
    translate(left = centerPoint.x, top = centerPoint.y) {
        // Dış puls halkası
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.45f * pulseAlpha),
            radius = cache.hexSize * 0.58f,
            center = Offset.Zero,
            style = Stroke(width = 2.2f)
        )

        // İç kalkan çekirdeği
        drawCircle(
            color = Color(0xFF1D4ED8).copy(alpha = 0.85f),
            radius = cache.hexSize * 0.36f,
            center = Offset.Zero
        )

        // Merkez Kalkan Can Göstergesi (centerHp: 2 -> iki mavi bar, 1 -> bir sarı bar)
        val barWidth = cache.hexSize * 0.16f
        val barHeight = cache.hexSize * 0.08f
        val gap = cache.hexSize * 0.05f

        if (shieldDome.centerHp >= 2) {
            drawRect(
                color = Color(0xFF93C5FD),
                topLeft = Offset(-barWidth - (gap / 2f), -barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
            drawRect(
                color = Color(0xFF93C5FD),
                topLeft = Offset((gap / 2f), -barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        } else if (shieldDome.centerHp == 1) {
            drawRect(
                color = Color(0xFFFBBF24), // 1 darbe almış, sarı uyarı barı
                topLeft = Offset(-barWidth / 2f, -barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}

/**
 * Dost filoyu ve batırılmış düşman gemilerini birbirine bağlı gerçekçi zırhlı savaş gemisi
 * gövdeleri, petek altı taktiksel modül noktaları (points), ilişkili omurga hatları (data-links)
 * ve yüksek çözünürlüklü 2D kuşbakışı Deep Strike donanma spritelarıyla çizer.
 */
private fun DrawScope.drawArmoredShips(
    state: HexBoardUiState,
    cache: HexPathCache,
    shipBitmaps: Map<String, ImageBitmap>,
    pulseAlpha: Float
) {
    for (ship in state.ships) {
        if (ship.parts.isEmpty()) continue

        val isEnemy = ship.isEnemy
        val isShipSelected = !isEnemy && (
            ship.parts.any { it in state.previewShipParts || it == state.selectedCoordinate }
        )
        val outerHullColor = if (isEnemy) Color(0xFF991B1B) else BoardColors.NavyDark
        val innerDeckColor = if (isEnemy) BoardColors.CrimsonRed else BoardColors.NavyAccent

        // =========================================================================
        // 1. GEMİ ALTI TAKTİKSEL NOKTALAR (POINT DOCKING PADS)
        // Her gemi peteği için altıgen modül yuvası çizilir
        // =========================================================================
        for (partCoord in ship.parts) {
            val c = cache.layout.hexToPixel(partCoord)
            val isDamaged = partCoord in ship.hitParts
            val isPartSelected = isShipSelected || partCoord == state.selectedCoordinate || partCoord in state.previewShipParts

            translate(left = c.x, top = c.y) {
                if (isDamaged) {
                    // Hasarlı petek altlığı (Alarm kırmızı yanık zemin)
                    drawPath(
                        path = cache.baseHexPath,
                        color = Color(0xFFEF4444).copy(alpha = 0.25f * pulseAlpha)
                    )
                    drawPath(
                        path = cache.baseHexPath,
                        color = Color(0xFFEF4444).copy(alpha = 0.85f * pulseAlpha),
                        style = Stroke(width = 2.5f)
                    )
                } else if (isPartSelected) {
                    // Seçili gemi petek altlığı (User Requirement 7: Yanıp sönen taktiksel sarı/amber enerji vurgusu)
                    drawPath(
                        path = cache.baseHexPath,
                        color = Color(0xFFEAB308).copy(alpha = 0.28f * pulseAlpha)
                    )
                    drawPath(
                        path = cache.baseHexPath,
                        color = Color(0xFFFACC15).copy(alpha = pulseAlpha),
                        style = Stroke(width = 2.6f)
                    )
                } else {
                    // Standart sağlam gemi petek altlığı (Siber-Lacivert / Gövde Dock Tabanı)
                    val padBg = if (isEnemy) Color(0xFF991B1B).copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.12f)
                    val padBorder = if (isEnemy) Color(0xFFEF4444).copy(alpha = 0.50f) else Color(0xFF38BDF8).copy(alpha = 0.45f)
                    drawPath(
                        path = cache.baseHexPath,
                        color = padBg
                    )
                    drawPath(
                        path = cache.baseHexPath,
                        color = padBorder,
                        style = Stroke(width = 1.6f)
                    )
                    // İç modül yuva halkası
                    drawCircle(
                        color = padBorder.copy(alpha = 0.20f),
                        radius = cache.hexSize * 0.38f,
                        center = Offset.Zero
                    )
                }
            }
        }

        // =========================================================================
        // 2. İLİŞKİLİ NOKTALAR BAĞLANTI HATTI (INTER-POINT DATA-LINK / SPINE CONDUITS)
        // Geminin petekleri arasındaki omurga ve veri hatları birbirine bağlanır
        // =========================================================================
        if (ship.isTriangleShape && ship.parts.size == 3) {
            val p0 = cache.layout.hexToPixel(ship.parts[0])
            val p1 = cache.layout.hexToPixel(ship.parts[1])
            val p2 = cache.layout.hexToPixel(ship.parts[2])
            val pts = listOf(Offset(p0.x, p0.y), Offset(p1.x, p1.y), Offset(p2.x, p2.y))

            for (i in 0 until 3) {
                val j = (i + 1) % 3
                val isLinkDamaged = ship.parts[i] in ship.hitParts || ship.parts[j] in ship.hitParts
                val conduitColor = when {
                    isLinkDamaged -> Color(0xFF7F1D1D)
                    isEnemy -> Color(0xFF991B1B)
                    isShipSelected -> Color(0xFFEAB308) // User Requirement 7: Sarı renk
                    else -> Color(0xFF0284C7)
                }
                val coreColor = when {
                    isLinkDamaged -> Color(0xFFEF4444).copy(alpha = 0.85f * pulseAlpha)
                    isEnemy -> Color(0xFFF87171)
                    isShipSelected -> Color(0xFFFEF08A)
                    else -> Color(0xFF38BDF8)
                }
                // Dış zırhlı kablo
                drawLine(
                    color = conduitColor.copy(alpha = 0.55f),
                    start = pts[i],
                    end = pts[j],
                    strokeWidth = cache.hexSize * 0.20f,
                    cap = StrokeCap.Round
                )
                // İç parlak veri akış çekirdeği
                drawLine(
                    color = coreColor,
                    start = pts[i],
                    end = pts[j],
                    strokeWidth = cache.hexSize * 0.08f,
                    cap = StrokeCap.Round
                )
            }
        } else if (ship.parts.size >= 2) {
            for (i in 0 until ship.parts.size - 1) {
                val pA = cache.layout.hexToPixel(ship.parts[i])
                val pB = cache.layout.hexToPixel(ship.parts[i + 1])
                val isLinkDamaged = ship.parts[i] in ship.hitParts || ship.parts[i + 1] in ship.hitParts
                val conduitColor = when {
                    isLinkDamaged -> Color(0xFF7F1D1D)
                    isEnemy -> Color(0xFF991B1B)
                    isShipSelected -> Color(0xFFEAB308) // User Requirement 7: Sarı renk
                    else -> Color(0xFF0284C7)
                }
                val coreColor = when {
                    isLinkDamaged -> Color(0xFFEF4444).copy(alpha = 0.85f * pulseAlpha)
                    isEnemy -> Color(0xFFF87171)
                    isShipSelected -> Color(0xFFFEF08A)
                    else -> Color(0xFF38BDF8)
                }
                // Dış zırhlı kablo
                drawLine(
                    color = conduitColor.copy(alpha = 0.55f),
                    start = Offset(pA.x, pA.y),
                    end = Offset(pB.x, pB.y),
                    strokeWidth = cache.hexSize * 0.22f,
                    cap = StrokeCap.Round
                )
                // İç parlak veri akış çekirdeği
                drawLine(
                    color = coreColor,
                    start = Offset(pA.x, pA.y),
                    end = Offset(pB.x, pB.y),
                    strokeWidth = cache.hexSize * 0.09f,
                    cap = StrokeCap.Round
                )
            }
        }

        // =========================================================================
        // 3. YÜKSEK ÇÖZÜNÜRLÜKLÜ 2D KUŞBAKIŞI SAVAŞ GEMİSİ SPRITE ÇİZİMİ
        // =========================================================================
        val shipKey = when {
            ship.name.contains("Korvet", ignoreCase = true) || ship.id.contains("corvette", ignoreCase = true) -> "corvette"
            ship.isTriangleShape || ship.name.contains("Delta", ignoreCase = true) || ship.id.contains("delta", ignoreCase = true) -> "delta"
            ship.name.contains("İHA", ignoreCase = true) || ship.id.contains("carrier", ignoreCase = true) -> "carrier"
            else -> "flagship"
        }
        val bitmapKey = "${shipKey}_${if (isEnemy) "enemy" else "friendly"}"
        val bmp = shipBitmaps[bitmapKey]

        if (ship.isTriangleShape && ship.parts.size == 3) {
            val p0 = cache.layout.hexToPixel(ship.parts[0])
            val p1 = cache.layout.hexToPixel(ship.parts[1])
            val p2 = cache.layout.hexToPixel(ship.parts[2])
            val center = Offset((p0.x + p1.x + p2.x) / 3f, (p0.y + p1.y + p2.y) / 3f)
            val sternMid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
            val dx = p0.x - sternMid.x
            val dy = p0.y - sternMid.y
            val (_, rotDeg) = ShipVisuals.calculateShipHeading(dx, dy, ShipVisuals.BASE_ANGLE_DELTA)

            if (bmp != null) {
                val (spriteWidth, spriteHeight) = ShipVisuals.getShipSpriteDimensions(
                    shipKey = "delta",
                    length = 3,
                    isTriangle = true,
                    hexSize = cache.hexSize,
                    bmpWidth = bmp.width,
                    bmpHeight = bmp.height
                )
                rotate(degrees = rotDeg, pivot = center) {
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(
                            (center.x - spriteWidth / 2f).roundToInt(),
                            (center.y - spriteHeight / 2f).roundToInt()
                        ),
                        dstSize = IntSize(spriteWidth.roundToInt(), spriteHeight.roundToInt()),
                        alpha = if (ship.isSunk) 0.55f else 1.0f
                    )
                }
            } else {
                val deltaPath = Path().apply {
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    close()
                }
                drawPath(path = deltaPath, color = outerHullColor.copy(alpha = 0.45f))
            }
        } else {
            val firstCenter = cache.layout.hexToPixel(ship.parts.first())
            val lastCenter = cache.layout.hexToPixel(ship.parts.last())
            val center = Offset((firstCenter.x + lastCenter.x) / 2f, (firstCenter.y + lastCenter.y) / 2f)
            val dx = firstCenter.x - lastCenter.x
            val dy = firstCenter.y - lastCenter.y
            val baseAngle = ShipVisuals.getShipBaseAngle(shipKey)
            val (_, rotDeg) = ShipVisuals.calculateShipHeading(dx, dy, baseAngle)

            if (bmp != null) {
                val (spriteWidth, spriteHeight) = ShipVisuals.getShipSpriteDimensions(
                    shipKey = shipKey,
                    length = ship.length,
                    isTriangle = false,
                    hexSize = cache.hexSize,
                    bmpWidth = bmp.width,
                    bmpHeight = bmp.height
                )
                rotate(degrees = rotDeg, pivot = center) {
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(
                            (center.x - spriteWidth / 2f).roundToInt(),
                            (center.y - spriteHeight / 2f).roundToInt()
                        ),
                        dstSize = IntSize(spriteWidth.roundToInt(), spriteHeight.roundToInt()),
                        alpha = if (ship.isSunk) 0.55f else 1.0f
                    )
                }
            } else {
                drawLine(
                    color = outerHullColor.copy(alpha = 0.45f),
                    start = Offset(firstCenter.x, firstCenter.y),
                    end = Offset(lastCenter.x, lastCenter.y),
                    strokeWidth = cache.hexSize * 0.95f,
                    cap = StrokeCap.Round
                )
            }
        }

        // =========================================================================
        // 4. GEMİ ÜSTÜ TAKTİKSEL POINTLER & PROFESYONEL VURULMA EFEKTİ (POINT FX)
        // Her modül hücresi için sağlam gösterge veya profesyonel alevli hasar efekti
        // =========================================================================
        for (partCoord in ship.parts) {
            val c = cache.layout.hexToPixel(partCoord)
            val isDamaged = partCoord in ship.hitParts

            if (isDamaged) {
                // BU ALAN VURULDUĞUNDA PROFESYONEL POINT EFEKT!
                drawProfessionalPointHitFx(
                    center = Offset(c.x, c.y),
                    hexSize = cache.hexSize,
                    pulseAlpha = pulseAlpha
                )
            } else if (ship.isSunk) {
                // Batık düşman gemisinin hasarlı peteklerinde de hasarlı retikül
                drawProfessionalPointHitFx(
                    center = Offset(c.x, c.y),
                    hexSize = cache.hexSize,
                    pulseAlpha = pulseAlpha
                )
            }
        }

        // =========================================================================
        // 5. BATIRILMIŞ GEMİ MERKEZİ UYARISI (SUNK SHIP ALERT BEACON)
        // =========================================================================
        if (ship.isSunk) {
            val shipCenter = if (ship.isTriangleShape && ship.parts.size == 3) {
                val p0 = cache.layout.hexToPixel(ship.parts[0])
                val p1 = cache.layout.hexToPixel(ship.parts[1])
                val p2 = cache.layout.hexToPixel(ship.parts[2])
                Offset((p0.x + p1.x + p2.x) / 3f, (p0.y + p1.y + p2.y) / 3f)
            } else {
                val pF = cache.layout.hexToPixel(ship.parts.first())
                val pL = cache.layout.hexToPixel(ship.parts.last())
                Offset((pF.x + pL.x) / 2f, (pF.y + pL.y) / 2f)
            }
            drawCircle(
                color = Color(0xBB0F172A),
                radius = cache.hexSize * 0.55f,
                center = shipCenter
            )
            drawCircle(
                color = BoardColors.CrimsonRed,
                radius = cache.hexSize * 0.48f,
                center = shipCenter,
                style = Stroke(width = 2.4f)
            )
        }
    }
}

/**
 * Vurulan gemi modül noktaları için çok katmanlı, profesyonel taktiksel patlama & darbe efekti (Point Efekt).
 */
private fun DrawScope.drawProfessionalPointHitFx(
    center: Offset,
    hexSize: Float,
    pulseAlpha: Float
) {
    translate(left = center.x, top = center.y) {
        // 1. Dinamik Dış Şok Dalgası Halesi (Pulsing Shockwave)
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.85f * pulseAlpha),
            radius = hexSize * (0.42f + 0.10f * pulseAlpha),
            center = Offset.Zero,
            style = Stroke(width = 2.8f)
        )

        // 2. Yanık & Krater Tabanı (Deep Scorched Breach Pool)
        drawCircle(
            color = Color(0xCC7F1D1D),
            radius = hexSize * 0.40f,
            center = Offset.Zero
        )

        // 3. Çok Katmanlı Plazma Alev Çekirdeği (Layered Fire Core)
        drawCircle(
            color = Color(0xFFEF4444),
            radius = hexSize * 0.32f,
            center = Offset.Zero
        )
        drawCircle(
            color = Color(0xFFF97316),
            radius = hexSize * 0.22f,
            center = Offset.Zero
        )
        drawCircle(
            color = Color(0xFFFDE047),
            radius = hexSize * 0.12f,
            center = Offset.Zero
        )
        drawCircle(
            color = Color.White,
            radius = hexSize * 0.06f,
            center = Offset.Zero
        )

        // 4. Kritik Gövde Yarığı & Çift Lazer Darbe Çaprazı (Laser Crosshair)
        val arm = hexSize * 0.26f
        drawLine(
            color = Color(0xFF450A0A),
            start = Offset(-arm, -arm),
            end = Offset(arm, arm),
            strokeWidth = 4.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF450A0A),
            start = Offset(arm, -arm),
            end = Offset(-arm, arm),
            strokeWidth = 4.8f,
            cap = StrokeCap.Round
        )
        val coreArm = arm * 0.78f
        drawLine(
            color = Color.White,
            start = Offset(-coreArm, -coreArm),
            end = Offset(coreArm, coreArm),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(coreArm, -coreArm),
            end = Offset(-coreArm, coreArm),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        // 5. Taktiksel Tehlike Köşebentleri (Tactical Corner Brackets ⌜ ⌝ ⌞ ⌟)
        val bDist = hexSize * 0.36f
        val bLen = hexSize * 0.11f
        val bracketColor = Color(0xFFFF2222).copy(alpha = pulseAlpha)
        for (sx in listOf(-1f, 1f)) {
            for (sy in listOf(-1f, 1f)) {
                val bx = sx * bDist
                val by = sy * bDist
                drawLine(
                    color = bracketColor,
                    start = Offset(bx, by),
                    end = Offset(bx - sx * bLen, by),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(bx, by),
                    end = Offset(bx, by - sy * bLen),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 6. Yayılan Kor Parçacıkları ve Kıvılcımlar (Orbiting Embers & Sparks)
        val sparkDist = hexSize * 0.44f
        for (deg in listOf(30f, 90f, 150f, 210f, 270f, 330f)) {
            val rad = Math.toRadians(deg.toDouble())
            val ox = (sparkDist * kotlin.math.cos(rad)).toFloat()
            val oy = (sparkDist * kotlin.math.sin(rad)).toFloat()
            drawCircle(
                color = Color(0xFFFB923C).copy(alpha = pulseAlpha),
                radius = hexSize * 0.045f,
                center = Offset(ox, oy)
            )
            drawCircle(
                color = Color.White,
                radius = hexSize * 0.020f,
                center = Offset(ox, oy)
            )
        }
    }
}

/**
 * Gemi gövdesinin altında kalan Radyoaktif Sis Bölgesi ve Manyetik Fırtına peteklerini
 * gövdenin altında/üstünde flu, estetik ve fark edilir bir siber hale ve 2D efekt olarak çizer (User Requirements 2 & 4).
 */
private fun DrawScope.drawSubmergedShipHazardOverlays(
    state: HexBoardUiState,
    cache: HexPathCache,
    radioactiveBitmap: ImageBitmap? = null,
    stormBitmap: ImageBitmap? = null,
    pulseAlpha: Float = 1f,
    stormWaveAngle: Float = 0f
) {
    val shipCoords = state.ships.flatMap { it.parts }.toSet() + state.previewShipParts.toSet()
    if (shipCoords.isEmpty()) return

    for (coord in shipCoords) {
        val isRadioactive = coord in state.radioactiveCoords
        val isMagneticStorm = state.tiles[coord]?.terrain == TerrainType.MAGNETIC_STORM

        if (!isRadioactive && !isMagneticStorm) continue

        val center = cache.layout.hexToPixel(coord)
        translate(left = center.x, top = center.y) {
            if (isRadioactive) {
                if (radioactiveBitmap != null) {
                    val radSize = cache.hexSize * 1.50f
                    val radAlpha = (0.42f + 0.16f * pulseAlpha).coerceIn(0.20f, 0.65f)
                    drawCircle(
                        color = Color(0xFF22C55E).copy(alpha = 0.18f * pulseAlpha),
                        radius = cache.hexSize * 0.48f,
                        center = Offset.Zero
                    )
                    drawImage(
                        image = radioactiveBitmap,
                        dstOffset = IntOffset((-radSize / 2f).roundToInt(), (-radSize / 2f).roundToInt()),
                        dstSize = IntSize(radSize.roundToInt(), radSize.roundToInt()),
                        alpha = radAlpha
                    )
                } else {
                    drawCircle(
                        color = Color(0xFF22C55E).copy(alpha = 0.25f + 0.10f * pulseAlpha),
                        radius = cache.hexSize * 0.48f,
                        center = Offset.Zero
                    )
                }
            }

            if (isMagneticStorm) {
                if (stormBitmap != null) {
                    val waveSin = sin(Math.toRadians(stormWaveAngle.toDouble())).toFloat()
                    val waveCos = cos(Math.toRadians(stormWaveAngle.toDouble())).toFloat()
                    val stormSize = cache.hexSize * (1.56f + 0.07f * waveSin)
                    val stormAlpha = (0.58f + 0.05f * waveSin).coerceIn(0.45f, 0.68f)
                    val driftDeg = waveCos * 5f
                    rotate(degrees = driftDeg, pivot = Offset.Zero) {
                        drawImage(
                            image = stormBitmap,
                            dstOffset = IntOffset((-stormSize / 2f).roundToInt(), (-stormSize / 2f).roundToInt()),
                            dstSize = IntSize(stormSize.roundToInt(), stormSize.roundToInt()),
                            alpha = stormAlpha
                        )
                    }
                } else {
                    drawCircle(
                        color = BoardColors.PurplePrimary.copy(alpha = 0.32f + 0.12f * pulseAlpha),
                        radius = cache.hexSize * 0.48f,
                        center = Offset.Zero
                    )
                }
            }
        }
    }
}

/**
 * Gemi ve radyoaktif sis yerleştirme aşamasında seçili nesneyi, gölgeyi ve holografik gövdeyi
 * sürükleme/taşıma esnasında estetik ve flu olarak önizler (User Requirement 3).
 */
private fun DrawScope.drawShipPlacementPreview(
    state: HexBoardUiState,
    cache: HexPathCache,
    pulseAlpha: Float,
    shipBitmaps: Map<String, ImageBitmap>
) {
    val isRadioactiveCat = state.previewCategory == "RADIOACTIVE"
    val previewColor = if (isRadioactiveCat) {
        if (state.isPreviewValid) Color(0xFF22C55E) else BoardColors.CrimsonRed
    } else {
        // User Requirement 7: Gemi yerleştirirken mor yerine sarı yanıp sönen taban
        if (state.isPreviewValid) Color(0xFFEAB308) else BoardColors.CrimsonRed
    }
    val previewCore = if (isRadioactiveCat) {
        if (state.isPreviewValid) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
    } else {
        if (state.isPreviewValid) Color(0xFFFEF08A) else Color(0xFFFCA5A5)
    }

    // 1. Petek Tabanı ve Flu Gölge Katmanı (User Requirement 3: Bırakmadan önce estetik ve flu gölge)
    for (coord in state.previewShipParts) {
        val c = cache.layout.hexToPixel(coord)
        translate(left = c.x, top = c.y) {
            // Yumuşak flu gölge dairesi
            drawCircle(
                color = previewColor.copy(alpha = 0.35f * pulseAlpha),
                radius = cache.hexSize * 0.72f,
                center = Offset(0f, 4f)
            )
            drawPath(
                path = cache.baseHexPath,
                color = previewColor.copy(alpha = 0.22f * pulseAlpha)
            )
            drawPath(
                path = cache.baseHexPath,
                color = previewColor.copy(alpha = pulseAlpha),
                style = Stroke(width = 3.0f)
            )
            drawCircle(
                color = previewCore.copy(alpha = 0.38f * pulseAlpha),
                radius = cache.hexSize * 0.38f,
                center = Offset.Zero
            )
        }
    }

    // 2. RADYOAKTİF SİS ÖNİZLEMESİ
    if (isRadioactiveCat) {
        val radBmp = shipBitmaps["hazard_radioactive"]
        val radSize = cache.hexSize * 1.55f
        if (radBmp != null) {
            for (coord in state.previewShipParts) {
                val c = cache.layout.hexToPixel(coord)
                translate(left = c.x, top = c.y) {
                    drawImage(
                        image = radBmp,
                        dstOffset = IntOffset((-radSize / 2f).roundToInt(), (-radSize / 2f - 4).roundToInt()),
                        dstSize = IntSize(radSize.roundToInt(), radSize.roundToInt()),
                        alpha = (if (state.isPreviewValid) 0.85f else 0.50f) * pulseAlpha
                    )
                }
            }
        }
        return
    }

    // 2. İlişkili Önizleme Noktaları Arası Bağlantı Omurgası (Data-Link)
    if (state.previewShipParts.size >= 2) {
        val count = state.previewShipParts.size
        val isTri = count == 3 && run {
            val d01 = state.previewShipParts[0].distanceTo(state.previewShipParts[1])
            val d02 = state.previewShipParts[0].distanceTo(state.previewShipParts[2])
            val d12 = state.previewShipParts[1].distanceTo(state.previewShipParts[2])
            d01 == 1 && d02 == 1 && d12 == 1
        }
        if (isTri) {
            val p0 = cache.layout.hexToPixel(state.previewShipParts[0])
            val p1 = cache.layout.hexToPixel(state.previewShipParts[1])
            val p2 = cache.layout.hexToPixel(state.previewShipParts[2])
            val pts = listOf(Offset(p0.x, p0.y), Offset(p1.x, p1.y), Offset(p2.x, p2.y))
            for (i in 0 until 3) {
                val j = (i + 1) % 3
                drawLine(
                    color = previewColor.copy(alpha = 0.60f * pulseAlpha),
                    start = pts[i],
                    end = pts[j],
                    strokeWidth = cache.hexSize * 0.20f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = previewCore.copy(alpha = pulseAlpha),
                    start = pts[i],
                    end = pts[j],
                    strokeWidth = cache.hexSize * 0.08f,
                    cap = StrokeCap.Round
                )
            }
        } else {
            for (i in 0 until count - 1) {
                val pA = cache.layout.hexToPixel(state.previewShipParts[i])
                val pB = cache.layout.hexToPixel(state.previewShipParts[i + 1])
                drawLine(
                    color = previewColor.copy(alpha = 0.60f * pulseAlpha),
                    start = Offset(pA.x, pA.y),
                    end = Offset(pB.x, pB.y),
                    strokeWidth = cache.hexSize * 0.22f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = previewCore.copy(alpha = pulseAlpha),
                    start = Offset(pA.x, pA.y),
                    end = Offset(pB.x, pB.y),
                    strokeWidth = cache.hexSize * 0.09f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // 3. Holografik Gemi Gövdesi Önizlemesi (Yükseltilmiş taşınma ve flu gölge efekti)
    if (state.previewShipParts.size >= 2) {
        val count = state.previewShipParts.size
        val isTri = count == 3 && run {
            val d01 = state.previewShipParts[0].distanceTo(state.previewShipParts[1])
            val d02 = state.previewShipParts[0].distanceTo(state.previewShipParts[2])
            val d12 = state.previewShipParts[1].distanceTo(state.previewShipParts[2])
            d01 == 1 && d02 == 1 && d12 == 1
        }
        val shipKey = when {
            count == 2 -> "corvette"
            isTri -> "delta"
            count == 3 -> "carrier"
            else -> "flagship"
        }
        val bmp = shipBitmaps["${shipKey}_friendly"]
        if (bmp != null) {
            if (isTri) {
                val p0 = cache.layout.hexToPixel(state.previewShipParts[0])
                val p1 = cache.layout.hexToPixel(state.previewShipParts[1])
                val p2 = cache.layout.hexToPixel(state.previewShipParts[2])
                val center = Offset((p0.x + p1.x + p2.x) / 3f, (p0.y + p1.y + p2.y) / 3f)
                val sternMid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                val (_, rotDeg) = ShipVisuals.calculateShipHeading(p0.x - sternMid.x, p0.y - sternMid.y, ShipVisuals.BASE_ANGLE_DELTA)
                val (spriteWidth, spriteHeight) = ShipVisuals.getShipSpriteDimensions(
                    shipKey = "delta",
                    length = 3,
                    isTriangle = true,
                    hexSize = cache.hexSize,
                    bmpWidth = bmp.width,
                    bmpHeight = bmp.height
                )
                rotate(degrees = rotDeg, pivot = center) {
                    // Gövde altı yerleşim gölgesi (Shadow)
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((center.x - spriteWidth / 2f).roundToInt(), (center.y - spriteHeight / 2f + 5).roundToInt()),
                        dstSize = IntSize(spriteWidth.roundToInt(), spriteHeight.roundToInt()),
                        alpha = 0.28f * pulseAlpha
                    )
                    // Yükseltilmiş taşınabilir gövde (Elevated Body)
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((center.x - spriteWidth / 2f).roundToInt(), (center.y - spriteHeight / 2f - 3).roundToInt()),
                        dstSize = IntSize(spriteWidth.roundToInt(), spriteHeight.roundToInt()),
                        alpha = (if (state.isPreviewValid) 0.82f else 0.50f) * pulseAlpha
                    )
                }
            } else {
                val firstCenter = cache.layout.hexToPixel(state.previewShipParts.first())
                val lastCenter = cache.layout.hexToPixel(state.previewShipParts.last())
                val center = Offset((firstCenter.x + lastCenter.x) / 2f, (firstCenter.y + lastCenter.y) / 2f)
                val baseAngle = ShipVisuals.getShipBaseAngle(shipKey)
                val (_, rotDeg) = ShipVisuals.calculateShipHeading(firstCenter.x - lastCenter.x, firstCenter.y - lastCenter.y, baseAngle)
                val (spriteWidth, spriteHeight) = ShipVisuals.getShipSpriteDimensions(
                    shipKey = shipKey,
                    length = count,
                    isTriangle = false,
                    hexSize = cache.hexSize,
                    bmpWidth = bmp.width,
                    bmpHeight = bmp.height
                )
                rotate(degrees = rotDeg, pivot = center) {
                    // Gövde altı yerleşim gölgesi (Shadow)
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((center.x - spriteWidth / 2f).roundToInt(), (center.y - spriteHeight / 2f + 5).roundToInt()),
                        dstSize = IntSize(spriteWidth.roundToInt(), spriteHeight.roundToInt()),
                        alpha = 0.28f * pulseAlpha
                    )
                    // Yükseltilmiş taşınabilir gövde (Elevated Body)
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset((center.x - spriteWidth / 2f).roundToInt(), (center.y - spriteHeight / 2f - 3).roundToInt()),
                        dstSize = IntSize(spriteWidth.roundToInt(), spriteHeight.roundToInt()),
                        alpha = (if (state.isPreviewValid) 0.82f else 0.50f) * pulseAlpha
                    )
                }
            }
        }
    }

    // 4. Önizleme Düğüm Noktaları (Tactical Node Reticles)
    for (coord in state.previewShipParts) {
        val c = cache.layout.hexToPixel(coord)
        translate(left = c.x, top = c.y) {
            drawCircle(
                color = previewColor.copy(alpha = pulseAlpha),
                radius = cache.hexSize * 0.20f,
                style = Stroke(width = 1.8f)
            )
            val r1 = cache.hexSize * 0.20f
            val r2 = cache.hexSize * 0.26f
            drawLine(color = previewColor.copy(alpha = pulseAlpha), start = Offset(r1, 0f), end = Offset(r2, 0f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            drawLine(color = previewColor.copy(alpha = pulseAlpha), start = Offset(-r1, 0f), end = Offset(-r2, 0f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            drawLine(color = previewColor.copy(alpha = pulseAlpha), start = Offset(0f, r1), end = Offset(0f, r2), strokeWidth = 1.8f, cap = StrokeCap.Round)
            drawLine(color = previewColor.copy(alpha = pulseAlpha), start = Offset(0f, -r1), end = Offset(0f, -r2), strokeWidth = 1.8f, cap = StrokeCap.Round)
            drawCircle(
                color = previewColor.copy(alpha = pulseAlpha),
                radius = cache.hexSize * 0.10f
            )
            drawCircle(
                color = Color.White,
                radius = cache.hexSize * 0.05f
            )
        }
    }
}

/**
 * 360 derece dönen siber-radar tarama ışını (User Requirement 4: Daha estetik, çok katmanlı flu lazer ışını ve zarif yavaş dönüş).
 */
private fun DrawScope.drawRadarSweep(maxRadius: Float, angle: Float) {
    rotate(degrees = angle, pivot = Offset.Zero) {
        // Yumuşak, kademeli, flu radar konisi (Soft holographic sweep gradient)
        val sweepBrush = Brush.sweepGradient(
            0.0f to Color.Transparent,
            0.80f to Color.Transparent,
            0.88f to Color(0xFF818CF8).copy(alpha = 0.03f),
            0.94f to Color(0xFFA855F7).copy(alpha = 0.08f),
            0.985f to Color(0xFFC084FC).copy(alpha = 0.16f),
            1.0f to Color(0xFFE9D5FF).copy(alpha = 0.25f),
            center = Offset.Zero
        )
        drawCircle(
            brush = sweepBrush,
            radius = maxRadius,
            center = Offset.Zero
        )

        // Çok Katmanlı Flu Radar Çubuğu / Işın Demeti (Layered Cyber-Glow Beam)
        // 1. En Dış Flu Geniş Işıma (Wide Ambient Glow)
        drawLine(
            color = Color(0xFFA855F7).copy(alpha = 0.18f),
            start = Offset.Zero,
            end = Offset(maxRadius, 0f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // 2. Orta Flu Işık Halesi (Mid-Glow)
        drawLine(
            color = Color(0xFFC084FC).copy(alpha = 0.40f),
            start = Offset.Zero,
            end = Offset(maxRadius, 0f),
            strokeWidth = 3.6f,
            cap = StrokeCap.Round
        )

        // 3. İnce Çekirdek Lazer Çizgisi (Sharp Neon Core)
        drawLine(
            color = Color(0xFFF3E8FF).copy(alpha = 0.85f),
            start = Offset.Zero,
            end = Offset(maxRadius, 0f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round
        )

        // 4. Merkez Taktiksel Radar Hub Düğümü
        drawCircle(
            color = Color(0xFFA855F7).copy(alpha = 0.35f),
            radius = 7f,
            center = Offset.Zero
        )
        drawCircle(
            color = Color(0xFFE9D5FF).copy(alpha = 0.75f),
            radius = 3.5f,
            center = Offset.Zero
        )
        drawCircle(
            color = Color.White,
            radius = 1.8f,
            center = Offset.Zero
        )

        // 5. Işının Dış Ucunda Taktiksel Fener / Parıltı Noktası
        drawCircle(
            color = Color(0xFFC084FC).copy(alpha = 0.50f),
            radius = 4f,
            center = Offset(maxRadius * 0.98f, 0f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.90f),
            radius = 1.8f,
            center = Offset(maxRadius * 0.98f, 0f)
        )
    }
}

/**
 * İsabet patlamaları (HIT), batık işaretleri (SUNK), su sıçramaları (MISS),
 * kalkan sönümleme halkası (SHIELD_ABSORBED), İHA keşif sinyalleri ve hedefleme nişangahı.
 */
private fun DrawScope.drawCombatMarkersAndReticle(
    state: HexBoardUiState,
    cache: HexPathCache,
    pulseAlpha: Float
) {
    for ((coord, tile) in state.tiles) {
        val center = cache.layout.hexToPixel(coord)

        when (tile.highlight) {
            TileHighlightState.HIT, TileHighlightState.SUNK_SHIP -> {
                drawProfessionalPointHitFx(
                    center = Offset(center.x, center.y),
                    hexSize = cache.hexSize,
                    pulseAlpha = pulseAlpha
                )
            }

            TileHighlightState.SHIELD_ABSORBED -> {
                translate(left = center.x, top = center.y) {
                    // Dış yumuşak flu kalkan parıltısı (Flu estetik dalga)
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.28f),
                        radius = cache.hexSize * 0.52f,
                        center = Offset.Zero
                    )
                    // Mavi Kalkan Sönümleme İzi / Enerji Dalgası
                    drawCircle(
                        color = Color(0xFF2563EB).copy(alpha = 0.85f),
                        radius = cache.hexSize * 0.40f,
                        center = Offset.Zero,
                        style = Stroke(width = 2.6f)
                    )
                    drawCircle(
                        color = Color(0xFF0EA5E9).copy(alpha = 0.45f),
                        radius = cache.hexSize * 0.22f,
                        center = Offset.Zero
                    )
                    // Merkez kalkan sönümleme sembolü
                    drawCircle(
                        color = Color.White,
                        radius = cache.hexSize * 0.08f,
                        center = Offset.Zero
                    )
                }
            }

            TileHighlightState.MISS -> {
                translate(left = center.x, top = center.y) {
                    // Dış su dalgası halkası (yumuşak su sıçraması)
                    drawCircle(
                        color = BoardColors.MissSplash.copy(alpha = 0.22f),
                        radius = cache.hexSize * 0.52f,
                        center = Offset.Zero
                    )
                    drawCircle(
                        color = BoardColors.MissSplash.copy(alpha = 0.60f),
                        radius = cache.hexSize * 0.36f,
                        center = Offset.Zero,
                        style = Stroke(width = 1.6f)
                    )
                    // Üst su damlası noktası
                    drawCircle(
                        color = BoardColors.MissSplash,
                        radius = cache.hexSize * 0.10f,
                        center = Offset(0f, -cache.hexSize * 0.16f)
                    )
                    // "Null" Metni (Altıgenin içine taşmadan estetik yerleşim)
                    drawContext.canvas.nativeCanvas.drawText(
                        "Null",
                        0f,
                        cache.hexSize * 0.22f,
                        cache.missTextPaint
                    )
                }
            }

            TileHighlightState.RECON_DETECTED -> {
                translate(left = center.x, top = center.y) {
                    // İHA Gemi Tespit Sinyali (Mor Radar Hedef Halkası)
                    drawCircle(
                        color = BoardColors.PurplePrimary.copy(alpha = pulseAlpha),
                        radius = cache.hexSize * 0.45f,
                        center = Offset.Zero,
                        style = Stroke(width = 2.8f)
                    )
                    drawCircle(
                        color = BoardColors.CrimsonRed,
                        radius = cache.hexSize * 0.18f,
                        center = Offset.Zero
                    )
                }
            }

            TileHighlightState.RECON_CLEAR -> {
                translate(left = center.x, top = center.y) {
                    // İHA Temiz Su İşareti
                    drawCircle(
                        color = BoardColors.PurplePrimary.copy(alpha = 0.45f),
                        radius = cache.hexSize * 0.12f,
                        center = Offset.Zero
                    )
                }
            }

            else -> Unit
        }
    }

    // Seçili Hedef Nişangahı (User Requirement 2: Sabit, Temiz ve Profesyonel Taktiksel Nişangah)
    state.selectedCoordinate?.let { selected ->
        val center = cache.layout.hexToPixel(selected)
        translate(left = center.x, top = center.y) {
            drawTacticalTargetReticle(
                cache = cache,
                isAttackBoard = state.isAttackBoard
            )
        }
    }
}

/**
 * Sabit, temiz ve profesyonel taktiksel hedefleme nişangahı (User Requirement 2).
 * Ekrana girip çıkan veya dönen kafa karıştırıcı animasyonlar iptal edilmiştir.
 * - Seçilen altıgen sınırları içinde net hedefleme çerçevesi,
 * - 4 kardinal yönde içe odaklanan hassas nişangah çizgileri,
 * - Merkez odak noktası ve halkası.
 */
private fun DrawScope.drawTacticalTargetReticle(
    cache: HexPathCache,
    isAttackBoard: Boolean
) {
    val primaryColor = if (isAttackBoard) BoardColors.CrimsonRed else BoardColors.NavyAccent

    // 1. Seçili altıgen sınır çerçevesi
    drawPath(
        path = cache.baseHexPath,
        color = primaryColor,
        style = Stroke(width = 2.8f)
    )

    // 2. 4 Ana Yönde (N, S, E, W) İçe Odaklanan Hassas Kilit Çizgileri ve Halka
    val r = cache.hexSize * 0.55f
    drawCircle(
        color = primaryColor,
        radius = r * 0.65f,
        center = Offset.Zero,
        style = Stroke(width = 2f)
    )
    drawLine(
        color = primaryColor,
        start = Offset(-r, 0f),
        end = Offset(-r * 0.35f, 0f),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = primaryColor,
        start = Offset(r * 0.35f, 0f),
        end = Offset(r, 0f),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = primaryColor,
        start = Offset(0f, -r),
        end = Offset(0f, -r * 0.35f),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = primaryColor,
        start = Offset(0f, r * 0.35f),
        end = Offset(0f, r),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )

    // 3. Merkez Odak Noktası
    drawCircle(
        color = primaryColor,
        radius = 2.5f,
        center = Offset.Zero
    )
}

/**
 * Seçili geminin altıgen koordinatlarına göre ekran sınırlarından taşmayan
 * estetik ve ergonomik HUD merkez noktasını hesaplar.
 */
private fun calculateShipHudCenter(
    parts: List<HexCoordinate>,
    layout: HexLayout,
    hexSize: Float,
    canvasHeight: Float
): Offset {
    val pts = parts.map { layout.hexToPixel(it) }
    val minY = pts.minOf { it.y }
    val maxY = pts.maxOf { it.y }
    val avgX = pts.map { it.x }.average().toFloat()

    val topTargetY = minY - (hexSize * 1.15f)
    val halfH = canvasHeight / 2f
    return if (topTargetY - (hexSize * 0.6f) > -halfH) {
        Offset(avgX, topTargetY)
    } else {
        Offset(avgX, maxY + (hexSize * 1.15f))
    }
}

/**
 * Seçili geminin üstünde flu, yüksek estetikli ve aerodinamik kırmızı animasyonlu
 * 2 yönlü (Saat Yönü ve Saatin Tersi) döndürme HUD'ını çizer (User Requirement 1).
 */
private fun DrawScope.drawShipRotationHud(
    hudCenter: Offset,
    hexSize: Float,
    pulseAlpha: Float,
    arrowPhase: Float
) {
    val capWidth = hexSize * 2.70f
    val capHeight = hexSize * 1.02f
    val capRadius = capHeight / 2f
    val topLeft = Offset(hudCenter.x - capWidth / 2f, hudCenter.y - capHeight / 2f)
    val capSize = Size(capWidth, capHeight)

    // 1. Çok Katmanlı Flu Kırmızı Ortam Parıltısı (Soft Frosted Ambient Glow)
    for (i in 1..3) {
        val expand = i * 2.0f
        drawRoundRect(
            color = Color(0xFFEF4444).copy(alpha = (0.20f / i) * pulseAlpha),
            topLeft = Offset(topLeft.x - expand, topLeft.y - expand),
            size = Size(capSize.width + expand * 2, capSize.height + expand * 2),
            cornerRadius = CornerRadius(capRadius + expand, capRadius + expand)
        )
    }

    // 2. Siber Koyu Flu Cam Gövde (Frosted Dark Glass Body)
    drawRoundRect(
        color = Color(0xD90B1120),
        topLeft = topLeft,
        size = capSize,
        cornerRadius = CornerRadius(capRadius, capRadius)
    )

    // 3. Parlak Flu Kırmızı Sınır Çizgisi (Neon HUD Border)
    drawRoundRect(
        color = Color(0xFFF43F5E).copy(alpha = 0.55f + 0.25f * pulseAlpha),
        topLeft = topLeft,
        size = capSize,
        cornerRadius = CornerRadius(capRadius, capRadius),
        style = Stroke(width = 1.4f)
    )

    val btnOffset = hexSize * 0.88f
    val ccwCenter = Offset(hudCenter.x - btnOffset, hudCenter.y)
    val cwCenter = Offset(hudCenter.x + btnOffset, hudCenter.y)
    val btnRadius = hexSize * 0.40f

    // 4. SOL BUTON (CCW - Sola / Saatin Tersine Döndür)
    // Flu dairesel zemin
    drawCircle(
        color = Color(0xFFF43F5E).copy(alpha = 0.12f + 0.08f * pulseAlpha),
        radius = btnRadius,
        center = ccwCenter
    )
    drawCircle(
        color = Color(0xFFFDA4AF).copy(alpha = 0.45f * pulseAlpha),
        radius = btnRadius,
        center = ccwCenter,
        style = Stroke(width = 1.2f)
    )

    // CCW Aerodinamik Kavisli Dönüş Oku (Zarif flu yay ve şık siber ok ucu)
    val arcR = hexSize * 0.22f
    val ccwArcRect = Rect(
        left = ccwCenter.x - arcR,
        top = ccwCenter.y - arcR,
        right = ccwCenter.x + arcR,
        bottom = ccwCenter.y + arcR
    )
    // Dış flu ışıma yayı
    drawArc(
        color = Color(0xFFF43F5E).copy(alpha = 0.40f * pulseAlpha),
        startAngle = 55f,
        sweepAngle = 230f,
        useCenter = false,
        topLeft = ccwArcRect.topLeft,
        size = ccwArcRect.size,
        style = Stroke(width = 4.0f, cap = StrokeCap.Round)
    )
    // İç net beyaz-pembe çekirdek yay
    drawArc(
        color = Color(0xFFFFF1F2),
        startAngle = 55f,
        sweepAngle = 230f,
        useCenter = false,
        topLeft = ccwArcRect.topLeft,
        size = ccwArcRect.size,
        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
    )
    // CCW Ok Ucu (Açı 55 derecede teğetsel şık ok ucu)
    val rad55 = Math.toRadians(55.0)
    val tipX = (ccwCenter.x + arcR * cos(rad55)).toFloat()
    val tipY = (ccwCenter.y + arcR * sin(rad55)).toFloat()
    val ccwHead = Path().apply {
        moveTo(tipX, tipY)
        lineTo(tipX - hexSize * 0.11f, tipY - hexSize * 0.03f)
        lineTo(tipX - hexSize * 0.03f, tipY - hexSize * 0.11f)
        close()
    }
    drawPath(path = ccwHead, color = Color(0xFFFFF1F2))
    drawPath(path = ccwHead, color = Color(0xFFF43F5E), style = Stroke(width = 1.0f))

    // 5. SAĞ BUTON (CW - Sağa / Saat Yönünde Döndür)
    // Flu dairesel zemin
    drawCircle(
        color = Color(0xFFF43F5E).copy(alpha = 0.12f + 0.08f * pulseAlpha),
        radius = btnRadius,
        center = cwCenter
    )
    drawCircle(
        color = Color(0xFFFDA4AF).copy(alpha = 0.45f * pulseAlpha),
        radius = btnRadius,
        center = cwCenter,
        style = Stroke(width = 1.2f)
    )

    // CW Aerodinamik Kavisli Dönüş Oku
    val cwArcRect = Rect(
        left = cwCenter.x - arcR,
        top = cwCenter.y - arcR,
        right = cwCenter.x + arcR,
        bottom = cwCenter.y + arcR
    )
    // Dış flu ışıma yayı
    drawArc(
        color = Color(0xFFF43F5E).copy(alpha = 0.40f * pulseAlpha),
        startAngle = 95f,
        sweepAngle = 230f,
        useCenter = false,
        topLeft = cwArcRect.topLeft,
        size = cwArcRect.size,
        style = Stroke(width = 4.0f, cap = StrokeCap.Round)
    )
    // İç net beyaz-pembe çekirdek yay
    drawArc(
        color = Color(0xFFFFF1F2),
        startAngle = 95f,
        sweepAngle = 230f,
        useCenter = false,
        topLeft = cwArcRect.topLeft,
        size = cwArcRect.size,
        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
    )
    // CW Ok Ucu (Açı 325 derecede teğetsel şık ok ucu)
    val rad325 = Math.toRadians(325.0)
    val tipRX = (cwCenter.x + arcR * cos(rad325)).toFloat()
    val tipRY = (cwCenter.y + arcR * sin(rad325)).toFloat()
    val cwHead = Path().apply {
        moveTo(tipRX, tipRY)
        lineTo(tipRX - hexSize * 0.03f, tipRY + hexSize * 0.11f)
        lineTo(tipRX - hexSize * 0.11f, tipRY + hexSize * 0.03f)
        close()
    }
    drawPath(path = cwHead, color = Color(0xFFFFF1F2))
    drawPath(path = cwHead, color = Color(0xFFF43F5E), style = Stroke(width = 1.0f))

    // 6. MERKEZ SİBER PUSULA VE TAKTİKSEL GYRO GÖSTERGESİ
    val gyroR = hexSize * 0.16f
    drawCircle(
        color = Color(0xFFF43F5E).copy(alpha = 0.35f * pulseAlpha),
        radius = gyroR,
        center = hudCenter,
        style = Stroke(width = 1.0f)
    )
    // Merkez Şık Elmas Düğüm (Cyber Diamond)
    val d = hexSize * 0.06f
    val diamondPath = Path().apply {
        moveTo(hudCenter.x, hudCenter.y - d)
        lineTo(hudCenter.x + d, hudCenter.y)
        lineTo(hudCenter.x, hudCenter.y + d)
        lineTo(hudCenter.x - d, hudCenter.y)
        close()
    }
    drawPath(path = diamondPath, color = Color(0xFFF43F5E).copy(alpha = 0.85f * pulseAlpha))
    drawCircle(
        color = Color.White,
        radius = hexSize * 0.025f,
        center = hudCenter
    )

    // 4 Kardinal Taktiksel Çentik
    val tick1 = hexSize * 0.10f
    val tick2 = hexSize * 0.18f
    val tickColor = Color(0xFFFDA4AF).copy(alpha = 0.65f * pulseAlpha)
    drawLine(color = tickColor, start = Offset(hudCenter.x - tick2, hudCenter.y), end = Offset(hudCenter.x - tick1, hudCenter.y), strokeWidth = 1.1f)
    drawLine(color = tickColor, start = Offset(hudCenter.x + tick1, hudCenter.y), end = Offset(hudCenter.x + tick2, hudCenter.y), strokeWidth = 1.1f)
    drawLine(color = tickColor, start = Offset(hudCenter.x, hudCenter.y - tick2), end = Offset(hudCenter.x, hudCenter.y - tick1), strokeWidth = 1.1f)
    drawLine(color = tickColor, start = Offset(hudCenter.x, hudCenter.y + tick1), end = Offset(hudCenter.x, hudCenter.y + tick2), strokeWidth = 1.1f)
}
