package com.hexafleet.feature.board.visuals

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.hexafleet.feature.board.R
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Deep Strike Gemi Görsel Yönetim ve Açı Hesaplama Sistemi.
 *
 * Kullanıcı Tanımlı 4 Temel Savaş Gemisi:
 * 1. Resim = Korvet (2 Petek - Hayalet Hücumbot)
 * 2. Resim = Delta (3 Petek - Üçgen Taarruz Kruvazörü)
 * 3. Resim = İHA G. (3 Petek - İHA Operasyon Gemisi)
 * 4. Resim = Amiral (4 Petek - Zırhlı Komuta Gemisi)
 */
object ShipVisuals {

    // 2D Kuşbakışı (Top-Down Orthographic) temel kaynak referans açıları (Doğu = 0°)
    const val BASE_ANGLE_CORVETTE = 0f
    const val BASE_ANGLE_DELTA = 0f
    const val BASE_ANGLE_CARRIER = 0f
    const val BASE_ANGLE_FLAGSHIP = 0f

    /**
     * Gemi ismi veya ID'sine göre uygun drawable kaynak ID'sini döndürür.
     */
    fun getShipDrawableRes(nameOrId: String, isEnemy: Boolean): Int {
        val isKorvet = nameOrId.contains("Korvet", ignoreCase = true) ||
            nameOrId.contains("Corvette", ignoreCase = true)
        val isDelta = nameOrId.contains("Delta", ignoreCase = true) ||
            nameOrId.contains("Aegis", ignoreCase = true)
        val isCarrier = nameOrId.contains("İHA", ignoreCase = true) ||
            nameOrId.contains("Carrier", ignoreCase = true)
        val isAmiral = nameOrId.contains("Amiral", ignoreCase = true) ||
            nameOrId.contains("Flagship", ignoreCase = true) ||
            nameOrId.contains("Dreadnought", ignoreCase = true)

        return when {
            isKorvet -> if (isEnemy) R.drawable.ship_corvette_enemy else R.drawable.ship_corvette
            isDelta -> if (isEnemy) R.drawable.ship_delta_enemy else R.drawable.ship_delta
            isCarrier -> if (isEnemy) R.drawable.ship_carrier_enemy else R.drawable.ship_carrier
            isAmiral -> if (isEnemy) R.drawable.ship_flagship_enemy else R.drawable.ship_flagship
            else -> if (isEnemy) R.drawable.ship_corvette_enemy else R.drawable.ship_corvette
        }
    }

    /**
     * Geminin orijinal çizim referans açısını döndürür.
     */
    fun getShipBaseAngle(nameOrId: String): Float {
        val isKorvet = nameOrId.contains("Korvet", ignoreCase = true) ||
            nameOrId.contains("Corvette", ignoreCase = true)
        val isDelta = nameOrId.contains("Delta", ignoreCase = true) ||
            nameOrId.contains("Aegis", ignoreCase = true)
        val isCarrier = nameOrId.contains("İHA", ignoreCase = true) ||
            nameOrId.contains("Carrier", ignoreCase = true)

        return when {
            isKorvet -> BASE_ANGLE_CORVETTE
            isDelta -> BASE_ANGLE_DELTA
            isCarrier -> BASE_ANGLE_CARRIER
            else -> BASE_ANGLE_FLAGSHIP
        }
    }

    /**
     * Kanonik kullanıcı sıralaması:
     * 1 = Korvet, 2 = Delta, 3 = İHA G., 4 = Amiral
     */
    fun getCanonicalShipOrder(nameOrId: String): Int {
        return when {
            nameOrId.contains("Korvet", ignoreCase = true) || nameOrId.contains("Corvette", ignoreCase = true) -> 1
            nameOrId.contains("Delta", ignoreCase = true) || nameOrId.contains("Aegis", ignoreCase = true) -> 2
            nameOrId.contains("İHA", ignoreCase = true) || nameOrId.contains("Carrier", ignoreCase = true) -> 3
            else -> 4
        }
    }

    /**
     * Modern kısa kanonik başlık.
     */
    fun getCanonicalShipName(nameOrId: String): String {
        return when {
            nameOrId.contains("Korvet", ignoreCase = true) || nameOrId.contains("Corvette", ignoreCase = true) -> "Korvet"
            nameOrId.contains("Delta", ignoreCase = true) || nameOrId.contains("Aegis", ignoreCase = true) -> "Delta"
            nameOrId.contains("İHA", ignoreCase = true) || nameOrId.contains("Carrier", ignoreCase = true) -> "İHA G."
            else -> "Amiral"
        }
    }

    /**
     * 2D Kuşbakışı (Top-Down) gemiler için tam altıgen yönelim açısını hesaplar.
     * Spritelar doğrudan 0° Doğu (+X) ekseninde modellendiği için atan2(dy, dx)
     * geminin burnunu altıgen ekseniyle sıfır sapmayla hizalar.
     *
     * @param dx Kıçtan burna doğru X piksel mesafesi
     * @param dy Kıçtan burna doğru Y piksel mesafesi
     * @param baseAngle Geminin ham görsel referans açısı (0° = Doğu)
     * @return Pair(flipX: Boolean, rotationDegrees: Float)
     */
    fun calculateShipHeading(
        dx: Float,
        dy: Float,
        baseAngle: Float = 0f
    ): Pair<Boolean, Float> {
        if (dx == 0f && dy == 0f) return Pair(false, 0f)
        val targetDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        return Pair(false, targetDeg - baseAngle)
    }

    /**
     * Altıgen ızgaradaki gemi tipi ve boyutuna göre en uygun piksel genişlik ve yükseklik değerini hesaplar.
     */
    fun getShipSpriteDimensions(
        shipKey: String,
        length: Int,
        isTriangle: Boolean,
        hexSize: Float,
        bmpWidth: Int,
        bmpHeight: Int
    ): Pair<Float, Float> {
        val aspect = bmpHeight.toFloat() / bmpWidth.toFloat()
        val spriteWidth = when {
            isTriangle || shipKey.contains("delta", ignoreCase = true) -> hexSize * 3.20f
            shipKey.contains("corvette", ignoreCase = true) || length == 2 -> hexSize * 3.20f
            shipKey.contains("carrier", ignoreCase = true) || length == 3 -> hexSize * 4.80f
            else -> hexSize * 6.20f // Amiral (Flagship - 4 petek)
        }
        val spriteHeight = spriteWidth * aspect
        return Pair(spriteWidth, spriteHeight)
    }

    private val bitmapCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

    /**
     * Canvas çizimi için tüm donanma bitmaplerini belleğe yükler ve tekil (Singleton) olarak önbelleğe alır.
     * Çift tahta (Dost + Düşman) ve PiP görünümlerinde aynı bitmap referansları paylaşılarak
     * yaklaşık 35-50 MB RAM tasarrufu sağlanır ve OOM riski sıfırlanır.
     */
    fun loadShipBitmaps(context: Context): Map<String, ImageBitmap> {
        if (bitmapCache.isNotEmpty()) return bitmapCache
        synchronized(bitmapCache) {
            if (bitmapCache.isNotEmpty()) return bitmapCache
            val res = context.applicationContext.resources

            // Arka plan okyanus dalgaları için RGB_565 (Alfa gerektirmez, bellek kullanımını yarıya indirir)
            val rgb565Options = BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }

            val decodedMap = mapOf(
                "corvette_friendly" to BitmapFactory.decodeResource(res, R.drawable.ship_corvette).asImageBitmap(),
                "corvette_enemy" to BitmapFactory.decodeResource(res, R.drawable.ship_corvette_enemy).asImageBitmap(),
                "delta_friendly" to BitmapFactory.decodeResource(res, R.drawable.ship_delta).asImageBitmap(),
                "delta_enemy" to BitmapFactory.decodeResource(res, R.drawable.ship_delta_enemy).asImageBitmap(),
                "carrier_friendly" to BitmapFactory.decodeResource(res, R.drawable.ship_carrier).asImageBitmap(),
                "carrier_enemy" to BitmapFactory.decodeResource(res, R.drawable.ship_carrier_enemy).asImageBitmap(),
                "flagship_friendly" to BitmapFactory.decodeResource(res, R.drawable.ship_flagship).asImageBitmap(),
                "flagship_enemy" to BitmapFactory.decodeResource(res, R.drawable.ship_flagship_enemy).asImageBitmap(),
                "terrain_mountain" to BitmapFactory.decodeResource(res, R.drawable.terrain_mountain).asImageBitmap(),
                "terrain_coral_reef" to BitmapFactory.decodeResource(res, R.drawable.terrain_coral_reef).asImageBitmap(),
                "hazard_radioactive" to BitmapFactory.decodeResource(res, R.drawable.hazard_radioactive).asImageBitmap(),
                "hazard_storm" to BitmapFactory.decodeResource(res, R.drawable.hazard_storm).asImageBitmap(),
                "board_ocean_waves" to (BitmapFactory.decodeResource(res, R.drawable.board_ocean_waves_bg, rgb565Options)
                    ?: BitmapFactory.decodeResource(res, R.drawable.board_ocean_waves_bg)).asImageBitmap(),
                "ic_target_3d" to BitmapFactory.decodeResource(res, R.drawable.ic_target_3d).asImageBitmap()
            )
            bitmapCache.putAll(decodedMap)
            return bitmapCache
        }
    }
}
