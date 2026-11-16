package com.hexafleet.feature.board.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.hexafleet.feature.board.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hexafleet.feature.board.settings.HexaFleetSettings
import com.hexafleet.feature.board.theme.BoardColors
import kotlin.math.roundToInt

/**
 * Üst Sağ Profesyonel Eylem Çubuğu: Info, Ayarlar, Uygulamadan Çıkış.
 */
@Composable
fun HexaFleetTopActions(
    onOpenInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Bilgi & Nasıl Oynanır (Info) Butonu
        IconButton(
            onClick = onOpenInfo,
            modifier = Modifier
                .size(40.dp)
                .background(BoardColors.PureWhite, CircleShape)
                .border(1.dp, BoardColors.BorderSubtle, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Nasıl Oynanır ve Bilgi",
                tint = BoardColors.PurplePrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Ayarlar Butonu
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(40.dp)
                .background(BoardColors.PureWhite, CircleShape)
                .border(1.dp, BoardColors.BorderSubtle, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Oyun Ayarları",
                tint = BoardColors.NavyAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        // 3. Uygulama Kapatma Butonu
        IconButton(
            onClick = onOpenExit,
            modifier = Modifier
                .size(40.dp)
                .background(BoardColors.RedLight, CircleShape)
                .border(1.dp, BoardColors.CrimsonRed.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Uygulamayı Kapat",
                tint = BoardColors.CrimsonRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 1. BİLGİ VE NASIL OYNANIR PENCERESİ (Info Dialog)
 */
@Composable
fun HexaFleetInfoDialog(
    onDismissRequest: () -> Unit
) {
    var selectedInfoTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BoardColors.PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 1. SABİT BAŞLIK & KAPAT BUTONU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DEEP STRIKE TAKTİK KILAVUZU",
                            color = BoardColors.NavyDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Altıgen Deniz Muharebe Doktrini",
                            color = BoardColors.TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = BoardColors.NavyDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. SABİT SEKME ÇUBUĞU (4 SEKME: GENEL, FİLO & YETENEK, MODLAR & AI, İLETİŞİM - User Requirement 4 & 5)
                TabRow(
                    selectedTabIndex = selectedInfoTab,
                    containerColor = BoardColors.SurfaceSubtle,
                    contentColor = BoardColors.NavyDark,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedInfoTab]),
                            color = BoardColors.PurplePrimary
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, BoardColors.BorderSubtle, RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedInfoTab == 0,
                        onClick = { selectedInfoTab = 0 },
                        text = { Text("GENEL", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    )
                    Tab(
                        selected = selectedInfoTab == 1,
                        onClick = { selectedInfoTab = 1 },
                        text = { Text("FİLO & YETENEK", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    )
                    Tab(
                        selected = selectedInfoTab == 2,
                        onClick = { selectedInfoTab = 2 },
                        text = { Text("MODLAR & AI", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    )
                    Tab(
                        selected = selectedInfoTab == 3,
                        onClick = { selectedInfoTab = 3 },
                        text = { Text("İLETİŞİM", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. KAYDIRILABİLİR İÇERİK ALANI + ÇİFT YÖNLÜ AKILLI SCROLL (User Requirement 4)
                val scrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(bottom = 28.dp, top = if (scrollState.canScrollBackward) 24.dp else 0.dp)
                    ) {
                        when (selectedInfoTab) {
                            0 -> GeneralRulesSection()
                            1 -> FleetAndAbilitiesSection()
                            2 -> ModesAndAiSection()
                            3 -> ContactSection()
                        }
                    }

                    // Üst Kaydırma İpucu (Yukarıda içerik varsa görünür ve dokunulduğunda yukarı kaydırır)
                    if (scrollState.canScrollBackward) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            BoardColors.PureWhite,
                                            BoardColors.PureWhite.copy(alpha = 0.95f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Surface(
                                color = BoardColors.PurplePrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.PurplePrimary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            scrollState.animateScrollTo(0)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = BoardColors.PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "YUKARI KAYDIRIN",
                                        color = BoardColors.PurplePrimary,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = BoardColors.PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Alt Kaydırma İpucu (Aşağıda daha fazla içerik varsa görünür ve dokunulduğunda aşağı kaydırır)
                    if (scrollState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            BoardColors.PureWhite.copy(alpha = 0.95f),
                                            BoardColors.PureWhite
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Surface(
                                color = BoardColors.PurplePrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.PurplePrimary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = BoardColors.PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "AŞAĞI KAYDIRIN",
                                        color = BoardColors.PurplePrimary,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = BoardColors.PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. SABİT ALT BUTON
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BoardColors.NavyAccent)
                ) {
                    Text(
                        text = "ANLADIM, SAHAYA DÖN",
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
private fun GeneralRulesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(
            title = "1. Altıgen Koordinat Sistemi (q, r, s)",
            description = "Klasik kare ızgara yerine 60 derecelik 6 yöne yayılan eksenel altıgen ızgara kullanılır. Gemiler Doğu-Batı, Kuzeydoğu-Güneybatı veya Kuzeybatı-Güneydoğu eksenlerinde düz hat boyunca yerleşir.",
            icon = Icons.Default.Sensors,
            iconTint = BoardColors.NavyAccent
        )
        InfoCard(
            title = "2. Komut Puanı (CP) Ekonomisi",
            description = "Her turun başında amiralinize Komut Puanı (CP) verilir. Her standart top atışı 1 CP, İHA keşfi 3 CP, EMP elektronik saldırısı 2 CP tüketir. CP bittiğinde veya isteğe bağlı olarak tur rakibe geçer.",
            icon = Icons.Default.ElectricBolt,
            iconTint = BoardColors.PurplePrimary
        )
        InfoCard(
            title = "3. Savaş Sisi (Fog of War) & Radar",
            description = "Düşman gemileri sis arkasında gizlidir. Yalnızca keşfedilen veya İHA taraması yapılan petekler aydınlanır. Dönen siber-radar aktif sonar verilerini haritaya aktarır.",
            icon = Icons.Default.Radar,
            iconTint = BoardColors.CrimsonRed
        )
        InfoCard(
            title = "4. Çevresel Tehlikeler",
            description = "Mercan Resifleri mermileri engeller ve gemiler yerleşemez. Manyetik Fırtınalar radarı ve İHA taramasını körleştirir. Okyanus akıntıları mermileri saptırabilir.",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            iconTint = BoardColors.CoralReef
        )
    }
}

@Composable
private fun FleetAndAbilitiesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // BÖLÜM 1: DONANMA UNSURLARI & GEMİLER
        Text(
            text = "⚓ DONANMA BİLEŞENLERİ VE GEMİ TİPLERİ",
            color = BoardColors.NavyDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        InfoCard(
            title = "1. ⚡ Korvet (Hayalet Korvet - 2 Petek)",
            description = "2 peteklik yüksek manevra kabiliyetine sahip hücumbottur. Harita kenarlarına ve resif aralarına gizlenmesi en kolay stealth gemidir.\n• ⚡ Pasif Yetenek — Hayalet Gövde (Stealth Hull): Düşmanın 7 peteklik İHA radar taraması Korvet'in üzerinden geçse dahi radar %50 olasılıkla sinyal yakalayamaz (tamamen gizlenir). Bu gerçek %50 olasılık tüm oyun modlarında (Yapay Zeka ve Çok Oyunculu) geçerlidir.",
            iconTint = BoardColors.PurplePrimary,
            imageRes = R.drawable.ship_corvette
        )

        InfoCard(
            title = "2. 🔺 Delta (Delta Taarruz Kruvazörü - 3 Petek Üçgen)",
            description = "1 önde (burun) ve 2 arkada (sol/sağ kanat) eşkenar üçgen gövdeye sahiptir. Doğrusal olmayan delta geometrisiyle tespit edilmesi zordur.\n• ⚡ Aktif Yetenek — İyon Şoku / EMP Darbesi (0 CP): Düşman haritası sol üst köşesindeki özel rozetten tetiklenir (Varsayılan maç başı 1 hak, oda ayarlarından 0-3 arası belirlenebilir). Seçilen 7 peteklik sektöre iyon şoku yollar: Bölgedeki düşman Kalkanı anında çöker; bölgede düşman gemisi varsa rakibin bir sonraki turdaki Komut Puanı (CP) 3 yerine 2'ye düşer (elektronik arıza). Delta Kruvazörü batarsa bu yetenek devre dışı kalır.",
            iconTint = BoardColors.CrimsonRed,
            imageRes = R.drawable.ship_delta
        )

        InfoCard(
            title = "3. 📡 İHA G. (İHA Taşıyıcısı - 3 Petek)",
            description = "3 doğrusal peteklik filo istihbarat ve radar üssüdür.\n• 📡 Kritik Bağlantı — Radar İstihbarat Merkezi (High-Value Target): Filonun İHA yeteneği doğrudan bu gemiye bağlanmıştır. Eğer İHA Taşıyıcısı batırılırsa radar istasyonu imha edilir ve İHA keşif kabiliyeti tamamen kilitlenir (kullanılamaz). Oyuncular bu gemiyi korumak için özel savunma stratejileri kurmalıdır.",
            iconTint = BoardColors.NavyAccent,
            imageRes = R.drawable.ship_carrier
        )

        InfoCard(
            title = "4. 🛳️ Amiral (Amiral Zırhlısı - 4 Petek)",
            description = "Filonun ana komuta merkezidir. 4 doğrusal petekten oluşan en uzun ve en zırhlı amiral gemisidir.\n• 💥 Aktif Yetenek — Ağır Batarya Bombardımanı (Line Barrage - 3 CP): Düşman haritası sol üst köşesindeki özel rozetten tetiklenir (Varsayılan maç başı 1 hak, oda ayarlarından 0-3 arası belirlenebilir). Tek petek yerine, altıgen doğrultuda arka arkaya 5 peteği aynı anda bombalar (Yön butonuyla Yatay, Çapraz Sağ veya Çapraz Sol seçilebilir). Resiflerin arkasına saklanmış gemileri havadan süpürmek için idealdir. Amiral Zırhlısı batarsa bu yetenek kaybedilir.",
            iconTint = BoardColors.NavyDark,
            imageRes = R.drawable.ship_flagship
        )

        InfoCard(
            title = "☢️ Radyoaktif Sis Bölgesi (2 Petek - İHA Kamuflajı)",
            description = "Oyun başında konuşlandırılan 2 peteklik taktiksel yeşil sis perdesidir. Düşman İHA keşfi bu bölgedeki gemileri ASLA tespit edemez (radar boş gösterir). Ancak doğrudan top atışlarına karşı koruma sağlamaz.",
            icon = Icons.Default.Sensors,
            iconTint = Color(0xFF16A34A)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // BÖLÜM 2: KOMUT PUANI (CP) VE HAREKÂT SENARYOLARI
        Text(
            text = "⚡ KOMUT PUANI (CP) VE TAARRUZ / SAVUNMA SENARYOLARI",
            color = BoardColors.NavyDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        InfoCard(
            title = "🎯 Top Atışı (1 CP) — Standart Taarruz",
            description = "Seçilen tek bir altıgen peteğe top mermisi ateşlenir.\n• İsabet (HIT): Düşman gemisi vurulur ve patlar.\n• Karavana (Null): Mermi denize düşer, haritada 'Null' su sıçraması izi kalır.\n• Kalkan Soğurması: Hedefte düşman kalkanı varsa mermi tamamen havada emilir, altındaki gemi hasar almaz (gemi olup olmadığı gizli kalır) ve 7 peteklik kalkan kubbesi çöker!",
            icon = Icons.Default.GpsFixed,
            iconTint = BoardColors.CrimsonRed
        )

        InfoCard(
            title = "📡 Taktik İHA Keşfi (3 CP) — Geniş Alan Taraması",
            description = "Seçilen merkez ve komşu 6 petek (toplam 7 peteklik sektör) anında taranır.\n• Sinyal Tespiti: Sektördeki düşman gemi parçası sayısı bildirilir (tam koordinatları gizli kalır).\n• İHA Taşıyıcı Şartı: İHA Taşıyıcınız batmışsa radar kilitlenir ve kullanılamaz.\n• Hayalet Korvet Gizliliği: Düşman Korveti %50 olasılıkla radardan tamamen gizlenir.\n• Radyoaktif Sis Engeli: Sis altındaki gemiler tespit edilemez (boş görünür).\n• Kalkan Teması: Kalkan merkezine İHA gelirse kalkanın 1 canı kalır; komşu peteğe İHA gelirse kalkan kubbesi anında çöker.",
            icon = Icons.Default.Radar,
            iconTint = BoardColors.PurplePrimary
        )

        InfoCard(
            title = "🛡️ Aegis Savunma Kubbesi (2 CP) — Elektronik Kalkan",
            description = "Seçilen dost sektör etrafına 7 peteklik heksagonal koruma kubbesi örter.\n• Darbe Soğurması: Sektör içindeki herhangi bir peteğe gelen ilk düşman top atışını hasarsız sönümler.\n• Kubbenin Çökmesi: Mermiyi sönümledikten sonra kalkan kubbesi tamamen çöker ve haritada flu mavi sönümleme izi kalır.\n• Gizlilik: Düşman kalkan açtığında muharebe günlüğünde koordinatı gizli tutulur.",
            icon = Icons.Default.Shield,
            iconTint = BoardColors.NavyAccent
        )

        InfoCard(
            title = "🎮 Altıgen Radyal Menü (Radial HUD Wheel) — Hızlı Dokunmatik",
            description = "Düşman haritasındaki herhangi bir peteğe hafifçe basılı tuttuğunuzda (~260ms), o peteğin etrafında fütüristik neon 3 dilimli kontrol HUD tekerleği açılır:\n• 🔴 Üst Dilim: Ateş Et (1 CP)\n• 🟣 Sol Dilim: İHA Taraması (3 CP - Taşıyıcı canlıyken)\n• 🔵 Sağ Dilim: Aegis Kalkanı (2 CP)\nParmağınızı ilgili yöne kaydırıp bıraktığınız anda (Flick & Release) komut anında ateşlenir. Mobil dokunmatik ekranlar için süper akıcı bir savaş gemisi radar konsolu hissi verir.",
            icon = Icons.Default.Radar,
            iconTint = BoardColors.PurplePrimary
        )
    }
}

@Composable
private fun ModesAndAiSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(
            title = "🌐 Çevrimiçi Çok Oyunculu (Multiplayer)",
            description = "Oda kurarak arkadaşınızla oynayın. Oyun, 2. oyuncu lobiye bağlandığı anda başlar. PIN koruması ile sadece davet ettiğiniz kişiler katılabilir.",
            icon = Icons.Default.Sensors,
            iconTint = BoardColors.NavyAccent
        )
        InfoCard(
            title = "🤖 Acemi Komutan (Recruit AI)",
            description = "Yeni başlayanlar için uygundur. Rastgele keşif atışları yapar ve isabet bulduğunda bazen komşu kareleri yoklar.",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            iconTint = BoardColors.TextSecondary
        )
        InfoCard(
            title = "🎯 Kıdemli Taktikçi (Veteran AI)",
            description = "6 eksenli Olasılık Yoğunluk Haritası (H-PDF) ve parite avlanma sistemini uygular. Bir isabet aldığında gemiyi batırana dek takip eder.",
            icon = Icons.Default.Radar,
            iconTint = BoardColors.PurplePrimary
        )
        InfoCard(
            title = "👑 Büyük Amiral (Grand Admiral AI)",
            description = "Üst düzey yapay zeka: H-PDF + kalan gemilerin boyutuna göre sığamayacak petekleri eleyen ship-fitting algoritması + İHA ve kalkan yeteneklerini en ölümcül şekilde kullanır.",
            icon = Icons.Default.ElectricBolt,
            iconTint = BoardColors.CrimsonRed
        )
    }
}

/**
 * 4. İLETİŞİM VE GERİ BİLDİRİM SEKME İÇERİĞİ (User Requirement 5)
 * honorificterra@gmail.com adresine tek tıkla e-posta oluşturma ve panoya kopyalama sağlar.
 */
@Composable
private fun ContactSection() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BoardColors.SurfaceSubtle,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.PurplePrimary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BoardColors.PurpleLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = BoardColors.PurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "DEEP STRIKE KOMUTA MERKEZİ",
                            color = BoardColors.NavyDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Geliştirici İletişim ve Destek Masası",
                            color = BoardColors.TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }
                }

                Text(
                    text = "Deep Strike Deniz Muharebe Doktrini geliştirme sürecine katkıda bulunmak, taktiksel denge ve mekanik önerilerinizi paylaşmak veya teknik geri bildirimde bulunmak için doğrudan geliştirici ekibimizle irtibata geçebilirsiniz.",
                    color = BoardColors.TextSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp
                )

                // E-posta Adresi Satırı & Kopyala Butonu
                Surface(
                    color = BoardColors.PureWhite,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "✉️", fontSize = 14.sp)
                            Text(
                                text = "honorificterra@gmail.com",
                                color = BoardColors.NavyDark,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("honorificterra@gmail.com"))
                                isCopied = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCopied) Color(0xFF16A34A) else BoardColors.SurfaceSubtle
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isCopied) "KOPYALANDI" else "KOPYALA",
                                color = if (isCopied) Color.White else BoardColors.NavyDark,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Otomatik E-Posta Açma Butonu (Intent mailto)
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:honorificterra@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Deep Strike Taktik Geri Bildirim")
                                putExtra(Intent.EXTRA_TEXT, "Merhaba Deep Strike Geliştirici Ekibi,\n\nOyun ile ilgili geri bildirimim:\n\n")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            clipboardManager.setText(AnnotatedString("honorificterra@gmail.com"))
                            isCopied = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BoardColors.PurplePrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "E-POSTA UYGULAMASINI AÇ",
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
private fun InfoCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = BoardColors.PurplePrimary,
    imageRes: Int? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BoardColors.SurfaceSubtle,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (imageRes != null) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 34.dp)
                        .background(BoardColors.PureWhite, RoundedCornerShape(8.dp))
                        .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            } else if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = BoardColors.NavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = BoardColors.TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

/**
 * 2. PROFESYONEL AYARLAR PENCERESİ (Settings Dialog)
 */
@Composable
fun HexaFleetSettingsDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BoardColors.PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. SABİT BAŞLIK & KAPAT BUTONU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DEEP STRIKE AYARLARI",
                            color = BoardColors.NavyDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Ses, Kurallar ve Arayüz Seçenekleri",
                            color = BoardColors.TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = BoardColors.NavyDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. KAYDIRILABİLİR AYARLAR LİSTESİ + ÇİFT YÖNLÜ AKILLI SCROLL (User Requirement 4)
                val settingsScrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(settingsScrollState)
                            .padding(bottom = 28.dp, top = if (settingsScrollState.canScrollBackward) 24.dp else 0.dp)
                    ) {
                        // Ses Ayarları Bölümü
                        SectionHeader("SES VE EFEKT AYARLARI", Icons.AutoMirrored.Filled.VolumeUp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Taktiksel Ses Efektleri", color = BoardColors.TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = HexaFleetSettings.isSoundEnabled,
                                onCheckedChange = { HexaFleetSettings.isSoundEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.PurplePrimary)
                            )
                        }

                        if (HexaFleetSettings.isSoundEnabled) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ses Seviyesi", color = BoardColors.TextSecondary, fontSize = 12.sp)
                                    Text(
                                        "${(HexaFleetSettings.soundVolume * 100).roundToInt()}%",
                                        color = BoardColors.PurplePrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Slider(
                                    value = HexaFleetSettings.soundVolume,
                                    onValueChange = { HexaFleetSettings.soundVolume = it },
                                    colors = SliderDefaults.colors(
                                        thumbColor = BoardColors.PurplePrimary,
                                        activeTrackColor = BoardColors.PurplePrimary,
                                        inactiveTrackColor = BoardColors.SurfaceSubtle
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Radar & Sonar Bip Sesleri", color = BoardColors.TextPrimary, fontSize = 13.sp)
                                Switch(
                                    checked = HexaFleetSettings.isRadarPingEnabled,
                                    onCheckedChange = { HexaFleetSettings.isRadarPingEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.CrimsonRed)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dokunsal Geri Bildirim
                        SectionHeader("DOKUNSAL GERİ BİLDİRİM", Icons.Default.Vibration)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Titreşim / Haptic Feedback", color = BoardColors.TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = HexaFleetSettings.isHapticsEnabled,
                                onCheckedChange = { HexaFleetSettings.isHapticsEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.CrimsonRed)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Oyun Kural Tercihleri
                        SectionHeader("OYUN KURAL TERCİHLERİ", Icons.Default.Timer)

                        Text("Varsayılan Tur Süresi", color = BoardColors.TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15 to "Blitz (15 sn)", 45 to "Standart (45 sn)", 60 to "Taktik (60 sn)").forEach { (sec, label) ->
                                val isSelected = HexaFleetSettings.defaultTurnDurationSeconds == sec
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { HexaFleetSettings.defaultTurnDurationSeconds = sec },
                                    color = if (isSelected) BoardColors.PurpleLight else BoardColors.SurfaceSubtle,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) BoardColors.PurplePrimary else BoardColors.BorderSubtle
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) BoardColors.PurplePrimary else BoardColors.TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Varsayılan Harita Boyutu", color = BoardColors.TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5 to "Standart (91 Bölge)", 6 to "Geniş (127 Bölge)", 7 to "Devasa (169 Bölge)").forEach { (r, label) ->
                                val isSelected = HexaFleetSettings.defaultMapRadius == r
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { HexaFleetSettings.defaultMapRadius = r },
                                    color = if (isSelected) BoardColors.PurpleLight else BoardColors.SurfaceSubtle,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) BoardColors.PurplePrimary else BoardColors.BorderSubtle
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) BoardColors.PurplePrimary else BoardColors.TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Savaş Sisi Varsayılanı", color = BoardColors.TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = HexaFleetSettings.isFogOfWarDefault,
                                onCheckedChange = { HexaFleetSettings.isFogOfWarDefault = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.PurplePrimary)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Siber-Radar Taraması", color = BoardColors.TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = HexaFleetSettings.isRadarSweepDefault,
                                onCheckedChange = { HexaFleetSettings.isRadarSweepDefault = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = BoardColors.PurplePrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tasarım & Tema Standart Bilgisi
                        SectionHeader("GÖRSEL STANDART TEMA", Icons.Default.Palette)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BoardColors.SurfaceSubtle,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(14.dp).background(BoardColors.PureWhite, CircleShape).border(1.dp, BoardColors.BorderSubtle, CircleShape))
                                    Box(modifier = Modifier.size(14.dp).background(BoardColors.NavyDark, CircleShape))
                                    Box(modifier = Modifier.size(14.dp).background(BoardColors.PurplePrimary, CircleShape))
                                    Box(modifier = Modifier.size(14.dp).background(BoardColors.CrimsonRed, CircleShape))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Standart Senkron Tema (Beyaz, Lacivert, Mor, Kırmızı)",
                                    color = BoardColors.NavyDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Üst Kaydırma İpucu (Yukarıda içerik varsa görünür ve dokunulduğunda yukarı kaydırır)
                    if (settingsScrollState.canScrollBackward) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            BoardColors.PureWhite,
                                            BoardColors.PureWhite.copy(alpha = 0.95f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Surface(
                                color = BoardColors.PurplePrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.PurplePrimary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            settingsScrollState.animateScrollTo(0)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = BoardColors.PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "YUKARI KAYDIRIN",
                                        color = BoardColors.PurplePrimary,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = BoardColors.PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Alt Kaydırma İpucu (Aşağıda daha fazla içerik varsa görünür ve dokunulduğunda aşağı kaydırır)
                    if (settingsScrollState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            BoardColors.PureWhite.copy(alpha = 0.95f),
                                            BoardColors.PureWhite
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Surface(
                                color = BoardColors.PurplePrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.PurplePrimary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            settingsScrollState.animateScrollTo(settingsScrollState.maxValue)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = BoardColors.PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "AŞAĞI KAYDIRIN",
                                        color = BoardColors.PurplePrimary,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = BoardColors.PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. SABİT ALT BUTON
        Button(
            onClick = onDismissRequest,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BoardColors.NavyAccent)
        ) {
            Text("AYARLARI KAYDET VE KAPAT", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}
}
}

/**
 * 3. UYGULAMA KAPATMA ONAY PENCERESİ (Exit Dialog)
 */
@Composable
fun HexaFleetExitDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BoardColors.PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.CrimsonRed.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(BoardColors.RedLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = BoardColors.CrimsonRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "UYGULAMADAN ÇIKILSIN MI?",
                    color = BoardColors.NavyDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Deep Strike komuta merkezinden ayrılmak üzeresiniz. Devam eden operasyonel oturumunuz kapatılacaktır.",
                    color = BoardColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle)
                    ) {
                        Text("İPTAL", color = BoardColors.NavyDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            (context as? Activity)?.finishAffinity()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BoardColors.CrimsonRed)
                    ) {
                        Text("ÇIKIŞ YAP", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = BoardColors.PurplePrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = BoardColors.PurplePrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Tersanede konuşlandırılan gemi ve çevre nesneleri (Radyoaktif Sis vb.) için zengin taktiksel bilgi modeli.
 */
data class DeployableObjectInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val sizeText: String,
    val roleText: String,
    val abilityText: String,
    val description: String,
    val tacticalImpact: String,
    val placementTip: String,
    val accentColor: Color,
    val imageRes: Int? = null,
    val icon: ImageVector? = null,
    val cellCount: Int = 2,
    val isTriangle: Boolean = false
)

fun getDeployableObjectInfo(nameOrId: String): DeployableObjectInfo {
    val lower = nameOrId.lowercase()
    return when {
        lower.contains("delta") -> DeployableObjectInfo(
            id = "delta",
            title = "Delta Taarruz Kruvazörü",
            subtitle = "3 Peteklik Üçgen Gövdeli Ağır Kruvazör",
            badge = "AKTİF: EMP İYON ŞOKU (0 CP)",
            sizeText = "3 Petek (Eşkenar Üçgen)",
            roleText = "Elektronik Harp & Kalkan Kırıcı",
            abilityText = "Kalkan Çökertme & 1 CP Silme",
            description = "1 önde (burun) ve 2 arkada (sağ/sol kanat) eşkenar üçgen gövdeye sahiptir. Doğrusal olmayan delta geometrisiyle geleneksel hat taramalarında vurulması ve tespit edilmesi zordur.",
            tacticalImpact = "• Aktif Yetenek — İyon Şoku / EMP (0 CP): Düşman haritasında seçilen 7 peteklik sektöre güçlü EMP şoku yollar.\n• Kalkan İmhası: Hedefteki düşman Aegis Savunma Kalkanı'nı anında çökerterek yok eder.\n• CP Sabotajı: Sektörde düşman gemisi varsa rakibin bir sonraki turdaki Komut Puanı 3 yerine 2'ye düşer!\n• Kritik Şart: Delta Kruvazörü batarsa bu EMP yeteneği devre dışı kalır.",
            placementTip = "Delta'nın üçgen gövdesi 6 farklı yöne dönebilir (Kuzey, Güney, KD, KB, GD, GB). Harita ortasındaki resiflerin girintilerine yerleştirerek düşmanın doğrusal (çizgisel) tahminlerini boşa çıkarın. EMP yeteneğini korumak için erken safhada batırılmayacak korunaklı sulara konuşlandırın.",
            accentColor = Color(0xFF06B6D4),
            imageRes = R.drawable.ship_delta,
            cellCount = 3,
            isTriangle = true
        )
        lower.contains("carrier") || lower.contains("iha") || lower.contains("i̇ha") -> DeployableObjectInfo(
            id = "carrier",
            title = "İHA Gemisi (Taşıyıcı)",
            subtitle = "3 Peteklik Filo İstihbarat & Radar Üssü",
            badge = "KRİTİK HEDEF: İHA RADAR MERKEZİ",
            sizeText = "3 Petek (Doğrusal)",
            roleText = "Hava İstihbarat & Radar Keşfi",
            abilityText = "7 Peteklik Sektörel İHA Keşfi (3 CP)",
            description = "Filonun tüm hava keşif ve istihbarat sistemlerini üzerinde taşıyan 3 peteklik yüksek stratejik öneme sahip filo üssüdür.",
            tacticalImpact = "• Taktik Radar İstasyonu: 7 peteklik geniş İHA tarama yeteneği doğrudan bu gemiye bağlıdır.\n• KRİTİK RİSK (High-Value Target): Eğer İHA Taşıyıcınız batırılırsa radar istasyonu imha edilir ve İHA keşif kabiliyeti maç boyunca tamamen kilitlenir (kullanılamaz)!\n• Stratejik Savunma: Filonun gözü kulağı olduğu için düşmanın birincil hedefidir.",
            placementTip = "Filonun en çok korunması gereken gemisidir! Üzerine mutlaka 'Radyoaktif Sis' yerleştirerek İHA'lara karşı kamufle edin veya yanına Aegis Kalkanı açabileceğiniz güvenli bir arka hat bölgesine yerleştirin.",
            accentColor = BoardColors.NavyAccent,
            imageRes = R.drawable.ship_carrier,
            cellCount = 3
        )
        lower.contains("flagship") || lower.contains("amiral") -> DeployableObjectInfo(
            id = "flagship",
            title = "Amiral Gemisi",
            subtitle = "4 Peteklik Donanma Sancak Zırhlısı",
            badge = "AKTİF: AĞIR BOMBARDIMAN (LINE BARRAGE)",
            sizeText = "4 Petek (Doğrusal)",
            roleText = "Ağır Bombardıman & Donanma Omurgası",
            abilityText = "5 Peteklik Ardışık Hat Süpürme (3 CP)",
            description = "Filodaki en uzun ve en dayanıklı ana komuta zırhlısıdır. Yüksek kalibreli toplarıyla tüm savaş alanına hükmeder.",
            tacticalImpact = "• Aktif Yetenek — Ağır Bombardıman (Line Barrage - 3 CP): Altıgen eksen boyunca ardışık 5 peteği aynı anda havadan bombalar.\n• Resifleri Aşar: Havadan kavisli atış yapıldığı için mercan resifleri mermileri durduramaz!\n• Kritik Şart: Amiral Gemisi batırılırsa 5'li bombardıman yeteneği kaybedilir.",
            placementTip = "4 peteklik uzunluğu nedeniyle düşmanın rastgele isabet bulma olasılığı yüksektir. Haritanın kenar hatlarına veya köşegenlerine yerleştirin. İki ucu resif arkasına denk gelecek şekilde konuşlandırmak düşmanın hat takibini zorlaştırır.",
            accentColor = BoardColors.CrimsonRed,
            imageRes = R.drawable.ship_flagship,
            cellCount = 4
        )
        lower.contains("radioactive") || lower.contains("radyoaktif") -> DeployableObjectInfo(
            id = "radioactive",
            title = "Radyoaktif Sis Bölgesi",
            subtitle = "2 Peteklik Elektronik & Radar Kamuflaj Perdesi",
            badge = "PASİF: İHA SİNYAL ENGELLEYİCİ (JAMMER)",
            sizeText = "2 Petek (Yönlendirilebilir)",
            roleText = "Taktiksel Alan Kamuflajı",
            abilityText = "Mutlak İHA Radar Görünmezliği",
            description = "Oyun başında konuşlandırılan 2 peteklik yüksek yoğunluklu kimyasal/radyoaktif yeşil sis perdesidir. Boş sulara veya gemilerinizin tam üzerine yerleştirilebilir.",
            tacticalImpact = "• İHA Sinyal Engeli: Bu sisin altındaki gemiler düşmanın 7 peteklik İHA keşif taramasında ASLA görünmez (radar o petekleri tamamen boş raporlar).\n• Top Atışı Farkı: Sis bölgesi doğrudan top atışlarını durdurmaz; sadece radar keşfini engeller.\n• Sınırlar: Mercan resiflerine veya harita sınırları dışına taşamaz.",
            placementTip = "En kritik gemilerinizin (özellikle batarsa İHA'nızı kilitleyen İHA Taşıyıcısının veya Amiral Geminizin) üzerine konuşlandırın! Düşman o sektörü tarasa bile sinyal '0' çıkacağı için orayı boş zannedecek ve vuruş yapmayacaktır.",
            accentColor = Color(0xFF16A34A),
            icon = Icons.Default.Sensors,
            imageRes = R.drawable.hazard_radioactive,
            cellCount = 2
        )
        else -> DeployableObjectInfo(
            id = "corvette",
            title = "Hayalet Korvet",
            subtitle = "2 Peteklik Çevik Stealth Hücumbot",
            badge = "PASİF: HAYALET GÖVDE (%50 STEALTH)",
            sizeText = "2 Petek (Doğrusal)",
            roleText = "Gizli Keşif & Çevik Hücumbot",
            abilityText = "%50 İHA Radar Sinyali Soğurma",
            description = "2 peteklik yüksek manevra kabiliyetine sahip çevik hücumbottur. Harita kenarlarına ve resif aralarına gizlenmesi en kolay stealth gemidir.",
            tacticalImpact = "• Pasif Yetenek — Hayalet Gövde (Stealth Hull): Düşmanın 7 peteklik İHA radar taraması Korvet'in üzerinden geçse dahi radar %50 olasılıkla sinyali yakalayamaz (tamamen gizlenir).\n• Gerçek İhtimal: Bu %50 gizlilik şansı tüm oyun modlarında (Yapay Zeka ve Çok Oyunculu) aktiftir.\n• Dayanıklılık: 2 isabette batar; küçük gövdesiyle düşmanın atışlarını boşa harcatmak için idealdir.",
            placementTip = "Resiflerin kuytularına veya haritanın en dış çeperlerine yerleştirin. İHA taramasını %50 şansla atlattığı için düşmanı yanıltmak adına tek başına bağımsız bir bölgeye konuşlandırmak harika bir taktiktir.",
            accentColor = BoardColors.PurplePrimary,
            imageRes = R.drawable.ship_corvette,
            cellCount = 2
        )
    }
}

fun getDeployableObjectAccent(nameOrId: String): Color {
    val lower = nameOrId.lowercase()
    return when {
        lower.contains("delta") -> Color(0xFF06B6D4)
        lower.contains("carrier") || lower.contains("iha") || lower.contains("i̇ha") -> BoardColors.NavyAccent
        lower.contains("flagship") || lower.contains("amiral") -> BoardColors.CrimsonRed
        lower.contains("radioactive") || lower.contains("radyoaktif") -> Color(0xFF16A34A)
        else -> BoardColors.PurplePrimary
    }
}

/**
 * Tersane ve konuşlandırma ekranındaki gemi/nesne kartlarında yer alan profesyonel estetik bilgi butonu.
 */
@Composable
fun DeployInfoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = BoardColors.NavyAccent,
    contentDescription: String = "Nesne Bilgisi"
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
        modifier = modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = contentDescription,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Gemi ve Taktik Nesneler için tüm ekran türlerine uyumlu, estetik ve profesyonel detay pop-up penceresi.
 */
@Composable
fun DeployableObjectInfoDialog(
    info: DeployableObjectInfo,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    val dialogWidth = when {
        screenWidthDp >= 840 -> 520.dp
        screenWidthDp >= 600 -> 480.dp
        else -> (screenWidthDp * 0.92f).dp
    }
    val maxContentHeight = (screenHeightDp * 0.85f).dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BoardColors.PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, info.accentColor.copy(alpha = 0.65f)),
            shadowElevation = 14.dp,
            modifier = Modifier
                .width(dialogWidth)
                .heightIn(max = maxContentHeight)
                .padding(horizontal = 6.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 1. ÜST BAŞLIK SATIRI (Thumbnail + Başlık/Alt Başlık + Kapat Butonu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Görsel Önizleme Kutusu
                        Box(
                            modifier = Modifier
                                .size(width = 54.dp, height = 34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(info.accentColor.copy(alpha = 0.12f))
                                .border(1.2.dp, info.accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (info.imageRes != null) {
                                Image(
                                    painter = painterResource(info.imageRes),
                                    contentDescription = info.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (info.icon != null) {
                                Icon(
                                    imageVector = info.icon,
                                    contentDescription = info.title,
                                    tint = info.accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = info.title,
                                color = BoardColors.NavyDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Text(
                                text = info.subtitle,
                                color = BoardColors.TextSecondary,
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(30.dp)
                            .background(BoardColors.SurfaceSubtle, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = BoardColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. TAKTİKSEL ROZET VE ÖZELLİK ETİKETLERİ (Spec Chips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = info.accentColor.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, info.accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "● ${info.badge}",
                            color = info.accentColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = BoardColors.SurfaceSubtle,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, BoardColors.BorderSubtle)
                    ) {
                        Text(
                            text = "📏 ${info.sizeText}",
                            color = BoardColors.NavyDark,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. KAYDIRILABİLİR İÇERİK KARTLARI (Tüm ekran boyutlarında sorunsuz kayar ve okunur)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // A. Genel Tanım ve Gövde Mekaniği
                    Surface(
                        color = BoardColors.SurfaceSubtle,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BoardColors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("ℹ️", fontSize = 11.sp)
                                Text(
                                    text = "GENEL TANIM VE GÖVDE MEKANİĞİ",
                                    color = BoardColors.NavyDark,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = info.description,
                                color = BoardColors.TextPrimary,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // B. Taktiksel Yetenek ve Muharebe Etkisi
                    Surface(
                        color = info.accentColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, info.accentColor.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("⚔️", fontSize = 11.sp)
                                Text(
                                    text = "TAKTİKSEL YETENEK & MUHAREBE ETKİSİ",
                                    color = info.accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = info.tacticalImpact,
                                color = BoardColors.NavyDark,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // C. Konuşlandırma ve Tersane Stratejisi İpucu
                    Surface(
                        color = Color(0xFFFEF3C7).copy(alpha = 0.60f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("💡", fontSize = 11.sp)
                                Text(
                                    text = "KONUŞLANDIRMA & GİZLENME İPUCU",
                                    color = Color(0xFFB45309),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = info.placementTip,
                                color = Color(0xFF92400E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. ALT ONAY / KAPATMA BUTONU
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
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
