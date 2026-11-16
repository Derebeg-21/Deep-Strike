package com.hexafleet.domain.game.campaign

import com.hexafleet.domain.game.model.ShipClass
import kotlinx.serialization.Serializable

/**
 * Sefer haritasındaki düğüm (Node) türleri
 */
@Serializable
enum class CampaignNodeType {
    BATTLE_PATROL,   // Standart keşif filosu çatışması
    BATTLE_ELITE,    // Elit düşman: Gelişmiş kruvazörler ve hayalet denizaltılar
    SUPPLY_DEPOT,    // İkmal istasyonu: Filoyu onarma ve komut puanı yükseltmesi
    ANOMALY_SIGNAL,  // Siber anomali: Risk/Ödül içeren rastgele taktiksel olay
    BOSS_LEVIATHAN   // Final Boss: 2x4 dev yüzen amiral kalesi (Leviathan Fortress)
}

/**
 * Haritadaki tek bir düğüm
 */
@Serializable
data class CampaignNode(
    val id: String,
    val stage: Int, // 1..5
    val indexInStage: Int,
    val type: CampaignNodeType,
    val title: String,
    val connectedNextNodeIds: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val isAvailable: Boolean = false
)

/**
 * Oyuncunun sefer ilerleme durumu
 */
@Serializable
data class CampaignProgress(
    val stage: Int = 1,
    val currentNodeId: String = "node_1_0",
    val nodes: Map<String, CampaignNode> = emptyMap(),
    val credits: Int = 150,
    val unlockedShips: Set<ShipClass> = setOf(
        ShipClass.CORVETTE_MINELAYER,
        ShipClass.AEGIS_DESTROYER,
        ShipClass.DRONE_CARRIER
    ),
    val activeFleet: List<ShipClass> = listOf(
        ShipClass.DRONE_CARRIER,
        ShipClass.AEGIS_DESTROYER
    )
)

/**
 * 5 Kademeli Prosedürel Sefer Haritası Üreticisi
 */
object CampaignMapGenerator {

    fun generateNewCampaign(): CampaignProgress {
        val nodesMap = mutableMapOf<String, CampaignNode>()

        // 1. Kademe: Başlangıç karakolu (2 düğüm)
        val stage1 = listOf(
            CampaignNode("node_1_0", 1, 0, CampaignNodeType.BATTLE_PATROL, "Alfa Boğazı Keşfi", listOf("node_2_0", "node_2_1"), isAvailable = true),
            CampaignNode("node_1_1", 1, 1, CampaignNodeType.ANOMALY_SIGNAL, "Kayıp Sinyal Şamandırası", listOf("node_2_1", "node_2_2"), isAvailable = true)
        )

        // 2. Kademe: Resif Koridoru (3 düğüm)
        val stage2 = listOf(
            CampaignNode("node_2_0", 2, 0, CampaignNodeType.BATTLE_PATROL, "Mercan Hattı Çatışması", listOf("node_3_0")),
            CampaignNode("node_2_1", 2, 1, CampaignNodeType.SUPPLY_DEPOT, "İkmal Platformu 'Nautilus'", listOf("node_3_0", "node_3_1")),
            CampaignNode("node_2_2", 2, 2, CampaignNodeType.BATTLE_ELITE, "Gölge Filo Tuzağı", listOf("node_3_1"))
        )

        // 3. Kademe: Manyetik Fırtına Bölgesi (2 düğüm)
        val stage3 = listOf(
            CampaignNode("node_3_0", 3, 0, CampaignNodeType.ANOMALY_SIGNAL, "İyon Girdabı", listOf("node_4_0")),
            CampaignNode("node_3_1", 3, 1, CampaignNodeType.BATTLE_ELITE, "EMP Kruvazör Avı", listOf("node_4_0"))
        )

        // 4. Kademe: Derin Hendek İkmal Noktası (1 düğüm)
        val stage4 = listOf(
            CampaignNode("node_4_0", 4, 0, CampaignNodeType.SUPPLY_DEPOT, "Son İkmal Üssü: 'Aegis Core'", listOf("node_5_0"))
        )

        // 5. Kademe: Final Boss Karşılaşması
        val stage5 = listOf(
            CampaignNode("node_5_0", 5, 0, CampaignNodeType.BOSS_LEVIATHAN, "LEVIATHAN AMİRAL KALESİ", emptyList())
        )

        val allStages = stage1 + stage2 + stage3 + stage4 + stage5
        for (node in allStages) {
            nodesMap[node.id] = node
        }

        return CampaignProgress(
            stage = 1,
            currentNodeId = "node_1_0",
            nodes = nodesMap
        )
    }
}

