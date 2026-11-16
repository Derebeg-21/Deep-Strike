package com.hexafleet.domain.game.ai

enum class AiDifficulty {
    RECRUIT,       // Acemi: Rastgele atışlar + basit hedef takibi
    VETERAN,       // Kıdemli: 3 eksenli altıgen olasılık yoğunluk haritası (H-PDF)
    GRAND_ADMIRAL  // Büyük Amiral: H-PDF + aktif yetenek (İHA/EMP) kullanımı ve taktiksel kaçış
}

