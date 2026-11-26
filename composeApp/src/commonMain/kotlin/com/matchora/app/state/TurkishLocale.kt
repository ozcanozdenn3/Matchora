package com.matchora.app.state

private val turkishFoldMap = mapOf(
    'İ' to 'i', 'I' to 'i', 'ı' to 'i',
    'Ş' to 's', 'ş' to 's',
    'Ğ' to 'g', 'ğ' to 'g',
    'Ü' to 'u', 'ü' to 'u',
    'Ö' to 'o', 'ö' to 'o',
    'Ç' to 'c', 'ç' to 'c',
    'Â' to 'a', 'â' to 'a',
    'Î' to 'i', 'î' to 'i',
    'Û' to 'u', 'û' to 'u'
)

/**
 * Normalizes Turkish diacritics and casing cleanly on every device locale (including en_US),
 * preventing "İstanbul" / "Istanbul" / "Kadıköy" / "Kadikoy" mismatches.
 */
fun canonicalTurkishKey(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return buildString(value.length) {
        for (ch in value.trim()) {
            if (ch == '\u0307') continue
            append((turkishFoldMap[ch] ?: ch).lowercaseChar())
        }
    }
}

private val canonicalCityLookup: Map<String, String> by lazy {
    turkeyCities.associateBy { canonicalTurkishKey(it) }
}

val turkeyDistricts: List<String> = (
    "Merkez|" +
        "Adalar|Arnavutköy|Ataşehir|Avcılar|Bağcılar|Bahçelievler|Bakırköy|Başakşehir|Bayrampaşa|Beşiktaş|" +
        "Beykoz|Beylikdüzü|Beyoğlu|Büyükçekmece|Çatalca|Çekmeköy|Esenler|Esenyurt|Eyüpsultan|Fatih|" +
        "Gaziosmanpaşa|Güngören|Kadıköy|Kağıthane|Kartal|Küçükçekmece|Maltepe|Pendik|Sancaktepe|Sarıyer|" +
        "Silivri|Sultanbeyli|Sultangazi|Şile|Şişli|Tuzla|Ümraniye|Üsküdar|Zeytinburnu|" +
        "Akyurt|Altındağ|Ayaş|Bala|Beypazarı|Çamlıdere|Çankaya|Çubuk|Elmadağ|Etimesgut|Evren|Gölbaşı|Güdül|" +
        "Haymana|Kahramankazan|Kalecik|Keçiören|Kızılcahamam|Mamak|Nallıhan|Polatlı|Pursaklar|Sincan|" +
        "Şereflikoçhisar|Yenimahalle|" +
        "Aliağa|Balçova|Bayındır|Bayraklı|Bergama|Beydağ|Bornova|Buca|Çeşme|Çiğli|Dikili|Foça|Gaziemir|" +
        "Güzelbahçe|Karabağlar|Karaburun|Karşıyaka|Kemalpaşa|Kınık|Kiraz|Konak|Menderes|Menemen|Narlıdere|" +
        "Ödemiş|Seferihisar|Selçuk|Tire|Torbalı|Urla|" +
        "Büyükorhan|Gemlik|Gürsu|Harmancık|İnegöl|İznik|Karacabey|Keles|Kestel|Mudanya|Mustafakemalpaşa|" +
        "Nilüfer|Orhaneli|Orhangazi|Osmangazi|Yenişehir|Yıldırım|" +
        "Akseki|Aksu|Alanya|Demre|Döşemealtı|Elmalı|Finike|Gazipaşa|Gündoğmuş|İbradı|Kaş|Kemer|Kepez|" +
        "Konyaaltı|Korkuteli|Kumluca|Manavgat|Muratpaşa|Serik|" +
        "Aladağ|Ceyhan|Çukurova|Feke|İmamoğlu|Karaisalı|Karataş|Kozan|Pozantı|Saimbeyli|Sarıçam|Seyhan|" +
        "Tufanbeyli|Yumurtalık|Yüreğir|" +
        "Selçuklu|Meram|Karatay|Ereğli|Akşehir|Beyşehir|Çumra|Ilgın|Kulu|Seydişehir|" +
        "Şahinbey|Şehitkamil|Nizip|İslahiye|Nurdağı|" +
        "Başiskele|Çayırova|Darıca|Derince|Dilovası|Gebze|Gölcük|İzmit|Kandıra|Karamürsel|Kartepe|Körfez|" +
        "Akdeniz|Mezitli|Toroslar|Erdemli|Silifke|Tarsus|Anamur|Bozyazı|Mut|Gülnar|Aydıncık|Çamlıyayla|" +
        "Bağlar|Kayapınar|Sur|Ergani|Bismil|Silvan|Çınar|Dicle|Eğil|Hani|Hazro|Kocaköy|Kulp|Lice|" +
        "Melikgazi|Kocasinan|Talas|Develi|Yahyalı|Bünyan|İncesu|Pınarbaşı|Sarıoğlan|Sarız|Tomarza|Yeşilhisar|" +
        "Odunpazarı|Tepebaşı|Sivrihisar|Çifteler|Alpu|Beylikova|Günyüzü|Han|İnönü|Mahmudiye|Mihalgazi|Mihalıççık|Sarıcakaya|Seyitgazi|" +
        "İlkadım|Atakum|Canik|Tekkeköy|Bafra|Çarşamba|Terme|Vezirköprü|Havza|Kavak|Ladik|Ondokuzmayıs|" +
        "Pamukkale|Merkezefendi|Acıpayam|Çivril|Tavas|Buldan|Sarayköy|Çal|Honaz|" +
        "Haliliye|Eyyübiye|Karaköprü|Siverek|Viranşehir|Suruç|Birecik|Akçakale|Ceylanpınar|Harran|Hilvan|" +
        "Ortahisar|Akçaabat|Araklı|Arsin|Beşikdüzü|Çarşıbaşı|Çaykara|Dernekpazarı|Düzköy|Hayrat|Köprübaşı|Maçka|Of|Sürmene|Şalpazarı|Tonya|Vakfıkebir|Yomra|" +
        "Bodrum|Dalaman|Datça|Fethiye|Kavaklıdere|Köyceğiz|Marmaris|Menteşe|Milas|Ortaca|Seydikemer|Ula|Yatağan|" +
        "Süleymanpaşa|Çorlu|Çerkezköy|Kapaklı|Ergene|Hayrabolu|Malkara|Marmaraereğlisi|Muratlı|Saray|Şarköy|" +
        "Adapazarı|Akyazı|Arifiye|Erenler|Ferizli|Geyve|Hendek|Karapürçek|Karasu|Kaynarca|Kocaali|Pamukova|Sapanca|Serdivan|Söğütlü|Taraklı|" +
        "Altıeylül|Karesi|Ayvalık|Balya|Bandırma|Bigadiç|Burhaniye|Dursunbey|Edremit|Erdek|Gömeç|Gönen|Havran|İvrindi|Kepsut|Manyas|Marmara|Savaştepe|Sındırgı|Susurluk|" +
        "Ahmetli|Akhisar|Alaşehir|Demirci|Gölmarmara|Gördes|Kırkağaç|Köprübaşı|Kula|Salihli|Sarıgöl|Saruhanlı|Selendi|Soma|Şehzadeler|Turgutlu|Yunusemre|" +
        "Antakya|Defne|İskenderun|Arsuz|Belen|Dörtyol|Erzin|Hassa|Kırıkhan|Kumlu|Payas|Reyhanlı|Samandağ|Yayladağı|" +
        "Efeler|Bozdoğan|Buharkent|Çine|Didim|Germencik|İncirliova|Karacasu|Karpuzlu|Koçarlı|Köşk|Kuşadası|Kuyucak|Nazilli|Söke|Sultanhisar|Yenipazar|" +
        "İpekyolu|Tuşba|Edremit|Erciş|Özalp|Çaldıran|Muradiye|Başkale|Gevaş|Gürpınar|" +
        "Dulkadiroğlu|Onikişubat|Elbistan|Afşin|Göksun|Pazarcık|Türkoğlu|Andırın|" +
        "Battalgazi|Yeşilyurt|Doğanşehir|Darende|Akçadağ|Hekimhan|" +
        "Artuklu|Kızıltepe|Midyat|Nusaybin|Derik|Mazıdağı|Savur|" +
        "Yakutiye|Palandöken|Aziziye|Horasan|Oltu|Pasinler|" +
        "Ereğli|Çaycuma|Devrek|Alaplı|Kozlu|Kilimli|Gökçebey|" +
        "Safranbolu|Yenice|Eskipazar|Eflani|Ovacık|" +
        "Bolu|Gerede|Mudurnu|Mengen|Göynük|" +
        "Düzce|Akçakoca|Kaynaşlı|Cumayeri|Çilimli|Gölyaka|Gümüşova|Yığılca|" +
        "Edirne|Keşan|Uzunköprü|İpsala|Havsa|" +
        "Lüleburgaz|Babaeski|Vize|Pınarhisar|Demirköy|" +
        "Biga|Gelibolu|Çan|Ezine|Ayvacık|Bozcaada|Gökçeada|Lapseki|" +
        "Afyonkarahisar|Sandıklı|Dinar|Bolvadin|Emirdağ|Şuhut|Çay|İscehisar|" +
        "Kütahya|Tavşanlı|Simav|Gediz|Emet|" +
        "Uşak|Banaz|Eşme|Sivaslı|Ulubey|" +
        "Isparta|Yalvaç|Eğirdir|Şarkikaraağaç|Senirkent|" +
        "Burdur|Bucak|Gölhisar|Tefenni|Yeşilova|" +
        "Aksaray|Ortaköy|Eskil|Gülağaç|Güzelyurt|" +
        "Nevşehir|Ürgüp|Avanos|Gülşehir|Hacıbektaş|Derinkuyu|" +
        "Niğde|Bor|Çiftlik|Ulukışla|" +
        "Karaman|Ermenek|Sarıveliler|" +
        "Kırıkkale|Yahşihan|Keskin|Delice|" +
        "Kırşehir|Kaman|Mucur|Çiçekdağı|" +
        "Yozgat|Sorgun|Akdağmadeni|Boğazlıyan|Yerköy|Çekerek|Sarıkaya|" +
        "Sivas|Şarkışla|Yıldızeli|Suşehri|Zara|Gürün|Kangal|Divriği|Gemerek|" +
        "Tokat|Erbaa|Turhal|Niksar|Zile|Reşadiye|Almus|" +
        "Amasya|Merzifon|Suluova|Taşova|Gümüşhacıköy|" +
        "Çorum|Sungurlu|Osmancık|İskilip|Alaca|Kargı|" +
        "Kastamonu|Tosya|Taşköprü|İnebolu|Cide|" +
        "Çankırı|Çerkeş|Ilgaz|Kurşunlu|" +
        "Sinop|Boyabat|Gerze|Ayancık|Durağan|Türkeli|" +
        "Bartın|Ulus|Amasra|Kurucaşile|" +
        "Altınordu|Ünye|Fatsa|Gölköy|Korgan|Kumru|Perşembe|" +
        "Giresun|Bulancak|Espiye|Görele|Tirebolu|Şebinkarahisar|" +
        "Gümüşhane|Kelkit|Şiran|Torul|Kürtün|Köse|" +
        "Rize|Çayeli|Ardeşen|Pazar|Fındıklı|İyidere|Kalkandere|" +
        "Artvin|Hopa|Arhavi|Borçka|Yusufeli|Şavşat|" +
        "Kars|Kağızman|Sarıkamış|Digor|Selim|" +
        "Ardahan|Göle|Çıldır|Posof|" +
        "Iğdır|Tuzluca|Aralık|Karakoyunlu|" +
        "Ağrı|Doğubayazıt|Patnos|Diyadin|Eleşkirt|Tutak|" +
        "Muş|Bulanık|Malazgirt|Varto|" +
        "Bitlis|Tatvan|Ahlat|Güroymak|Hizan|Adilcevaz|" +
        "Hakkâri|Yüksekova|Şemdinli|Çukurca|" +
        "Şırnak|Cizre|Silopi|İdil|Uludere|Beytüşşebap|" +
        "Batman|Kozluk|Beşiri|Gercüş|Hasankeyf|Sason|" +
        "Siirt|Kurtalan|Pervari|Baykan|Şirvan|" +
        "Elazığ|Kovancılar|Karakoçan|Palu|Baskil|" +
        "Bingöl|Genç|Solhan|Karlıova|" +
        "Tunceli|Pertek|Çemişgezek|Hozat|Mazgirt|Ovacık|" +
        "Adıyaman|Kahta|Besni|Gölbaşı|Gerger|" +
        "Osmaniye|Kadirli|Düziçi|Bahçe|" +
        "Kilis|Musabeyli|Polateli|Elbeyli|" +
        "Yalova|Çiftlikköy|Çınarcık|Altınova|Armutlu|Termal|" +
        "Bilecik|Bozüyük|Osmaneli|Söğüt|Pazaryeri"
).split("|").distinct()

private val canonicalDistrictLookup: Map<String, String> by lazy {
    turkeyDistricts.associateBy { canonicalTurkishKey(it) }
}

fun canonicalTurkishCityName(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim()
    return canonicalCityLookup[canonicalTurkishKey(trimmed)] ?: trimmed
}

fun canonicalTurkishDistrictName(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim()
    return canonicalDistrictLookup[canonicalTurkishKey(trimmed)] ?: trimmed
}

fun matchesTurkishPlace(left: String?, right: String?): Boolean {
    val a = canonicalTurkishKey(left)
    val b = canonicalTurkishKey(right)
    return a.isNotEmpty() && b.isNotEmpty() && a == b
}

