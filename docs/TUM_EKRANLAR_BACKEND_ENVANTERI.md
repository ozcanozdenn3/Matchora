# Matchora — tüm ekran, bölüm, pencere ve backend envanteri

Güncelleme: **10 Kasım 2026**. Bu belge, ekrandaki özellikleri tek tek backend ve cihaz bağlantılarıyla eşler. **406 benzersiz özellik satırı** içerir; önceki 253 satırın ID'leri korunmuş, yeni pencereler ayrı ID'lerle eklenmiştir. Uygulamanın tamamının bitmiş olduğu anlamına gelmez. **Preview mock verileri korur; canlı mod bağlantıları aşağıda ayrıca belirtilir.**

Kaynak kapsamı: ortak UI dosyalarının tamamı, App yönlendirmeleri, Android/iOS uygulamaları ve sunucu komutları. Bütün fonksiyonlar, pencere/menü açılış noktaları ve sunucu komutlarının kaynak referansları [otomatik kaynak dizininde](UI_KAYNAK_DIZINI.md); makinece okunur karşılığı [ui-inventory.json](validation/2026-11-10/ui-inventory.json). `python3 scripts/ui_backend_inventory.py --check` envanterin güncelliğini ve her UI dosyasının bu belgede anılmasını denetler. Bu kontrol davranış testi değildir.

**Durumların anlamı:**

- **Bağlı:** istemci çağrısı ve sunucu uygulaması var. Ayrı bir sağlayıcı veya cihaz testi gerekip gerekmediği son sütunda yazılı.
- **Kısmi:** özelliğin bir kısmı bağlı; açık kalan kısmı var.
- **API var / UI yok:** sunucu işlemi yazıldı; kullanılacağı yönetim ekranı yapılmadı.
- **Servis bekliyor:** SDK/adaptör var; hesap, anahtar ve gerçek servis doğrulaması gerekiyor.
- **Cihaz / yerel UI:** ayrı backend gerektirmeyen yerel seçici veya sistem işlemi. Formun kaydı varsa ayrıca gösterildi.
- **Eski / pasif:** kaynak korunuyor; mevcut akıştan açılan tam ürün özelliği olarak sayılmıyor.
- **Yok:** iş kuralı veya entegrasyon yazılmadı.

## A. Açılış, ortak çerçeve ve gezinme

Kaynaklar: `App.kt`, `SplashScreen.kt`, `CommonUi.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| A01 | Splash logosu ve geçiş animasyonu | Yerel UI; ayrı API gerekmez | İlk açılış/arka plandan dönüş cihaz testi |
| A02 | Başlangıç veri ve oturum yükleme | **Bağlı:** config, bootstrap, token yenileme | Gerçek HTTPS ortamı |
| A03 | Misafir önizleme | Mock/kamuya açık görünüm korunur; tüm dokunma ve özellik girişleri giriş/kayda yönelir | Giriş + pasaport + güncel yasal onay olmadan kullanım açılmaz |
| A04 | Üst çubuk: logo, giriş/cüzdan, bildirim, şehir | İlgili ekranlara yönlendirme | Her girişin backend'i aşağıda ayrı |
| A05 | Alt dock: Akış, Harita, Şimdi Oyna, Sohbet, Pasaport | Hesap kapısı hem ortak etkileşim katmanında hem gezinme callbacklerinde | Eski/kapalı oturum geçişlerinin fiziksel cihaz testi |
| A06 | Keşif menüsü: Yarış ve Ağ | Yerel pencere; sıralama/turnuva/takım/topluluk/tesis girişleri | Ayrı menü API'si gerekmez |
| A07 | Global “İşlem tamamlanamadı / tekrar dene” penceresi | **Bağlı:** sunucu hatası ve aynı işlem anahtarıyla tekrar | Kalıcı çevrimdışı işlem kuyruğu yok |
| A08 | Toast ve yükleniyor göstergeleri | Yerel UI; ayrı veri kaydı gerekmez | Bazı formlar sunucu onayından önce kapanır; hata/tekrar global pencerede |
| A09 | Push ve bildirim hedefleri | **Bağlı:** maç, topluluk, tesis, rezervasyonlar, işletmeci ve moderasyon hedefleri; Android/iOS köprüsü | Süresi dolmuş ve çok eski katalog kayıtlarının ayrıntı davranışı cihazda genişletilmeli |
| A10 | Kalıcı komut yürütücüsü | **Bağlı:** MainAppShell içindeki LocalBackendAction, alt form kapansa bile isteği ve aynı anahtarla tekrarı yaşatır | Canlı çok cihaz/çevrimdışı UI turu ayrıca gerekir |
| A11 | Eski kişisel katalog kayıtları | **Bağlı:** sahip/üye/davetli takım ve topluluklar, aktif kişisel turnuvalar ve şehir dışı aktif başvurular genel katalog sınırının dışında da getirilir | Genel katalog/geçmiş sayfalama sınırları ayrıca sürer |


## B. Login, e-posta ve ilk giriş pasaportu

Kaynak: `AuthAndOnboardingSheet.kt`. Aynı çekmecenin iç durumları aşağıda ayrı ayrı gösterildi.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| B01 | Apple ile devam et · yalnız iOS | **Servis bekliyor:** gerçek Supabase PKCE, tek kullanımlık sunucu akışı, ASWebAuthenticationSession; demo hesap yolu kaldırıldı | Apple/Supabase anahtarları ve gerçek giriş testi; Android’de düğme yok |
| B02 | Google ile devam et | **Servis bekliyor:** gerçek Supabase PKCE; Android sistem tarayıcısı/iOS sistem auth oturumu; demo hesap yolu kaldırıldı | Google/Supabase ayarı ve gerçek giriş testi |
| B03 | E-posta ile devam et | Telefon girişinin yerine; normalize e-posta, POST /v1/auth/email/start → Supabase OTP; yeni hesap oluşturabilir | Email sağlayıcısı, kodlu e-posta şablonu ve üretim SMTP |
| B04 | E-posta 6 haneli kod ekranı / yanlış kod / süre dolması | POST /v1/auth/email/verify; doğrulanmış e-posta eşleşmesi; sağlayıcının tek kullanımlık kodu; oturum sonra oluşturulur | Gerçek e-posta teslimi ve cihaz testi |
| B05 | Tekrar kod iste sayacı | 60 sn yerel sayaç + kalıcı sunucu adres/IP sınırı; yanlış doğrulama denemeleri de sayılır | Supabase ayrıca kendi gönderim limitlerini uygular |
| B06 | Misafir devam / kapat / geri | Belirgin renkli önizleme kartı; oturum temizleme, işlem kapısı ve derin bağlantı koruması | Salt görsel önizleme; özellik kullanımı giriş ister |
| B07 | Mevcut kullanıcıyla devam | **Bağlı:** kayıtlı profil ve oturum geri yükleme | Gerçek hesap değiştirme; sağlayıcı hesap birleştirme yok |
| B08 | Pasaport adım 1: kimlik | Ad/fotoğraf ve bölge taslağı; sonunda gerçek profile.save ve medya yükleme | Yeni hesapta doğrulanmış oturum olmadan pasaport kaydı yok |
| B09 | Pasaport adım 2: spor, rol ve seviye | Branşa göre alan; sunucu profil doğrulaması | Seviye kullanıcı beyanı; gerçek yetenek ölçümü yok |
| B10 | Pasaport adım 3: tercihler | Tercih/isteğe bağlı ayrıntılar, yasal onay ve gerçek profil kaydı; bekleyen işlemde çift gönderim kapalı | Telefonun ayrı profil doğrulaması sürer; sosyal sağlayıcı fotoğrafı varsayılmaz |
| B11 | İlk profil ve tamamlama ödülleri | **Bağlı:** sunucuda tekil ödül ve finans defteri | Gösterim/ödül koşullarının son ürün kararı |
| B12 | Yeni belge sürümünü inceleme/onay | **Bağlı:** `legal.accept`, sürüm kontrolü, yeniden onay | Gerçek metin/URL; onay gerektiren canlı akış testi |
| B13 | Login hata/yüklenme durumları | **Bağlı:** iptal, sağlayıcı ve API hatası | Fiziksel cihazda sistem tarayıcısından dönüş |
| B14 | Eski saat seçim menüsü yardımcı kodu | **Yerel UI:** `TimePickerMenu`; kendi backend'i yok | Güncel tarih/saat kaydı F bölümündeki akıştan gider |

## C. Şehir, konum ve ortak seçim pencereleri

Kaynaklar: `CityAndLocationSelectorSheet.kt`, `SportPicker.kt`, `EventDateTimePicker.kt`, `EventLocationPicker.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| C01 | Şehir arama ve 81 il açılır listesi | Yerel katalog; hesapta `discovery.save`; canlı keşif sorgusu seçili şehre bağlanır | İl/ilçe için dış resmi katalog servisi yok |
| C02 | İlçe/bölge alanı ve uygula | **Bağlı:** keşif tercihini saklama; canlı haritada native adres çözümleme | Bölge adının resmi katalogla doğrulanması yok; geocoder çevrimdışı başarısız olabilir |
| C03 | Güncel konumumu kullan | **Cihaz:** Android/iOS izin, GPS, geocoder, yaklaşık mesafe | Azaltılmış izin/servis kapalı/zaman aşımı cihaz testi |
| C04 | Konum hata/izin kapalı/ayarlar penceresi | **Cihaz:** elle seçim ve ayarlara gitme | Fiziksel cihaz doğrulaması |
| C05 | Branş seçici: kategori/arama/listeden seçim | Yerel UI; profil/ilan/takım/turnuva kaydına aktarılır | Kendi başına yeni kayıt veya API gerektirmez |
| C06 | Takvim günü seçme penceresi | Yerel UI; gerçek başlangıç epoch değeri kayda gider | Sistem saat farkı ve sınır durumları cihaz testi |
| C07 | Saat seçme penceresi | Yerel UI; tarih ile birleşip sunucuda doğrulanır | Eski serbest metinli formlar U bölümünde |
| C08 | İlan konumu haritası ve açık adres | **Cihaz + bağlı:** native nokta seçimi, konum izni, event payload | Harita noktasından otomatik tesis doğrulaması yok |
| C09 | Manuel şehir/ilçe için harita merkezini çözme | **Cihaz:** `DeviceServices.kt` → Android Geocoder IO ve iOS CLGeocoder; izin istemeyen `rememberRegionCenter`, 12 saniye timeout/iptal; boş şehirde ağ isteği yok | Adres çözümleme servisi/bağlantısı başarısızsa Türkiye genel merkezi; gerçek cihaz testi |

## D. Ana akış ve akıllı eşleşme

Kaynaklar: `HomeScreen.kt`, `BackendMoreEventsButton.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| D01 | Canlı radar ve yakındaki maçlar | **Bağlı:** gerçek maç listesi/koordinat, seçili şehir sorgusu ve cursor sayfalama; preview örnekleri ayrı | GPS/geocoder ve büyük veri yük testi fiziksel cihazda doğrulanmalı |
| D02 | Branş, bölge ve ağ kapsamı filtreleri | **Bağlı:** canlı şehir kapsamı; kampüs/şirket görünürlüğü sunucuda onaylı üyelikten gelir; preview profil filtreleri korunur | Her görsel filtre ayrı kalıcı tercih değildir; kurumsal SSO yok |
| D03 | Acil oyuncu/eksik kadro kartı | **Bağlı:** gerçek maç/açık kontenjan; uygun il/ilçe ve branşa acil bildirim üreticisi | Tam kilometre yarıçapı/geospatial acil dağıtım ve gerçek push testi yok |
| D04 | Challenge özet kartı | **Bağlı:** takım ve teklifler; detay/takım geçişi | Kemer ve gerçek AI ayrı; kart yüksekliği sıralamayla eşit |
| D05 | Sıralama özet kartı | **Bağlı:** yerel sıra, MMR ve son 7 günde kesinleşen fark | Büyük sıralama listesi sayfalaması |
| D06 | Akıllı Eşleşme çekmecesi | **Bağlı:** kurallı skor, uygun oyun sayısı, en iyi maç, nedenler | Eğitilmiş AI/otomatik organizasyon yok |
| D07 | “Bu akşam / hafta sonu / acil / takım” ve sıfırla | Yerel filtre; seçilen gerçek maça geçer | Ayrı backend gerektirmez |
| D08 | Maç kartını açma / kısa maç dizini | **Bağlı:** G bölümündeki ayrıntı/katılım | Tüm karttan hata/boş veri cihaz testi |
| D09 | AutoSquad kadro penceresi | **Kısmi:** gerçek katılımcılar/açık yerler, detay ve sohbet geçişi | Otomatik dengeli kadro oluşturma yok |
| D10 | Akış sonunda Daha fazla maç göster / yükleniyor / hata | **Bağlı:** `BackendMoreEventsButton.kt`; `nextEventCursor` varsa `loadMoreEvents`, yeniden dene ve sayfaları birleştirme | Yalnız canlı modda; tüm diğer kataloglarda sınırsız sayfalama uygulanmış değil |

## E. Harita ekranı

Kaynaklar: `DiscoverMapScreen.kt`, `NativeMapView.kt`, `BackendMoreEventsButton.kt`, `DeviceServices.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| E01 | Harita pinleri ve seçili maç | **Bağlı + cihaz:** gerçek yaklaşık konum, native harita | Canlıda koordinatsız maça uydurma pin verilmez |
| E02 | Filtre/sıralama ve şehir değiştirme | **Bağlı:** canlı şehir filtresi ve sunucu keşif kapsamı; manuel bölgede geocoder; C bölümüne gider | Tüm kombinasyonların cihaz testi; her filtre kalıcı tercih değildir |
| E03 | Alt maç kartı / liste görünümü | **Bağlı:** aynı gerçek maç kaynağı ve liste sonunda cursor ile daha fazla maç | Harita carousel içinde ayrı sayfa yükleme düğmesi yok; diğer kataloglar tam sayfalanmış değil |
| E04 | Hızlı katıl / bekle / listeden ayrıl | **Bağlı:** event join/waitlist; hesap kapısı | Çok cihazda son yer testi gerçek ortamda yapılmalı |
| E05 | Haritadan Akıllı Eşleşme penceresi | D06 ile aynı ortak pencere | Ayrı AI servisi yok |
| E06 | Canlı haritanın boş/koordinatsız bölge merkezi | **Bağlı + cihaz:** seçili maç noktası, çözümlenen bölge, seçili bölgeyle uyumlu cihaz konumu veya Türkiye merkezi; şehir filtresi | Canlıda demo koordinatı üretilmez; preview eski merkez/pin davranışını korur |
| E07 | Harita liste görünümünde sonraki maç sayfası | **Bağlı:** ortak `BackendMoreEventsButton`; sunucu cursoru, hata ve tekrar durumu | Harita carousel içinde ayrı yükleme düğmesi yok |

## F. Şimdi Oyna ve yeni ilan akışı

Kaynak: `PlayNowSheet.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| F01 | Oyun arama ve uygun maçlar | **Bağlı:** gerçek maçlar, hızlı katılım | Keşif sayfalamasının tüm ekranlara yayılması |
| F02 | Açılan arama seçenekleri | Yerel branş/mesafe/mod/doğrulama/tarih/saat filtreleri | Filtre seçimi tek başına backend kaydı değildir |
| F03 | İlan adım 1: kategori ve ilan niyeti | **Bağlı:** açık maç/acil oyuncu/rakip/özel branş verisi | Otomatik organizatör/oyuncu atama yok |
| F04 | İlan adım 2: açıklama, rol, kapasite, süre, ücret | **Bağlı:** `event.create`, alan/kapsam/kapasite doğrulama | Fiziksel saha ücretini tahsil etmez |
| F05 | E-spor platformu, sunucu, oyun etiketi ve ses kanalı | **Bağlı:** ilan metadatası saklanır | Platform hesabı doğrulama, oyun API'si, sesli görüşme yok |
| F06 | Bölge ve haritadan buluşma noktası pencereleri | C01–C04/C08 ile ortak | Tesis rezervasyonuna otomatik dönüştürme yok |
| F07 | Gün, saat ve süre seçimi | C06/C07; sunucu gerçek başlangıç zamanını doğrular | Sürekli tekrar eden maç serisi yok |
| F08 | Şehir/kampüs/şirket kapsamı ve ağ seçimi | **Bağlı:** onaylı üyelik ve özel ağ erişimi | Kurumsal aidiyetin dış sistemden doğrulanması yok |
| F09 | Önizleme/özet ve yayınla | **Bağlı:** kalıcı maç, üyelik/sohbet ve tekil taahhüt | Form hata/yeniden deneme deneyimi cihazda sınanmalı |
| F10 | Token yetmiyorsa mağazaya geçiş | **Bağlı:** cüzdan/mağaza yönlendirmesi | Mağaza canlı kurulumu R bölümünde |
| F11 | Şimdi oyna şehir kapsamı | **Bağlı:** canlı arama seçili şehri filtreler; başka şehirdeki kişisel maçlar bu önerilere karışmaz | Preview örnekleri korunur |


## G. Maç ayrıntısı ve bütün alt işlemleri

Kaynaklar: `EventDetailSheet.kt`, `SubmitJoinRequestSheet.kt`, `ManageJoinRequestsSheet.kt`, `BackendEditingActions.kt`, `CheckInCodeActions.kt`, `EventDeviceActions.kt`, `InteractiveActionSheets.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| G01 | Başlık, branş, yer, zaman, ücret, açıklama, kontenjan | **Bağlı:** maç ayrıntısı ve bootstrap | Silinmiş/erişimi kaybolmuş açık ayrıntının tüm UI durumları |
| G02 | Match Score açılan açıklaması | **Bağlı:** zaman/bölge/seviye/güvenilirlik/tercih skorları | Eğitilmiş AI değildir; kapalı kişisel sinyaller nötrleştirilir |
| G03 | Kadro ve organizatör kartı | **Bağlı:** gerçek katılımcı/kaptan; ayrı kadro penceresi | Diğer sporcu anlık müsaitlik servisi yok |
| G04 | Katıl / maçtan ayrıl | **Bağlı:** uygunluk, kapasite, taahhüt ve iade | Canlı çok cihaz testi |
| G05 | Katılım başvuru penceresi: mevki/not | **Bağlı:** `event.request` | Başarısız işlem global tekrar penceresinden yürür |
| G06 | Başvurumu geri çek | **Bağlı:** `event.request.cancel` | — |
| G07 | Organizatör başvuru yönetim penceresi | **Bağlı:** liste, onay/ret, yetki kontrolü | Toplu/karmaşık kadro otomasyonu yok |
| G08 | Bekleme listesine katıl / ayrıl / açılan yeri kabul et | **Bağlı:** süreli teklif ve rezervasyon | Gerçek push ayarı bekliyor |
| G09 | Organizatöre DM / maç sohbeti | **Bağlı:** sohbet üyeliği, DM politikası, engel kontrolü | DM isteği için ayrı gelen kutusu yok |
| G10 | “Maç planını düzenle” penceresi | **Bağlı:** başlık/yer/not/zaman/kontenjan, açıldığı sürüm, değişiklik bildirimi | Resmi takım/turnuva maçının serbest düzenlemesi kapalı |
| G11 | Başlat onay penceresi | **Bağlı:** `event.transition`, rol/zaman/sürüm | — |
| G12 | Tamamla onay penceresi | **Bağlı:** durum geçişi; sonucu ayrıca bildirme | Tek tıklamayla doğrulanmış sonuç üretmez |
| G13 | İptal onay penceresi | **Bağlı:** durum/taahhüt kapatma ve bildirim | Ödenmiş fiziksel rezervasyon iadesi ayrı |
| G14 | Saha check-in penceresi | **Bağlı:** güncel imzalı kod, `event.checkin` | Geofence/cihaz bütünlüğü yok |
| G15 | Organizatör kodu oluştur / QR göster / kopyala | **Bağlı + yerel UI:** süreli kod, QR, pano | Sürekli otomatik yenilenen video tarayıcı yok |
| G16 | QR fotoğrafını çek/seç ve okunamadı penceresi | **Cihaz:** ortak kamera/galeri, gerçek QR çözümleme | Sahada fiziksel kamera testi |
| G17 | Paylaş / yol tarifi / cihaz takvimine ekle | **Cihaz:** native paylaşım/harita/takvim düzenleyicisi | Takvim izin/ret ve uygun uygulama yok senaryoları |
| G18 | Cihaz işlemi açılamadı penceresi | Yerel hata; alternatif elle işlem | Ayrı API gerekmez |
| G19 | Maçı bildir / şikayet penceresi | **Bağlı:** `report.create`; yetkili operasyon merkezinde inceleme | Otomatik yaptırım ve harici moderasyon servisi yok |
| G20 | Maç sonucu penceresi: skor/taraf/not | **Bağlı:** `result.submit`, check-in; bireysel maçta tam ve ayrık taraf seçimi, resmi takım/turnuva kadrosu koruması | Branşa özel set/raunt gibi gelişmiş skor şemaları yok |
| G21 | Skoru onayla / sonuca itiraz alanı | **Bağlı:** `result.confirm/dispute`; check-in, karşı taraf çoğunluğu ve 24 saatlik itiraz süresi; operatör karar penceresi var | Otomatik uyuşmazlık çözümü yok; yetkili insan incelemesi gerekir |
| G22 | Katılımcı değerlendirme penceresi | **Bağlı:** kişi bazlı fair play/tekrar oynama; kendini oylama engeli | Toplu otomatik değerlendirme yok |
| G23 | Sonuç penceresinde MMR durum kartı | **Bağlı:** `RatingAndMissionSheets.kt` içindeki `RatingStatusCard`; sunucudan onaylanan/gereken sayısı, bekleme/itiraz ve gerçek delta | Katılım/karşı taraf/branş eksikse MMR yok; müşteri puanı değiştiremez |
| G24 | MMR nasıl değişir? politika penceresi | **Bağlı:** `RatingPolicySheet`; config ratingPolicy, 1000 başlangıç, check-in, karşı taraf çoğunluğu, 24 saat, Elo K=24; denk rakipte +12/−12/0 örneği | Sosyal maç/itiraz/eksik katılım MMR sağlamaz; görev/Token/Plus puan satın almaz |
| G25 | Sonuç bildirirken ev/rakip taraflarını seçme | **Bağlı:** bireysel maçta tüm katılımcılar ayrık iki tarafa atanır; takım/turnuva tarafları sunucuda donuk kadroyla korunur | Ayrıntılı mevki bazlı otomatik dengeli kadro kurma yok |
| G26 | Organizatörün katılımcıyı kadrodan çıkarması ve onayı | **Bağlı:** event.member.remove; yalnız organizatör/admin, sürüm ve maç öncesi kontrol; tam taahhüt iadesi, sohbet erişimi kapanışı, sıradaki oyuncuya teklif | Resmi takım/turnuva kadrosu bu işlemle değiştirilemez |
| G27 | Çıkarılan katılımcılar ve yeniden izin verme | **Bağlı:** event.member.restore; başvuru/katılım/sıra engeli yalnız organizatörce kaldırılır | İzin otomatik katılım veya ücret oluşturmaz |
| G28 | Başvuru sonrası doğrudan katılım uzlaştırması | **Bağlı:** pending başvuru approved olur; iptal/ret ile kadrodaki kişinin taahhüdü tekrar çözülemez; tam kadroda tekrar katıl idempotent | — |
| G29 | Zaman ve katılım korumaları | **Bağlı:** başlamış maçtan sıradan ayrılma, bitmiş maçı başlatma ve no-show skor bildirme engelli; sonuçtan sonra yeni check-in kapalı | Operatör inceleme akışı korunur |
| G30 | Eksik sayfadaki maçın ayrıntısı ve gerçek katılımcıları | **Bağlı:** kutu/push bağlantısı eventDetails fallback; ayrıntı API gerçek katılımcı profillerini taşır | Silinen/yetkisiz maçta sunucu hatası gösterilir |


## H. Yarış — sıralama ve diğer sporcu profili

Kaynaklar: `CompetitionScreen.kt`, `CompetitionHighlights.kt`, `InteractiveActionSheets.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| H01 | Sıralama sekmesi; haftanın sporcusu; satırlar | **Bağlı:** gerçek rating/maç sayısı/son 7 günde kesinleşen fark | Lider listesi büyük veri sayfalaması; ayrı editoryal ödül servisi yok |
| H02 | Branş / ilçe-şehir-ulusal / konum seçicileri | Yerel filtre; gerçek sıralama verisi | Ayrı tercih kaydı şart değil |
| H03 | Sporcu profil penceresi: kimlik/kademe/istatistik | **Bağlı:** public profil; özel iletişim alanları dönmez | Anlık müsaitlik ve mesafe servisi yok |
| H04 | Son beş maç formu ve MMR farkı | **Bu tur bağlandı:** `GET /v1/players/:id?sport=...`, doğrulanmış ve görülebilir maçlar | Maç başına 8.x türü bireysel performans notu yok; mock örnekleri preview'da |
| H05 | Başarı rozetleri | **Bu tur bağlandı:** gerçekten kazanılmış progression rozetleri | MVP/namağlup gibi ayrı kurgusal ödül sistemleri yok |
| H06 | Birlikte maç sayısı ve uyum | **Bu tur bağlandı:** doğrulanmış ortak maç ve tekrar oynama geri bildirimi; iki tarafın tercihleri/engel kontrolü | Örnek yoksa puan uydurulmaz; gerçek AI tahmini değildir |
| H07 | Direkt mesaj / takıma davet | **Bağlı:** DM politikası, takım kaptanlığı ve davet | Takımsız kullanıcı için davet gönderilemez |
| H08 | Profilde “Meydan Oku” | **Kısmi:** takım sekmesine yönlendirir | Bağımsız birebir sporcu challenge ürünü yok |
| H09 | Profil ayrıntısı yüklenme/kapalı geçmiş/hata/tekrar | **Bu tur bağlandı:** gizlilik kontrollü boş/hata durumları | Fiziksel cihazda görsel doğrulama |
| H10 | Gerçek sporcu profil fotoğrafı | **Bağlı:** diğer sporcu ayrıntısı, maç/AutoSquad kadrosunda gerçek avatar upload kimliği ile yetkili medya okuma; fotoğraf yoksa baş harfler | Preview görünümü değişmez; fiziksel kamera/galeri turu ayrıca gerekir |


## I. Yarış — takım, kulüp ve kadro

Kaynaklar: `CreateTeamSheet.kt`, `TeamDetailSheet.kt`, `ApplyToTeamSheet.kt`, `ManageTeamRequestsSheet.kt`, `BackendManagementActions.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| I01 | Takımlar sekmesi ve kendi takım kartı | **Bağlı:** gerçek kadro/kaptan/istatistik | Tüm rol durumlarının cihaz testi |
| I02 | Kulüp kur penceresi: ad/branş/bölge/arma/renk/not/MMR/başvuru | **Bağlı:** `team.create`; renk kalıcılığı bu tur eklendi | Bölge seçenekleri statik; tam adres/şehir kataloğu genişletilmeli |
| I03 | Takım detay penceresi | **Bağlı:** doğrulanmış maçlardan kayıt/puan, kadro ve ilan | Gelişmiş kulüp başarı/yardımcı kaptan/yedek sistemi yok |
| I04 | Takım ilanını düzenleme penceresi | **Bağlı:** açıklama ve başvuruya açıklık | Her takım alanını düzenleyen kapsamlı form yok |
| I05 | Takıma başvuru penceresi | **Bağlı:** mevki/not, MMR/branş/üyelik denetimi | — |
| I06 | Başvurumu geri çek | **Bağlı:** `team.request.cancel` | — |
| I07 | Kaptanın başvuru yönetimi | **Bağlı:** onay/ret, gerçek üyelik ve sohbet | Gelişmiş rol atama yok |
| I08 | Gelen takım davetlerini kabul/ret | **Bağlı:** davet ve kadro üyeliği | Ayrı kalıcı davet merkezi yok; mevcut alanlar kullanılır |
| I09 | Takımdan ayrıl onay penceresi | **Bağlı:** üyelik/sohbet yetkisi | Kaptan önce devir yapmalı veya resmi taahhüdü olmayan takımı kapatmalı |
| I10 | Kaptanlık devri kişi seçme penceresi | **Bağlı:** yalnız gerçek üyeye devir | Yardımcı kaptan yetki matrisi yok |
| I11 | Kadrodan DM / rakibe Challenge | **Bağlı:** ilgili N/J akışları | — |
| I12 | Takım kadrosunu yönet ve çıkarma onayı | **Bağlı:** team.member.remove; kaptan yetkisi, aktif üyelik, çıkarılan kişinin sohbet erişimi kapanır | Kaptan kendini çıkaramaz; resmi kadro taahhüdü önce sonuçlandırılmalı |
| I13 | Çıkarılan takım üyesini yeniden davet | **Bağlı:** çıkarılan sporcu başvuruyla dönemiyor; kaptanın team.invite daveti yeniden katılım yolunu açar | Sporcu daveti kendisi kabul eder |
| I14 | Takımı kapatma onayı | **Bağlı:** team.archive; üyelik/davet/başvurular kapanır, bekleyen challenge taahhütleri asıl sahibine iade edilir | Aktif resmi kadro kaydı varken kapanmaz; geçmiş sonuçlar silinmez |
| I15 | Devir ve ayrılma doğrulaması | **Bağlı:** başka aktif üyeye kaptanlık devri; üye olmayan kişi ayrılamaz; arşivli takım yeni işlem alamaz | Yardımcı kaptan rol modeli yok |


## J. Takım Challenge pencereleri

Kaynaklar: `SendTeamChallengeSheet.kt`, `VenueChoiceField.kt`; ek görsel kartlar `NetworkSpotlightCards.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| J01 | Teklif gönder: rakip/zaman/saha/not/taahhüt | **Bağlı:** `challenge.create`, kaptanlık/kilit; `VenueChoiceField` canlı tesis kataloğu veya elle yer | Saha seçimi rezervasyon oluşturmaz; venue ID ile fiziksel rezervasyon bağlantısı yok |
| J02 | Maç sözleşmesi / teklif detay penceresi | **Bağlı:** kalıcı teklif ve durum | Hukuki sözleşme/imza servisi değildir |
| J03 | Kabul / reddet | **Bağlı:** kadro dondurma, gerçek maç ve sohbet, taahhüt çözme | Bahis/ödül havuzu servisi yok |
| J04 | Gönderilen teklifi geri çek onayı | **Bağlı:** `challenge.cancel` | — |
| J05 | Challenge'dan kaptana sohbet | **Bağlı:** DM politikasıyla | — |
| J06 | AI Match Scouter / şehir kemeri penceresi | **Eski / pasif:** açan aktif atama bulunmadı; örnek kaynak korunur | AI keşif, kemer sahipliği/maç sistemi yazılmadı |

## K. Turnuva, kupa ve lig pencereleri

Kaynaklar: `CreateTournamentSheet.kt`, `CompetitionScreen.kt`, `InteractiveActionSheets.kt`, `BackendManagementActions.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| K01 | Turnuvalar sekmesi / kupa kartları | **Bağlı:** gerçek katılım/kapasite/durum | Büyük liste sayfalaması |
| K02 | Kupanı oluştur penceresi | **Bağlı:** ad/branş/tarih açıklaması/iki desteklenen format/kapasite/MMR/doğrulama/ücret notu/şehir | Previewdaki İsviçre/grup gibi örnek metinler canlı format motoru sayılmaz |
| K03 | Özel branş, organizatör adı, platform ve sunucu/lobi | **Bu tur kalıcılık tamamlandı:** API/model aktarımı | Platform hesabı/organizatör kurumu doğrulama yok; bazı metadatanın ayrıntı gösterimi genişletilmeli |
| K04 | Kapasite ve format açıklaması | **Bağlı:** canlı form `KNOCKOUT` ve `ROUND_ROBIN`; eleme 2/4/8/16/32, lig 2/4/6/8/10/12/16 takım seçenekleri; sunucuda kapasite doğrulaması | İsviçre ve grup+eleme yok; lig API sınırı 2–16 takım |
| K05 | MMR/doğrulama katılım koşulu metni | **Bağlı:** yapılandırılmış `minRating` ve `verifiedOnly`; kayıtta ve fikstürden önce her kadro üyesinin branş/MMR/BASIC doğrulaması | Serbest açıklama ek kural motoru değildir; okul/şirket veya oyun hesabı doğrulaması yok |
| K06 | Para/Token ödül açıklaması | **Kısmi:** açıklama saklanır | Ödül fonlama, tahsilat, hakem ve dağıtım yok |
| K07 | Turnuva detay penceresi ve genel bilgiler | **Bağlı:** gerçek veri | — |
| K08 | Fikstür sekmesi | **Bağlı:** maçlar, taraflar, doğrulanmış skor | İsviçre/grup+eleme/ileri eşitlik bozma yok |
| K09 | Puanlar sekmesi | **Bağlı:** doğrulanmış sonuçlardan lig puanı | Gelişmiş branşa özgü lig kuralları yok |
| K10 | Kurallar sekmesi | **Kısmi:** format/kapasite ve yapılandırılmış MMR/telefon doğrulaması uygulanır; açıklamalar gösterilir | Gösterilen her serbest kural sunucu motoru tarafından uygulanmış değildir |
| K11 | AI Radar / Kurallı Özet sekmesi | **Kısmi:** canlıda gerçek puanların kurallı özeti; preview demo | Kazanma ihtimali/AI modeli yok |
| K12 | Kaydol / kaydı geri çek ve onay penceresi | **Bağlı:** kaptan, takım branşı, kapasite, mükerrer oyuncu ve donmuş kadro | Otomatik takım seçimi yerine çoklu takım seçimi UX'i genişletilmeli |
| K13 | Turnuvayı iptal et onay penceresi | **Bağlı:** başlamadan iptal ve organizatör kilidi iadesi | Başlamış turnuva için operatör akışı gerekir |
| K14 | Fikstür / sonraki tur planlama penceresi | **Bağlı:** zaman/saha, önceki sonuçlar, sunucu kura/eşleştirme | Birden fazla saha/hakem ve gelişmiş zaman planlayıcı yok |
| K15 | Ligi sonuçlandır | **Bağlı:** tüm sonuçlar doğrulanınca kapanış | Gerçek ödül dağıtımı yok |
| K16 | Kayıt/çekilme için kaptan takımını seçme penceresi | **Bağlı:** aynı branştaki kaptan takımları; çekilmede yalnız gerçekten kayıtlı takım seçenekleri | Yetki ve güncel turnuva durumu sunucuda tekrar denetlenir |
| K17 | Gelecekteki fikstürleri topluca iptal | **Bağlı:** hiç maç başlamamış running turnuva kapatılır, fikstürler ve üyelikler kapanır | Başlamış veya sonuçlanmış fikstür varsa moderasyon gerekir |
| K18 | Eleme skoru eşitlik bozucu kontrolü | **Bağlı:** KO beraberliği result.submit sırasında reddedilir; tek sonuç kaydı tüketilip sonraki tur kilitlenmez | Eşitlik bozucu dahil kazanan skor girilir; ayrıntılı penaltı set modeli yok |


## L. Ağ, topluluk ve seyahat

Kaynaklar: `CommunityAndVenueScreen.kt`, `NetworkSpotlightCards.kt`, `BackendManagementActions.kt`, `CreateCommunitySheet.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| L01 | Topluluklar sekmesi: tür/bölge/açıklama/üye sayısı | **Bağlı:** gerçek üyelikler; CITY/CAMPUS/COMPANY/CLUB/TRAVEL ve oluşturma girişi | Büyük topluluk kataloğu sayfalaması ve dış aidiyet doğrulaması yok |
| L02 | Yaklaşan topluluk etkinlik sayısı | **Bu tur bağlandı:** kullanıcının görebildiği yaklaşan maçlar | Sayı bootstrap kapsamıyla sınırlı; büyük veri aggregate servisi genişletilmeli |
| L03 | Açık topluluğa katıl / ayrıl | **Bağlı:** `community.join/leave` | Sahibi önce yönetimi devretmeli |
| L04 | Onaylı topluluk başvurusu / geri çekme | **Bağlı:** pending üyelik | — |
| L05 | Topluluk başvurularını onay/ret | **Bağlı:** yalnız sahibi; gerçek başvuranlar, düzenleme ve üye çıkarma pencereleri | Çok kademeli yardımcı yönetici/rol matrisi yok |
| L06 | Topluluk daveti kabul/ret | **Bağlı:** davetli üyelik | Bildirimden doğrudan topluluk detayı genişletilmeli |
| L07 | Üyeye davet gönder / yönetimi devret kişi penceresi | **Bağlı:** gerçek kullanıcı ve üyelik denetimi | Kurumsal yönetici doğrulaması yok |
| L08 | Topluluk oluşturma | **Bağlı:** `CreateCommunitySheet.kt`; ad/tür/katılım/branş/il-ilçe/açıklama → `community.create`; kurucu üyeliği | Kurumsal kimlik veya SSO doğrulaması yok; preview yerel örnek oluşturur |
| L09 | Kampüs / şirket erişimi | **Bağlı:** onaylı topluluk üyeliğiyle özel maça erişim; canlı Home/Map profil serbest metniyle yetkili maçı tekrar gizlemez | Okul/şirket e-postası ya da SSO aidiyet doğrulaması yok |
| L10 | Seyahat modu aç/kapat ve şehir seçimi | **Bağlı:** `discovery.save`; kalıcı pasaport şehrini değiştirmez | Seyahat için ayrı kurumsal üyelik/yerel rehber hizmeti yok |
| L11 | Topluluğu düzenle penceresi | **Bağlı:** `BackendManagementActions.kt`; ad/açıklama/katılım tipi, `community.update`, sahiplik ve expectedVersion | Tür/şehir/branş değiştiren kapsamlı yönetim veya çoklu yönetici yok |
| L12 | Topluluk üyeleri listesi penceresi | **Bağlı:** yalnız sahibi kendi topluluğunun gerçek üyelerini görür ve çıkarma hedefi seçer | Geniş üye arama/sayfalama ve yardımcı yönetici yetkileri yok |
| L13 | Topluluk üyesi çıkarma onayı | **Bağlı:** `community.member.remove`; üyelik ve gelecekteki özel maç erişimi kapanır, ilgili taahhütler çözülür | Geçmiş kayıtlar silinmez; kurumsal dizin/SSO ile senkronizasyon yok |
| L14 | Topluluk kapatma onayı | **Bağlı:** community.archive; üyelik/başvurular kapanır, gelecekteki özel maçlar iptal ve taahhütler iade edilir | Devam eden özel maç sonuçlanmalı |
| L15 | Topluluktan ayrılmada özel maç temizliği | **Bağlı:** gelecekteki özel kadro/başvuru/sıra/taahhüt temizlenir; çıkarılan kişi leave/join ile yönetici engelini aşamaz | Yönetici davetiyle yeniden katılım mümkün |
| L16 | Davetli üyeliğin onaylı topluluğa kabulü | **Bağlı:** yöneticinin daveti ayrıca bekleyen başvuru üretmeden kabul edilebilir; askıdaki hesap onaylanamaz | Kurumsal SSO/aidiyet doğrulaması ayrıca gerekir |


## M. Tesis, saat ve rezervasyon

Kaynaklar: `CommunityAndVenueScreen.kt`, `VenueSpotlightCard.kt`, `InteractiveActionSheets.kt`, `VenueReviewSheets.kt`, `VenueChoiceField.kt`, `OperationsSheets.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| M01 | Tesisler sekmesi: saha/branş/olanak/saat/fiyat | **Bağlı:** gerçek tesis/saat kataloğu; yayınlanmış yorumlardan puan ve yorum sayısı | Dış harita/tesis kataloğu veya koordinat doğrulama sağlayıcısı yok; gerçek katalog işletmeci girişi bekler |
| M02 | Müsait saat seçme | **Bağlı:** kapalı/dolu saat ve çakışma kontrolü | Canlı işletmeci veri girişi gerekir |
| M03 | Rezervasyon penceresi: tesis/saat/toplam | **Bağlı:** `booking.create`; fiyat sunucudan | Gerçek kart tahsilatı yok; tesiste ödeme |
| M04 | Ücreti bölüş seçimi / kişi başı hesap | **Kısmi:** paylaşım sayısı kaydedilir; toplam saha bedeli korunur | Ayrı kişilerden tahsilat/borç takibi yok |
| M05 | Oyun takviminde rezervasyon ve iptal | **Bağlı:** rezervasyon, iptal son tarihi/yetki; ödenmiş rezervasyona yetkili offline iade kaydı | Otomatik para transferi yok; tesiste yapılan iade operatörce kaydedilir |
| M06 | Tesis/saat oluşturma ve saati kapatma | **Bağlı:** `OperationsSheets.kt` işletmeci paneli; venue/slot oluşturma, çakışma denetimi ve saati kapatma | Tüm tesis alanlarını/olanakları düzenleyen kapsamlı form yok; gerçek veri girişi gerekir |
| M07 | Onay / ödeme kaydı / tamamla | **Bağlı:** işletmeci panelinden booking onayı, makbuz referanslı tesiste ödeme, saat bitince tamamlama ve offline iade kaydı | Gerçek kart tahsilatı/otomatik banka iadesi/kişi bazlı bölüşüm sağlayıcısı yok |
| M08 | Tesis değerlendirmeleri penceresi | **Bağlı:** `VenueReviewSheets.kt` içindeki `VenueReviewsSheet`; GET `/v1/venues/:id/reviews`, yayınlanan yorumlar ve kendi bekleyen yorumu, yükleniyor/boş/hata/tekrar | Liste son 50 kayıtla sınırlı; tam yorum cursor sayfalaması yok; preview örnek yorumdur |
| M09 | Tamamlanan rezervasyonu değerlendir penceresi | **Bağlı:** `VenueReviewEditorSheet`; 1–5 puan/not, `venue.review.save`, rezervasyon sahibi/tamamlandı koşulu, tek yorum ve sürüm denetimi | Yayın moderatör onayı bekler; otomatik içerik taraması yok |
| M10 | Değerlendirmemi kaldır onayı | **Bağlı:** `venue.review.delete`; yazar/admin yetkisi, yorum ve puanı yayından kaldırma | Eski puanın toplamdan düşmesi sunucu verisinin yenilenmesiyle görünür |
| M11 | Tesis yorumunu bildir penceresi | **Bağlı:** `report.create` hedefi `venue_review`, operatör kuyruğu ve kaldırma kararı | Otomatik yaptırım yok |
| M12 | Tesis yönetimi penceresi ve yenile/hata durumu | **Bağlı:** `OperationsSheets.kt` içindeki `VenueOperatorSheet`; Hesap/Tesisler girişleri, GET `/v1/operator/venues`, admin/venue_owner ve tesis sahipliği | Tesis/saat/rezervasyon sorguları sınırlı listelerdir; ileri operasyon sayfalaması yok |
| M13 | Tesis ekle penceresi | **Bağlı:** ad/il/ilçe/branş/saha sayısı/iptal sınırı → `venue.create`; yetki sunucuda doğrulanır | Form tam adres/koordinat/fotoğraf/olanak ve dış katalog doğrulaması sağlamaz |
| M14 | Müsait saat ekle penceresi | **Bağlı:** kort/tarih/saat/süre/toplam TL → `venue.slot.create`; gerçek epoch, fiyat ve çakışma denetimi | Çoklu/tekrarlı slot takvimi ve otomatik kaynak planlayıcı yok |
| M15 | Tesiste yapılan tam iadeyi kaydet onayı | **Bağlı:** `booking.refund.record`, iade referansı, yalnız ödenmiş onaylı/tamamlanmış rezervasyon; ödeme refunded, rezervasyon cancelled ve bildirim/audit | Bu işlem para göndermez; gerçek offline iade işletmeci tarafından önceden yapılmalıdır; kısmi iade yok |
| M16 | Ortak gerçek tesis / başka saha seçicisi | **Bağlı:** `VenueChoiceField.kt`; Challenge ve anı oluştururken canlı katalog/elle yer; anıda şehir filtresi; preview eski öneriler | Yer metni saklanır; venue ID ile rezervasyon/harita konumu otomatik bağlanmaz |

## N. Sohbet ekranı ve bütün alt pencereleri

Kaynak: `ChatHubScreen.kt`; yönetim: `BackendManagementActions.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| N01 | Sohbet listesi: DM/maç/takım, okunmamış sayısı | **Bağlı:** kalıcı threads/messages ve üyelik | Büyük sohbet listesi sayfalaması |
| N02 | Sohbet türü/branş filtre açılır menüleri | Yerel UI; aynı gerçek veriyi filtreler | Ayrı backend gerekmez |
| N03 | Sohbet ayrıntısı ve mesaj balonları | **Bağlı:** üyelik ve iki yönlü engel kontrolü | Realtime/WebSocket, yazıyor/çevrimiçi bilgisi yok |
| N04 | Metin gönder | **Bağlı:** `chat.send`, kalıcı kayıt ve bildirim | Kalıcı çevrimdışı gönderim kuyruğu yok; hata tekrar akışı var |
| N05 | Fotoğraf seç/çek ve gönder | **Bağlı + cihaz:** özel medya ve mesaj | Ses/video/dosya eki servisi yok |
| N06 | Fotoğraf balonunu büyütme | O bölümündeki ortak fotoğraf/anı detay penceresi | Salt özel fotoğraf bir açık vitrin paylaşımı sayılmaz |
| N07 | Önceki mesajları yükle | **Bağlı:** cursor sayfalama; polling eski mesajları silmez | Çok büyük geçmişte bellek sınırı geliştirilmeli |
| N08 | Sohbet açıldığında okundu | **Bağlı:** `chat.read` | Her mesaj için ayrı teslim/okundu işareti yok |
| N09 | Sohbet bilgi penceresi | **Bağlı:** tür/üye sayısı, sessiz/engel/bildir | Ayrıntılı üye yönetimi, sohbetten mesaj silme/düzenleme yok |
| N10 | Sessize al / aç | **Bağlı:** `chat.mute` ve push tercihine uyma | — |
| N11 | DM engelle / kaldır | **Bağlı:** iki yönlü engel; özel mesaj/fotoğraf erişimi | Uygulama genelinde tüm engellenen kişileri yöneten ayrı kişi rehberi yok |
| N12 | Sohbeti bildir | **Bağlı:** rapor kaydı ve yetkili operasyon kuyruğu | Otomatik yaptırım yok |
| N13 | DM başlatma | **Bağlı:** karşı tarafın politikası/ortak tamamlanmış etkinlik | Ayrı mesaj isteği onay kutusu yok |
| N14 | Aktif sohbette yeni mesajların okundu durumu | **Bağlı:** görünür, kilitsiz odada yeni unread geldiğinde chat.read; oda dışındaki mesajlar okunmuş sayılmaz | Polling kullanılır; Realtime ayrı |
| N15 | Kendi engeli ile karşı tarafın erişim engelini ayırma | **Bağlı:** Engeli kaldır yalnız kullanıcının kendi blocks kaydı için; hesap engel listesi aynı kapsamda | Başkasının koyduğu engel kaldırılamaz |
| N16 | Özel sohbet fotoğrafı ayrıntı işlemleri | **Bağlı:** fotoğraf görüntüleme/iletme sürer; memory.like/delete yalnız gerçek vitrin anı kimliğine uygulanır | Mesaj fotoğrafı vitrin anısı sayılmaz |
| N17 | Ayrılan kullanıcıya bildirim erişim denetimi | **Bağlı:** yeni sohbet bildirimi üyeliği tekrar doğrular; kuyruktaki sohbet/katılım/hatırlatma/sıra pushları gönderim öncesi güncel erişimi denetler | Push sağlayıcı hesapları ve gerçek cihaz testi bekler |


## O. Anı vitrini, fotoğraf ve paylaşım pencereleri

Kaynak: `ChatHubScreen.kt`; görseller: `BackendPhoto.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| O01 | Vitrin/mini hikaye/öne çıkan/grid ve filtreler | **Bağlı:** onaylı anılar, yazarın bekleyen anısı; yerel filtre | Tam feed sayfalaması ve gerçek mesafe/coğrafi keşif genişletilmeli |
| O02 | Saha anısı oluştur penceresi | **Bağlı:** fotoğraf/açıklama/branş; canlıda şehre göre gerçek tesis kataloğu veya elle yer, previewda eski örnek seçenekler | Seçilen yer metindir; otomatik rezervasyon/koordinat doğrulaması yok |
| O03 | Skor etiketi ve sahne teması | **Bu tur bağlandı:** doğrulanmış alan sınırlarıyla kaydet/geri yükle | Etiket serbest kullanıcı metnidir, resmi sonuç doğrulaması değildir |
| O04 | Kamera/galeri kaynak penceresi | **Cihaz:** ortak seçici | İzinler V bölümünde |
| O05 | Yalnız vitrine yayınla | **Bağlı:** `memory.create`, inceleme kuyruğu | Otomatik içerik taraması yok |
| O06 | Vitrin + sohbet hedefi seçimi | **Bağlı:** tek atomik komutla anı ve mesaj; tekrar istekte çift kayıt yok | Açık vitrine çıkış moderasyon bekler |
| O07 | Anı/fotoğraf detay penceresi | **Bağlı:** görsel, yazar, açıklama, beğeni, sohbet hedefi | Özel sohbet fotoğrafına kaynak erişimi gerekir |
| O08 | Beğen / geri al | **Bağlı:** tekil beğeni ve engel kontrolü | — |
| O09 | Sohbete ilet | **Bağlı:** onaylı açık anı veya erişilebilir kaynak sohbet; özel dosya ID'siyle erişim engeli | Medya yükleme/silme çok cihaz testi |
| O10 | Anıyı sil onay penceresi | **Bağlı:** sahip/yetki kontrolü, vitrinden kaldırma | Kaynak mesaj kopyasının da silinmesi ayrı ürün kararı |
| O11 | Anıyı bildir | **Bağlı:** report kaydı; moderasyon panelinde inceleme ve içerik kaldırma | Otomatik içerik taraması yok |
| O12 | Görsel yükleniyor/hata/yeniden dene | **Bağlı:** yetkili medya isteği ve önbellek | Zayıf ağ/büyük liste cihaz testi |

## P. Sporcu Pasaportu ekranı ve alt pencereleri

Kaynaklar: `SportsPassportScreen.kt`, `AthleteQrPassSheet.kt`, `InteractiveActionSheets.kt`, `RatingAndMissionSheets.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| P01 | Misafir pasaport görünümü ve avantaj kartları | Yerel tanıtım; giriş penceresine gider | Tanıtım metni tamamlanmış tüm avantajların kanıtı değildir |
| P02 | Sporcu kartı: fotoğraf/ad/bölge/MMR/güvenilirlik/Plus çerçeve | **Bağlı:** gerçek profil, fotoğraf ve aktif mağaza hakkı | Gerçek hak/restore cihaz testi |
| P03 | Profil düzenle penceresi: kimlik/spor/iletişim bölümleri | **Bağlı:** ad/bölge/bio/doğum yılı/tercih/iletişim; sürüm kontrolü | Telefon formdan doğrulanmış sayı olarak değiştirilemez; OTP gerekir |
| P04 | Profil fotoğrafı değiştir | **Bağlı + cihaz:** yükle/kaydet; fotoğraf değişince onay korunmaz | Kamera gerçek cihaz testi |
| P05 | Branşlarım / tümünü göster / yeni branş seç | **Bağlı:** profil skills; sunucu rating değerlerini korur | Ayrı spor platformu hesabı eşlemesi yok |
| P06 | Branş rol/seviye düzenleme penceresi | **Bağlı:** `profile.save` | Seviye kullanıcı beyanı |
| P07 | Yetenek/MMR ayrıntı diyaloğu | **Bağlı:** MMR, kademe, kazanma oranı, doğrulanmış maç ve ortak MMR politika penceresi | Bireysel 8.x performans ölçümü yok |
| P08 | Yerel sıra | **Bu tur bağlandı:** aynı il/ilçe ve görünürlük politikasından hesap | Büyük ölçek sayfalama/yük doğrulaması |
| P09 | Rozetler ve rozet detay diyaloğu | **Bağlı:** sekiz rozetin tümü doğrulanmış kayıtlarla hesaplanır; b3 ilk kez katılan sporcuları ağırlayan organizatör, b5 şampiyon kadroda katılım, b8 acil ilanda gerçek katılım | b3 harici referans linki değildir; ilk maç organizasyonu kuralı canlı açıklamada belirtilir |
| P10 | Haftalık görev kartı ve ilerleme | **Bağlı:** gerçek üç görev/tekil Token ödülü; `MissionsSheet` ile tüm görevler, ilerleme ve ödül yüklendi durumu | Yeni görev türleri ayrı ürün kuralı ister; görev/Token/Plus MMR kazandırmaz |
| P11 | Güvenilirlik geçmişini aç/kapat | **Bağlı:** gerçek check-in/sonuç kayıtları | No-show politikası fiziksel senaryolarla doğrulanmalı |
| P12 | Son maçlar / tümünü göster | **Bağlı:** kendi doğrulanmış maç/geçmiş verisi | Sınırsız geçmiş sayfalaması |
| P13 | Dijital sporcu kartı/QR penceresi ve yenileme | **Bağlı:** `passport.code`, süreli doğrulama API'si | Resmi kimlik/üyelik kartı değildir |
| P14 | İletişimi doğrula / fotoğrafı doğrula | S bölümündeki ortak pencereler | Fotoğraf canlılık/kimlik doğrulaması değildir |
| P15 | Oyun takvimim / hesap ayarları / cüzdan / çıkış | İlgili Q/S/R akışlarına bağlı | — |
| P16 | Tüm haftalık görevler penceresi | **Bağlı:** `RatingAndMissionSheets.kt` içindeki `MissionsSheet`; üç farklı rakip +15 TKN, tekrar oynama +10 TKN, iki check-in +10 TKN; ilerleme/ödül yüklendi | Yalnız doğrulanmış haftalık maçlardan hesaplanır; ödül sunucuda haftalık tekil anahtarla otomatik verilir |
| P17 | Pasaporttan MMR kuralını açma | **Bağlı:** branş ayrıntısı ve diğer sporcu profilinden ortak `RatingPolicyButton`/G24 penceresi | İstemci bilgi gösterir; sunucu puanını düzenleme aracı değildir |

## Q. Oyun takvimim, başvurular ve sahalar penceresi

Kaynak: `AccountAndActivitySheets.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| Q01 | Maçlarım sekmesi | **Bağlı:** katıldığı/organize ettiği maçlar, ayrıntıya geçiş | Çok eski kayıtların sayfalaması |
| Q02 | Başvurular sekmesi | **Bağlı:** bekleme listesi ve kendi bekleyen başvuruları | Ayrı birleşik tüm davet merkezi yok |
| Q03 | Sahalar sekmesi | **Bağlı:** gerçek fiziksel rezervasyon/ödeme durumu | İşletmeci operasyon ekranı ayrı |
| Q04 | Rezervasyonu iptal onay penceresi | **Bağlı:** süre ve ödeme denetimi; ödeme yapılmışsa işletmecinin offline iade kaydı gerekir | Kullanıcı düğmesi otomatik para iadesi yapmaz; gerçek transfer sağlayıcısı yok |
| Q05 | Tamamlanan saha rezervasyonundan yorum düzenleyiciyi açma | **Bağlı:** kendi tamamlanmış rezervasyonundan M09; kaydedilmiş yorumu güncelleme/kaldırma | Tamamlanmamış veya başkasının rezervasyonuna yorum yazılamaz |

## R. Cüzdan, Token, Plus ve mağaza pencereleri

Kaynaklar: `WalletAndPaymentSheet.kt`, `TokenTopUpCardSheet.kt`, `MatchoraWalletCard.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| R01 | Cüzdan kartı: bakiye/kilit/kullanıcı | **Bağlı:** sunucu defteri/hold | İstemci sahte bakiye vermez |
| R02 | Token paketleri sekmesi ve paket seçimi | **Servis bekliyor:** RevenueCat native ürün/fiyat kataloğu | Mağaza ürünleri/anahtarları; sandbox testi |
| R03 | Native satın alma penceresi | **Servis bekliyor:** StoreKit/Play üzerinden SDK; sunucu doğrulama | Gerçek satın alma/iptal/bekleyen ödeme/iade testleri |
| R04 | Yetersiz bakiye/top-up penceresi | Aynı `TokenWalletSheet` bileşenini açar; ayrı kredi kartı backend'i yok | Mağaza kurulumu |
| R05 | Hareketler sekmesi: tümü/yükleme/kilit | **Bağlı:** kalıcı ledger ve tekil işlem anahtarı | Eski hareketlerin sayfalaması |
| R06 | Plus aylık/yıllık plan ve avantajlar | **Kısmi + servis bekliyor:** aktif hak, altın çerçeve, ücretli dönemlerden aylık Token | VIP filtre/öncelik/komisyon avantajlarının tamamı yazılmadı |
| R07 | Satın alımları geri yükle | **Servis bekliyor:** native restore + sunucu sync | Transfer ve bazı iade/hesap değişimlerinin otomatik çözümü |
| R08 | Üyeliği yönet | **Cihaz:** mağaza abonelik sayfasını açar | Gerçek abonelik hesabıyla doğrulama |
| R09 | Mağaza hata/bilgi penceresi | **Bağlı:** ürün/iptal/hata durumunu açık gösterir | Tüm mağaza hata kodlarının cihaz testi |
| R10 | Satış kapatma bayrağı | **Bu tur bağlandı:** admin billing bayrağı config'i kapatır; satın alma öncesi yenilenir | Açılmış mağaza işlemi geriye alınmaz; ödenmiş hakların sync/webhook'u açık kalır |
| R11 | Privacy/Terms bağlantıları | U bölümündeki ortak bağlantılar | Gerçek URL'ler |

## S. Hesap, gizlilik, doğrulama ve destek

Kaynaklar: `AccountAndActivitySheets.kt`, `BackendManagementActions.kt`, `OperationsSheets.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| S01 | Hesap/Gizlilik sekmesi | **Bağlı:** sıralama, güvenilirlik, puan geçmişi ve eşleşme sinyali tercihleri | Gizli/bilinmeyen bazı sayılar diğer kartlarda 0 görünebilir; görünüm tutarlılığı genişletilmeli |
| S02 | DM politikası açılır menüsü | **Bağlı:** herkes/ortak etkinlik/kapalı sunucuda uygulanır | Ayrı DM istek onay sistemi yok |
| S03 | Engellenen sohbetler ve kaldır | **Bağlı:** iki yönlü kullanıcı engeli | Sohbeti olmayan tüm engelli kişilerin ayrı listesi yok |
| S04 | Bildirim tercihleri sekmesi ve OS izin düğmesi | **Bağlı + cihaz:** tercih kaydı ve izin kontrolü | Bildirim üreticileri T bölümünde |
| S05 | Destek formu: konu/başlık/açıklama | **Bağlı:** `support.create`, kullanıcıya özel kayıt ve operasyon merkezinden yanıt | Karşılıklı canlı destek konuşması/ek dosya yok |
| S06 | Talepler ve şikayetler listesi | **Bu tur bağlandı:** durum ve destek yanıtı; yalnız talep sahibine döner | Karşılıklı mesaj dizisi/ek dosyalı destek konuşması yok |
| S07 | İletişim doğrulama penceresi: numara/kod/sonuç | **Bağlı:** mevcut hesaba bağlı OTP, numara çakışması/deneme kontrolü | Gerçek SMS; WhatsApp yok |
| S08 | Fotoğraf doğrulama penceresi: kaynak/seç/gönder/sonuç | **Bağlı:** güncel profil fotoğrafı ve operasyon merkezinde manuel onay/ret; eski fotoğraf talebini kapatma | Otomatik canlılık/kimlik yüz eşleme yok; yetkili incelemeci ve cihaz testi gerekir |
| S09 | Kendi verilerimi dışa aktar | **Bağlı + cihaz:** kendi JSON verisi, native paylaşım | Toplu medya arşivi yok |
| S10 | Açık oturumlar penceresi | **Bağlı:** oturum listele ve diğerini iptal et | Cihaz adı/oturum coğrafyası kaydı yok |
| S11 | Oturumu kapat | **Bağlı:** sunucu revoke ve yerel özel veri temizleme | Apple/Google hesabından çıkış/erişim iptali ayrı |
| S12 | Hesap silme onay penceresi | **Bağlı:** gecikmeli talep, redaksiyon/medya/üyelik temizleme | Dış Auth/mağaza/ödenmiş rezervasyon/tesis sahipliği uzlaştırması tamamlanmadı |
| S13 | Silme talebini geri çek | **Bağlı:** sonlandırılmadan iptal | Gerçek takvim/işçi servis doğrulaması |
| S14 | Hesap işlemi hata penceresi | Yerel hata; export/session/deletion API hatası | — |
| S15 | Şikayet/itiraz neden ve not penceresi | **Bağlı:** event/thread/memory/user/team/venue_review raporu; operatör UI ve karar kayıtları | Otomatik yaptırım/otomatik içerik taraması yok |
| S16 | Ortak backend yönetim hata/tekrar penceresi | **Bağlı:** kalıcı işlem anahtarıyla tekrar | Uygulama kapanınca kalan işlem kuyruğu yok |
| S17 | Hesap ayarlarından Operasyon merkezi girişi | **Bağlı:** yalnız admin rolüne gösterilir; `OperationsSheets.kt` ve rol kontrollü admin inbox/komutlar | İlk admin hesabının güvenli kurulumu ve yetkili personel ataması gerekir |

## T. Bildirim kutusu ve bütün tercihler

Kaynak: `NotificationsSheet.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| T01 | Bildirim listesi, okunmamış öncelik/başlık/açıklama | **Bağlı:** kalıcı notifications/outbox | Eski bildirim sayfalaması |
| T02 | Bildirime tıkla / hepsini okundu yap | **Bağlı:** `notification.read`, hedefe geçiş | Bazı hedeflerin doğrudan ayrıntı yönlendirmesi genişletilmeli |
| T03 | Açılan bildirim tercihleri bölümü | **Bağlı:** `preferences.save` | Tercihin saklanması ilgili üretici işinin varlığını kanıtlamaz |
| T04 | Maç hatırlatma / Challenge / bekleme / sosyal | **Bağlı:** mevcut olaylardan bildirim ve hatırlatma; outbox tercih/engel/üyelik denetimleri | APNs/FCM hesap/anahtarları, worker yayını ve cihaz testi |
| T05 | Sessiz saat / günlük kota / acil mesafe / kategoriler | **Bağlı:** saat ve kota seçicileri; güncel yaklaşık konum varken gerçek kilometre yarıçapı, konum yoksa seçilen il/ilçe; konum 15 dakika sonra silinir | Gerçek APNs/FCM cihaz teslimi ve üretim worker kurulumu |
| T06 | Haftalık özet / rematch / kampanya tercihleri | **Bağlı:** haftalık doğrulanmış katılım/MMR özeti, karşılıklı tekrar oynama önerisi; pazarlama iznine bağlı admin duyurusu | Üreticilerin çalışması worker yayınına bağlı; kampanya takvimi/segment editörü yok |
| T07 | Bildirim izni sonucu ve cihaz ayarları penceresi | **Cihaz:** yerel izin/ayar akışı | Gerçek push; foreground/background/uygulama kapalı testi |
| T08 | Acil oyuncu çağrısı üreticisi | **Bağlı:** worker üç saat içindeki acil/açık maçlarda aynı il/ilçe/branş, güvenilirlik/doğrulama, ağ üyeliği/engel/katılım kontrolleriyle tekil bildirim üretir | Kilometre yarıçapı uygulanmıyor; canlı worker ve APNs/FCM kurulumu gerekir |
| T09 | Haftalık spor özeti üreticisi | **Bağlı:** önceki haftanın doğrulanmış check-in sayısı ve MMR toplamı; haftalık tekil bildirim ve tercih denetimi | Aktivitesi olmayan kullanıcıya özet üretmez; çalışan worker ve push servisleri gerekir |
| T10 | Yeniden oyna önerisi üreticisi | **Bağlı:** karşılıklı tekrar oynama geri bildirimi, doğrulanmış geçmiş, iki kullanıcının tercihi ve engel kontrolü; keşfe yönlendirme | Kendiliğinden DM göndermez veya maça katmaz; gerçek zamanlı uygunluk eşleştirmesi yok |
| T11 | Yönetici duyuru oluşturma alanı | **Bağlı:** `ModerationSheet` duyuru bölümü; başlık/metin/isteğe bağlı il, `admin.announcement.send`; yalnız marketing=true aktif hesaplara | Admin başına 24 saatlik pencerede bir duyuru; zamanlı kampanya/ileri segmentasyon yok |
| T12 | Duyuruyu gözden geçir / gönder onayı | **Bağlı:** `OperatorActionDialog` başlık/metin/bölgeyi gösterir; yetki/rate limit/tekil işlem anahtarı ve audit | Gerçek gönderim worker ve APNs/FCM sağlayıcı kurulumuna bağlı |
| T13 | Duyuru geçmişi / alıcı sayısı | **Bağlı:** admin inbox son duyuru auditleri ve alıcı sayısı | Teslim/açılma pazarlama analitiği veya kampanya ayrıntı paneli yok |

## U. Yasal bağlantılar, pasif akışlar ve API'si olmayan ürünler

Kaynaklar: `AppLinks.kt`, `CreateEventAndChallengeSheets.kt`, `SendTeamChallengeSheet.kt`.

| ID | Ekran / pencere / özellikte ne var? | Backend / cihaz bağlantısı ve durum | Yazılmayan / bekleyen |
|---|---|---|---|
| U01 | Privacy Policy / Terms of Use düğmeleri | **Bağlı:** config'den HTTPS URL; eksikse açıklama penceresi | Gerçek metin/URL/sürüm |
| U02 | Topluluk kuralları / hesap silme bilgisi / destek bağlantısı | **Bağlı:** URL veya destek e-postası; açılamadı/eksik durumları | Gerçek adresler |
| U03 | Yasal belgeleri incele onay penceresi | **Bağlı:** sürümlü `legal.accept` | Gerçek belgeler; hukuki değerlendirme |
| U04 | Eski `CreateEventSheet` | **Eski / pasif:** aktif açan atama bulunmadı; kaynak duruyor | Form gerçek başlangıç zamanı üretmiyor; açılacaksa F akışına taşınmalı |
| U05 | Eski `TeamChallengeSheet` | **Eski / pasif:** aktif çağrısı bulunmadı; örnek havuz/hakem metinleri duruyor | Havuz, bahis ve hakem tahsilatı backend'i yok |
| U06 | AI Scouter / kemer | **Eski / pasif / yok:** J06 | Ayrı ürün iş kuralları ve servisi yok |
| U07 | Birebir sporcu Challenge, referans, gelişmiş AutoSquad | **Yok:** ilgili görsel vaat/yan kaynak tamamlanmış ürün sayılmaz | Ayrı backend/ürün akışları |

## V. Sistem pencereleri ve KMP cihaz bağlantıları

Kaynaklar: `PhotoPicker.kt`, `DeviceServices.kt`, `CheckInCodeActions.kt`, `EventDeviceActions.kt`, Android/iOS `actual` dosyaları ve `iosApp/iOSApp.swift`.

| ID | Sistem penceresi / özellik | Yapılan | Bekleyen |
|---|---|---|---|
| V01 | Kamera / Galeri seçim penceresi | Profil, sohbet, anı, doğrulama ve QR aynı seçiciyi kullanır | Gerçek cihaz |
| V02 | Android kamera izni ve kamera uygulaması | Permission + TakePicture + özel FileProvider | Ret/iptal/kalıcı ret/geri dönüş |
| V03 | iOS kamera izni ve kamera penceresi | AVFoundation kontrolü + UIImagePicker | Fiziksel iPhone |
| V04 | Android Photo Picker / iOS PHPicker | Yalnız seçilen görsele erişim; tüm galeriyi okuma şart değil | Büyük/bozuk/iCloud görsel senaryoları |
| V05 | Fotoğraf erişim hatası / ayarlar penceresi | Hata ve native uygulama ayarına geçiş | Tüm OS sürümleri |
| V06 | Android/iOS konum izni | Ön plan coarse/fine veya WhenInUse, zaman aşımı, geocoder | Fiziksel/azaltılmış konum |
| V07 | Android/iOS bildirim izni | Sürüm kontrollü izin, kanal/APNs kayıt/FCM token | Sağlayıcı anahtarı ve cihaz push testi |
| V08 | Sistem paylaşım penceresi | Maç daveti ve hesap JSON dışa aktarımı | Hedef uygulama/iptal testi |
| V09 | Harita uygulamasında yol tarifi | Yer/adres/koordinatla native açılış | Uygun uygulama yok durumu |
| V10 | Cihaz takvim düzenleyicisi / iOS 16 izni | Kullanıcı onayıyla etkinlik ekleme; ret/ayar yolu | Minimum OS/cihaz testi |
| V11 | Apple/Google sistem auth penceresi | PKCE ve güvenli callback | Sağlayıcı kurulumu |
| V12 | App Store / Google Play satın alma ve biyometri pencereleri | RevenueCat üzerinden mağaza yönetir | Gerçek ürün/sandbox testi |
| V13 | Oturum saklama | Android Keystore/AES-GCM; iOS Keychain | Gerçek cihaz imzası, yazma/yenileme/restore testi |
| V14 | İstenmeyen izinler | Mikrofon/kişiler/Bluetooth/arka plan konumu eklenmedi; mevcut ürün bunları kullanmıyor | Sesli görüşme gibi yeni ürün eklenirse ayrıca tasarlanmalı |
| V15 | Manuel bölge için izin istemeden adres geocoding | **Cihaz:** Android IO/iptal ve iOS CLGeocoder iptal; bölge değişince eski sonuç kullanılmaz, timeout, boş şehirde istek yok | Çevrimdışı/servis hatasında sonuç olmayabilir; fiziksel cihaz/OS testi gerekir |

## W. Yalnız görsel olan ortak bileşenler

Bu bileşenlerin kendi kayıt/komut backend'i gerekmiyor; aldıkları verinin ve düğme callback'inin sorumluluğu yukarıdaki ekranlarda. Bunları “unutulan backend” olarak saymak yanlış olur.

| ID | Dosya / bileşen | İşlev | Durum |
|---|---|---|---|
| W01 | `CommonUi.kt` | Kart, alt kart, başlık, etiket, uyum rozeti, chip, düğme, segment, top bar, toast | Yerel UI; callback'ler ilgili akışa bağlı |
| W02 | `PremiumUi.kt` | Premium yüzey/basma efekti, pencere başlığı/tutamacı, input, dropdown, aksiyon satırı, alan renkleri | Yerel UI; dropdown penceresi seçimi form kaydına aktarır |
| W03 | `CompetitionHighlights.kt` | Sporcu ödül/Scouter/kupa/klüp görsel kartları | Görsel kaynak; aktif bağlantı ilgili Yarış akışında, Scouter pasif |
| W04 | `NetworkSpotlightCards.kt` | Topluluk/sıralama/turnuva/takım/Challenge kartları | Aynı gerçek modelleri ve callback'leri kullanır |
| W05 | `VenueSpotlightCard.kt` | Tesis/olanak/saat kartı | M bölümüne bağlı |
| W06 | `MatchoraWalletCard.kt` | Cüzdan kartının çizimi | R bölümündeki gerçek bakiye/hold ile |
| W07 | `BackendPhoto.kt` | Yetkili görsel yükleme/önbellek/hata görünümü | İzin kontrollü medya API'sine bağlı |
| W08 | `DockCutoutShape.kt` | Alt dock geometrisi | Backend gerekmez |
| W09 | `SportsFlowBackground.kt` | Spor şekilleri/arka plan çizimi | Backend gerekmez |
| W10 | `Theme.kt` | Renk, font, ortak tema | Backend gerekmez |
| W11 | App dock/FloatingPlayButton/DockNavItem | Ortak gezinme | A bölümündeki yönlendirme |

## X. Sunucu işleri ve yetkili operasyon pencereleri

Kaynaklar: `OperationsSheets.kt`, `RatingAndMissionSheets.kt`, `backend/src/operations.mjs`, `rating.mjs`, `engagement.mjs`, `jobs.mjs`.

| ID | Alan | Yazılan | Yazılmayan / gereken |
|---|---|---|---|
| X01 | Moderasyon | **Bağlı:** `OperationsSheets.kt` operasyon merkezi; rapor/sonuç/fotoğraf/anı/tesis yorumu kararları ve admin inbox | Yetkili personel/ilk admin hesabı, gerçek operasyon ve otomatik tarama ayrı |
| X02 | Tesis işletmeciliği | **Bağlı:** normal kullanıcı başvurusu, admin onayı/yayını ve rol; tesis/saat düzenleme/kapatma/yeniden açma, kort çakışması/kapasitesi, rezervasyon ve tesiste ödeme/iade | İşletme yetkisini doğrulayacak insan; gerçek tahsilat sağlayıcısı yok |
| X03 | Medya | Gerçek decode/resize/metadata temizleme, özel ACL, kota, orphan temizliği/silme kuyruğu | Bulut anahtar/bucket ve gerçek test; otomatik içerik taraması yok |
| X04 | Token/abonelik defteri | Doğrulanmış webhook/sync, tekil hak, aylık dönem pencereleri, iade | Tüm transfer/iade senaryoları otomatik değil |
| X05 | İşçi görevleri | Süre sonu, yaklaşık konum temizliği, waitlist, taahhüt, sonuç, ödül, silme, outbox; kilometreli acil çağrı/haftalık özet/rematch | Canlı worker zamanlaması, izleme ve gerçek push sağlayıcıları |
| X06 | API güvenliği | Rol/durum/sürüm/üyelik, rate limit, idempotency, SQL transaction, RLS paketi | Canlı yük/güvenlik/izleme/yedekten dönüş doğrulaması |
| X07 | Supabase ve yayın | 67 tablo, 108 komut; PostgreSQL/Storage adaptörleri; 001/003/005/006/007/008/009 + RLS SQL Editor paketi | Supabase yanında Node API HTTPS/worker, sağlayıcılar, imzalar ve mağaza testleri gerekir |
| X08 | Uygulama kalitesi | Android/iOS build ve otomatik testler | Tam fiziksel cihaz, minimum OS, erişilebilirlik/büyük font, bağlantı kaybı ve mağaza testleri |
| X09 | Operasyon: destek talebini yanıtla onayı | **Bağlı:** admin inbox; `admin.support.resolve`, yanıt ve durum kullanıcıya geri döner | Karşılıklı destek konuşması, SLA/atama veya ek dosya servisi yok |
| X10 | Operasyon: şikayet kuyruğu ve karar pencereleri | **Bağlı:** gerçek hedef bağlamı; işlendi/kapat, gerekçeli `admin.report.resolve`; anı/tesis yorumunu kaldırma | Her hedef türü için tek tık otomatik yaptırım yok; yetkili insan karar verir |
| X11 | Operasyon: anı fotoğrafı incele/onayla/reddet | **Bağlı:** yetkili görsel ve açıklama; `admin.memory.review`, gerekçe ve audit | Otomatik çıplaklık/şiddet/telif taraması yok |
| X12 | Operasyon: profil fotoğrafını doğrula / eski talebi kapat | **Bağlı:** güncel uploadId/requestId kontrolü; `admin.verification.photo`; eski talep kapanışı | Canlılık/kimlik eşleme servisi yok; manuel fotoğraf onayıdır |
| X13 | Operasyon: itirazdaki/pending maç sonucuna karar | **Bağlı:** skor/maç bağlamı; gerekçeli `admin.result.resolve`; onay normal çoğunluğa operatör istisnası, puan uygunluğu korunur | Otomatik hakemlik veya branşa özel skor doğrulama yok |
| X14 | Operasyon: tesis yorumunu yayınla/reddet | **Bağlı:** bekleyen puan/metin; `admin.venue.review`, gerekçe ve audit; yalnız published yorumlar tesis toplamına girer | Otomatik yorum taraması yok |
| X15 | Operasyon: başarısız bildirimi tekrar sıraya al onayı | **Bağlı:** outbox failure listesi ve `admin.outbox.retry`; yalnız failed kayıt, deneme sayacı/next_at sıfırlama | Sağlayıcı anahtarı veya cihaz token sorununu kendiliğinden düzeltmez |
| X16 | Operasyon: kullanıcı ara / işletmeci yetkisi ver-kaldır | **Bağlı:** `admin.operator.assign`, aktif ve admin olmayan hesap; gerekçe/audit | Yeni admin yaratma veya çok kademeli rol editörü yok |
| X17 | Operasyon: hesabı askıya al / yeniden aç onayı | **Bağlı:** `admin.user.status`, gerekçe/audit ve sunucu erişim denetimi | Dış kimlik sağlayıcısındaki hesabın silinmesi/kapatılması bu işlem değildir |
| X18 | Operasyon: mağaza/anı/turnuva/tesis/sohbet anahtarları | **Bağlı:** `admin.feature.set`, gözden geçirme onayı ve canlı config; ödenmiş hak uzlaştırması açık kalır | Daha önce açılmış sistem satın alma penceresini geri alamaz |
| X19 | Ortak operatör onay/gerekçe/makbuz referansı penceresi | **Bağlı:** `OperatorActionDialog`; işleme göre minimum gerekçe veya referans, rol/durum sunucu denetimi ve global tekrar | Kalıcı çevrimdışı operatör işlem kuyruğu yok |
| X20 | MMR, güvenilirlik ve görev kesinleştirme işi | **Bağlı:** işlem içinde Elo K=24, taraf ortalaması/check-in/branş uygunluğu, alt sınır 0; güvenilirlik katılım +2/yok −10; tekil geçmiş/hold/ödül | İtirazlı sonuç veya eksik karşı çoğunluk otomatik kesinleşmez; üretim worker takibi gerekir |
| X21 | Bildirim üreticilerinin çalışma kontrolü | **Bağlı:** engagement checkpoint en az beş dakika arayla, olay/hafta/kullanıcı anahtarıyla tekilleştirme; outbox tercihler/limitler | Canlı worker deploy/izleme ve APNs/FCM hesapları henüz dış kurulumdur |

## Bu incelemede tamamlanan bağlantılar

1. Gerçek sporcu formu, MMR farkı, kazanılmış rozet, ortak maç ve uyum; gizlilik/engel/boş/hata durumları bağlı. Preview örnekleri korunuyor.
2. MMR politikası config ve ortak pencerelerde açıklanıyor. Canlı sonuç; check-in, ayrık taraflar, rakip çoğunluğu ve 24 saatlik süreyle kesinleşiyor; Elo K=24 uygulanıyor. Operatör onayı çoğunluk için açık bir istisna; katılım/branş uygunluğu korunuyor.
3. Pasaport tüm gerçek haftalık görevleri, ilerlemeyi ve tekil TKN ödüllerini gösteriyor. Görevler MMR artırmıyor.
4. Canlı turnuva formu iki uygulanmış formatı, gerçek kapasiteyi ve kadrodaki her sporcu için MMR/telefon doğrulama koşulunu kullanıyor. Previewdaki ileri format örnekleri ayrıca korunuyor.
5. Topluluk oluşturma, düzenleme, üye listesi ve çıkarma; mevcut davet/onay/devir akışlarıyla bağlı. Canlı özel ağ maçları profil metniyle yeniden gizlenmiyor.
6. Challenge/anı saha seçiminde canlı tesis kataloğu ve elle yer girişi var. Tesis yorumları tamamlanan rezervasyona bağlı; yayınlama/ret, raporlama, yazarın güncelleme/kaldırma akışları ve gerçek puan toplamı bağlı.
7. Yetkili operasyon merkezi destek/şikayet/anı/fotoğraf/sonuç/yorum/outbox/kullanıcı/duyuru/bayrak pencerelerini içeriyor. Tesis paneli tesis/saat/rezervasyon, tesiste ödeme ve yapılmış tam iadenin kaydını yönetiyor; para transferi yapmıyor.
8. Canlı keşif şehir kapsamıyla sunucudan yenileniyor; Home ve harita listesi cursor ile sonraki maçları yüklüyor. Elle bölge seçiminde izin istemeyen native geocoder kullanılıyor; canlı haritaya demo koordinatı verilmez.
9. Acil oyuncu, haftalık özet ve karşılıklı rematch önerisi üreticileri; ayrıca izinli kullanıcılara yönetici duyurusu var. Sessiz saat/günlük limit/tekilleştirme korunuyor; gerçek push teslimi dış kurulum gerektiriyor.
10. Anı skor etiketi/teması, takım rengi, turnuva metadatası, destek yanıtı ve mağaza satış kapatma akışı önceki bağlantılarıyla korunuyor.

## Kalanların önceliği

**Mevcut akışların sınırları:** M04 gerçek kişi bazlı ücret bölüşümü/tahsilat; S01 bilinmeyen/gizli sayı sunumu; diğer katalogların/bildirim/geçmiş/yorum/operasyon listelerinin tam sayfalaması; bazı hedef yönlendirmeleri. Acil dağıtım güncel yaklaşık konumda kilometre yarıçapını, konum yoksa seçili il/ilçeyi kullanır. Canlı tesis seçimi ve tüm görev listesi artık mevcut; bunlar eksik olarak sayılmamalı.

**Yönetim ve veri hazırlığı:** Topluluk oluşturma/düzenleme, moderasyon ve tesis işletmeci pencereleri artık bağlı. İlk admin/işletmeci hesabı, yetkili personel, gerçek tesis/saat kataloğu ve operasyon çalışma düzeni kurulmalı. Tesis panelinin yinelenen toplu takvim şablonları; otomatik içerik taraması ve gerçek ödeme/iade sağlayıcısı ayrı iş.

**Ayrı ürün/servis isteyenler:** gerçek AI, sürekli QR/geofence, birebir Challenge, kemer/referans, gelişmiş AutoSquad, İsviçre/grup+eleme gibi ileri turnuva formatları, gerçek ödül/hakem, tüm Plus avantajları, Realtime ve dış sağlayıcı hesap kapatma uzlaştırması.

**Yapılandırma bekleyenler:** Supabase tablo/RLS/Storage kurulumu, Node API HTTPS ve worker yayını, Apple/Google, SMS, RevenueCat/mağaza ürünleri, APNs/FCM, gerçek yasal bağlantılar ve dağıtım imzaları. Bu sağlayıcıların gerçek hesap/mağaza/fiziksel cihaz testleri tamamlanmış sayılmaz. Yalnız SQL Editor işlemleri yeterli değildir.

Bu belgede **Bağlı** yazılması gerçek sağlayıcı/mağaza/fiziksel cihaz testinin geçtiğini iddia etmez. Önceki görsel turun sınırı [adım adım incelemede](UYGULAMA_ADIM_ADIM_INCELEME.md); güncel test/build sonuçları [8 Kasım kanıtlarında](validation/2026-11-08/) tutulur.

## Y. 8 Kasım kapsamlı işlev tamamlamaları

Kaynaklar: `App.kt`, `ChatHubScreen.kt`, `CompetitionScreen.kt`, `OperationsSheets.kt`, `backend/src/venue-applications.mjs`, `rankings.mjs`, `geo.mjs`, `chat-management.mjs`, `memory-feed.mjs`.

| ID | Pencere / özellik | Yazılan backend / bağlantı | Sınır / gereken |
|---|---|---|---|
| Y01 | Pusula simgesi | Giriş öncesi/sonrası aynı Explore ikonu; tıklama giriş veya profil hedefini korur | Tema ve mock görünümü korunur |
| Y02 | Sohbet temizle onayı | chat.clear; kişisel sıra sınırı, karşı taraf geçmişi korunur | Sadece yerel cihaz silme değil, hesaba bağlı kişisel tercih |
| Y03 | Sohbet sil / silerken engelle onayı | chat.delete; kişisel gizleme + DM engeli tek işlemde; yeni mesaj eski geçmişi geri getirmez | Grup üyeliğinden ayrılma ayrı iş kuralıdır |
| Y04 | Mesaj işlemleri menüsü | Yanıtla/düzenle/benden sil/herkesten sil; sürüm ve üyelik denetimi | Düzenleme 15 dakika ile sınırlı |
| Y05 | Mesaj okundu / gün başlıkları | Aynı milisaniyede mesajları sıra numarası ayırır; temizleme sahte okundu üretmez | Canlı güncelleme polling; websocket yok |
| Y06 | Sohbet özel görseli | Temizlenmiş/silinmiş görsel forward ile geri getirilemez; cache sürümle temizlenir | Gerçek bulut Storage kurulumu gerekir |
| Y07 | Vitrin modları | GET /v1/memories random/nearby/top/newest/mine; tüm uygun kayıtlar üzerinde sıralama | Öğrenilmiş öneri/AI modeli yok |
| Y08 | Vitrin sayfalama ve yenile | Şifreli, kullanıcı/filtreye bağlı cursor; kararlı random seed; tekrar eden kayıtlar ayıklanır | Beğeni sırası değişirse yenileme gerekir |
| Y09 | Vitrin yaklaşık konum | İsteğe bağlı nokta, gerçek uzaklık; nokta yoksa mesafe boş ve ilçe temelli sıra | GPS izni ve gerçek cihaz testi |
| Y10 | Paylaşımlarım ve düzenleme | Bekleyen/onaylanan/reddedilen kendi içerikleri; sürümlü güncelleme tekrar moderasyona gider | Onaylayan insan ve canlı medya servisi |
| Y11 | Sporcu profilinde güvenlik | Kullanıcı engelle/kaldır ve bildir; gerçek user.block/report.create | Canlı moderasyon operasyonu |
| Y12 | Bildirim tekil / tümünü temizle onayı | notification.clear kişisel gizleme; push kuyruğu gizlenen bildirimi atlar | Kategoriye göre sunucu sayfalaması ve tüm kayıtlardan okunmamış sayısı bağlı |
| Y13 | Bildirim kategori filtreleri | Gerçek kategori alanı; yalnız başlık veya önceliğe göre tahmin edilmez | Preview eski örneklerin görünümünü korur |
| Y14 | Bildirim saat / kota seçicileri | Sessiz başlangıç/bitiş, günlük limit, haftalık özet/rematch ve acil yarıçapı | Sağlayıcı/OS teslim garantisi değildir |
| Y15 | Yaklaşık canlı keşif konumu | discovery.location; 2 ondalık, 15 dakika TTL, süre sonunda silme; halka açık profil dönmez | Arka plan takip ürünü yok |
| Y16 | Konum izni geri alma / güncellik | Ön planda verilmiş izni tekrar denetleme; eski nokta temizliği ve aktif GPS göstergesi | İzin iptalini iki fiziksel platformda doğrulama gerekir |
| Y17 | Android gerçek harita | Canlı modda MapLibre; preview eski çizimi korur; pin kimliği/zoom/lifecycle | Üretim harita style/tile URL; demo URL release kapısında reddedilir |
| Y18 | iOS gerçek harita | MapKit pinleri benzersiz ID ile; izinsiz kullanıcı konumu açılmaz | Gerçek cihaz/bağlantı testleri |
| Y19 | İlan katılım şekli seçimi | Doğrudan katılım / organizatör onayı; API approvalRequired saklar | İlgili mevcut mock ilanlar korunur |
| Y20 | Onaylı ilanda haritadan başvuru | event.join onayı atlayamaz; hızlı düğme başvuru formuna gider | Token taahhüdü başvuruda kilitlenir |
| Y21 | Bekleme teklifi kabulü / ayrılma | Kabul ve ayrılma ayrı düğme/komut; onaylı ilanda kabul başvuru üretir | Teklif 10 dakika; organizatörün onayı ayrıca gerekir |
| Y22 | Takım challenge kaynak seçimi | Seçilen kaptan takımı App gönderimine aynen taşınır; aynı branş denetimi | Düşünülen ilk takıma sessizce dönülmez |
| Y23 | Sporcuya takım daveti kaynak seçimi | Kaptan olunan uygun branş takımı açık seçilir; üye/engel ve mükerrer davet denetlenir | Genel sporcu kataloğu henüz tam sayfalı değildir |
| Y24 | Takım talepleri ve kadro | Her kendi takımında talep yönetimi; kaptan yetkisi; resmi kadro taahhütleri çıkarma/ayrılmayı korur | Kaptanlığı insan devreder; otomatik kadro dengesi yok |
| Y25 | Topluluk yayın zinciri | community.create; kurucu owner ve ilk üye; OPEN/APPROVAL/INVITE seçimi | Açık topluluk doğrudan katılır; bu seçilmiş erişim kuralıdır |
| Y26 | Topluluk başvuru bildirimleri | Pending yalnız yönetici kuyruğuna; onay/ret başvurana; NETWORK hedefi ilgili kartı öne getirir | Push sağlayıcısı kurulumu |
| Y27 | Topluluk davet / çıkarma / devir | Davet yanıtları, yöneticiye devir bildirimi, çıkarılana bildirim ve özel maç temizliği | Kurum kimliği doğrulama sağlayıcısı yok |
| Y28 | Tesisini ekle formu | venue.application.create; adres/branş/olanak/saha/iptal; en fazla üç bekleyen başvuru ve mükerrer kontrolü | İşletme yetkisi doğrulama insan operasyonudur |
| Y29 | Tesis başvurularım / geri çek | Sahibine özel durum/yanıt; sürümlü withdraw; export ve hesap silmede redaksiyon | Gerçek işletme verileri girilmeli |
| Y30 | Admin tesis başvurusu inceleme | admin.venue.application.review; sürümlü onay/ret; onayda venue + venue_owner rolü atomik | İlk admin güvenli kurulum ve yetkili incelemeci |
| Y31 | Tesis bilgisi düzenleme / kapatma | venue.update/archive; aktif/ödenmiş rezervasyon koruması, ücretsiz gelecek kayıtların iptali | Gerçek iadeyi operatör yapıp kaydeder |
| Y32 | Tesis günü / saati / süre / ücret | Tarih/saat seçici ve kuruş tutarı; venue.slot.create/update; geçmiş/rezervasyonlu saat değişmez | Tekrarlayan toplu saat şablonu yok |
| Y33 | Saha saati kapat / yeniden aç | Sürüm, aynı kort çakışması, eşzamanlı kort kapasitesi; cancel/reopen | Mevcut rezervasyon önce sonuçlandırılmalı |
| Y34 | Çok kortlu veri geçişi | 009 eski tek zaman kısıtını kaldırır; SQLite veri ve ödeme FK koruması, PostgreSQL tekrar uygulanabilirlik testi | Canlı veritabanı yedek ve migration uygulaması |
| Y35 | Rezervasyon bildirim hedefleri | İşletmeci kuyruğu / kendi rezervasyonlarım / onay/ödeme/iade/tamamlama bildirimleri | Fiziksel ödeme tesiste; kart tahsilatı yok |
| Y36 | Sıralama gerçek bölge / branş sayfaları | GET /v1/rankings; filtre önce, sayfalama sonra; kendi gerçek sıra konumu; gizlilik/aktif hesaplar | Yüksek trafikte profil JSON okuma optimizasyonu gerekir |
| Y37 | Sıralama 500 kayıt hatası | Düşük ulusal puanlı ilçedeki sporcu yerel sıralamadan kaybolmaz; regresyon testi | Gerçek kullanıcı veri ve yük testi |
| Y38 | Canlı turnuva tarih alanı | Geçmiş demo tarih canlı varsayılandan kaldırıldı; kullanıcı tarih girer | Tarih açıklaması metindir; fikstürün gerçek zamanı ayrıca seçilir |
| Y39 | Tüm turlarda kadro uygunluğu | Her turda kayıtlı kadro/aktif hesap/branş/MMR/telefon koşulları yeniden kontrol edilir | Gelişmiş eleme/grup formatları ayrı geliştirme |
| Y40 | Pasaport sekiz rozet | Katılım, fair play, ilk maç organizasyonu, 100 maç, turnuva şampiyonluğu, şehir, hafta serisi ve acil dolgu | Kazanma koşulları canlı açıklamalarla gösterilir |
| Y41 | Son maçlar sonucu | Kendi check-in ve verified maçlarından gerçek galibiyet/mağlubiyet/beraberlik ve MMR farkı | Son 100 kayıt; tüm geçmiş için sayfalama ayrı |
| Y42 | Güvenilirlik geçmişi | Gerçek kayıt tarihi, maç başlığı ve uygulanmış delta | Mock örneklerinin tüm özel teşekkür/azalan ceza kuralları ürün motorunda yok |
| Y43 | Telefon numarası düzenleme | Doğrulanmış numara pasaport metniyle değişmez; iletişim OTP akışına yönlendirme | Gerçek SMS yapılandırması |
| Y44 | Oturum / kişisel önbellek | Hesap değişiminde önceki mesaj, görsel, feed ve sayfalar silinir; sürüm değişimi eski mesajları düşürür | Fiziksel çok hesap turu ayrıca gerekir |
| Y45 | Büyük canlı işletmeci listeleri | Aktif rezervasyonlar ve gelecek saatler en yeni 100 kaydın dışında da korunur | Eski tamamlanmış operasyon geçmişi tam sayfalı değil |

## Z. İkinci kapsamlı devam — liste/izin/gizlilik tamamlaması

Kaynaklar: `BackendMoreCatalogButton.kt`, `BackendRepository.kt`, `BackendEditingActions.kt`, `PrivacyPresentationTest.kt`, `NativeDeviceContractTest.kt`, `catalog.mjs`, `notification-feed.mjs`, `event-locations.mjs`. Bu ekler 196 backend ve57 ortak istemci testiyle doğrulandı; Android/iOS derlemeleri başarılı. Son GUI kapsamı 9 Kasım raporundadır.

| ID | Özellik / pencere | Bağlantı | Sınır |
|---|---|---|---|
| Z01 | Takım/turnuva/topluluk/tesis sonraki sayfaları | Public katalog cursorları, kendi/aktif kayıtları koruma, doğru player referansları, sekme değişince ek sayfaları bırakma | Genel sporcu araması ve işletmeci geçmişi ayrıca değerlendirilir |
| Z02 | Bildirim kategori bazlı geçmiş | Kategori filtresi sayfa sınırından önce; fullunreadcount, eski sayfaları yükle, kapalı pencereye geç cevap uygulanmaz | Gerçek push/provider testi kalır |
| Z03 | Kesin buluşma noktası | 010event_locations; host/aktifüyeACL ve 24saatsonu; genel pin yaklaşık | Eski ilanlarda kesin nokta otomatik üretilmez; sahibi seçebilir |
| Z04 | Konum düzenleme penceresi | İlan düzenlerken harita seçimi; yer metni değişirse stale koordinat silinir | Adres araması internet/harita uygulamasına bağlı |
| Z05 | Gizli/bilinmeyen gerçek değerler | reliabilityVisible/profileAvailable, nullablemesafe; hidden— ile gerçek0 ayrımı | Her fiziksel font/erişilebilirlik varyantı GUItestli değil |
| Z06 | Sohbetsiz engellenen sporcular | Tüm blockIDs ve publicadreferansları; AccountSettingsunblock | Karşı tarafın koyduğu engel kaldırılmaz |
| Z07 | Takım bilgilerini düzenleme | Ad/arma/renk/şehir/bölge/MMR/müsaitlik/açıklama/başvuru açıklığı; sürümlü kaptan işlemi | Branş ve resmi kayıtlı kadro bu formdan değiştirilmez |
| Z08 | Profil fotoğrafını kaldır | Onay, explicitclientintent ve avatarUploadIdnull, PHOTOonayı düşer; yeni foto intenti temizler | Bulut fiziksel dosya temizliği orphanworkerile |
| Z09 | Eski tesis değerlendirmeleri | Venue/actorcursor, gizlilik filtresi sayfa öncesi, loadmore | Otomatik içerik taraması ayrı |
| Z10 | Önizleme pasaportu mockları | Yeni previewpasaporttan sonrahistory/badges/missions/reliabilityörnekleri korunur | Canlı yeni hesap geçmişi boşgerçekkayıttır |
| Z11 | GPS/camera latecallbackları | Dispose/generation cancellation; eski/düşükdoğrulukfixret; gerçekcapturedAt; otomatikyenilemeizinpromptuaçamaz | Fiziksel Android/iOSizin ve kamera turu gerekir |

| Z12 | Tesis açık adresini oluştur/düzenle/kaldır | Create/update/DTO/form/seçici ve rezervasyon metni; aktif rezervasyon adres değişikliğini korur | Gerçek işletme/adres bilgisi işletmeci tarafından sağlanır |


## 9 Kasım — ilan ID, ağlar, radar ve vitrin ek denetimi

| ID | Ekran / özellik | Bağlı davranış | Sınır / doğrulama |
|---|---|---|---|
| Z13 | Radar · ID ile bul | İl/branş satırının üzerinde alan; UUID veya matchora://event bağlantısı; farklı şehirdeki ilan doğrudan detaydan bulunur | Katılım açık onayla; organizatör onayı/kapasite/özel ağ üyeliği korunur |
| Z14 | İlan oluşturma · kalıcı kimlik | Sunucuda crypto.randomUUID; idempotent yeniden denemede aynı ID; yayın sonrası detay açılır | Preview mevcut örnek ID’lerini korur; demo yeni ilanları oturumluk |
| Z15 | İlan detayı · ID paylaş | Seçilebilir ID metni; iOS/Android ortak paylaşım içeriği, ID/bağlantı/radar tarifi | Mesajı hangi kişiye göndereceğini kullanıcı seçer |
| Z16 | İlan formu · Saha/Kampüs/Şirket/Seyahat | Dört seçim ilk adımda; radarın seçilen ağı başlangıç değeridir; onaylı topluluk seçicisi | Üyelik yoksa Ağ bölümünde gerçek başvuru/onay gerekir |
| Z17 | Ağ doğruluğu | Aktif topluluk + onaylı üyelik + CAMPUS/COMPANY tür eşleşmesi; ad sunucudan; seyahat hedefi gerçek ilan şehri | Paylaşılan ID gizli ağ üyeliğini atlamaz |
| Z18 | Radar/harita il-ilçe merkezi | İki modda native bölge çözümleme; seçilen bölge ilk ilandan önce; bölge değişince eski pin odağı temizlenir; sunucu cevabı elle seçimi geri almaz | Geocoder/harita servisi bağlantısı gerekir; mock ilanlar silinmez |
| Z19 | Sohbet listesi eski sayfalar | GET /v1/threads; kullanıcıya bağlı süreli cursor, eski sohbet yükleme, her yenilemede erişim tekrar kontrolü | İsim/mesaj araması yüklü sohbetlerde; kalıcı offline kuyruk/Realtime ayrı |
| Z20 | Eski sohbet bildirimi ve okunmamış rozet | GET /v1/threads/:id; ilk 100 dışında aktif sohbet açılır/yenilenir; okunmamış sohbet sayısı tüm kayıtlardan | Silinmiş/gizlenmiş/erişimi kalkmış içerik dönmez; engellenen mesajları boş |
| Z21 | Anı düzenle · şehir/ilçe/fotoğraf | Paylaşımın özgün bölgesi korunur; ayrıca bölge seçilebilir; tema fotoğrafı silmez; konum değişince eski nokta temizlenir | Kamera/galeri native kaynak penceresi ortak; fiziksel kamera testi ayrı |
| Z22 | Vitrin konum backend’i | Ham koordinat doğrulama; create/update/public bootstrap/feed iki ondalık; eski hassas payload’lar da yanıt sırasında yuvarlanır | Yaklaşık konumdur; birebir canlı konum takibi değildir |


## 9 Kasım — kupa, kulüp, topluluk ve tesis yaşam döngüsü

Kaynaklar: `TournamentRegistrationSheet.kt`, `CreateTournamentSheet.kt`, `CreateTeamSheet.kt`, `CreateCommunitySheet.kt`, `BackendManagementActions.kt`, `VenueReviewSheets.kt`, `OperationsSheets.kt`; `tournament-management.mjs`, `tournament-details.mjs`, `venue-schedule.mjs`.

| ID | Ekran / özellik | Bağlı davranış | Sınır / doğrulama |
|---|---|---|---|
| Z23 | Topluluğun yapısı | Kalıcı spor grubu / kampüs topluluğu / şirket spor topluluğu; CITY/TRAVEL yeni topluluk oluşturmaz | Eski mock topluluklar ve okuma desteği korunur; ilan ayrı tek buluşmadır |
| Z24 | Kulüp oluştur / bölge | Gerçek il-ilçe seçici; city DTO/API; yeni kayıt detayına odak; başlangıç MMR şartı 1000 | Mevcut örnek kulüpler silinmez; başvurular kaptana gider |
| Z25 | Kupa oluştur | Aynı çalışan format/kapasite seçicisi, kadro sayısı, bölge, her oyuncu için MMR/iletişim koşulu | Yeni kupa sıfır kayıt ve boş fikstürle başlar; eski örnek fikstürler korunur |
| Z26 | Takım ve maç kadrosu kaydı | Organizatör kendi takımıyla da kaydolabilir; kaptan oynayacak kişileri seçer; sabit kadro sayısı, aktif üyelik, branş, MMR, mükerrer sporcu kontrolü | Seçilmeyen kulüp üyeleri otomatik maç kadrosuna girmez |
| Z27 | Kupa Kayıtlar sekmesi | Takım/kadro ve durum; organizatör gerekçeli çıkarır, yeniden kayıt izni verir | Başlayan turnuvada doğrudan çıkarma yok; izin otomatik kayıt değildir |
| Z28 | Fikstürden gerçek maç | Gerçek eventId, isim, tarih/saat, tesis; satır gerçek maç detayına gider | Aynı detaydan check-in, sohbet, sonuç ve itiraz; örnek eski fikstürde gerçek eventId yoksa geçiş uydurulmaz |
| Z29 | Şampiyon kesinleştirme | Sonuçlar doğrulanınca tur/lig ilerler; championTeamId kalıcı; kayıtlı kadroya bildirim; kilit bir kez çözülür | Gerçek worker ve sonuç onay penceresi gerekir |
| Z30 | Kayıt / takım bildirim hedefleri | TEAM/TOURNAMENT; katalogda ilk sayfa dışı tek kayıt GET’i; tazelemede tekrar yetki; kapanan kayıt geri gelmez | Bildirim teslimi sağlayıcı kurulumuna bağlı |
| Z31 | Haftalık tesis müsaitliği | Günler/tarih/saat/süre/saha/bedel; en fazla 120 aralık; tek çakışmada tüm plan geri alınır; tekrar istek aynı ID’leri döner | İşletmeci/admin; mevcut rezervasyonlar bozulmaz; kullanıcı açık saatlerden rezervasyon yapar |
| Z32 | Tesis ekranından değerlendirme | Yalnız kişinin tamamlanmış/ödenmiş rezervasyonları yazma girişi açar; yorum sürümlü incelemeye gider | Kişisel son 100 uygun rezervasyon döner; otomatik moderasyon ayrı |
| Z33 | Önizleme form ve pasaport bütünlüğü | Tesis başvuru/organizasyon planı görünür; preview gönderimi açıkça canlı hesap bekler. Yeni preview pasaportta demo istatistik/branş geçmişi korunur; eski örnek kupa Kayıtlar sekmesinde mevcut fikstür/puan tablosu isimleri görünür | Canlı ilk pasaportta sahte MMR/maç/rozet eklenmez |
| Z34 | Birleşik yaşam döngüsü doğrulaması | Gerçek kayıt→fikstür→check-in→skor→rakip onayı→worker→şampiyon; kulüp başvuru/karar/çıkarma/yeniden davet; tesis plan/rezervasyon/yorum | Bu otomatik kanıt her pencerenin fiziksel cihazda gezildiği iddiası değildir |


## 10 Kasım — gerçek giriş ve ortak hesap kapısı

Kaynaklar: `AuthAndOnboardingSheet.kt`, `AppLinks.kt`, `App.kt`, `AuthenticationPolicy.kt`, `BackendRepository.kt`, `BrowserAuthentication.android.kt`, `BrowserAuthentication.ios.kt`; `email-auth.mjs`, `oauth.mjs`, `auth.mjs`.

| ID | Ekran / özellik | Bağlı davranış | Sınır / doğrulama |
|---|---|---|---|
| Z35 | Telefon yerine e-posta | Yeni/mevcut kullanıcıya Supabase 6 haneli kod; normalleştirme ve API uçları | SMS giriş düğmesi kaldırıldı; profil iletişim doğrulaması ayrı korunur |
| Z36 | E-posta doğrulama güveni | Güvenilen Supabase /user ile doğrulanmış ve eşleşen e-posta; önce doğrula sonra Matchora oturumu | Yerel/demo kod üretilmez; kod kalıcı istemci deposuna yazılmaz |
| Z37 | Yeniden gönderim/deneme | Adres/IP kalıcı rate limit; başarısız doğrulama girişimleri de commit edilir; 60 sn sayaç | Sağlayıcı limitleri ve teslimi ayrıca |
| Z38 | Apple platform ayrımı | iOS Apple/Google/e-posta; Android Google/e-posta; ortak platform politikası | Otomatik istemci testi ve iki platform derlemesi; gerçek Android GUI ayrıca |
| Z39 | Gerçek sosyal giriş | Sistem auth tarayıcısı → Supabase PKCE → sunucu doğrulaması; local modda bile sahte hesaba düşmez | Canlı Apple/Google yapılandırması ve gerçek hesap testi bekler |
| Z40 | Misafir ve görünür yasal bağlantılar | Büyük önizleme kartı; ikonlu Privacy Policy/Terms of Use kartları; HTTPS bağlantı veya henüz paylaşılmadı bilgisi | Gerçek URL/metin/sürüm kullanıcı tarafından verilecek |
| Z41 | Uygulama çapında giriş kapısı | Dokunma/erişilebilirlik, dock/üst çubuk, komutlar ve derin bağlantılar; giriş+pasaport+yasal onay gerekir | Mocklar silinmez; oturumsuz özellik kullanımı açılmaz |
| Z42 | Oturum ve ilk pasaport | Gerçek oturum geri yükleme; API’ye kayıt; uygulama kapsamlı tekrar; çıkışta anında kilit; doğrulama sırasında çift gönderim engeli | Canlı sağlayıcı, fiziksel cihaz ve ağ geçişi testi bekler |
