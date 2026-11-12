Son doğrulama ve açık işler: [10 Kasım son durum](SON_DURUM_2026-11-10.md).

# Matchora — 9 Kasım 2026 işlev ve backend raporu

Güncel kaynaklar: **56 ortak UI dosyası, 261 fonksiyon bildirimi, 95 pencere/menü çağrı noktası, 111 sunucu komutu, 67 tablo**. Kaynak sayımı işlevlerin cihazda geçtiği iddiası değildir. [398 özellik satırı](TUM_EKRANLAR_BACKEND_ENVANTERI.md), [dosya/pencere denetimi](KAPSAMLI_ISLEV_DENETIMI_2026-11-08.md) ve [kaynak dizini](UI_KAYNAK_DIZINI.md) ayrıntıları içerir. Önceki raporlardaki açık iş notları bu güncel durumla değerlendirilmelidir.


Son ek çalışma: [ilan ID, ağlar, konum, mesajlar ve vitrin raporu](ILAN_ID_AG_KONUM_RAPORU_2026-11-09.md). Bu rapordaki önceki simülatör PID kaydı tarihsel doğrulamadır; yeni kurulum ve GUI sonuçları ek raporda ayrıca belirtilir.

Son devam: [yarış, kulüp, topluluk ve tesis yaşam döngüsü raporu](YARIS_AG_TESIS_TAMAMLAMA_2026-11-09.md). Kayıt/kadro seçimi, gerçek fikstür hedefleri, şampiyon kesinleştirme, haftalık müsaitlik ve yorum yazma girişi bu devamda genişletildi.

## Son istekte eklenenler

- Radarın il/branş satırının üzerinde **ID ile bul**. Sunucunun kalıcı UUID’si veya davet bağlantısı başka şehirdeki ilanı açar; katıl/başvur/bekleme kuralları aynen yürür.
- İlanın ilk adımında **Saha, Kampüs, Şirket, Seyahat** seçimi; seçilmiş radar ağı başlangıç değeridir. Sunucu tür/aktif topluluk/onaylı üyelik eşleşmesini doğrular; kurum adı istemciden uydurulamaz. Yayın sonrası ID’siyle detay açılır; iOS/Android paylaşımı ID ve bağlantıyı içerir.
- Radar ve büyük harita elle seçilen il/ilçeye odaklanır; ilk mock ilan seçimi geri çevirmez. Preview şehir/ağ değişikliklerinde mock kayıtlar korunur.
- Sohbet listesinde eski konuşmaların sayfalaması; ilk 100 dışında bildirim hedefi; tüm konuşmalardan okunmamış rozet sayısı; silme/engel/yetki değişiminde tekrar doğrulama.
- Anı düzenlerken özgün şehir/ilçe ve fotoğraf korunur; bölge değiştirme eklenir. Oluşturma/düzenleme/okumada konum yaklaşık tutulur; eski kesin noktalar yanıtla sızmaz.

## Önceki devamlarla birlikte mevcut zincirler

| Bölüm | Tamamlanan davranış ve backend |
|---|---|
| Akış / spor radarı / acil oyuncu | Gerçek ilanlar, şehir/branş filtreleri, kurallı eşleşme; güncel yaklaşık konumla mesafe ve acil çağrı yarıçapı. Konum 15 dakika geçerli; yoksa seçili ilçe kullanılır. Bölge tercihi güncel GPS kaydını yanlışlıkla silmez. |
| Harita / alttaki ilanlar | Native harita ve gerçek pinler; doğrudan katılım veya organizatör onayı. Onaylı ilana hızlı düğmeyle doğrudan girilemez. |
| İlan / başvuru / kadro | Oluşturma, düzenleme, başvuru/geri çekme, kabul/ret, ayrılma, organizatörün çıkarma/yeniden izin verme akışı; kapasite, sürüm, rol ve Token kilidi. Bekleme teklifi kabulü ile listeden ayrılma ayrı işlemler. |
| Buluşma noktası / yol tarifi | Kesin nokta ayrı özel tabloda. Yalnız organizatör ve mevcut kadro okuyabilir; başvuran/çıkarılan okuyamaz. Üyelerde maç sonrası 24 saat sınırı. Genel pin yaklaşık kalır. Rota kesin nokta veya adres kullanır; yuvarlanmış pin rota hedefi olmaz. Yer değişince eski koordinat temizlenir. |
| Yarış / sıralama | Branş/il/ilçe filtresi sayfa sınırından önce uygulanır. İlk 500 genel kayıt nedeniyle yerel sporcunun kaybolması giderildi. Kendi gerçek sıra konumu sayfa dışında da gösterilir. |
| Takımlar / challenge | Seçilen kaynak takım gönderimde korunur. Sporcu davetinde uygun kaptan takımı seçilir. Başvuru/kabul/ret/davet/ayrılma/çıkarma/devir/kapama; resmi kadro taahhütleri korunur. Ad, arma, renk, şehir/bölge, MMR koşulu ve müsaitlik sürümlü düzenlenebilir; yeni MMR koşulu mevcut üyeleri çıkarmaz. |
| Turnuvalar | Kapasite, uygulanan iki format, kayıt/çekilme, kadro, fikstür, tur ilerletme, sonuç/itiraz ve tamamlama. Her turda gerçek kadro uygunluğu tekrar denetlenir. Canlı formda geçmiş demo tarihi varsayılan değildir. |
| Topluluklar | Oluşturucu yönetici ve ilk üye olur. OPEN doğrudan, APPROVAL yönetici onayıyla, INVITE davetle katılır. Başvuru ilgili yönetici kuyruğuna ve bildirimine; karar başvurana gider. Düzenleme, üye çıkarma, davet, ayrılma, yönetici devri/kapama ve özel maç temizliği bağlı. |
| Tesis başvurusu | Normal kullanıcı adres/branş/olanak/saha sayısı/iptal politikasıyla başvurur. Kendi durumunu görür ve geri çeker. Yetkili yönetici onayladığında tesis ve işletmeci rolü aynı işlemde oluşur; ret yanıtı kullanıcıya döner. |
| Tesis / gün / saat / ücret | İşletmeci gün/saat/süre ve kuruş bedelini belirler; tesis/saat/adres düzenleme, kapatma/yeniden açma vardır. Aynı kort çakışması, eşzamanlı kort kapasitesi, çift rezervasyon ve aktif/ödenmiş kayıtlar korunur. Çok kortlu eski şema geçişi ödeme/rezervasyon bağlantılarını korur. |
| Rezervasyon / yorum | İstek, işletmeci onayı, tesiste alınmış ödeme kaydı, tamamlama, iptal ve gerçekten yapılmış iade kaydı; doğru bildirim hedefleri. Tamamlanmış rezervasyon yorumu, moderasyon, düzenle/sil/bildir ve eski yorum sayfalaması. |
| Pasaport | Doğrulanmış katılımdan gerçek sonuç, galibiyet/mağlubiyet, MMR farkı ve güvenilirlik tarihi. Sekiz rozetin tümüne hesaplama; haftalık görevler ve tekil TKN ödülleri. Telefon değişimi OTP ile; profil fotoğrafını kaldırma niyeti eski upload'ı geri getirmez. |
| Katalog / bildirim geçmişi | Takım, turnuva, topluluk ve tesis listelerinde sonraki sayfalar; eski kendi/aktif kayıtları koruma, yetki/silme değişikliklerinde yeniden doğrulama. Bildirimde kategori önce filtrelenir, eski sayfalar yüklenir ve okunmamış sayısı tüm kayıtları kapsar. |
| Gizlilik | Gizli/bilinmeyen güvenilirlik ve mesafe gerçek sıfırdan ayrılır. Sohbeti olmayan engellenmiş sporcular da listeden kaldırılabilir. Hesap değişiminde özel mesaj/görsel/akış önbelleği temizlenir. |
| KMP cihaz işlemleri | Android/iOS GPS, kamera/galeri, QR, bildirim, ayarlar, paylaşım, harita ve takvim adaptörleri. Eski/yanlış zamanlı konum, düşük doğruluk, geç callback ve dispose durumları ele alındı. Otomatik konum yenileme izin penceresi açamaz; arka plan konum izni istenmez. |
| Token / Plus / hukuk | RevenueCat SDK satın alma/restore ve sunucu doğrulama/tekil defter korunuyor. Mobil callback bakiye uydurmaz. Privacy Policy/Terms ve diğer yasal bağlantı alanları tıklanabilir; gerçek URL/sürüm yapılandırması beklenir. |

## Görünüm ve mock koruması

`DemoData.kt` değiştirilmedi. Varsayılan ve simülatör modu **preview**. Yeni önizleme pasaportundan sonra da örnek maç/güvenilirlik/rozet/görevler gösterilir. Canlı yeni hesap kendi boş gerçek geçmişini gösterir. Premium tema ve ana yerleşim korunmuştur. Sol üst Explore pusulası girişten önce ve sonra aynı kalır; önceki simülatör turunda iki durum da gözlendi.

## Doğrulama

- **213/213 backend testi geçti**, atlanan yok. HTTP, gerçek PostgreSQL motoru/PGlite, RLS, tekrarlı migration, katalog/bildirim cursorları, özel nokta erişimi, üyelik, satın alma/ödül ve hesap silme dahil.
- **65/65 ortak istemci testi geçti**, hata yok. Önbellek/hesap değişimi, katalog ve bildirim sayfaları, gizli/sıfır değer, fotoğraf kaldırma, native konum/yönlendirme ve tesis adres sözleşmeleri dahil.
- Android debug APK ve iOS Simulator framework derlemesi başarılı. Swift preview uygulaması derlendi ve açık simülatöre kuruldu (PID58525). Son CUA kontrolünde ana mock akış/pusula gözlendi; ayrıntılı kurulum kaydı kanıt klasöründedir.
- APK: 12 native kütüphane. 64 bit arm64-v8a/x86_64 ELF yükleme hizaları 16 KB; APK `zipalign -P 16` başarılı. 32 bit MapLibre hizası 4 KB'dir. Bu statik kontrol fiziksel 16 KB cihaz/release AAB testi değildir.
- 010 özel buluşma noktası migrationı dahil Supabase SQL Editor paketi yeniden üretildi; canlı Supabase'e bağlanılmadı.

Kanıtlar: [9 Kasım klasörü](validation/2026-11-09/). Önceki GUI turunda ana akış, giriş/üç pasaport adımı ve pusula gözlendi; Ağ girişinde CUA bağlantısı koptu. Bu nedenle tüm yeni backend akışlarının simülatörde uçtan uca gezildiği iddia edilmez. Fiziksel kamera/GPS/QR ve gerçek mağaza testleri ayrı kalır.

## Kalanlar

| Tür | Kalan iş |
|---|---|
| Dış kurulum | Supabase DB/RLS/özel Storage/Auth; Node API HTTPS ve worker yayını; Apple/Google/SMS; RevenueCat ürün/anahtar/webhookları; APNs/FCM; üretim Android harita URL'si; dağıtım imzaları. Yalnız SQL Editor çalıştırmak bütün servisi yayına almaz. |
| Kullanıcı içeriği / operasyon | Gerçek Privacy Policy/Terms URL ve sürümleri, destek adresi; ilk admin; işletme yetkisini doğrulayacak personel ve gerçek tesis/saat verileri. |
| Kalan geliştirme | Tüm kişisel/operasyon geçmişinde sayfalama; genel sporcu araması; kalıcı çevrimdışı işlem kuyruğu; WebSocket/Realtime; sağlayıcı hesap birleştirme/iptal/silme uzlaştırması; tüm mağaza transfer/iade olaylarının otomasyonu; otomatik içerik taraması; tesis planlarının bitince otomatik uzatılması ve kişi bazlı fiziksel tahsilat/borç/iade sağlayıcısı. |
| Ayrı ileri ürünler | Gelişmiş turnuva formatları ve ödül fonlama; birebir challenge, referans/şehir kemeri, öğrenilmiş AI ve otomatik dengeli kadro. Pasif örnek kaynaklar tamamlanmış canlı ürün sayılmadı. |
| Yayın testi | Gerçek giriş/SMS/satın alma/restore/iade; fiziksel Android/iPhone izinleri; minimum OS, erişilebilirlik/büyük yazı, ağ kopması, üretim yükü ve yedekten dönüş; mağaza gizlilik beyanları ve inceleme hesabı. |

Bu rapor tamamlanan kodu, kalan geliştirmeyi ve dış kurulumları açıkça ayırır; uygulamanın tümü için yayın onayı veya tüm pencereler için cihaz testi iddiası değildir.
