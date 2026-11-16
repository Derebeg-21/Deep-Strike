# Deep Strike (HexaFleet: Tactical Waters) ⚓🔷

> **Yakın Geleceğin Siber-Deniz Harbi: Altıgen Okyanus, Savaş Sisi ve Taktiksel Manevralar.**

**Deep Strike**, klasik Amiral Battı (Battleship) mekaniğini 90 derecelik ızgaralardan kurtararak **ortak altıgen (hexagonal) tahta**, **Savaş Sisi (Fog of War)**, **elektronik harp yetenekleri** ve **dinamik okyanus koşullarıyla** yeniden yorumlayan, Android platformu için **%100 Kotlin ve Jetpack Compose Canvas** ile geliştirilen yeni nesil bir sıra tabanlı taktik strateji oyunudur.

---

## 🧭 Dokümantasyon İndeksi

Bu depoda proje için hazırlanmış kapsamlı mimari, tasarım ve geliştirme belgeleri yer almaktadır:

1. 📄 **[GDD_HEXAFLEET.md](file:///d:/Yazılım%20Projeleri/Battleship/GDD_HEXAFLEET.md)**: Oyun Tasarım Belgesi (Game Design Document). Konsept anlatımı, altıgen kural seti, Komut Puanı (CP) sistemi, gemi sınıfları ve yetenekleri, çevre etmenleri ve oyun modları.
2. 📐 **[TECH_ARCHITECTURE_SPEC.md](file:///d:/Yazılım%20Projeleri/Battleship/TECH_ARCHITECTURE_SPEC.md)**: Teknik Mimari ve Mühendislik Şartnamesi. Çok modüllü Clean Architecture yapısı, saf Kotlin altıgen matematik motoru (`core:hexmath`), Compose Canvas render hattı, hile önlemeli çevrimiçi çok oyunculu protokol (Zero-Knowledge Fog) ve yapay zeka algoritması.
3. 📋 **[ROADMAP_AND_TASK_LIST.md](file:///d:/Yazılım%20Projeleri/Battleship/ROADMAP_AND_TASK_LIST.md)**: Faz ve Sprint bazlı İş Kırılım Ağacı (WBS) ve Görev Listesi. Faz 0'dan Faz 6'ya kadar kabul kriterleriyle tanımlanmış kontrol edilebilir görevler.

---

## ⚡ Temel Özellikler

- 🔷 **6 Yönlü Altıgen Okyanus:** Eksenel (Axial $q, r$) ve Kübik ($x, y, z$) koordinat sistemiyle hesaplanan 60 derecelik torpido açıları ve kusursuz halka (Ring AoE) etki alanları.
- 🌫️ **Ortak Tahta & Savaş Sisi (Fog of War):** İki amiral aynı suyun üzerinde! İHA keşifleri, radar süpürmesi ve görüş hattı (Line of Sight - LoS) ile dinamik sis açma.
- ⚡ **Komut Puanı (CP) & Dinamik Manevra:** Gemiler sabit hedef değildir; hasar almadan önce akıntılara bırakılabilir veya taktiksel olarak 1 petek kaydırılabilir.
- 🚢 **Modern Gemi Sınıfları & Aktif Yetenekler:**
  - **İHA Taşıyıcı:** Halka tarama dairesi.
  - **EMP Kruvazörü:** 2 tur radar ve kalkan kitleme.
  - **Aegis Muhribi:** Enerji koruma kalkanı.
  - **Hayalet Denizaltı:** Çizgisel su altı torpidoları.
  - **Hücumbot:** Mayın ve Sahte Sinyal Şamandırası (Decoy).
- 🌐 **Esnek Çevrimiçi Çok Oyunculu (Multiplayer):**
  - 6 Haneli Oda Kodu (`HX-7482`) ile anında arkadaş eşleşmesi.
  - Şifreli (4 Haneli PIN) veya Şifresiz Özel / Genel Lobiler.
  - Kamerayla hızlı katılım için **QR Kod** ve **Deep Link** desteği.
  - 10 saniyelik **Blitz Modu** ve 100 puanlık bütçeli **Gemi Taslaklama (Draft)**.
- 🤖 **Akıllı Çevrimdışı Yapay Zeka (AI):** Altıgen Olasılık Yoğunluk Haritası (Hex-Probability Density Function) ile akıllı av ve hedef takip stratejisi.
- 📱 **Tablet & Mobil Uyarlanabilir Ergonomi:**
  - Katlanabilir ve tabletlerde çift panelli (Dual-Pane) komuta güvertesi.
  - Android Haptic titreşim motoru ile sonar tıklamaları ve keskin isabet hissi.

---

## 🛠️ Teknoloji Yığını (Tech Stack)

| Katman | Teknoloji |
| :--- | :--- |
| **Dil** | Kotlin 2.0+ (100% Native) |
| **Arayüz & Grafik** | Jetpack Compose + Custom Hardware Canvas + Android Shaders |
| **Mimari** | Clean Architecture + Multi-Module + MVI / StateFlow |
| **Eşzamanlılık** | Kotlin Coroutines & Flow |
| **Ağ & Senkronizasyon** | Ktor Client / WebSockets & Firebase Realtime Database Adaptörü |
| **Veri Kalıcılığı** | Room Database (Sefer Modu & İstatistik) + Jetpack DataStore |
| **Bağımlılık Enjeksiyonu** | Google Hilt |
| **Test** | JUnit 5, MockK, Turbine, Compose UI Test |

---

## 🚀 Başlarken & Önerilen Geliştirme Sırası

1. `ROADMAP_AND_TASK_LIST.md` dosyasındaki **FAZ 0** adımlarını uygulayarak Gradle çoklu modül yapısını ve `core:hexmath` matematik kütüphanesini oluşturun.
2. Birim testlerle eksenel koordinat doğruluğunu garanti altına alın.
3. **FAZ 1** ile Compose Canvas üzerinde ilk altıgen ızgarayı ekrana basın.

