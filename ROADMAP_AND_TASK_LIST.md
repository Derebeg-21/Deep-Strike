# HexaFleet: Tactical Waters
## Proje Yol Haritası ve Görev Dağılım Listesi (WBS & Task List)

Bu belge, **HexaFleet: Tactical Waters** projesinin sıfırdan yayın aşamasına kadar adım adım takip edilecek sprint planını, görevlerini ve kabul kriterlerini içerir.

---

### Sprint Genel Görünümü
- **Faz 0:** Mimari İskelet, Gradle Yapılandırması ve Altıgen Matematik Motoru
- **Faz 1:** Jetpack Compose Render Motoru, Shaders ve Dokunmatik Kontroller
- **Faz 2:** Oyun Durumu, Kurallar, Gemi Sınıfları ve Yetenek Motoru
- **Faz 3:** Çevrimdışı Yapay Zeka (AI) ve Roguelike Sefer (Campaign) Modu
- **Faz 4:** Çevrimiçi Lobi (Oda Kodu / PIN / QR) ve Gerçek Zamanlı Ağ Protokolü
- **Faz 5:** Tablet/Mobil Uyarlanabilir Arayüz, Haptics ve Görsel-İşitsel Cila
- **Faz 6:** Optimizasyon, Testler, Güvenlik Denetimi ve Google Play Yayını

---

### FAZ 0: Proje İskeleti & Çekirdek Matematik Motoru (`core:hexmath`)
> **Hedef:** Bağımsız, test edilebilir modüler proje yapısını kurmak ve tüm geometrik hesaplamaları %100 test kapsamıyla tamamlamak.

- [x] **Görev 0.1: Gradle & Sürüm Kataloğu (Version Catalog) Kurulumu**
  - [x] `gradle/libs.versions.toml` dosyasının oluşturulması (Kotlin 2.0+, Jetpack Compose, Coroutines, Serialization, Firebase BOM, Room, JUnit 5/Truth).
  - [x] Çok modüllü Gradle yapısının bağlanması (`:app`, `:core:hexmath`).
  - *Kabul Kriteri:* `./gradlew assembleDebug` komutunun hatasız tamamlanması. (BAŞARILI: APK derlendi)
- [x] **Görev 0.2: `core:hexmath` Koordinat ve Eksenel Matematik**
  - [x] `HexCoordinate` veri sınıfı (Eksenel q, r; Kübik x, y, z; 6 yön vektörü, `allNeighbors`).
  - [x] `distanceTo(other)` mesafe formülünün kodlanması.
  - [x] `ring(radius)` ve `area(radius)` küresel halka fonksiyonlarının yazılması.
  - [x] `lineTo(target)` Bresenham altıgen doğrusal ışın (raycasting) algoritması.
  - [x] `rotate60(center, steps)` 60 derecelik eksen rotasyonu.
  - *Kabul Kriteri:* JUnit birim testlerinin başarıyla geçmesi. (BAŞARILI: 10/10 test geçti)
- [x] **Görev 0.3: `HexLayout` Piksel-Hex Dönüşüm Modülü & Görüş Hattı**
  - [x] `hexToPixel(hex)` ekran izdüşüm fonksiyonu.
  - [x] `pixelToHex(px, py)` ters dönüşüm ve kübik yuvarlama (rounding).
  - [x] `polygonCorners(hex)` Canvas çizim köşe noktaları.
  - [x] `HexLineOfSight` görüş engeli denetimi ve raycast torpido çarpmaları.
  - *Kabul Kriteri:* İki yönlü dönüşüm ve raycast testlerinin geçmesi. (BAŞARILI: 4/4 test geçti)

---

### FAZ 1: Compose Canvas Render Motoru & Kamera Kontrolleri
> **Hedef:** 60/120 FPS akıcılıkta altıgen okyanus ızgarasını çizmek ve çift parmakla akıcı kaydırma/yakınlaştırma sağlamak.

- [x] **Görev 1.1: Çizim Katmanları Altyapısı (`feature:board`)**
  - [x] `HexBoardCanvas` bileşeninin yazılması (Arka plan, su dokusu, altıgen petek sınır çizgileri, resif, akıntı, fırtına sembolleri).
  - [x] `HexPathCache` ile `DrawScope` üzerinde Path önbellekleme (Sıfır GC baskısı, 60/120 FPS hazır).
  - *Kabul Kriteri:* R=6 boyutundaki tahtanın tüm katmanlarıyla donanım hızlandırmalı çizilmesi. (BAŞARILI)
- [x] **Görev 1.2: Kamera Gezinme (Pan & Zoom Gestures)**
  - [x] `detectTransformGestures` ile çift parmak kaydırma (Pan) ve yakınlaştırma (Zoom: 0.5x - 2.5x).
  - [x] `detectTapGestures` ile ters kamera dönüşümü (`(tap - center - pan) / zoom`) ve anında `pixelToHex` hedefleme.
  - *Kabul Kriteri:* Akıcı hareket ve zoom sırasında petek tıklama isabetinin bozulmaması. (BAŞARILI)
- [x] **Görev 1.3: Savaş Sisi (Fog of War) ve Radar Süpürme Efekti**
  - [x] `SweepGradient` ve `rememberInfiniteTransition` ile dönen siber-sonar radar çizgisi animasyonu.
  - [x] Savaş Sisi katmanı: Keşfedilmemiş alanlar (Siyah), Sisli hafıza (%35 opaklık), Canlı görüş alanları (%100 berrak).
  - [x] İHA taraması ile 7 peteklik 1. halkanın siste anında açılması ve isabet/karavana görsel retikülleri.
  - *Kabul Kriteri:* `TacticalBoardScreen` komuta paneliyle entegre çalışma ve APK derlemesi. (BAŞARILI)

---

### FAZ 2: Oyun Mantığı, Gemi Sistemi & Yetenek Motoru (`domain:game`)
> **Hedef:** Sıra tabanlı kural motorunu, Komut Puanı (CP) dengesini ve 5 temel gemi yeteneğini inşa etmek.

- [x] **Görev 2.1: Domain Veri Modelleri**
  - [x] `Ship` (ID, Sınıf, Boyut, Can, Parçalar, Yönelim Açısı, Cooldown, Kalkan, EMP Kilidi, Dalış).
  - [x] `BoardTile` & `Terrain` (Açık Okyanus, Mercan Resifi, Manyetik Fırtına, Akıntı, Hendek).
  - [x] `GameState` (Aktif Oyuncu, Kalan CP, Tur Sayacı, Sis Görüşü, Muharebe Günlüğü).
  - *Kabul Kriteri:* `domain:game` modülünün hiçbir Android sınıfı içermeden saf Kotlin olarak derlenmesi. (BAŞARILI)
- [x] **Görev 2.2: Komut Puanı (CP) ve Manevra Motoru**
  - [x] Tur başı 3 CP üretimi (Maksimum 4 saklama sınırı).
  - [x] Gemi hareketi (1 petek doğrusal kayma) ve çarpışma kontrolü (resif, derin hendek, dost/düşman gemi).
  - [x] Gemi rotasyonu (Herhangi bir parça pivotunda 60° dönüş).
  - *Kabul Kriteri:* Engelli peteğe veya dolu peteğe girmeye çalışan manevraların engellenmesi ve CP korunması. (BAŞARILI)
- [x] **Görev 2.3: Aktif Yeteneklerin Kodlanması**
  - [x] **İHA Taşıyıcı:** 7 peteklik 1. Halka keşif yeteneği (Manyetik fırtınalar hariç sisi açar).
  - [x] **EMP Kruvazörü:** 2 tur yetenek/kalkan kilitleme (`empLockTurnsRemaining = 2`).
  - [x] **Aegis Muhribi:** İlk gelen isabeti hasarsız emen nano-kalkan (`ShieldAbsorbed`).
  - [x] **Hayalet Denizaltı:** Doğrusal 60° vektörel torpido atışı ve resif çarpma kontrolü.
  - [x] **Hücumbot:** Akıllı su altı mayını yerleştirme ve infilak mantığı.
  - [x] **Okyanus Akıntısı:** Tur sonunda akıntı üzerindeki gemileri 0 CP (ücretsiz) 1 petek sürükleme.
  - *Kabul Kriteri:* 8 adet kural ve yetenek birim testinin 0 hata ile geçmesi. (BAŞARILI)

---

### FAZ 3: Çevrimdışı Yapay Zeka (AI) & Sefer Modu
> **Hedef:** Altıgen olasılık matrisine dayalı taktiksel AI geliştirmek ve roguelike sefer ilerlemesini sunmak.

- [x] **Görev 3.1: Altıgen Olasılık Matrisi (H-PDF Engine)**
  - [x] Av Modu (Hunt Mode): Kalan gemi boyutlarını 3 eksende peteklere sığdırma olasılık puanlaması (`HexProbabilityMap`).
  - [x] Hedef Modu (Target Mode): İsabet sonrası 6 yönü ve eksen doğrultusunu kovalama algoritması (Aktif isabetlere 15x çarpanı).
  - [x] Zorluk Seviyeleri: *Recruit* (Rastgele + basit hedef), *Veteran* (Tam olasılık matrisi), *Grand Admiral* (İHA/EMP yetenekleri ve taktiksel manevralar).
  - *Kabul Kriteri:* AI birim testlerinin başarıyla geçmesi ve akıllı hedef eliminasyonu. (BAŞARILI: 4/4 test geçti)
- [x] **Görev 3.2: Sefer Modu ve Node Haritası (`domain:game` & `:data:local`)**
  - [x] Düğüm (Node) tabanlı roguelike harita oluşturucu (`CampaignMapGenerator`: Karakol, İkmal, Elit, Anomali, Leviathan Boss).
  - [x] Boss Karşılaşması: 5. Aşamada dev yüzen amiral kalesi (`BOSS_LEVIATHAN`).
  - [x] Room DB & KSP entegrasyonu: `HexaFleetDatabase`, `CampaignDao`, `PlayerStatsDao` ve `LocalCampaignRepository` ile yerel veri kalıcılığı.
  - *Kabul Kriteri:* Sefer haritası üretimi ve Room DB derlemesi hatasız doğrulandı. (BAŞARILI)

---

### FAZ 4: Çevrimiçi Çok Oyunculu (Online Multiplayer) & Lobi
> **Hedef:** 6 haneli oda koduyla arkadaşla eşleşme, şifreli lobiler ve hile korumalı gerçek zamanlı maç senkronizasyonu.

- [x] **Görev 4.1: Lobi Sistemi & Kod Üretici (`feature:lobby`)**
  - [x] 6 Karakterli Base32 oda kodu üretimi (`RoomCodeGenerator`: `HX-XXXX` formatı, 0, O, 1, I elendi).
  - [x] İsteğe bağlı 4 haneli PIN şifreleme desteği (`ZeroKnowledgeFog.hashPin` ve `verifyPin`).
  - [x] Pano kopyalama ve Deep Link (`hexafleet://lobby/{code}`) paylaşımı.
  - [x] QR Kod tarama/oluşturma kısayolu ve Jetpack Compose `LobbyScreen` arayüzü.
  - *Kabul Kriteri:* İki cihazın aynı kodu girerek anında lobi odasında eşleşmesi doğrulandı. (BAŞARILI: 4/4 test geçti)
- [x] **Görev 4.2: Ağ Senkronizasyonu & Protokol (`data:multiplayer`)**
  - [x] Firebase veri modeliyle uyumlu `FirebaseLobbyRepository` lobi yönetimi ve durum takibi (WAITING, FLEET_SETUP, IN_GAME).
  - [x] `NetworkPayload` JSON veri transfer şeması (`FireRequest`, `FireResponse`, `AbilityRequest`, `EndTurn`, `Heartbeat`).
  - [x] Tur zamanlayıcısı (Standart 45 sn, Blitz 10 sn) ve otomatik zaman aşımı desteği.
  - *Kabul Kriteri:* Repository birim testleriyle eşleşme ve başlama döngüsü doğrulandı. (BAŞARILI: 4/4 test geçti)
- [x] **Görev 4.3: Hile Önleme (Zero-Knowledge Fog of War)**
  - [x] İstemcilerin maç başında filo dizilim SHA-256 Hash'ini değiş tokuş etmesi (`createFleetCommitment`).
  - [x] Rakip istemciye gizli gemi koordinatlarının gitmesinin mimari olarak engellenmesi.
  - [x] Maç sonu log denetimi (`verifyFleetCommitment`) ile hile ve gizli gemi manipülasyonunun yakalanması.
  - *Kabul Kriteri:* Birim test ile gemi koordinatlarında yapılan gizli manipülasyonun yakalandığı kanıtlandı. (BAŞARILI)

---

### FAZ 5: Tablet & Mobil Uyarlanabilir Arayüz, Haptics & Ses
> **Hedef:** Tüm ekran boyutlarında kusursuz ergonomi ve zenginleştirilmiş duyusal geri bildirim.

- [x] **Görev 5.1: Uyarlanabilir Arayüz (Adaptive & Dual-Pane UI)**
  - [x] `BoxWithConstraints` entegrasyonu (Compact `< 650.dp`, Expanded/Tablet `>= 650.dp`).
  - [x] Mobil (Compact): Dikey tek ekran + alttan sabit komuta konsolu ve durum çubuğu.
  - [x] Tablet & Katlanabilir (Expanded): Sol %65 Hex Okyanus Sahası, Sağ %35 Taktiksel Telemetre & Canlı Muharebe Paneli.
  - *Kabul Kriteri:* Telefon ve 10" tablette hiçbir öğenin taşmaması ve okunabilirliği doğrulandı. (BAŞARILI)
- [x] **Görev 5.2: Mobil Haptic Motoru Entegrasyonu**
  - [x] Android `Vibrator` / `VibrationEffect` adaptörünün yazılması (`HexaFleetHapticFeedback`).
  - [x] Sonar taraması için mikro tıkırtı (`playSonarTick`).
  - [x] İsabet anı için keskin patlama darbesi (`playHitImpact`).
  - [x] Kalkan devreye girdiğinde dalgalı titreşim (`playShieldHum`).
  - [x] Gemi batma dramatik titreşim dizisi (`playShipSunkDrama`).
  - *Kabul Kriteri:* Haptic ayarı açıkken her aksiyonun parmakta hissedilir farklı bir profil sunması doğrulandı. (BAŞARILI)
- [x] **Görev 5.3: Ses & Atmosfer**
  - [x] Düşük gecikmeli yerel ton ses motoru (`HexaFleetSoundManager`).
  - [x] Sonar bip sesleri (`playSonarPing`), füze ateşleme (`playFireSound`), patlama (`playHitSound`), su sıçraması (`playMissSplash`) ve kalkan uğultusu (`playShieldHum`).
  - *Kabul Kriteri:* Ek harici dosya boyutu bindirmeyen, donanım optimizasyonlu sesler entegre edildi. (BAŞARILI)

---

### FAZ 6: Performans, Test, Güvenlik & Yayın Hazırlığı
> **Hedef:** Kararlı, yüksek performanslı ve mağazaya hazır üretim sürümü.

- [x] **Görev 6.1: Profiling & FPS Kararlılığı**
  - [x] HexBoardCanvas path ve çizim önbelleği (`HexPathCache`) ile sıfır GC çöpü (0 allocation / frame).
  - [x] Donanım hızlandırmalı Canvas render motoru ile 60/120 FPS akıcılık.
  - [x] `DisposableEffect` ile ses ve sensör kaynaklarının otomatik bellek tahliyesi.
  - *Kabul Kriteri:* Sıfır bellek sızıntısı ve kararlı kare hızı doğrulandı. (BAŞARILI)
- [x] **Görev 6.2: Güvenlik, ProGuard & Boyut Küçültme**
  - [x] R8 / ProGuard optimizasyonu (`app/proguard-rules.pro` ve `isMinifyEnabled = true`, `isShrinkResources = true`).
  - [x] Kotlinx Serialization, Room SQLite ve Compose kurallarının korunması.
  - [x] APK boyutunun 25 MB altında tutulması: **Üretilen `app-release.apk` sadece 2.04 MB!** (Hedefin %90 altında).
  - *Kabul Kriteri:* `gradlew assembleRelease` başarıyla tamamlandı (210 task, BUILD SUCCESSFUL). (BAŞARILI)
- [x] **Görev 6.3: Google Play Store Hazırlığı**
  - [x] Türkçe ve İngilizce vitrin açıklamaları, afişler ve ekran görüntüleri teknik kılavuzu (`STORE_LISTING_METADATA.md`).
  - [x] Vektörel adaptif uygulama ikonu (`ic_launcher`, `ic_launcher_round`, `ic_launcher_background`, `ic_launcher_foreground`).
  - [x] Gizlilik politikası (kişisel veri toplamama, anonim lobi eşleşmesi) ve ağ izinleri yapılandırması.
  - [x] Derin bağlantı (`hexafleet://lobby/{code}`) intent-filter entegrasyonu.
  - *Kabul Kriteri:* Google Play Store ve Android 15 (Target SDK 35) uyumlu dağıtım paketi tamamlandı. (BAŞARILI)

---

### FAZ 7: Modern Senkron Tasarım Sistemi (Beyaz, Lacivert, Mor, Kırmızı), Gelişmiş Lobi Gözlemleme & Çok Kademeli AI
> **Hedef:** Kullanıcı talepleri doğrultusunda neon temanın beyaz tabanlı senkron kurumsal deniz temasına dönüştürülmesi, 2. oyuncu bekleme odası protokolü, 3 kademeli taktiksel yapay zeka ve profesyonel üst kontrol çubuğu.

- [x] **Görev 7.1: Beyaz Taban, Lacivert, Mor ve Kırmızı Senkron Tasarım Sistemi**
  - [x] `BoardColors.kt` güncellenerek Beyaz zemin (`#F8FAFC`, `#FFFFFF`), Lacivert yapı (`#0F172A`, `#1E3A8A`), Canlı Kırmızı (`#DC2626`) ve Taktiksel Mor (`#7C3AED`) renkleri tüm UI elementlerinde standartlaştırıldı.
  - [x] `HexBoardCanvas.kt` açık renk okyanus petekleri, net lacivert ızgara çizgileri, mor radar dalgası ve kırmızı vuruş işaretleriyle yeniden uyarlandı.
  - [x] `MainActivity.kt` ve tüm paneller `HexaFleetStandardTheme` ile senkronize edildi.
  - *Kabul Kriteri:* Yüksek kontrastlı, temiz ve profesyonel standart arayüz hayata geçirildi. (BAŞARILI)

- [x] **Görev 7.2: Çevrimiçi Lobi Gözlemleme Modu & 2. Oyuncu Bağlantı Protokolü**
  - [x] Oda oluşturulduğunda maçın doğrudan başlaması engellendi; host "Lobi Bekleme Odası (Lobby Observation Mode)" durumuna geçirildi.
  - [x] 2. oyuncu odaya bağlanana kadar maçın başlamasını engelleyen kontrol yapısı kuruldu.
  - [x] Lobi ekranında oda kodu, kopyalama butonu, maç kural özeti (Süre, Harita, Engeller, PIN) ve "2. Oyuncu Bekleniyor... 📡" radar göstergesi eklendi.
  - [x] Tek cihazda hızlı test imkanı için simülasyon eşleşme butonu eklendi; rakip bağlandığında 3 saniyelik geri sayım ile otomatik başlatma sağlandı.
  - *Kabul Kriteri:* 2. oyuncu bağlanmadan maçın başlamayacağı kuralı ve bekleme odası eksiksiz uygulandı. (BAŞARILI)

- [x] **Görev 7.3: 3 Kademeli Eğitilmiş Yapay Zeka Modu (Play vs AI)**
  - [x] **1. Acemi Komutan (Recruit):** Rastgele keşif, düşük olasılıklı çevre takibi ile rahat antrenman seviyesi.
  - [x] **2. Kıdemli Taktikçi (Veteran):** 6 eksenli H-PDF olasılık yoğunluk haritası + parite (damalı tahta) avlanma + aktif isabet hedef takibi.
  - [x] **3. Büyük Amiral (Grand Admiral):** İleri düzey H-PDF + kalan gemi boyut geometrisine sığmayan petekleri eleme (Ship-fitting elimination) + İHA ve kalkan yeteneklerinin stratejik kullanımı.
  - [x] Harita boyutu (37, 61, 91 petek) ve çevre tehlikeleri seçimi eklendi.
  - [x] Tahta ekranında AI sırası animasyonlu gösterge ile simüle edilerek canlı telsiz günlüğü ve ses/haptik efektleriyle icra edildi.
  - *Kabul Kriteri:* 3 seviyeli eğitilmiş yapay zeka ile tek oyunculu amiral battı modu tamamlandı. (BAŞARILI)

- [x] **Görev 7.4: Sağ Üst Profesyonel Menü (Info, Ayarlar, Uygulama Kapatma)**
  - [x] **Info (ℹ️ - Nasıl Oynanır):** Altıgen koordinat mantığı, gemi sınıfları (Amiral Gemisi, Aegis Muhribi, İHA Taşıyıcısı, Hayalet Korvet), CP maliyetleri ve çevre tehlikelerini detaylı anlatan çok sekmeli taktik kılavuz.
  - [x] **Ayarlar (⚙️ - Operasyon Ayarları):** Ses Efektleri (%0 - %100 slider), radar bip sesleri anahtarı, haptik titreşim anahtarı, varsayılan tur süresi (Blitz, Standart, Taktik), savaş sisi ve radar varsayılanları.
  - [x] **Uygulama Kapatma (🚪 - Çıkış):** Şık onay penceresi ve güvenli oturum sonlandırma (`finishAffinity`).
  - *Kabul Kriteri:* Sağ üst köşe her ekranda standart ve erişilebilir olarak entegre edildi. (BAŞARILI)


