# HexaFleet: Tactical Waters
## Game Design Document (GDD) & Oyun Konsept Belgesi

---

### 1. Vizyon, Konsept ve Vitrin Anlatımı

#### 1.1. Oyunun Özeti ve Temel Felsefesi
**HexaFleet: Tactical Waters**, klasik Amiral Battı (Battleship) mekaniğini alıp 90 derecelik ızgara zincirlerinden kurtararak, yakın gelecekte geçen **ortak altıgen (hexagonal) okyanus haritası**, **dinamik Savaş Sisi (Fog of War)**, **elektronik harp sistemleri** ve **taktiksel gemi manevraları** ile donatan bir yeni nesil mobil/tablet sıra tabanlı strateji oyunudur.

Geleneksel oyundaki "iki ayrı kağıtta kör atış yapma" mantığı tamamen terk edilmiştir. İki amiral, aynı okyanus alanında, adalar, mercan resifleri, manyetik fırtınalar ve akıntılarla bölünmüş tek bir taktiksel petek ızgara üzerinde çarpışır.

```
       / \     / \     / \
      /   \___/   \___/   \
      \ q,-1/ \0,-1 / \1,-1/
      / \__/   \__/   \__/ \
     /   \ -1,0/ \ 0,0 / \ 1,0 \
     \   / \__/   \__/   \__/   /
      \ /     \   /   \   / \  /
               \ / 0,1 \ /
```

#### 1.2. Mağaza (Google Play Store) Tanıtım Metni
> **Doksan Derecelik Savaşlar Bitti: Altıgen Okyanusta Yeni Bir Taktik Çağı Başlıyor!**
>
> Klasik Amiral Battı’nın statik ve tahmin edilebilir dünyasını unutun. Yakın geleceğin siber-deniz harbinde iki filo artık birbirinden habersiz ayrı tablolarda değil; **Savaş Sisi (Fog of War)** ile örtülü, dinamik bir altıgen okyanus ızgarasında aynı suyun üzerinde kozlarını paylaşıyor!
>
> 6 eksenli hareket ve atış serbestisiyle her hamle yeni bir taktik kapısı aralar. Gemilerinizi mercan resiflerinin ve manyetik fırtınaların ardına 60 derecelik açılarla gizleyin; İHA geminizden kalkan insansız keşif dronlarıyla altıgen radar halkaları oluşturarak düşman hattını deşifre edin. Rakibiniz koordinatlarınızı kilitlediğinde çaresiz değilsiniz: Komut Puanı (CP) harcayarak kruvazörünüzü akıntılara bırakıp 1 petek kaydırın, sahte sinyal şamandıralarıyla düşmanı kör atışa sürükleyin veya EMP patlamasıyla düşman kalkanlarını felç edin.
>
> Tablet ve telefonlara özel optimize edilmiş neon-taktiksel dokunmatik arayüz, akıcı radar süpürme efektleri ve Android Haptic titreşim motoruyla her sonar yankısını ve isabeti parmak uçlarınızda hissedin!
>
> - **İster Çevrimdışı (Offline):** Gelişmiş yapay zeka amirallerine karşı taktiksel roguelike sefer moduna atılın.
> - **İster Çevrimiçi (Online):** 6 haneli oda koduyla anında arkadaşınızla eşleşin (şifreli/şifresiz özel lobiler) veya Blitz modunda saniyelerle yarışın!

---

### 2. Çekirdek Oynanış Mekanikleri (Core Gameplay)

#### 2.1. Ortak Altıgen Izgara (Shared Hex-Grid) & Koordinat Sistemi
- **Harita Boyutları:**
  - *Hızlı Düello / Mobil Standart:* Yarıçap R=6 (91 Hex Petek) veya 11x11 Eksenel Izgara.
  - *Büyük Filo / Tablet / Dereceli:* Yarıçap R=8 (169 Hex Petek) veya 15x15 Eksenel Izgara.
- **Eksenel ve Kübik Koordinasyon:**
  - Kübik Formül: $x + y + z = 0$
  - Eksenel Formül: $(q, r) \implies x = q, z = r, y = -q - r$
  - İki petek arası mesafe:
    $$\text{Distance}(A, B) = \frac{|A.q - B.q| + |A.q + A.r - B.q - B.r| + |A.r - B.r|}{2}$$
- **Simetrik Başlangıç Bölgeleri:**
  - Haritanın iki zıt kutbu (Kuzeybatı vs Güneydoğu) oyuncuların konuşlanma bölgeleridir.
  - Ortada tarafsız, kaynak ve taktiksel engeller içeren "Sıcak Çatışma Bölgesi" yer alır.

#### 2.2. Savaş Sisi (Fog of War) ve Görüş Hattı (Line of Sight - LoS)
Harita üç katmandan oluşur:
1. **Keşfedilmemiş Bölge (Pitch Black):** Hiçbir sinyalin ulaşmadığı tamamen karanlık petekler.
2. **Sinyal Hafızası / Sis (Fogged):** Daha önce taranmış veya geçilmiş, zemin topoğrafyası (resif, derinlik) bilinen ancak düşman varlığı anlık güncellenmeyen alan. Düşmanın son görüldüğü yerde hayalet siluet (Ghost Marker) kalır.
3. **Canlı Görüş Menzili (Active LoS):** Gemilerin kendi etrafındaki görüş halkası (gemi sınıfına göre 1 veya 2 halka yarıçapı) veya aktif radar/İHA taraması altındaki petekler.

**Görüş Hattı Kuralları (Hex LoS):**
- İki petek arasında Bresenham Hexagonal Raycasting çalıştırılır.
- Araya giren "Yüksek Ada / Kara Parçası" görüş hattını ve radar sinyalini keser.

#### 2.3. Komut Puanı (Command Points - CP) & Enerji Sistemi *(Öneri/Geliştirme)*
Sıra tabanlı oyunlarda sırf sıra bekleme hissini dinamikleştirmek ve oyuncuyu stratejik seçim yapmaya zorlamak için **Komut Puanı (CP)** sistemi devreye alınır:
- Her oyuncu tur başına **3 CP** ile başlar (Tur sonu kullanılmayan puanların en fazla 1 tanesi devredilir; Maksimum depo = 4 CP).
- **Eylemlerin CP Maliyetleri:**
  - Standart Ana Batarya Atışı: **1 CP**
  - Gemi Manevrası (1 Petek İleri/Geri): **1 CP**
  - Gemi Rotasyonu (Pivot 60 Derece Çevirme): **1 CP**
  - Özel Yetenek Kullanımı (İHA, EMP, Kalkan vb.): **1 ila 2 CP** (Ayrıca tur bekleme süresi - Cooldown vardır).
- *Taktiksel Anlamı:* Oyuncu bir turda isterse "1 Keşif İHA'sı (2 CP) + 1 Atış (1 CP)", isterse "1 Gemiyi Akıntıya Kaçırma (1 CP) + 2 Standart Atış (2 CP)" yapabilir.

---

### 3. Gemi Sınıfları, Özellikler ve Yetenekler

Her filo maç öncesi toplam **Filo Bütçesi (Örn: 100 Puan)** dahilinde seçilir veya standart filo formatında oynanır.

| Gemi Sınıfı | Boyut (Hex) | Can (HP) | Temel Rol | Özel Yetenek (Active Ability) | Cooldown & CP |
| :--- | :---: | :---: | :--- | :--- | :--- |
| **İHA / Drone Taşıyıcı** | 3 Petek | 3 | Keşif & İstihbarat | **Halka Tarama Dronu:** Seçilen bir merkez peteğin etrafındaki 1. Halkayı (7 petek) açar. Hasar vermez; gemileri 1 tur ifşa eder. | 3 Tur / 2 CP |
| **EMP Kruvazörü** | 3 Petek | 3 | Elektronik Harp | **Elektromanyetik Şok:** Seçilen peteğe EMP mühimmatı atar. Hedef petek ve komşularındaki gemilerin yetenek ve kalkanlarını 2 tur kilitler. | 4 Tur / 2 CP |
| **Aegis Muhribi** | 2 Petek | 2 | Savunma & Eskort | **Nano-Kalkan Matrisi:** Kendisinin veya 1 petek mesafedeki dost geminin üzerine 1 turluk koruma kalkanı örter; ilk gelen isabeti sıfırlar. | 3 Tur / 1 CP |
| **Hayalet Denizaltı** | 2 Petek | 2 | Suikast & Sızma | **Vektörel Çizgisel Torpido:** Seçilen eksen (60°) boyunca doğrusal hatla giden torpido ateşler. İlk çarptığı gemide veya resifte patlar (2x Hasar). | 3 Tur / 2 CP |
| **Hücumbot & Mayın Gemisi** | 1 Petek | 1 | Tuzak & Hızlı Manevra | **Akıllı Mayın / Decoy Bırakma:** Bulunduğu peteğe görünmez mayın bırakır veya radarda gerçek gemi sinyali veren Sahte Şamandıra (Decoy) yerleştirir. | 2 Tur / 1 CP |
| **Amiral Zırhlısı (Dreadnought)** *(Öneri)* | 4 Petek | 4 | Ağır Bombardıman | **Aşırı Yüklü Salvo:** 3 doğrusal peteği aynı anda bombalar. Hantal yapıdadır (manevra maliyeti 2 CP). | 4 Tur / 2 CP |

---

### 4. Çevre Unsurları ve Dinamik Harita Katmanları

Harita jeneratörü maç başında belirli oranlarda taktiksel çevre etmenleri yerleştirir:

1. **Mercan Resifleri ve Kayalık Adalar (Impassable Terrains):**
   - Gemiler bu peteklere giremez.
   - Doğrusal torpidolar bu kayalıklara çarptığında patlar; arkasındaki gemileri korur.
2. **Manyetik Fırtına Bulutları (Magnetic Storms / Ion Clouds):**
   - Bu peteklerin içine giren gemiler İHA ve radar taramalarında görünmez olur.
   - *Dezavantaj:* Fırtınadaki gemi de dışarıya yetenek kullanamaz (kendi sensörleri de körleşir).
3. **Termal Okyanus Akıntıları (Ocean Currents):**
   - Harita üzerinde yön oku bulunan akıntı petekleri.
   - Tur sonunda bu petekte duran gemi, yön vektörüne doğru **0 CP (ücretsiz)** 1 petek sürüklenir. Taktiksel kaçış veya düşmanı tuzağa düşürme unsuru olarak kullanılabilir.
4. **Derin Hendekler (Deep Trenches):**
   - Yalnızca denizaltıların dalış yaparak geçebildiği derinlikler. Yüzey gemileri burayı normal su olarak algılarken, denizaltı hendek içinde ek gizlilik bonusu kazanır.

---

### 5. Oyun Modları

#### 5.1. Çevrimdışı (Single Player) Modlar
- **Taktiksel Sefer / Roguelike Sefer (Campaign Mode):**
  - Düğüm (Node) tabanlı bir harita üzerinde ilerleme (Slay the Spire / FTL tarzı harita ağı).
  - Her düğümde yapay zeka amiralleri, özel görevler (örneğin: *"3 tur içinde İHA gemisini kurtar"*, *"Mayın tarlasından hasarsız geç"*).
  - Boss Savaşları: 2x4 kare kaplayan, çoklu taretlere ve kalkan jeneratörlerine sahip dev yüzen platformlar (**Leviathan Mobile Fortress**).
  - Kazanılan kredilerle yeni gemi sınıfları, radar menzil geliştirmeleri ve pasif modüller açılır.
- **Hızlı Yapay Zeka Karşılaşması (Skirmish):**
  - Zorluk seviyeleri: *Acemi (Recruit)*, *Kıdemli (Veteran)*, *Büyük Amiral (Grand Admiral)*.
  - Özel kural setleri ve harita boyutu seçimi.

#### 5.2. Çevrimiçi Çok Oyunculu (Online Multiplayer)
- **Özel Oda (Private Room / Custom Lobby):**
  - **Oda Kodu Üretimi:** 6 haneli alfa-nümerik benzersiz kod (Örn: `HEX-709`).
  - **Şifreli veya Şifresiz:** Oda kurucu dilerse 4 haneli PIN belirleyebilir; dilerse herkese açık bırakabilir.
  - **Doğrudan Bağlantı & Paylaşım:** Tek tıkla panoya kopyalama, QR Kod oluşturma (özellikle yan yana iki mobil/tablet cihazın kamerayla anında eşleşmesi için) ve Deep Link (`hexafleet://lobby/HEX-709`).
- **Dereceli Eşleşme (Ranked Matchmaking):**
  - ELO / MMR puanına göre küresel havuzdan rakip bulma.
  - Maç öncesi **Gemi Taslaklama (Draft Phase):** 100 puanlık bütçe ile 45 saniyede filo dizilimi.
- **Blitz Düello (Blitz Mode):**
  - Tur süresi **10 saniye**.
  - R=5 daha kompakt tahta, yüksek adrenalin ve hızlı karar gerektiren maçlar.

---

### 6. Kullanıcı Deneyimi, Ergonomi & Platform Tasarımı

#### 6.1. Mobil ve Tablet Uyarlanabilirliği (Responsive & Adaptive UI)
- **Akıllı Telefonlar (Portrait & Landscape Desteği):**
  - Varsayılan Dikey (Portrait): Üstte kompakt radar mini-map, ortada ana Hex Canvas, altta parmak ergonomisine uygun açılır taktiksel çekmece (Tactical Command Drawer).
  - Çift parmakla serbest Pan & Zoom, seçilen peteğe yumuşak odaklanma (Camera Hex-Snap).
- **Tabletler ve Katlanabilir Cihazlar (Foldables):**
  - Bölünmüş Ekran (Dual-Pane Command Deck): Ekranın sol %65'inde geniş açılı yüksek çözünürlüklü Hex Okyanus Haritası; sağ %35'lik panelde filo telemetrisi, gemi can durumları, aktif yetenek butonları ve canlı muharebe günlüğü (Battle Log).
  - Geniş ekranın avantajıyla dokunmatik hedefleme hataları sıfıra indirilir.

#### 6.2. Mobil Duyusal Tasarım (Haptics & Audio)
- **Android Haptic Feedback Entegrasyonu:**
  - *Sonar Araması:* Hafif, ritmik ve frekansı azalan çift tıkırtı (`PRIMITIVE_TICK`).
  - *İsabet (Hit):* Keskin ve tok darbe (`PRIMITIVE_HEAVY_CLICK`).
  - *EMP Patlaması:* Uzayan mikroskobik titreşim dalgası.
  - *Gemi Batması:* 3 kademeli dramatik titreşim dizisi.
- **Görsel & Ses Atmosferi:**
  - Koyu lacivert, antrasit ve neon camgöbeği (Cyan / Radar Green / Alert Amber) taktiksel askeri siber-radar teması.
  - Ses efektleri: Düşük frekanslı okyanus dip homurtusu, hidrofon sesleri, mekanik taret dönme klikleri ve telsiz kod onayları.

