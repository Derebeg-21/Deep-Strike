package com.hexafleet.feature.board.canvas

import androidx.compose.ui.graphics.Path
import com.hexafleet.core.hexmath.HexLayout
import com.hexafleet.core.hexmath.HexPoint

/**
 * 60 ve 120 FPS akıcılıkta sıfır çöp toplama (0 GC allocations) sağlamak için
 * altıgen ve taktiksel sembol Path nesnelerini önbelleğe alan sınıf.
 */
class HexPathCache(
    val hexSize: Float
) {
    val layout = HexLayout(size = hexSize)

    // Merkez 0,0 olan temel altıgen yolu
    val baseHexPath: Path = Path().apply {
        val corners = (0 until 6).map { layout.hexCornerOffset(it) }
        moveTo(corners[0].x, corners[0].y)
        for (i in 1 until 6) {
            lineTo(corners[i].x, corners[i].y)
        }
        close()
    }

    // Seçim retikülü (Köşeli hedefleme çerçevesi)
    val reticleCornerPath: Path = Path().apply {
        val scale = hexSize * 0.85f
        moveTo(-scale * 0.5f, -scale * 0.9f)
        lineTo(0f, -scale)
        lineTo(scale * 0.5f, -scale * 0.9f)
    }

    // Akıntı yön oku (Chevron)
    val currentChevronPath: Path = Path().apply {
        val len = hexSize * 0.4f
        moveTo(-len * 0.6f, -len * 0.8f)
        lineTo(len * 0.6f, 0f)
        lineTo(-len * 0.6f, len * 0.8f)
    }

    // Mercan resifi pürüzlü kaya sembolü
    val reefPath: Path = Path().apply {
        val r = hexSize * 0.6f
        moveTo(-r * 0.7f, r * 0.5f)
        lineTo(-r * 0.3f, -r * 0.7f)
        lineTo(0f, -r * 0.2f)
        lineTo(r * 0.5f, -r * 0.8f)
        lineTo(r * 0.8f, r * 0.6f)
        close()
    }

    // Karavana / Boş Alan Metin Boyası (Altıgen içine taşmadan sığan taktiksel font)
    val missTextPaint: android.graphics.Paint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(230, 2, 132, 199) // #0284C7 Su sıçraması / Karavana mavisi
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = (hexSize * 0.20f).coerceIn(7.5f, 12f)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        isAntiAlias = true
    }
}

