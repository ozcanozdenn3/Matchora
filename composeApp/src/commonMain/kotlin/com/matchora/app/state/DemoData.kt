package com.matchora.app.state

import androidx.compose.ui.graphics.Color
import com.matchora.app.domain.EventStatus
import com.matchora.app.domain.Sport
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// DEMO VERİSİ — Kadıköy pilot şehir senaryosu (belge §26: 1 şehir + 2–3 spor +
// mikro topluluklar). Backend bağlandığında repository bu kaynağın yerini alır.
// ═══════════════════════════════════════════════════════════════════════════════
object DemoData {

    const val ME = "me"

    val me = UserProfile(
        name = "Özcan Özden",
        city = "İstanbul",
        area = "Kadıköy",
        bio = "Hafta içi akşam halı saha, hafta sonu tenis. Fair play önce gelir.",
        birthYear = 1996,
        phone = "+90 5•• ••• 42 18",
        email = "ozcan@matchora.app",
        skills = listOf(
            SportSkill(Sport.FOOTBALL, SkillLevel.ADVANCED, 1482, 38, 61, 12, "Orta saha"),
            SportSkill(Sport.TENNIS, SkillLevel.INTERMEDIATE, 1236, 17, 54, 8, "Sağ el · Baseline"),
            SportSkill(Sport.PADEL, SkillLevel.BEGINNER, 1060, 4, 50, null, "Sol taraf"),
            SportSkill(Sport.BASKETBALL, SkillLevel.INTERMEDIATE, 1340, 22, 58, 15, "Şutör Guard"),
            SportSkill(Sport.SWIMMING, SkillLevel.ADVANCED, 1410, 19, 68, 6, "Serbest & Kelebek"),
            SportSkill(Sport.TABLE_TENNIS, SkillLevel.INTERMEDIATE, 1190, 11, 52, 9, "Ofansif Forehand"),
            SportSkill(Sport.VOLLEYBALL, SkillLevel.INTERMEDIATE, 1220, 14, 55, 11, "Smaçör"),
            SportSkill(Sport.FITNESS, SkillLevel.ADVANCED, 1380, 45, 75, null, "Fonksiyonel & HIIT"),
            SportSkill(Sport.ESPORTS_FC, SkillLevel.ADVANCED, 1580, 24, 65, 5, "FUT Champions · PS5"),
            SportSkill(Sport.CHESS, SkillLevel.INTERMEDIATE, 1340, 31, 58, 8, "Lichess 1800 ELO")
        ),
        verifications = setOf(VerificationLevel.BASIC, VerificationLevel.PHOTO),
        reliability = 94,
        fairPlay = 4.8f,
        verifiedMatches = 59,
        streakWeeks = 8,
        campus = "İTÜ",
        company = "Acıbadem",
        dominantSide = "Sağ ayak / Sağ el",
        availability = listOf("Hafta içi 19:00–23:00", "Cumartesi 09:00–13:00")
    )

    private val c1 = Color(0xFF7457F5)
    private val c2 = Color(0xFF0FA968)
    private val c3 = Color(0xFF2A7FF0)
    private val c4 = Color(0xFFE88A00)
    private val c5 = Color(0xFFE5484D)
    private val c6 = Color(0xFF0B1220)
    private val c7 = Color(0xFF3A9FA8)

    val players = listOf(
        Player("p1", "Mert Kaya", "Moda", listOf(Sport.FOOTBALL), SkillLevel.ADVANCED, 97, 64, 1530, 94, true, "Defans", 0.8f, 11, true, c1),
        Player("p2", "Can Demir", "Fenerbahçe", listOf(Sport.FOOTBALL, Sport.PADEL), SkillLevel.ADVANCED, 92, 41, 1495, 91, true, "Kaleci", 1.4f, 9, true, c2),
        Player("p3", "Burak Şen", "Caddebostan", listOf(Sport.FOOTBALL, Sport.BASKETBALL), SkillLevel.INTERMEDIATE, 88, 29, 1390, 87, true, "Orta saha", 2.1f, 6, false, c3),
        Player("p4", "Elif Yıldız", "Kalamış", listOf(Sport.TENNIS, Sport.PADEL), SkillLevel.INTERMEDIATE, 96, 33, 1262, 92, true, "Sağ el", 1.1f, 4, true, c5),
        Player("p5", "Deniz Aksoy", "Göztepe", listOf(Sport.TENNIS), SkillLevel.ADVANCED, 90, 52, 1408, null, true, "Sol el", 3.2f, 0, true, c4),
        Player("p6", "Selin Arda", "Moda", listOf(Sport.RUNNING, Sport.VOLLEYBALL), SkillLevel.INTERMEDIATE, 99, 71, 1310, 78, true, null, 0.6f, 2, false, c7),
        Player("p7", "Kerem Polat", "Acıbadem", listOf(Sport.BASKETBALL), SkillLevel.ADVANCED, 84, 22, 1455, null, false, "Guard", 2.8f, 0, true, c6),
        Player("p8", "Zeynep Er", "Suadiye", listOf(Sport.PADEL, Sport.TENNIS), SkillLevel.ADVANCED, 95, 48, 1520, 89, true, "Sağ taraf", 4.0f, 3, false, c1),
        Player("p9", "Arda Koç", "Kozyatağı", listOf(Sport.FOOTBALL), SkillLevel.INTERMEDIATE, 81, 18, 1288, null, true, "Forvet", 3.6f, 0, true, c3),
        Player("p10", "Ece Tan", "Bostancı", listOf(Sport.VOLLEYBALL, Sport.RUNNING), SkillLevel.BEGINNER, 93, 12, 1120, null, true, "Pasör", 4.4f, 0, true, c2),
        Player("p11", "Onur Ak", "Moda", listOf(Sport.FOOTBALL, Sport.BASKETBALL), SkillLevel.ADVANCED, 91, 57, 1561, 90, true, "Kanat", 0.9f, 8, false, c4),
        Player("p12", "Hakan Tur", "Feneryolu", listOf(Sport.FOOTBALL), SkillLevel.INTERMEDIATE, 86, 25, 1350, 84, true, "Defans", 1.7f, 5, true, c5)
    )

    val mePlayer = Player(
        id = ME,
        name = me.name,
        area = me.area,
        sports = me.skills.map { it.sport },
        level = SkillLevel.ADVANCED,
        reliability = me.reliability,
        verifiedMatches = me.verifiedMatches,
        rating = me.skills.firstOrNull()?.rating ?: 1500,
        chemistry = 96,
        verified = true,
        position = "Orta saha",
        distanceKm = 0.5f,
        playedTogether = 0,
        availableNow = true,
        color = MColors.Brand
    )

    fun player(id: String): Player? = if (id == ME) mePlayer else players.firstOrNull { it.id == id }

    val events = listOf(
        SportEvent(
            id = "e1", title = "Moda Akşam Halı Saha", sport = Sport.FOOTBALL, venue = "Moda Spor Parkı · Saha 2",
            area = "Moda", day = "Bugün", time = "20:30", startsInMin = 46, durationMin = 60, distanceKm = 0.9f,
            capacity = 10, participantIds = listOf("p1", "p2", "p3", "p11", "p12", "p9", "p6", "p7"), waitlistCount = 0,
            mode = PlayMode.COMPETITIVE, levelRange = "Orta – İleri", minReliability = 80, verifiedOnly = true,
            pricePerPersonTry = 180, hostId = "p1", status = EventStatus.FILLING,
            score = ScoreBreakdown(98, 96, 92, 95, 88, listOf("Müsaitlik saatinle birebir", "900 m uzaklıkta", "Seviye aralığın içinde", "Kadronun ortalama Reliability'si 91", "Rekabetçi tercihine uygun")),
            neededRoles = listOf("Kaleci", "Orta saha"), emergency = true,
            description = "5v5 rekabetçi maç. Kaleci rotasyonlu. Sonuç iki kaptan onayıyla rating'e işlenir."
        ),
        SportEvent(
            id = "e2", title = "Kalamış Kort · Tekler", sport = Sport.TENNIS, venue = "Kalamış Tenis Kulübü · Kort 3",
            area = "Kalamış", day = "Bugün", time = "18:00", startsInMin = 85, durationMin = 90, distanceKm = 1.2f,
            capacity = 2, participantIds = listOf("p4"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "Orta", minReliability = 70, verifiedOnly = false,
            pricePerPersonTry = 250, hostId = "p4", status = EventStatus.FILLING,
            score = ScoreBreakdown(90, 94, 96, 98, 90, listOf("Seviyeniz neredeyse aynı (1236 vs 1262)", "1,2 km", "Elif'in katılım oranı %96", "Sosyal maç tercihi")),
            description = "Rahat tempoda tekler. Toplar benden, kort ücreti bölüşülür."
        ),
        SportEvent(
            id = "e3", title = "Caddebostan 3v3 Açık Pota", sport = Sport.BASKETBALL, venue = "Caddebostan Sahil Potaları",
            area = "Caddebostan", day = "Bugün", time = "19:15", startsInMin = 120, durationMin = 75, distanceKm = 2.4f,
            capacity = 6, participantIds = listOf("p7", "p3", "p11", "p9"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "Tüm seviyeler", minReliability = 60, verifiedOnly = false,
            pricePerPersonTry = 0, hostId = "p7", status = EventStatus.FILLING,
            score = ScoreBreakdown(80, 78, 74, 85, 82, listOf("Akşam müsaitliğin uygun", "2,4 km", "Ücretsiz açık saha")),
            description = "Sahil potalarında kazanan kalır formatı. Ücretsiz."
        ),
        SportEvent(
            id = "e4", title = "Sahil Gün Batımı Koşusu · 8K", sport = Sport.RUNNING, venue = "Moda İskele Meydanı",
            area = "Moda", day = "Yarın", time = "18:30", startsInMin = 1560, durationMin = 60, distanceKm = 0.6f,
            capacity = 20, participantIds = listOf("p6", "p10", "p1", "p12", "p4", "p2", "p3", "p8", "p9", "p11", "p5", "p7"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "5:30–6:15 /km", minReliability = 0, verifiedOnly = false,
            pricePerPersonTry = 0, hostId = "p6", status = EventStatus.FILLING,
            score = ScoreBreakdown(76, 99, 80, 90, 70, listOf("600 m uzaklıkta", "Selin ile 2 kez koştunuz")),
            description = "Sahil boyunca 8K, iki tempo grubu. Bitişte kahve."
        ),
        SportEvent(
            id = "e5", title = "Suadiye Padel · Çiftler", sport = Sport.PADEL, venue = "Suadiye Padel Club · Kort 1",
            area = "Suadiye", day = "Cumartesi", time = "10:00", startsInMin = 2580, durationMin = 90, distanceKm = 4.1f,
            capacity = 4, participantIds = listOf("p8", "p4", "p2", ME), waitlistCount = 2,
            mode = PlayMode.COMPETITIVE, levelRange = "Orta – İleri", minReliability = 85, verifiedOnly = true,
            pricePerPersonTry = 320, hostId = "p8", status = EventStatus.FULL,
            score = ScoreBreakdown(88, 70, 72, 96, 92, listOf("Cumartesi sabahı müsaitsin", "Zeynep ile kimya %89")),
            description = "Rekabetçi çiftler. Doğrulanmış sonuç padel rating'ine işlenir."
        ),
        SportEvent(
            id = "e6", title = "Fenerbahçe Parkı Voleybol", sport = Sport.VOLLEYBALL, venue = "Fenerbahçe Parkı Kumsal",
            area = "Fenerbahçe", day = "Pazar", time = "16:00", startsInMin = 4300, durationMin = 120, distanceKm = 2.9f,
            capacity = 12, participantIds = listOf("p10", "p6", "p12", "p3", "p9"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "Başlangıç – Orta", minReliability = 60, verifiedOnly = false,
            pricePerPersonTry = 0, hostId = "p10", status = EventStatus.FILLING,
            score = ScoreBreakdown(70, 80, 66, 92, 74, listOf("Pazar öğleden sonra boşsun", "Ücretsiz")),
            description = "6v6 plaj voleybolu, file ve top hazır."
        ),
        SportEvent(
            id = "e7", title = "Kadıköy Utd – Ataşehir FC", sport = Sport.FOOTBALL, venue = "Fikirtepe Arena · Saha 1",
            area = "Fikirtepe", day = "Perşembe", time = "21:00", startsInMin = 7300, durationMin = 60, distanceKm = 2.2f,
            capacity = 14, participantIds = listOf(ME, "p1", "p2", "p3", "p11", "p12", "p9"), waitlistCount = 0,
            mode = PlayMode.COMPETITIVE, levelRange = "Takım maçı", minReliability = 85, verifiedOnly = true,
            pricePerPersonTry = 200, hostId = ME, status = EventStatus.PUBLISHED,
            score = ScoreBreakdown(95, 88, 90, 94, 96, listOf("Takım challenge'ı kabul edildi")),
            description = "Team Challenge maçı. Kadrolar kilitlendi, takım rating'i güncellenecek.", isTeamEvent = true
        ),
        SportEvent(
            id = "e8", title = "Kalamış Marina · Serbest Stil Yüzme", sport = Sport.SWIMMING, venue = "Kalamış Su Sporları Kulübü",
            area = "Kalamış", day = "Yarın", time = "08:00", startsInMin = 720, durationMin = 60, distanceKm = 1.1f,
            capacity = 8, participantIds = listOf("p4", "p6", "p2"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "Orta – İleri", minReliability = 80, verifiedOnly = true,
            pricePerPersonTry = 160, hostId = "p4", status = EventStatus.FILLING,
            score = ScoreBreakdown(85, 92, 88, 90, 85, listOf("Sabah kulvar antrenmanı", "Temiz tesis")),
            description = "Olimpik havuzda antrenör eşliğinde serbest ve karışık kulvar yüzüşü."
        ),
        SportEvent(
            id = "e9", title = "Moda Gençlik Masa Tenisi", sport = Sport.TABLE_TENNIS, venue = "Moda Gençlik Kulübü · Masa 1-2",
            area = "Moda", day = "Bugün", time = "19:00", startsInMin = 90, durationMin = 90, distanceKm = 0.7f,
            capacity = 6, participantIds = listOf("p3", "p9", "p11"), waitlistCount = 0,
            mode = PlayMode.COMPETITIVE, levelRange = "Tüm Seviyeler", minReliability = 75, verifiedOnly = false,
            pricePerPersonTry = 80, hostId = "p3", status = EventStatus.FILLING,
            score = ScoreBreakdown(90, 96, 85, 88, 80, listOf("Yürüme mesafesinde", "ITTF standart masa")),
            description = "Dönerli tekler ve çiftler maçları. Raket ve kaliteli toplar salonda mevcut."
        ),
        SportEvent(
            id = "e10", title = "Caddebostan Sunset Bootcamp", sport = Sport.FITNESS, venue = "Caddebostan Sahil Çimleri",
            area = "Caddebostan", day = "Yarın", time = "18:45", startsInMin = 1400, durationMin = 60, distanceKm = 2.1f,
            capacity = 16, participantIds = listOf("p6", "p10", "p7", "p12", "p1"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "Her Seviye", minReliability = 70, verifiedOnly = false,
            pricePerPersonTry = 120, hostId = "p6", status = EventStatus.FILLING,
            score = ScoreBreakdown(82, 85, 90, 92, 88, listOf("Açık hava fonksiyonel fitness", "Grup motivasyonu")),
            description = "Kettlebell, direnç lastiği ve kardiyo kombiniyle gün batımı egzersizi."
        ),
        SportEvent(
            id = "e11", title = "Bostancı – Kartal Sahil Bisiklet", sport = Sport.CYCLING, venue = "Bostancı Vapur İskelesi Önü",
            area = "Bostancı", day = "Pazar", time = "08:30", startsInMin = 4100, durationMin = 120, distanceKm = 3.8f,
            capacity = 20, participantIds = listOf("p1", "p4", "p8", "p11", "p2", "p9"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "24–28 km/s", minReliability = 70, verifiedOnly = false,
            pricePerPersonTry = 0, hostId = "p1", status = EventStatus.FILLING,
            score = ScoreBreakdown(75, 78, 82, 90, 80, listOf("Geniş bisiklet yolu rotası", "Ücretsiz katılım")),
            description = "Kask zorunludur. Kartal sahiline kadar gidip dönüşte kahve molası."
        ),
        SportEvent(
            id = "e12", title = "FC 25 Kadıköy E-Kupa Elemesi", sport = Sport.ESPORTS_FC, venue = "Online · PS5 / Crossplay Lobi",
            area = "Kadıköy Online", day = "Bugün", time = "21:30", startsInMin = 110, durationMin = 45, distanceKm = 0.1f,
            capacity = 2, participantIds = listOf("p1"), waitlistCount = 0,
            mode = PlayMode.COMPETITIVE, levelRange = "Div 1-3", minReliability = 85, verifiedOnly = true,
            pricePerPersonTry = 50, hostId = "p1", status = EventStatus.FILLING,
            score = ScoreBreakdown(95, 90, 88, 92, 94, listOf("Düşük ping lobi", "PSN & EA ID doğrulanmış")),
            description = "FC 25 1v1 rekabetçi eleme maçı. Maç sonucu ekran görüntüsü ile sisteme onaylatılır."
        ),
        SportEvent(
            id = "e13", title = "Valorant 5v5 Topluluk Scrim", sport = Sport.ESPORTS_VALORANT, venue = "Online · Riot TR & Discord",
            area = "Online", day = "Yarın", time = "20:00", startsInMin = 1380, durationMin = 60, distanceKm = 0.1f,
            capacity = 10, participantIds = listOf("p2", "p3", "p7", "p9", "p11", "p12", "p4", "p5"), waitlistCount = 0,
            mode = PlayMode.COMPETITIVE, levelRange = "Plat – Ascendant", minReliability = 80, verifiedOnly = true,
            pricePerPersonTry = 40, hostId = "p2", status = EventStatus.FILLING,
            score = ScoreBreakdown(92, 94, 91, 89, 90, listOf("Özel sunucu", "Discord ses odası hazır")),
            description = "Ascent & Haven haritalarında 5v5 scrim. Saygılı ve taktiksel oyun esastır."
        ),
        SportEvent(
            id = "e14", title = "Moda İskelesi Hızlı Satranç", sport = Sport.CHESS, venue = "Moda Tarihi İskele Bahçesi",
            area = "Moda", day = "Cumartesi", time = "15:00", startsInMin = 2800, durationMin = 90, distanceKm = 0.8f,
            capacity = 8, participantIds = listOf("p6", "p8", "p10"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "1400–1900 ELO", minReliability = 75, verifiedOnly = false,
            pricePerPersonTry = 30, hostId = "p6", status = EventStatus.FILLING,
            score = ScoreBreakdown(88, 91, 95, 87, 85, listOf("Açık hava deniz manzarası", "Ahşap tahtalar")),
            description = "10+5 zaman temposunda dönerli maçlar. Kahve eşliğinde keyifli zeka mücadeleleri."
        ),
        SportEvent(
            id = "e15", title = "İTÜ Maslak Akşam Voleybolu", sport = Sport.VOLLEYBALL,
            venue = "İTÜ Spor Salonu · Saha 2", area = "Maslak", day = "Bugün", time = "19:30",
            startsInMin = 210, durationMin = 90, distanceKm = 2.6f, capacity = 12,
            participantIds = listOf("p6", "p10", "p4", "p8", "p12", "p3", "p11", "p9"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "Başlangıç – Orta", minReliability = 65, verifiedOnly = false,
            pricePerPersonTry = 0, hostId = "p10", status = EventStatus.FILLING,
            score = ScoreBreakdown(88, 82, 86, 90, 94, listOf("Kampüs ağı", "Bu akşam müsait", "2,6 km")),
            description = "Kampüs topluluğuyla rahat tempoda 6v6 voleybol.",
            networkScope = EventNetworkScope.CAMPUS,
            networkName = "İTÜ"
        ),
        SportEvent(
            id = "e16", title = "Acıbadem Kurumsal 5v5", sport = Sport.FOOTBALL,
            venue = "Acıbadem Spor Kompleksi · Saha 1", area = "Acıbadem", day = "Yarın", time = "20:00",
            startsInMin = 1560, durationMin = 60, distanceKm = 3.1f, capacity = 10,
            participantIds = listOf("p1", "p2", "p3", "p11", "p12", "p9"), waitlistCount = 0,
            mode = PlayMode.COMPETITIVE, levelRange = "Orta – İleri", minReliability = 80, verifiedOnly = true,
            pricePerPersonTry = 150, hostId = "p1", status = EventStatus.FILLING,
            score = ScoreBreakdown(86, 78, 90, 92, 88, listOf("Şirket ligi", "Kadroda 4 boş yer")),
            neededRoles = listOf("Kaleci", "Defans"), description = "Kurumsal wellness ligi için haftalık 5v5 karşılaşma.",
            networkScope = EventNetworkScope.COMPANY,
            networkName = "Acıbadem"
        ),
        SportEvent(
            id = "e17", title = "Ankara’da Sabah Koşusu · 6K", sport = Sport.RUNNING,
            venue = "Seğmenler Parkı · Ana giriş", area = "Çankaya", day = "Cumartesi", time = "08:00",
            startsInMin = 3120, durationMin = 55, distanceKm = null, capacity = 16,
            participantIds = listOf("p6", "p10", "p4", "p8", "p5"), waitlistCount = 0,
            mode = PlayMode.SOCIAL, levelRange = "5:45–6:30 /km", minReliability = 0, verifiedOnly = false,
            pricePerPersonTry = 0, hostId = "p6", status = EventStatus.FILLING,
            score = ScoreBreakdown(84, 0, 82, 90, 92, listOf("Seyahat modu", "Cumartesi sabahı ortak")),
            description = "Ankara ziyaretinde katılabileceğin, iki tempolu grup koşusu.",
            networkScope = EventNetworkScope.TRAVEL,
            networkName = "Ankara", city = "Ankara"
        ),
        SportEvent(
            id = "e18", title = "Kalamış Voleybol · Akşam Ligi", sport = Sport.VOLLEYBALL,
            venue = "Kalamış Kum Sahası · Kort 1", area = "Kalamış", day = "Pazar", time = "17:00",
            startsInMin = 5400, durationMin = 90, distanceKm = 1.4f, capacity = 6,
            participantIds = listOf("p4", "p6", "p8", "p10", "p12", "p3"), waitlistCount = 3,
            mode = PlayMode.COMPETITIVE, levelRange = "Orta – İleri", minReliability = 80, verifiedOnly = true,
            pricePerPersonTry = 120, hostId = "p4", status = EventStatus.FULL,
            score = ScoreBreakdown(86, 90, 88, 92, 85, listOf("Yakın konum", "Seviye aralığın içinde")),
            description = "Düzenli pazar ligi. Dolu kadro; boş yer açılırsa sıradaki oyuncuya haber verilir.",
            networkScope = EventNetworkScope.CITY
        )
    ).let { featured ->
        featured + Sport.entries.filter { sport -> featured.none { it.sport == sport } }.mapIndexed { index, sport ->
            SportEvent(
                id = "demo_${sport.name.lowercase()}", title = "${sport.label} · birlikte başla",
                sport = sport, venue = if (sport.isESport) "Matchora online lobi" else "${sport.label} buluşma noktası",
                area = if (sport.isESport) "Online" else "Kadıköy", day = "Bugün",
                time = "19:00", startsInMin = 120, durationMin = 60,
                distanceKm = if (sport.isESport) null else 1.5f,
                capacity = if (sport.isTeamSport) 12 else 6,
                participantIds = listOf("p1", "p4"), waitlistCount = 0,
                mode = PlayMode.SOCIAL, levelRange = "Tüm seviyeler", minReliability = 0,
                verifiedOnly = false, pricePerPersonTry = 0, hostId = "p1", status = EventStatus.FILLING,
                score = ScoreBreakdown(88, 90, 85, 94, 90, listOf("Her seviyeye açık", "Ücretsiz demo buluşması")),
                description = "${sport.label} için örnek buluşma. Katılım, sohbet ve check-in akışlarını deneyebilirsin. Bu etkinlik demo verisidir.",
                approximateLocation = if (sport.isESport) null else EventGeoPoint(40.982 + (index % 6) * 0.003, 29.025 + (index / 6) * 0.003)
            )
        }
    }

    val teams = listOf(
        TeamProfile("t1", "Kadıköy Utd", Sport.FOOTBALL, "Kadıköy", "KU", MColors.Brand, ME, "p1",
            listOf(ME, "p1", "p2", "p3", "p11", "p12", "p9"), listOf("p6"), 1640, 18, 6, 4, 28, 4.7f, 93, true, "Çar & Per 20:00–23:00", isMine = true,
            achievements = listOf("🏆 Kadıköy Ligi '25", "🥇 Sezon 3 Bronz Kupa", "⚔️ 12 maçlık seri", "🤝 Centilmen takım")),
        TeamProfile("t2", "Moda Tenis Çevresi", Sport.TENNIS, "Moda", "MT", MColors.Mint, ME, "p4",
            listOf(ME, "p4", "p5", "p8"), emptyList(), 1310, 9, 5, 0, 14, 4.9f, 95, false, "Hafta sonu sabah", isMine = true,
            achievements = listOf("🥇 Moda Açık Çiftler", "🎾 8 maçlık seri", "🤝 Fair Play ödülü")),
        TeamProfile("t3", "Ataşehir FC", Sport.FOOTBALL, "Ataşehir", "AF", MColors.Sky, "x1", null, listOf("x1", "x2", "x3", "x4", "x5", "x6", "x7"), emptyList(), 1612, 16, 7, 3, 26, 4.5f, 90, true, "Per 21:00",
            achievements = listOf("🏆 Ataşehir Bahar Kupası", "🥉 Şehir ligi üçüncüsü")),
        TeamProfile("t4", "Üsküdar Kartalları", Sport.FOOTBALL, "Üsküdar", "ÜK", c6, "x8", null, listOf("x8", "x9", "x10", "x11", "x12", "x13"), emptyList(), 1588, 14, 8, 2, 24, 4.6f, 88, true, "Cmt 18:00",
            achievements = listOf("🏆 Üsküdar Kış Ligi", "🤝 Fair Play ödülü")),
        TeamProfile("t5", "Bostancı Rovers", Sport.FOOTBALL, "Bostancı", "BR", c7, "x14", null, listOf("x14", "x15", "x16", "x17", "x18", "x19", "x20"), emptyList(), 1695, 21, 5, 3, 29, 4.8f, 96, true, "Çar 21:00",
            achievements = listOf("🏆 Anadolu Yakası Kupası", "🥇 Sonbahar Ligi", "⚔️ 10 maçlık seri")),
        TeamProfile("t6", "Maltepe Strikers", Sport.FOOTBALL, "Maltepe", "MS", c5, "x21", null, listOf("x21", "x22", "x23", "x24", "x25", "x26"), emptyList(), 1520, 11, 9, 1, 21, 4.3f, 84, true, "Paz 20:00",
            achievements = listOf("🥉 Maltepe Dostluk Kupası"))
    )

    val challenges = listOf(
        ChallengeOffer("c1", "t5", "t1", "Çarşamba 21:00", "Bostancı Halı Saha", ChallengeStatus.PENDING, incoming = true, fitScore = 93, stakeTokens = 50, note = "7v7 kadromuz tam, maç teklifimizi bekliyoruz!"),
        ChallengeOffer("c2", "t1", "t3", "Perşembe 21:00", "Fikirtepe Arena", ChallengeStatus.SCHEDULED, incoming = false, fitScore = 95, stakeTokens = 50, note = "Haftalık rövanş maçı."),
        ChallengeOffer("c3", "t1", "t4", "Cumartesi 18:00", "Üsküdar Spor Tesisi", ChallengeStatus.PENDING, incoming = false, fitScore = 88, stakeTokens = 20, note = "Dostluk müsabakası.")
    )

    val teamJoinRequests = listOf(
        TeamJoinRequest("tjr1", "t1", players[0], "Orta Saha / Defans", "Hafta içi maçlarınıza katılmak istiyorum, Kadıköy'de oturuyorum.", "10 dk önce"),
        TeamJoinRequest("tjr2", "t1", players[3], "Kaleci", "Eksik maçlarınızda kaleci olarak görev yapabilirim.", "2 saat önce")
    )

    fun leaderboard(sport: Sport, scope: String, region: String = ""): List<LeaderboardRow> {
        val base = when (sport) {
            Sport.TENNIS -> listOf("Zeynep Er" to 1588, "Deniz Aksoy" to 1520, "Elif Yıldız" to 1402, "Kaan Uslu" to 1380, "Mira Ateş" to 1344, "Tolga Ün" to 1298, "Ada Sarı" to 1271)
            else -> listOf("Onur Ak" to 1712, "Mert Kaya" to 1655, "Can Demir" to 1610, "Efe Genç" to 1588, "Hakan Tur" to 1561, "Burak Şen" to 1530, "Arda Koç" to 1512, "Kerem Polat" to 1499, "Sarp Ilgaz" to 1490, "Tuna Kılıç" to 1487, "Emir Bal" to 1485)
        }
        val shift = when (scope) { "İlçe" -> 0; "Şehir" -> 40; "Kampüs" -> -60; else -> -20 }
        val regionalSeed = region.fold(0) { seed, char -> (seed * 31 + char.code).and(0x7fffffff) }
        val regionalBase = if (region.isEmpty()) base else base.drop(regionalSeed % base.size) + base.take(regionalSeed % base.size)
        val rows = regionalBase.mapIndexed { i, (n, r) ->
            val regionalRating = if (region.isEmpty()) r else 1750 - i * 27 + regionalSeed % 35
            LeaderboardRow(i + 1, "lb${base.indexOfFirst { it.first == n }}", n, regionalRating + shift, listOf(12, -4, 8, 0, 15, -9, 6, 3, -2, 10, 1)[i % 11], 20 + (11 - i) * 3)
        }.toMutableList()
        val mySkill = me.skills.firstOrNull { it.sport == sport }
        val myRank = when (scope) { "Arkadaşlar" -> 4; "Kampüs" -> 3; else -> mySkill?.localRank ?: 15 }
        rows.add(LeaderboardRow(myRank, ME, me.name, mySkill?.rating ?: 1000, 18, mySkill?.verifiedMatches ?: 0, isMe = true))
        return rows.sortedBy { it.rank }
    }

    val tournaments = listOf(
        TournamentInfo(
            "tr1", "Kadıköy Sonbahar Kupası", Sport.FOOTBALL, "Tek eleme · 16 takım", 12, 16, "25 Eki – 15 Kas",
            "Moda Spor Kulübü", "Takım · Reliability 80+ · Doğrulanmış kaptan", "Kayıtlar açık",
            "Saha ücreti takım başı tesis checkout'unda ödenir",
            listOf(
                BracketMatch(1, "Kadıköy Utd", "Maltepe Strikers", 4, 2), BracketMatch(1, "Ataşehir FC", "Kartal Gençlik", 3, 3),
                BracketMatch(1, "Bostancı Rovers", "Pendik United", 5, 1), BracketMatch(1, "Üsküdar Kartalları", "Göztepe SK", 2, 1),
                BracketMatch(2, "Kadıköy Utd", "Ataşehir FC", null, null), BracketMatch(2, "Bostancı Rovers", "Üsküdar Kartalları", null, null),
                BracketMatch(3, "TBD", "TBD", null, null)
            ),
            emptyList(), isLeague = false
        ),
        TournamentInfo(
            "tr2", "Anadolu Yakası Tenis Ligi", Sport.TENNIS, "Lig · Round robin + play-off", 8, 8, "Sezon 3 · 6. hafta",
            "Kalamış Tenis Kulübü", "Bireysel · 1100+ rating", "Devam ediyor", "Kort ücreti maç başı paylaşılır",
            emptyList(),
            listOf(
                StandingRow("Zeynep Er", 6, 6, 0, 0, 22, 18), StandingRow("Deniz Aksoy", 6, 5, 0, 1, 15, 15),
                StandingRow("Özcan Özden", 6, 4, 0, 2, 8, 12), StandingRow("Elif Yıldız", 6, 3, 0, 3, 2, 9),
                StandingRow("Kaan Uslu", 6, 2, 0, 4, -6, 6), StandingRow("Mira Ateş", 6, 1, 0, 5, -18, 3)
            ),
            isLeague = true
        ),
        TournamentInfo(
            "tr3", "Kampüs 3v3 Basket Gecesi", Sport.BASKETBALL, "Grup → Eleme · 12 takım", 9, 12, "2 Kas",
            "Boğaziçi Spor Topluluğu", "Kampüs e-postası doğrulanmış", "Kayıtlar açık", "Ücretsiz",
            listOf(BracketMatch(1, "Hoopers", "Rim Runners", null, null), BracketMatch(1, "Swish", "Paint Kings", null, null)),
            emptyList(), isLeague = false
        ),
        TournamentInfo(
            "tr4", "FC 25 Kadıköy E-Kupa 🏆", Sport.ESPORTS_FC, "Tek eleme · 16 Oyuncu", 14, 16, "28 Eki – 05 Kas",
            "Online Discord Arena", "Crossplay (PS5 / PC / Xbox)", "Kayıtlar açık", "Ödül: 5.000 TKN & Kupa",
            listOf(BracketMatch(1, "Özcan Ö. (TR)", "Emre K. (TR)", null, null), BracketMatch(1, "Kaan U. (TR)", "Burak S. (TR)", null, null)),
            emptyList(), isLeague = false
        ),
        TournamentInfo(
            "tr5", "Valorant 5v5 Topluluk Ligi 🎯", Sport.ESPORTS_VALORANT, "Grup + Play-off · 8 Takım", 7, 8, "01 Kas – 20 Kas",
            "Riot TR Sunucusu", "Doğrulanmış Riot ID & Takım", "Son 1 Takım", "Ödül: 10.000 TKN Havuzu",
            emptyList(),
            listOf(
                StandingRow("Kadıköy Aimers", 4, 4, 0, 0, 52, 12),
                StandingRow("Moda Esports", 4, 3, 0, 1, 38, 9),
                StandingRow("Bostancı Vanguards", 4, 2, 0, 2, 12, 6),
                StandingRow("Fikirtepe Titans", 4, 1, 0, 3, -24, 3)
            ),
            isLeague = true
        )
    )

    val communities = listOf(
        CommunityInfo("cm1", "Kadıköy Spor Çevresi", CommunityType.CITY, "Kadıköy", 842, CommunityAccess.OPEN, "🌊", 18, "Kadıköy'ün en aktif çok-sporlu topluluğu. Haftalık garantili etkinlik takvimi.", listOf(Sport.FOOTBALL, Sport.TENNIS, Sport.RUNNING)),
        CommunityInfo("cm2", "Boğaziçi Üniversitesi Spor", CommunityType.CAMPUS, "Bebek", 1240, CommunityAccess.APPROVAL, "🎓", 11, "Fakülte ligleri ve kampüs ranking'i. @boun.edu.tr doğrulaması gerekir.", listOf(Sport.BASKETBALL, Sport.VOLLEYBALL, Sport.FOOTBALL)),
        CommunityInfo("cm3", "Moda Koşu Kulübü", CommunityType.CLUB, "Moda", 315, CommunityAccess.OPEN, "🏃", 6, "Salı ve Perşembe sahil koşuları, aylık 10K.", listOf(Sport.RUNNING)),
        CommunityInfo("cm4", "TechCorp Wellness Ligi", CommunityType.COMPANY, "Maslak", 210, CommunityAccess.INVITE, "🏢", 4, "Departmanlar arası futbol ve padel ligi.", listOf(Sport.FOOTBALL, Sport.PADEL)),
        CommunityInfo("cm5", "Anadolu Padel Topluluğu", CommunityType.CLUB, "Suadiye", 498, CommunityAccess.OPEN, "🏓", 9, "Her seviyeden padel oyuncusu, haftalık americano.", listOf(Sport.PADEL)),
        CommunityInfo("cm6", "Kadın Futbol Kadıköy", CommunityType.CITY, "Kadıköy", 176, CommunityAccess.APPROVAL, "⚽", 3, "Kadınlara özel güvenli halı saha topluluğu.", listOf(Sport.FOOTBALL))
    )

    private fun slots(prefix: String, base: Int, courts: List<String>): List<VenueSlotInfo> {
        val days = listOf("Bugün", "Yarın", "Cmt", "Paz")
        val times = listOf("17:00", "18:00", "19:00", "20:00", "21:00", "22:00")
        var i = 0
        return days.flatMap { d ->
            courts.flatMap { c ->
                times.map { t ->
                    i++
                    val peak = t >= "19:00" && t <= "21:00"
                    VenueSlotInfo("$prefix-$i", c, d, t, 60, if (peak) base + base / 4 else base, (i * 7 + c.length) % 3 != 0)
                }
            }
        }
    }

    val venues = listOf(
        VenueInfo("v1", "Moda Spor Parkı", "Moda", listOf(Sport.FOOTBALL, Sport.BASKETBALL), 4.7f, 312, true, 0.9f, 1600,
            listOf("Duş", "Soyunma odası", "Otopark", "Kafe", "Işıklandırma", "Erişilebilir giriş"), 3, "24 saat öncesine kadar ücretsiz iptal", slots("v1", 1600, listOf("Saha 1", "Saha 2")), MColors.Mint),
        VenueInfo("v2", "Kalamış Tenis Kulübü", "Kalamış", listOf(Sport.TENNIS), 4.8f, 204, true, 1.2f, 500,
            listOf("Raket kiralama", "Top makinesi", "Duş", "Kafe"), 6, "12 saat öncesine kadar ücretsiz iptal", slots("v2", 500, listOf("Kort 1", "Kort 3")), MColors.Sky),
        VenueInfo("v3", "Suadiye Padel Club", "Suadiye", listOf(Sport.PADEL), 4.9f, 167, true, 4.1f, 1280,
            listOf("Raket kiralama", "Kapalı kort", "Duş", "Pro shop"), 4, "24 saat öncesine kadar ücretsiz iptal", slots("v3", 1280, listOf("Kort 1", "Kort 2")), MColors.Lilac),
        VenueInfo("v4", "Fikirtepe Arena", "Fikirtepe", listOf(Sport.FOOTBALL), 4.4f, 98, false, 2.2f, 1800,
            listOf("Soyunma odası", "Otopark", "Tribün"), 2, "48 saat öncesine kadar %50 iade", slots("v4", 1800, listOf("Saha 1")), MColors.Amber)
    )

    val initialMemoryPosts = listOf(
        MatchMemoryPost(
            id = "mem_1",
            authorId = "p1",
            authorName = "Mert Kaya",
            authorInitials = "MK",
            sport = Sport.FOOTBALL,
            venue = "Moda Sahil Arena",
            area = "Moda",
            city = "İstanbul",
            distanceKm = 0.8f,
            caption = "Maç öncesi ısınma ve kadro karesi! Saha hazır 🔥",
            matchScoreTag = "Isınma · Kadro Tamam",
            timeAgo = "18 dk önce",
            likes = 48,
            likedByMe = true,
            sceneTheme = MemorySceneTheme.NIGHT_TURF
        ),
        MatchMemoryPost(
            id = "mem_2",
            authorId = "p4",
            authorName = "Elif Yıldız",
            authorInitials = "EY",
            sport = Sport.TENNIS,
            venue = "Kalamış Tenis Kulübü",
            area = "Kalamış",
            city = "İstanbul",
            distanceKm = 1.2f,
            caption = "Gün batımında 3 setlik tie-break klasiği 🎾",
            matchScoreTag = "6-4 · 4-6 · 10-8",
            timeAgo = "42 dk önce",
            likes = 64,
            sceneTheme = MemorySceneTheme.SUNSET_TENNIS
        ),
        MatchMemoryPost(
            id = "mem_3",
            authorId = "p6",
            authorName = "Caner Aksoy",
            authorInitials = "CA",
            sport = Sport.PADEL,
            venue = "Suadiye Padel Loft",
            area = "Suadiye",
            city = "İstanbul",
            distanceKm = 2.4f,
            caption = "Cam kortta kesintisiz ralli gecesi, rövanş haftaya!",
            matchScoreTag = "2 - 1 Set",
            timeAgo = "1 sa önce",
            likes = 37,
            sceneTheme = MemorySceneTheme.PADEL_GLASS
        ),
        MatchMemoryPost(
            id = "mem_4",
            authorId = "p3",
            authorName = "Burak Demir",
            authorInitials = "BD",
            sport = Sport.BASKETBALL,
            venue = "Caddebostan Sahil Kortu",
            area = "Caddebostan",
            city = "İstanbul",
            distanceKm = 1.9f,
            caption = "Sahil rüzgarına karşı 3x3 sokak turnuvası finali 🏀",
            matchScoreTag = "21 - 19",
            timeAgo = "2 sa önce",
            likes = 53,
            sceneTheme = MemorySceneTheme.ARENA_HOOPS
        ),
        MatchMemoryPost(
            id = "mem_5",
            authorId = "p8",
            authorName = "Zeynep Er",
            authorInitials = "ZE",
            sport = Sport.VOLLEYBALL,
            venue = "Fenerbahçe Kum Kortu",
            area = "Fenerbahçe",
            city = "İstanbul",
            distanceKm = 1.5f,
            caption = "Sahil kortunda ekip tam kadro ⚡",
            matchScoreTag = "2 - 0 Set",
            timeAgo = "3 sa önce",
            likes = 41,
            sceneTheme = MemorySceneTheme.BEACH_VOLLEY
        ),
        MatchMemoryPost(
            id = "mem_6",
            authorId = "p7",
            authorName = "Kerem Polat",
            authorInitials = "KP",
            sport = Sport.FOOTBALL,
            venue = "Acıbadem Kampüs Sahası",
            area = "Acıbadem",
            city = "İstanbul",
            distanceKm = 3.1f,
            caption = "Kadıköy Sonbahar Kupası çeyrek final hatırası 🏆",
            matchScoreTag = "4 - 2 · Çeyrek Final",
            timeAgo = "5 sa önce",
            likes = 79,
            likedByMe = true,
            sceneTheme = MemorySceneTheme.TROPHY_MOMENT
        ),
        MatchMemoryPost(
            id = "mem_7",
            authorId = "p10",
            authorName = "Ece Tan",
            authorInitials = "ET",
            sport = Sport.TENNIS,
            venue = "Bostancı Merkez Kort",
            area = "Bostancı",
            city = "İstanbul",
            distanceKm = 3.8f,
            caption = "Sabah antrenmanı ve kahve molası ✨",
            matchScoreTag = "6-3 · 6-2",
            timeAgo = "Dün",
            likes = 29,
            sceneTheme = MemorySceneTheme.SUNSET_TENNIS
        ),
        MatchMemoryPost(
            id = "mem_8",
            authorId = "p11",
            authorName = "Onur Şahin",
            authorInitials = "OŞ",
            sport = Sport.FOOTBALL,
            venue = "Çankaya Yıldız Arena",
            area = "Çankaya",
            city = "Ankara",
            distanceKm = 4.5f,
            caption = "Deplasman gecesi dostluk maçı karesi 🤝",
            matchScoreTag = "5 - 5",
            timeAgo = "Dün",
            likes = 33,
            sceneTheme = MemorySceneTheme.NIGHT_TURF
        )
    )

    val chats = listOf(
        ChatThread("ch1", ThreadType.EVENT, "Moda Akşam Halı Saha", "⚽", 8, listOf(
            ChatMsg("m1", "sys", "Matchora", "Etkinlik sohbeti yalnızca katılımcılara açıktır.", "19:02", isSystem = true),
            ChatMsg("m2", "p1", "Mert", "Kaleci eksik, Emergency Fill açtım 🧤", "19:10"),
            ChatMsg("m3", "p2", "Can", "Ben kaleye geçebilirim ilk yarı", "19:12"),
            ChatMsg("m4", "p11", "Onur", "Formalar turuncu vs beyaz mı?", "19:20"),
            ChatMsg("m5", "p1", "Mert", "Geçen haftaki maç sonu karemizi de bırakıyorum, akşam 20:00'de aynı enerjiyle sahadayız 👊", "19:24", attachedMemory = initialMemoryPosts.first())
        ), unread = 2, relatedEventId = "e1"),
        ChatThread("ch_dm_p1", ThreadType.DIRECT, "Mert Kaya", "💬", 2, listOf(
            ChatMsg("m_dm_1", "p1", "Mert Kaya", "Selam Özcan! Akşamki Moda maçında krampon getirmem gerekiyor mu?", "17:15"),
            ChatMsg("m_dm_2", "me", "Özcan Özden", "Selam Mert, halı saha ayakkabısı daha uygun, zemin kuru.", "17:20", isMine = true),
            ChatMsg("m_dm_3", "p1", "Mert Kaya", "Tamamdır, 20:00'de sahada görüşürüz 👍", "17:22")
        ), unread = 1),
        ChatThread("ch2", ThreadType.TEAM, "Kadıköy Utd", "🛡️", 8, listOf(
            ChatMsg("m1", "p1", "Mert", "Bostancı Rovers challenge yolladı, %93 uyum!", "18:40"),
            ChatMsg("m2", "p3", "Burak", "Çarşamba olur benim için", "18:45"),
            ChatMsg("m3", "me", "Özcan", "Kabul etmeden önce kadroyu netleştirelim", "18:47", isMine = true)
        ), unread = 1),
        ChatThread("ch_dm_p4", ThreadType.DIRECT, "Elif Yıldız", "💬", 2, listOf(
            ChatMsg("m_dm_e1", "p4", "Elif Yıldız", "Selam! Kalamış kort 3'ü rezerve ettim, toplar bende 🎾", "Dün"),
            ChatMsg("m_dm_e2", "me", "Özcan Özden", "Süper, kort ücretini Matchora üzerinden bölelim.", "Dün", isMine = true)
        ), unread = 0),
        ChatThread("ch3", ThreadType.SQUAD, "Perşembe Auto-Squad", "✨", 6, listOf(
            ChatMsg("m1", "sys", "Matchora", "Bu grup her Perşembe 20:00'de oynamaya uygun görünüyor.", "Dün", isSystem = true),
            ChatMsg("m2", "p12", "Hakan", "Bu hafta ben de varım", "Dün")
        ), unread = 0),
        ChatThread("ch4", ThreadType.BROADCAST, "Kadıköy Sonbahar Kupası", "🏆", 96, listOf(
            ChatMsg("m1", "org", "Organizatör", "Kura çekimi Cuma 20:00'de canlı yapılacak. Kadro kilidi Perşembe 23:59.", "Pzt")
        ), unread = 0),
        ChatThread("ch5", ThreadType.EVENT, "Kalamış Kort · Tekler", "🎾", 2, listOf(
            ChatMsg("m1", "p4", "Elif", "Toplar bende, kort 3'teyiz", "Paz")
        ), unread = 0, relatedEventId = "e2")
    )

    val initialJoinRequests = listOf(
        JoinRequest(
            id = "jr1",
            eventId = "e1",
            player = player("p7")!!, // Kerem Polat
            requestedPosition = "Orta Saha / Forvet",
            message = "Moda'dayım, forma ve ayakkabım hazır. Hemen dahil olabilirim.",
            timestamp = "12 dk önce",
            status = JoinRequestStatus.PENDING
        ),
        JoinRequest(
            id = "jr2",
            eventId = "e1",
            player = player("p10")!!, // Ece Tan
            requestedPosition = "Defans / Kanat",
            message = "Kadroda yer varsa katılmak isterim.",
            timestamp = "24 dk önce",
            status = JoinRequestStatus.PENDING
        ),
        JoinRequest(
            id = "jr3",
            eventId = "e7", // User is host of e7
            player = player("p8")!!, // Zeynep Er
            requestedPosition = "Yedek Forvet",
            message = "Kadıköy Utd maçına destek olmak isterim, lisansım aktif.",
            timestamp = "45 dk önce",
            status = JoinRequestStatus.PENDING
        )
    )

    val notifications = listOf(
        AppNotification("n1", NotifPriority.P1, "🧤", "900 m uzağında 1 kaleci aranıyor", "Moda Akşam Halı Saha · 46 dk sonra başlıyor", "2 dk", eventId = "e1", expiresIn = "40 dk"),
        AppNotification("n2", NotifPriority.P2, "⏳", "Waitlist'te sıra sana geldi", "Suadiye Padel · Cumartesi 10:00 — 10 dk içinde kabul et", "8 dk", eventId = "e5", expiresIn = "10 dk"),
        AppNotification("n3", NotifPriority.P2, "🛡️", "Takımına challenge geldi", "Bostancı Rovers · Çarşamba 21:00 · %93 uyum", "25 dk", destination = NotificationDestination.CHALLENGES),
        AppNotification("n4", NotifPriority.P1, "🎾", "%94 uyumlu tenis rakibi bulundu", "Elif Yıldız · Kalamış · Bugün 18:00", "1 sa", eventId = "e2"),
        AppNotification("n5", NotifPriority.P0, "🔐", "Yeni cihazdan giriş", "iPhone 16 · İstanbul — sen değilsen hemen şifreni sıfırla", "3 sa", read = true, destination = NotificationDestination.ACCOUNT),
        AppNotification("n6", NotifPriority.P3, "🏅", "Yeni rozet: Perfect Attendance", "Son 10 maçının tamamına zamanında check-in yaptın", "Dün", read = true, destination = NotificationDestination.PASSPORT),
        AppNotification("n7", NotifPriority.P3, "📈", "Kadıköy futbol sıralamasında 12. sıraya yükseldin", "+18 rating · Gold lig", "Dün", read = true, destination = NotificationDestination.RANKING),
        AppNotification("n8", NotifPriority.P2, "🔁", "Rematch zamanı", "Mert, Can ve 3 kişi Perşembe 20:00'de müsait", "2 g", read = true, destination = NotificationDestination.PLAY),
        AppNotification("n9", NotifPriority.P4, "🚀", "Pro'yu 7 gün ücretsiz dene", "Gelişmiş filtreler ve derin istatistikler", "3 g", read = true, destination = NotificationDestination.PLUS)
    )

    val initialLedger = listOf(
        LedgerEntry("l1", LedgerType.BONUS, 50, "Hoş geldin bonusu", "1 Eyl", idempotencyKey = "bonus-welcome-me"),
        LedgerEntry("l2", LedgerType.PURCHASE, 100, "100 Token paketi", "3 Eyl", idempotencyKey = "iap-7f2a91", channel = "App Store"),
        LedgerEntry("l3", LedgerType.HOLD, -10, "Taahhüt · Kalamış Kort", "14 Eki", ref = "e2-old", idempotencyKey = "hold-e2old-me"),
        LedgerEntry("l4", LedgerType.RELEASE, 10, "Zamanında check-in · Kalamış Kort", "14 Eki", ref = "e2-old", idempotencyKey = "rel-e2old-me"),
        LedgerEntry("l5", LedgerType.SPEND, -15, "İlan boost · Kadıköy Utd kadro ilanı", "15 Eki", idempotencyKey = "spend-boost-881"),
        LedgerEntry("l6", LedgerType.HOLD, -10, "Taahhüt · Suadiye Padel", "16 Eki", ref = "e5", idempotencyKey = "hold-e5-me"),
        LedgerEntry("l7", LedgerType.HOLD, -10, "Taahhüt · Kadıköy Utd – Ataşehir FC", "16 Eki", ref = "e7", idempotencyKey = "hold-e7-me"),
        LedgerEntry("l8", LedgerType.BONUS, 15, "Haftalık görev: 3 farklı rakip", "16 Eki", idempotencyKey = "mission-w41-me")
    )

    val initialVenuePayments = listOf(
        VenuePayment("vp1", "Kalamış Tenis Kulübü", "Kort 3 · 14 Eki 18:00 · payın", 250, "Visa •• 4242", "14 Eki", PaymentStatus.PAID),
        VenuePayment("vp2", "Moda Spor Parkı", "Saha 2 · 9 Eki 20:30 · payın", 180, "Mastercard •• 8812", "9 Eki", PaymentStatus.PAID),
        VenuePayment("vp3", "Fikirtepe Arena", "İptal edilen maç · 2 Eki", 200, "Visa •• 4242", "2 Eki", PaymentStatus.REFUNDED)
    )

    val initialPaymentMethods = listOf(
        PaymentMethod("pm1", "Visa", "4242", "08/28", "OZCAN OZDEN", true),
        PaymentMethod("pm2", "Mastercard", "8812", "11/27", "OZCAN OZDEN", false)
    )

    val tokenPacks = listOf(
        TokenPack("tkn_50", 50, 0, "₺49,99"),
        TokenPack("tkn_120", 120, 10, "₺109,99", "En popüler"),
        TokenPack("tkn_300", 300, 45, "₺249,99", "En avantajlı"),
        TokenPack("tkn_700", 700, 140, "₺549,99", "Organizatör")
    )

    val proPlans = listOf(
        ProPlan("pro_monthly", "Aylık", "₺129,99", "/ay", null),
        ProPlan("pro_yearly", "Yıllık", "₺999,99", "/yıl", "%36 tasarruf · 7 gün ücretsiz")
    )

    val badges = listOf(
        BadgeInfo("b1", "Perfect Attendance", "🎯", "Art arda 10 maça zamanında check-in", true, 1f),
        BadgeInfo("b2", "Fair Play", "🤝", "20 maçta 4.5+ fair play puanı", true, 1f),
        BadgeInfo("b3", "Community Builder", "🌱", "5 yeni oyuncuyu ilk maçına getir", true, 1f),
        BadgeInfo("b4", "100 Matches", "💯", "100 doğrulanmış maç", false, 0.59f),
        BadgeInfo("b5", "Tournament Winner", "🏆", "Bir turnuvayı kazan", false, 0f),
        BadgeInfo("b6", "Traveler", "✈️", "3 farklı şehirde oyna", false, 0.33f),
        BadgeInfo("b7", "Iron Streak", "🔥", "12 hafta kesintisiz spor", false, 0.66f),
        BadgeInfo("b8", "Clutch Filler", "🦸", "3 Emergency Fill çağrısına yanıt ver", true, 1f)
    )

    val missions = listOf(
        Mission("ms1", "3 farklı rakiple oyna", "Bu hafta yeni insanlarla tanış", 2, 3, 15, "🧩"),
        Mission("ms2", "Bir rematch kur", "Birlikte oynadığın biriyle tekrar eşleş", 0, 1, 10, "🔁"),
        Mission("ms3", "Zamanında check-in", "2 maçta pencerede check-in yap", 1, 2, 10, "📍")
    )

    val reliabilityRecords = listOf(
        ReliabilityRecord("Zamanında doğrulanmış check-in", +2, "14 Eki", "Kalamış Kort · QR"),
        ReliabilityRecord("Zamanında doğrulanmış check-in", +2, "9 Eki", "Moda Spor Parkı · geofence"),
        ReliabilityRecord("Erken iptal (deadline öncesi)", 0, "5 Eki", "Ceza uygulanmadı"),
        ReliabilityRecord("Son dakika iptal", -4, "18 Eyl", "Ağırlığı zamanla azalıyor"),
        ReliabilityRecord("Zamanında doğrulanmış check-in", +2, "11 Eyl", "Fikirtepe Arena · QR"),
        ReliabilityRecord("Organizatör teşekkürü", +1, "4 Eyl", "Kadıköy Spor Çevresi")
    )

    val history = listOf(
        MatchHistoryItem("h1", Sport.TENNIS, "Kalamış Kort · Elif Y.", "14 Eki", "6-4 · 4-6 · 10-7", true, +14, true, PlayMode.COMPETITIVE),
        MatchHistoryItem("h2", Sport.FOOTBALL, "Moda 5v5", "9 Eki", "7 – 5", true, +11, true, PlayMode.COMPETITIVE),
        MatchHistoryItem("h3", Sport.FOOTBALL, "Kadıköy Utd – Maltepe", "2 Eki", "4 – 2", true, +9, true, PlayMode.COMPETITIVE),
        MatchHistoryItem("h4", Sport.PADEL, "Suadiye Americano", "28 Eyl", "Sosyal maç", null, 0, true, PlayMode.SOCIAL),
        MatchHistoryItem("h5", Sport.FOOTBALL, "Fikirtepe 7v7", "21 Eyl", "3 – 5", false, -8, true, PlayMode.COMPETITIVE)
    )

    val autoSquads = listOf(
        AutoSquadInfo("as1", "Perşembe Akşam Squad", Sport.FOOTBALL, listOf("p1", "p2", "p11", "p12", "p3"), 94, "Her Perşembe 20:00",
            listOf("6 kişinin 5'i Perşembe 20–22 arası müsait", "Karşılıklı 'tekrar oynarım' sinyali", "Rol dengesi: 1 kaleci, 2 defans, 2 orta saha", "Ortalama Reliability 93", "Hepsi 2,5 km içinde")),
        AutoSquadInfo("as2", "Hafta Sonu Raket Çevresi", Sport.TENNIS, listOf("p4", "p8", "p5"), 89, "Cumartesi 10:00",
            listOf("Seviyeler 1236–1408 aralığında", "Cumartesi sabahları ortak", "3 maçta karşılıklı rematch"))
    )

    val areas = listOf("Moda", "Kalamış", "Fenerbahçe", "Caddebostan", "Göztepe", "Suadiye", "Bostancı", "Acıbadem", "Kozyatağı", "Ataşehir")
    val travelCities = listOf("Ankara", "İzmir", "Antalya", "Bursa", "Eskişehir", "Londra", "Berlin", "Barselona")
}
