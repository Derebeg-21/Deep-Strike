# HexaFleet: Tactical Waters
## Technical Architecture & Engineering Specifications

---

### 1. Sistem Mimarisi & Modüler Yapı (Clean Architecture)

Proje, Google'ın önerdiği modern Android Clean Architecture standartlarına tam uyumlu, bağımsız test edilebilir ve çok modüllü (multi-module) bir yapıda kurgulanmıştır.

```
HexaFleet/
├── app/                        # Android Giriş Noktası, DI Kökü (Hilt), NavHost
├── core/
│   ├── hexmath/                # %100 Saf Kotlin: Eksenel/Kübik Matematik, Mesafe, Raycasting
│   ├── common/                 # Coroutine Dispatchers, Result Wrapper, Logging
│   ├── designsystem/           # Jetpack Compose Teması, Renkler, Radar Shaders, Tipografi
│   └── network/                # Ktor HTTP/WebSocket istemcisi, Firebase adaptörü, Serializers
├── domain/
│   ├── game/                   # Modeller (Ship, Board, Cell), UseCase'ler, Oyun Kuralları Motoru
│   ├── lobby/                  # Lobi oluşturma, Kod doğrulama, Eşleşme UseCase'leri
│   └── campaign/               # Sefer ilerlemesi, Node haritası kuralları
├── data/
│   ├── local/                  # Room DB (Sefer, İstatistik, Filolar), Jetpack DataStore
│   ├── multiplayer/            # Online Lobi & Maç Senkronizasyonu, ActionPayload transferi
│   └── ai/                     # Çevrimdışı Yapay Zeka (Olasılık Matrisi, Karar Ağaçları)
└── feature/
    ├── board/                  # Compose Canvas Tabanlı Hex Harp Sahası ve HUD
    ├── lobby/                  # Oda Kurma, PIN Girişi, QR Kod Tarama / Paylaşma Ekranları
    ├── campaign/               # Node Tabanlı Roguelike Sefer Haritası ve Boss Ekranı
    └── fleetbuilder/           # Filo Dizilimi, Puan Dağılımı ve Gemi Kartları Ekranı
```

#### Modül Bağımlılık Hiyerarşisi (Dependency Rules)
- `domain` katmanı hiçbir Android kütüphanesine veya harici framework'e bağımlı değildir (`pure Kotlin`).
- `core:hexmath` tamamen izoledir, sıfır harici kütüphane bağımlılığı taşır ve mikrosaniye seviyesinde birim testleriyle (JUnit) doğrulanır.
- `presentation (feature:*)` -> `domain` -> `data` (Arayüz repository implementasyonunu bilmez, UseCase arayüzleriyle çalışır).

---

### 2. Altıgen Matematik Motoru (`core:hexmath`)

Hexagonal ızgara hesaplamalarında **Pointy-Topped (Sivri Uçlu)** altıgen yapısı tercih edilmiştir (Mobil dikey ekranda görüş açısını ve torpido koridorlarını optimize eder).

```
         /\
        /  \
       |    |
       |    |
        \  /
         \/
```

#### 2.1. Eksenel (Axial) ve Kübik (Cube) Koordinat Modeli
```kotlin
package com.hexafleet.core.hexmath

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class HexCoordinate(
    val q: Int, // Eksenel Sütun (X ekseni)
    val r: Int  // Eksenel Satır (Z ekseni)
) {
    // Kübik koordinat türetimi (x + y + z = 0 kuralı)
    val x: Int get() = q
    val z: Int get() = r
    val y: Int get() = -q - r

    companion object {
        val ZERO = HexCoordinate(0, 0)
        
        // 6 Temel Yön Vektörü (Pointy-Topped)
        val DIRECTIONS = listOf(
            HexCoordinate(1, 0),   // Doğu (E)
            HexCoordinate(1, -1),  // Kuzeydoğu (NE)
            HexCoordinate(0, -1),  // Kuzeybatı (NW)
            HexCoordinate(-1, 0),  // Batı (W)
            HexCoordinate(-1, 1),  // Güneybatı (SW)
            HexCoordinate(0, 1)    // Güneydoğu (SE)
        )
    }

    // 6 Komşu Peteği Getirme
    fun neighbor(directionIndex: Int): HexCoordinate {
        val dir = DIRECTIONS[Math.floorMod(directionIndex, 6)]
        return HexCoordinate(q + dir.q, r + dir.r)
    }

    // İki Altıgen Arasındaki Manhattan-Hex Mesafesi
    fun distanceTo(other: HexCoordinate): Int {
        return (abs(x - other.x) + abs(y - other.y) + abs(z - other.z)) / 2
    }

    // Belirli Yarıçaptaki Halka (Ring AoE) Hesaplama
    fun ring(radius: Int): List<HexCoordinate> {
        if (radius <= 0) return listOf(this)
        val results = mutableListOf<HexCoordinate>()
        var current = HexCoordinate(q + DIRECTIONS[4].q * radius, r + DIRECTIONS[4].r * radius)
        
        for (i in 0 until 6) {
            for (j in 0 until radius) {
                results.add(current)
                current = current.neighbor(i)
            }
        }
        return results
    }

    // Belirli Yarıçaptaki Tüm Alanı (Spiral / Daire) Hesaplama
    fun area(radius: Int): List<HexCoordinate> {
        val results = mutableListOf<HexCoordinate>()
        for (dq in -radius..radius) {
            for (dr in max(-radius, -dq - radius)..kotlin.math.min(radius, -dq + radius)) {
                results.add(HexCoordinate(q + dq, r + dr))
            }
        }
        return results
    }
}
```

#### 2.2. Pikselden Altıgene (Screen to Hex) ve Ters Dönüşüm
Kullanıcının ekrana dokunduğu piksel koordinatını (`Offset(x, y)`), milisaniyenin altında altıgen petek koordinatına çevirir:

```kotlin
data class HexLayout(
    val size: Float, // Altıgenin merkezinden köşesine olan yarıçap (R)
    val originX: Float,
    val originY: Float
) {
    private val sqrt3 = Math.sqrt(3.0).toFloat()

    // Hex'ten Ekrana Piksel Merkezini Bulma
    fun hexToPixel(hex: HexCoordinate): Pair<Float, Float> {
        val x = size * (sqrt3 * hex.q + sqrt3 / 2f * hex.r) + originX
        val y = size * (3f / 2f * hex.r) + originY
        return Pair(x, y)
    }

    // Ekrana Dokunulan Pikselden Kesirli Hex Hesaplama ve Yuvarlama (Round)
    fun pixelToHex(px: Float, py: Float): HexCoordinate {
        val ptX = (px - originX) / size
        val ptY = (py - originY) / size

        val qFraction = (sqrt3 / 3f * ptX - 1f / 3f * ptY)
        val rFraction = (2f / 3f * ptY)
        val yFraction = -qFraction - rFraction

        var rx = qFraction.roundToInt()
        var ry = yFraction.roundToInt()
        var rz = rFraction.roundToInt()

        val xDiff = abs(rx - qFraction)
        val yDiff = abs(ry - yFraction)
        val zDiff = abs(rz - rFraction)

        if (xDiff > yDiff && xDiff > zDiff) {
            rx = -ry - rz
        } else if (yDiff > zDiff) {
            ry = -rx - rz
        } else {
            rz = -rx - ry
        }
        return HexCoordinate(rx, rz)
    }
}
```

---

### 3. Grafik ve Render Motoru (Jetpack Compose Canvas)

Oyun, üçüncü parti ağır bir motor (Unity vb.) yerine **Android Jetpack Compose Custom Canvas** ile geliştirilerek APK boyutunu minimumda (~15 MB) tutar ve batarya verimliliği sağlar.

```
+-----------------------------------------------------------+
| Layer 4: HUD & UI Overlay (Komut Puanı, Butonlar, Canlar) |
+-----------------------------------------------------------+
| Layer 3: Efekt & Parçacık Katmanı (Torpido izi, Patlama)   |
+-----------------------------------------------------------+
| Layer 2: Gemi & Görüş Katmanı (Dost gemiler, Keşfedilen)  |
+-----------------------------------------------------------+
| Layer 1: Savaş Sisi Katmanı (PorterDuff Mask / Radar Sweep) |
+-----------------------------------------------------------+
| Layer 0: Zemin Izgarası (Su, Resif, Akıntı, Hex Çizgileri) |
+-----------------------------------------------------------+
```

#### 3.1. Radar Sweep Shader & Savaş Sisi Renderlama
- `SweepGradient` kullanılarak altıgen harita üzerinde 360 derece dönen bir sonar süpürme efekti elde edilir.
- Savaş sisi (Fog of War), donanım hızlandırmalı `BlendMode.DstIn` veya `Path` maskeleme ile sadece görüş menziline giren peteklerin opaklığını 1.0f yapar; sisli alanlar %30 yarı saydam koyulukta kalır.
- 60 ve 120 Hz ekranlarda ekran yenileme hızıyla (`withFrameNanos`) senkronize parçacık simülasyonu çalıştırılır.

#### 3.2. Çift Parmak Pan & Zoom (Gestures)
- `Modifier.pointerInput` ve `detectTransformGestures` ile harita yakınlaştırma (Zoom 0.5x - 2.5x) ve kaydırma (Pan) hareketleri doğal akışkanlıkla sağlanır.
- Dokunma bırakıldığında en yakın peteğe yumuşak kinetik sürtünme (Deceleration Snapping) uygulanır.

---

### 4. Çok Oyunculu (Online Multiplayer) & Güvenlik Mimarisi

Çevrimiçi mod, hem **Ktor WebSockets (Self-Hosted/Dedicated)** hem de sıfır sunucu maliyetiyle başlamak için **Firebase Realtime Database / Firestore** adaptörlerine uygun soyut bir `NetworkSession` arayüzü ile tasarlanır.

```
Oyuncu A (Host/Lobi)              Doğrulama & Senkronizasyon             Oyuncu B (İstemci)
     |                                        |                                  |
     |--- 1. CreateRoom(PIN: optional) ------>|                                  |
     |<-- 2. RoomCode ("HX-8492") ------------|                                  |
     |                                        |<-- 3. JoinRoom("HX-8492", PIN) --|
     |<========== 4. GameReady (Fleet Hash Exchange) ===========================>|
     |                                        |                                  |
     |--- 5. ActionPayload(FIRE, q=2, r=-1) ->|                                  |
     |                                        |--- 6. MaskedResult(HIT, Sunk:0)->|
```

#### 4.1. Oda Kodu ve PIN Güvenliği
- **Kod Formatı:** `AAA-999` veya 6 karakterli Base32 (karışıklığı önlemek için 'O', '0', 'I', '1' elenmiştir: örn. `23456789ABCDEFGHJKLMNPQRSTUVWXYZ`).
- **Lobi Türleri:**
  1. *Açık Lobi (Public):* Şifresiz, lobi listesinde görünür veya rastgele eşleşme.
  2. *Özel Lobi (Private):* Sadece oda kodu ile girilebilir.
  3. *Korumalı Lobi (PIN-Protected):* 4 haneli sayısal şifre ile korunur; yetkisiz girişleri engeller.

#### 4.2. Hile Önleme ve "Sıfır Bilgi Savaş Sisi" Protokolü (Zero-Knowledge Fog)
> **KRİTİK GÜVENLİK KURALI:** Rakibin istemcisine (Client Memory) gizli gemi koordinatları ASLA gönderilmez!
- Klasik Amiral Battı klonlarında bellek tarayıcıları (Cheat Engine vb.) ile düşman gemileri kolayca okunabilir.
- **HexaFleet Çözümü:**
  - Maç başında her iki taraf filo diziliminin kriptografik hash'ini (`SHA-256(FleetSalt + Coordinates)`) birbirine teyit ettirir (Commitment Scheme).
  - Oyuncu A bir peteğe ateş ettiğinde, sadece hedef koordinat (`q, r`) gönderilir.
  - Hedef taraf bu koordinatta isabet olup olmadığını (`HitResponsePayload`) döner.
  - Oyun sonunda maçın tüm koordinat logları ve orjinal Salt anahtarı açılarak istemci tarafından hile denetimi (Post-Match Audit) otomatik yapılır.

#### 4.3. Ağ Aksiyon Şeması (ActionPayload JSON)
```kotlin
@Serializable
sealed class GameAction {
    @Serializable
    data class Fire(val target: HexCoordinate) : GameAction()

    @Serializable
    data class MoveShip(val shipId: String, val newHead: HexCoordinate, val rotation: Int) : GameAction()

    @Serializable
    data class UseAbility(val shipId: String, val abilityId: String, val targetArea: HexCoordinate) : GameAction()

    @Serializable
    data class EndTurn(val turnNumber: Int) : GameAction()

    @Serializable
    data class Surrender(val reason: String) : GameAction()
}
```

---

### 5. Çevrimdışı Yapay Zeka Motoru (`data:ai`)

Yapay Zeka (AI), rastgele atış yapmak yerine altıgen ızgaraya uyarlanmış **Olasılık Yoğunluk Haritası (Hex-Probability Density Function - H-PDF)** algoritması kullanır.

```
       [0.05]   [0.10]   [0.12]
    [0.10]   [0.45]   [0.60]   [0.10]     <-- İsabet alınan petek çevresinde
       [0.15]   [0.85*]  [0.35]              yoğunlaşan 6 eksenli ısı haritası
          [0.20]   [0.15]
```

1. **Av Modu (Hunt Mode):**
   - Henüz isabet alınmamışken çalışır.
   - Kalan düşman gemilerinin boyutlarını (1, 2, 3, 4 petek) ve 3 ana eksen doğrultusunu simüle eder.
   - Mercan resiflerine veya daha önce ateş edilmiş boş peteklere sığamayacak durumları eler.
   - En yüksek yerleşim ihtimali olan peteğe ateş eder (Tahta üzerinde dama tahtası benzeri altıgen eş-aralıklı tarama).
2. **Hedef Modu (Target Mode):**
   - Bir gemi isabet aldığında devreye girer.
   - İsabet alan peteğin 6 komşusunu önceliklendirir.
   - İkinci bir isabet alındığında geminin 3 ana eksenden hangisinde durduğu kesinleşir; yapay zeka sadece o doğrultuda ileri-geri atış yaparak gemiyi batırana kadar hatta kalır.
3. **Elektronik Harp & Yetenek Zekası:**
   - EMP kruvazörünü oyuncunun en çok atış yaptığı koordinat merkezine atar.
   - Hasar alan İHA gemisini akıntı koridorlarına doğru kaçırır.

---

### 6. Veri Kalıcılığı & Durum Yönetimi (Room & DataStore)

#### 6.1. Room Database Modelleri
- `CampaignNodeEntity`: Sefer modunda geçilen düğümler, kazanılan ödüller.
- `FleetPresetEntity`: Oyuncunun tasarladığı hazır gemi filoları ve yetenek dizilimleri.
- `MatchHistoryEntity`: Yapılan maçların istatistikleri (isabet oranı, kullanılan yetenekler, zafer/yenilgi).

#### 6.2. Jetpack DataStore (Preferences)
- Ses seviyeleri (SFX, Ambient, Telsiz).
- Haptic titreşim yoğunluğu (Kapalı, Düşük, Yüksek).
- Grafik ayarları (60 FPS / 120 FPS / Batarya Tasarrufu).
- Oyuncu Çağrı Kodu (Callsign) ve Seçili Profil Amblemi.

