# Native cihaz ve izin incelemesi — 8 Kasım 2026

Bu inceleme KMP `expect/actual` kaynaklarını, Android manifestini ve iki iOS plist dosyasını kapsar. Preview örnekleri ve tasarım korunmuştur. Bu dosyadaki kaynak denetimi, fiziksel cihazda izin verilmesi veya sağlayıcı teslim testi anlamına gelmez. Son kaynak değişikliklerinin Gradle/build ve ortak test yürütmesi ana görev tarafından yapılacaktır.

## Tamamlanan düzeltmeler

| Alan | Somut sorun | Son davranış |
|---|---|---|
| Android keşif ve nokta seçimi haritaları | Canlı modda koordinatları kullanmayan çizim ve indeks bazlı pinler vardı | MapLibre Native 13.7.1 gerçek `MapView`, koordinat/pin ID/seçim/zoom/sürükleme ve merkezden nokta seçimi kullanır. Preview eski çizime döner. SDK konum bileşeni etkinleştirilmez; haritayı görüntülemek kamera/GPS izni istemez. |
| Android harita yaşam döngüsü | Native view için Activity/GL yaşam döngüsü yönetilmiyordu | Create/start/resume/pause/stop/destroy ve düşük bellek iletilir. TextureView kullanılır; Compose yüzeyleri üzerinde çalışır. Camera güncellemesi yalnız giriş koordinatı/zoom/viewport değişiminde uygulanır. |
| Harita ilk merkezi | Native map'in ilk varsayılan merkezi nokta seçilmiş gibi algılanabiliyordu | Stil hazır ve viewport uygulanmış olmadan merkez callback'i verilmez. Nokta seçicide sayısal yuvarlama farkı kullanıcı hareketi sayılmaz. |
| iOS pin seçimi | Aynı başlığa sahip maçlar başlıkla eşleşip yanlış etkinliği açabiliyordu | `SportPinAnnotation.pinId` ile birebir eşleşir. Geçersiz koordinat filtrelenir; gösterim merkezi/span güvenli aralıkta tutulur. Kendiliğinden user-location açılmaz. |
| Manuel il/ilçe | Beş şehir dışındaki ilan başlangıcı Kadıköy'e düşebiliyordu | `EventLocationPicker` nullable mevcut nokta ve district alır; canlıda native geocoder ile seçilen bölge çözülür. Boş şehirde istek yoktur. Geocoding başarısızsa kullanıcı sürükleme veya GPS ile nokta seçmeden onay etkinleşmez. Preview örnek merkezleri korunur. |
| Android GPS | İlk uygun provider'a bağımlılık, eski callback ve reverse-geocoder bekleme riski vardı | İzinle kullanılabilen NETWORK/GPS sağlayıcıları dinlenir; geçersiz/eski fix reddedilir. Yeni istek/dispose generation ve reverse job'ı iptal eder. 25 saniyelik fix penceresi; reverse lookup en fazla 6 saniye sonra geçerli koordinatı koruyarak sonuçlanır. |
| iOS GPS | Önceki isteğin reverse callback'i yeni isteği bitirebiliyordu | Her istek ayrı CLLocationManager/delegate/geocoder nesnesine sahiptir. İptal, eski fix, negatif/invalid accuracy ve zaman aşımı denetlenir. Reverse-geocoder sorunu geçerli GPS koordinatını silmez. |
| GPS zaman damgası | Cache veya geocoding sonucu yeni alınmış fix gibi damgalanabilirdi | `capturedAt` gerçek native fix zamanından üretilir. Ortak tazelik kuralı koordinat, yaş ve accuracy değerlerini denetler. |
| İzin iptali ve foreground yenileme | Ayarlardan iptal edilen izin veya arka plana geçiş eski aktif isteği bırakabilirdi | Ön plana dönüşte iptal edilen konum izni mevcut cihaz konumunu temizler. Foreground yenileme active=false/konum=null olunca requester kaldırılıp iptal edilir. Otomatik yenileme `requestPermission=false` kullanır; izin penceresi açamaz. Arka plan takip eklenmedi. |
| Şehir yazımı | Native geocoder İngilizce yazım döndürünce şehir eşleşmesi bozulabiliyordu | Bilinen şehirler Türkçe katalog yazımına eşlenir; örneğin Istanbul → İstanbul, Sanliurfa → Şanlıurfa. |
| Yol tarifi | Public yuvarlanmış nokta hassas rota hedefi gibi kullanılabiliyordu | Ortak `eventDirectionsPoint` yalnız yetkili `meetingLocation` ve geçerli koordinatı kabul eder. Bu alan yoksa venue/ilçe/şehir adresi aranır; public `approximateLocation` rota hedefi yapılmaz. iOS adres query'si ayırıcı karakterlere karşı encode edilir. |
| Online/e-spor | Online buluşmaya fiziksel yol tarifi açılabiliyordu | Online ilanlarda yol tarifi gösterilmez; native çağrı da reddeder. Fiziksel LAN/noktası bulunan buluşma desteklenir. Online takvim notlarında platform/lobi/ses kanalı saklanır. |
| Android kamera | Dış kamera dönüşünde Activity yeniden oluşursa geçici dosya referansı kaybolabiliyordu | Pending dosya yolu `rememberSaveable` ile saklanır; başarısız/iptal çekim dosyaları temizlenir. EXIF yönü ve sınırlı boyutlu decode korunur. |
| iOS kamera/galeri | Kapanmış akışa geç izin veya iCloud yükleme callback'i dönebiliyordu | Picker lifetime dispose'da kapanır; yükleme iptal edilir, geç sonuçlar uygulamaya yazılmaz. Kamera/galeri iptali başarı sayılmaz. |

## İzin ve sistem akışlarının durumu

- Android manifest: INTERNET, CAMERA, COARSE/FINE konum ve POST_NOTIFICATIONS var. Kamera ve konum donanımı zorunlu değildir. Kullanılmayan READ/WRITE_CONTACTS, geniş galeri okuma, READ/WRITE_CALENDAR ve ACCESS_BACKGROUND_LOCATION eklenmemiştir.
- Android Photo Picker, desteklenen sistem seçicisini/fallback'ini kullanır; yalnız seçilen fotoğraf okunur. Kamera açık kullanıcı eyleminde izin ister. Dosya özel FileProvider üzerinden paylaşılır.
- iOS kamera için `NSCameraUsageDescription`, ön plan konum için `NSLocationWhenInUseUsageDescription`, iOS 16 takvim erişimi için `NSCalendarsUsageDescription` hem geliştirme hem dağıtım plist'inde var. Galeri PHPicker kullanır; tüm fotoğraf arşivine izin istemez.
- Takvim Android'de sistem ACTION_INSERT düzenleyicisidir; uygulama takvim veritabanına doğrudan yazmaz. iOS 16 ret durumunda izin/ayar açıklaması gösterilir. iOS 17+ EventKitUI düzenleyicisi izin istemeden kullanıcı kaydıyla çalışır; bu yolda ek geniş takvim erişimi istenmez. Sistem düzenleyicisinin açılması, kullanıcının kaydettiği iddiası değildir.
- Bildirimlerde OS izin sonucu döndürülür. APNs/FCM token/anahtar ve sunucu worker teslimi ayrı kurulum/testtir; izin verilmesi gerçek push teslimi olarak raporlanmaz.
- Hesap telefonu OTP alanı sistem rehberini kullanmaz; rehber izni gerektiren mevcut bir özellik yoktur.
- QR fotoğrafı aynı kamera/galeri seçicisini kullanır; çözümleme ayrı iş parçacığında yapılır. Geçersiz/okunamayan QR hata durumuna gider; sunucuda imza/süre/etkinlik kontrolü yapılmadan check-in başarı sayılmaz. Sürekli video tarayıcı eklenmemiştir.
- Paylaşım ve takvim iOS presenter'larında key-window fallback vardır; Android sistem uygulaması açılamazsa directions/calendar false döndürür. Cihazdaki gerçek hedef uygulamalarla doğrulama gereklidir.

Statik manifest/plist kontrolü: [native-permissions-static.json](native-permissions-static.json). Bu kontrol yalnız kaynak sözleşmesini doğrular.

## Test ve binary kanıtı

`NativeDeviceContractTest.kt` içinde **7 yeni ortak test** var: invalid koordinat, stale/future/accuracy fix, metreden zoom dönüşümü, Türkçe şehir yazımı, online/LAN ve exact/approx rota ayrımı, online takvim bilgisi ve antipodal mesafe. Bu alt görev Gradle çalıştırmamıştır. Ana görev son test/build sonucunu ayrıca kaydeder; önceki 37 test sonucu yeni testlerin geçtiği anlamına gelmez.

Mevcut Android APK için ana görevin [ELF kanıtı](android-native-alignment.json) ve [zipalign çıktısı](zipalign.txt) incelendi: APK'da 12 native kütüphane var; 6 adet 64-bit kütüphanenin PT_LOAD hizaları 16 KB ve `pass64Bit=true`. Zipalign doğrulaması başarılı. 32-bit MapLibre armeabi-v7a/x86 kütüphaneleri 4 KB hizalıdır; “12 kütüphanenin tamamı 16 KB” denmez. Bunlar ilgili APK hash'ine ait statik kanıtlardır; yeni kaynaklar veya release AAB/gerçek 16 KB cihaz testi yerine geçmez. Son native tur SDK/.so sürümünü değiştirmediğinden aynı binary kontrolü tekrarlanmadı.

## Harita sağlayıcısı ve lisans sınırı

MapLibre renderer hazırdır; harita içeriği ayrı sağlayıcıdır. `-Pmatchora.mapStyleUrl=https://.../style.json` veya `MATCHORA_MAP_STYLE_URL` ile HTTPS stil adresi gerekir. Yerel mod varsayılan demo stili yalnız entegrasyon kontrolü içindir; production release boş sağlayıcıyı ve `demotiles.maplibre.org` demo hostunu reddeder. Gerçek üretim sağlayıcısı/hesabı, ücret-kota, atıf ve kullanım koşulları kullanıcı tarafından seçilip yapılandırılmalıdır. Demo tile hizmeti üretim altyapısı olarak sunulmaz. SDK atıf kontrolleri kapatılmamıştır.

Sürüm ve API [MapLibre 13.7.1 resmi sürümü](https://github.com/maplibre/maplibre-native/releases/tag/android-v13.7.1), [resmi quickstart](https://maplibre.org/maplibre-native/android/examples/getting-started/) ve [resmi API](https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-map-libre-map/index.html) üzerinden doğrulandı. SDK'nın [BSD 2-Clause lisansı](https://github.com/maplibre/maplibre-native/blob/android-v13.7.1/LICENSE.md) ve bağımlılık bildirimleri dağıtım materyallerinde korunmalıdır. Takvim izin yaklaşımı [Apple EventKit açıklaması](https://developer.apple.com/documentation/eventkit/accessing-calendar-using-eventkit-and-eventkitui) ile, Android geo intent biçimi [Android resmi dokümanıyla](https://developer.android.com/guide/components/intents-common) eşleşir.

## Fiziksel cihazda kalan doğrulama

| Senaryo | Beklenen / kalan |
|---|---|
| Android minimum OS ve Android 12+ yaklaşık konum | Yaklaşık/precise izin, reddet/kalıcı ret, GPS/NETWORK ayrı ayrı kapalı; gerçek sağlayıcı ve zaman aşımı sınanmalı. |
| iOS 16 ve güncel iOS | WhenInUse, reduced accuracy, konum servisi kapalı, ayarlardan izin iptali ve uygulamaya dönüş gerçek cihazda sınanmalı. |
| Yeni istek / arka plan / geciken geocoder | Eski callback yeni isteği değiştirmemeli; arka plan foreground yenilemesini durdurmalı; offline lookup geçerli koordinatı kaybetmemeli. Kaynakta korumaları var, sensör/servis koşulları yürütülmedi. |
| Android canlı harita | Gerçek sağlayıcıda tile, ağ kesintisi/tekrar, marker seçimi, kaydırma/zoom ve nokta seçimi sınanmalı. Preview görüntüsü gerçek canlı harita testi değildir. |
| Kamera/galeri | Çek/iptal/ret, EXIF rotasyonu, HEIC/iCloud, bozuk/büyük resim, Android Activity yeniden oluşması sınanmalı. Tüm uygulamanın OS process-death sonrası form/gezinti taslağını eksiksiz geri yüklediği iddia edilmez. |
| QR | Basılı/ekrandaki güncel kod, ters açı/karanlık, bozuk/eski kod ve fiziksel kamera denenmeli. |
| Takvim ve paylaşım | iOS 16 izin ret/ayar dönüşü, iOS 17+ düzenleyici kaydet/iptal; Android takvim/harita uygulaması olmayan cihaz; iPad popover ve gerçek paylaşım hedefleri denenmeli. |
| Push | APNs/FCM gerçek hesapları, gerçek token ve foreground/background/kapalı uygulama teslimi doğrulanmalı. |

Bu sınırlar nedeniyle “native bağlantılar yazıldı” ile “bütün cihaz ve üretim testleri tamamlandı” ayrı durumlar olarak tutulmalıdır.
