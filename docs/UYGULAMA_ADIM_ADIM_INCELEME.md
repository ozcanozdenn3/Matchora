# Matchora — bölüm bölüm uygulama incelemesi

**Tarihsel kayıt:** bu belge 5 Kasım incelemesidir. Sonraki düzeltmeler ve güncel durum [6 Kasım tam envanterinde](TUM_EKRANLAR_BACKEND_ENVANTERI.md) yer alır.

Tarih: **5 Kasım 2026**. Sonuç: **uygulamanın tamamı bitmiş veya yayınlanmaya hazır değil.** Çekirdek API ve çok sayıda mobil bağlantı mevcut; aşağıdaki eksikler yalnız Supabase tabloları açılarak kapanmaz.

## Bu inceleme neyi doğruladı?

- **Ekranda görüldü:** açık iPhone 17 Pro simülatöründe ana akış, maç detayı, keşif menüsü, sıralama, diğer sporcu profili, turnuva listesi, fikstür, puanlar, analiz, takımlar ve Challenge teklif detayı açıldı. Örnek Onur Ak, Kadıköy Utd ve Kadıköy Sonbahar Kupası içerikleri duruyor.
- **Koddan izlendi:** diğer ekranların çağrıları, App yönlendirmeleri, ortak repository, sunucu komutları, Android/iOS cihaz uygulamaları ve testleri incelendi. Bu, her düğmenin cihazda uçtan uca çalıştırıldığı anlamına gelmez.
- **Görsel inceleme sınırı:** Ağ bölümüne geçişte bilgisayar kontrol aracı `Sky Computer Use native pipe closed before response` hatası verdi. Yeniden bağlanma ve araç oturumunu sıfırlama da başarısız oldu. Bu nedenle kalan ekranlara “simülatörde gezildi” denmiyor.
- Simülatör **preview** modunda tutuldu. `DemoData.kt` değiştirilmedi. Gerçek ödeme, gerçek SMS veya dış kullanıcıya mesaj gönderilmedi.

“Bağlantı var” aşağıda istemci çağrısının ve sunucu uygulamasının bulunduğunu belirtir. Gerçek sağlayıcı hesabıyla doğrulama yapıldığı anlamına gelmez. Genel API matrisi: [EKRAN_BACKEND_MATRISI.md](EKRAN_BACKEND_MATRISI.md).

## 1. Açılış, misafir modu ve menü

**Ekranda:** örnek maçlar ve ana akış açılıyor; menüden Yarış/Ağ girişleri mevcut. **Kodda:** bootstrap, oturum yükleme/yenileme, hata ve yeniden deneme; yazma işleminde hesap kapısı var. Canlı mod başarısız olduğunda mock başarıya geçmiyor.

**Kalan:** bütün ekranlarda çevrimdışı, ilk yükleme, boş veri ve erişim kaybının cihazda sınanması; büyük listelerde sayfalamanın tamamlanması. Çevrimdışı değişiklik kuyruğu yok.

## 2. Apple / Google / telefonla giriş

**Kodda:** Apple/Google için sistem tarayıcısı ve Supabase PKCE akışı; iOS ASWebAuthenticationSession, callback kontrolü, tek kullanımlık akış, sunucuda kimlik doğrulama; telefon için süreli OTP ve SMS adaptörü var. Oturumlar Android Keystore/iOS Keychain ile saklanıyor.

**Kalan:** gerçek sağlayıcı yapılandırması, redirect adresleri, SMS hesabı ve cihazda giriş/çıkış/hesap değiştirme testleri. WhatsApp OTP yok. Ayrı sağlayıcı hesaplarını güvenle birleştirme ve sağlayıcı erişimini iptal etme tamamlanmadı. Simülatörde Keychain'e gerçek oturum yazma/yenileme ayrıca doğrulanmalı.

## 3. İlk girişte Sporcu Pasaportu

**Kodda:** üç adım; branş ve bölge, rol/seviye, fotoğraf ve ad. İlk kayıt, sonraki düzenleme sürüm kontrolü, sunucunun koruduğu puanlar ve tek seferlik ödüller bağlı. Kamera/galeri ortak seçici kullanılıyor. Yasal belge sürümleri ve yeniden onay akışı var.

**Kalan:** gerçek girişten dönen kullanıcıyla tüm adımları cihazda tamamlama. Apple'dan profil fotoğrafı alınacağı varsayılmıyor; kullanıcı fotoğraf seçiyor. Gerçek yasal metinler/URL'ler bekleniyor.

## 4. Ana akış, radar ve akıllı eşleşme

**Ekranda:** maç radarları, örnek içerik, Challenge ve sıralama kartları görüldü. **Kodda:** bölge/branş/ağ filtreleri ve açıklanabilir kurallı eşleşme skoru var; iki özet kart ortak yüksekliği dolduruyor.

**Bu incelemede düzeltildi:** gizlilikte kişisel eşleşme sinyalleri kapatıldığında sunucu artık kişisel tercihleri skorlamıyor. Başka sporcunun tercihi etkilenmiyor; regresyon testi eklendi.

**Kalan:** eğitilmiş AI, otomatik maç/roster organizasyonu, tüm listelerde büyük veri sayfalaması. Bu ekranların “AI” ifadeleri gerçek bir model servisi bulunduğu anlamına gelmez.

## 5. Harita, il/ilçe ve gerçek konum

**Kodda:** native harita, gerçek etkinlik koordinatı, ön planda konum izni, GPS/geocoder, elle bölge seçimi ve konumdan yaklaşık mesafe var. Canlıda eksik koordinata mock pin üretilmiyor. Kullanıcının kesin cihaz konumu bellekte tutuluyor.

**Kalan:** fiziksel cihazda azaltılmış konum, hizmet kapalı, zaman aşımı ve kalıcı izin reddi testleri. İlçe alanı elle yazılıyor; tüm ilçelerin doğrulanmış katalog servisi yok. Arka plan konumu ve canlı konum paylaşımı yok.

## 6. Şimdi Oyna, acil oyuncu, rakip ve ilan oluşturma

**Kodda:** etkinlik arama/katılma ve üç adımlı ilan akışı; takvim tarihi, saat, konum, branş, kapasite, rol, özel ağ üyeliği ve taahhüt sunucuda doğrulanıyor. E-spor platform/sunucu/oyun etiketi ve ses kanalı metni saklanıyor.

**Kalan:** oyun hesaplarını platformdan doğrulama, oyun sunucusu API'leri ve uygulama içi sesli görüşme yok. Ses kanalı alanı yalnız metindir. Acil oyuncu çağrısının yakındaki uygun kişilere otomatik dağıtımı yok.

**Kaynakta kalan eski akış:** `CreateEventSheet` ve `TeamChallengeSheet` eski görsel kodları duruyor. Eski ilan formunda gerçek başlangıç tarihi üretilmiyor; bu formu açan aktif atama bulunmadı. Yeni bir giriş bağlanacaksa güncel Şimdi Oyna akışına taşınmalı. Eski bahis/hakem metinleri bir ödeme servisi sayılmamalı.

## 7. Maç detayı, katılım ve başvurular

**Ekranda:** Moda Akşam Halı Saha detayı, yer/zaman/ücret bilgileri açıldı. **Kodda:** katıl/ayrıl, başvur/geri çek, organizatör onay/ret, kapasite yarışı, bekleme listesi ve süreli kontenjan teklifleri var. Organizatör başlat/bitir/iptal ve plan düzenleme bağlı.

**Bu incelemede düzeltildi:** maç düzenleyicisi açıldığı sürümü saklıyor. Arka planda maç değişmişse yeni sürüm numarasıyla eski formu yazmak yerine sunucudan sürüm hatası alıyor.

**Kalan:** çok cihazlı gerçek ağ testi; resmi maçların gelişmiş yeniden planlama/operasyon akışları.

## 8. Check-in ve dijital kart

**Kodda:** süreli imzalı organizatör kodu, QR üretimi, kamera veya galeriden QR fotoğrafı okuma ve elle giriş var. Pasaport QR'si ayrı imzalı doğrulama akışı kullanıyor. Taahhüt iadesi sunucuda tekil.

**Kalan:** sürekli video QR tarama, geofence ve cihaz bütünlüğü kontrolü yok. Fotoğrafla QR okuma gerçek kamerayla sınanmalı. Pasaport QR'si resmi kimlik belgesi değildir.

## 9. Sonuç, itiraz, MMR, güvenilirlik ve değerlendirme

**Kodda:** kadro/taraf doğrulama, skor gönderme, rakip onayı, itiraz, operatör kararı ve süreli kesinleşme var. Doğrulanmış rekabetçi sonuçtan MMR; check-in kaydından güvenilirlik, fair play ve taahhüt sonucu üretiliyor. Kendi kendini değerlendirme engelleniyor.

**Kalan:** itirazları yönetecek operasyon ekranı ve yayınlanmış ürün kuralları. Check-in olmaması tek başına fiziksel olarak maça gelinmediğini kanıtlamaz; no-show politikası cihaz/operasyon senaryolarıyla doğrulanmalı.

## 10. Sıralama ve diğer sporcu profili

**Ekranda:** sıralama kapsam/branş alanları, Onur Ak profili ve DM/davet aksiyonları görüldü. **Kodda:** gerçek sıralama, haftalık MMR farkı ve görünürlük tercihleri; DM ve takım daveti bağlı.

**Bu incelemede düzeltildi:** diğer sporcu profilindeki sabit `W/W/W/L/W`, 8.x maç notları, MVP/namağlup rozetleri yalnız preview'da kalıyor. Canlıda eksikliği belirten durum gösteriliyor. Canlı oyuncu eşlemesindeki kanıtsız `%0 uyum` değeri kaldırıldı; katalogda bulunmayan sporcuda örnek `%95` güvenilirlik, doğrulama ve ortak maç sayısı üretilmiyor.

**Kalan:** diğer sporcunun son maç formu, gerçek başarıları, ortak maç sayısı, müsaitliği ve Sports Chemistry hesabının profil ekranına bağlanması. Bu bölümün tüm detayları tamamlanmış değildir. Sporcudaki “Meydan Oku” aksiyonu takım sekmesine götürüyor; bağımsız birebir challenge ürünü yok.

## 11. Takımlar, kadro ve başvurular

**Ekranda:** Kadıköy Utd, kadro/başvuru girişleri ve teklifler görüldü. **Kodda:** takım kurma, ilan düzenleme, başvuru/geri çekme/inceleme, davet/kabul/ret, ayrılma, kaptan devri, gerçek kadro ve doğrulanmış maç istatistikleri var.

**Kalan:** geniş kulüp yönetimi, yardımcı kaptan/yedek rolü ve takım başarılarının otomatik yönetimi. Tüm rol/yetki durumları cihazda gezilmedi.

## 12. Challenge ve maç sözleşmesi

**Ekranda:** Bostancı Rovers teklif detayı açıldı; zaman, saha ve taahhüt görüldü. **Kodda:** kaptan yetkisi, teklif/geri çekme/kabul/ret, kabulde kalıcı maç/kadro/sohbet oluşturma ve taahhüt çözme var.

**Kalan:** şehir kemeri, hakem hizmeti, ödül/bahis havuzu ve gerçek rakip AI servisi yok. Bazı saha önerileri statik; gerçek tesis kataloğuyla bütünleşmeli. Kaynakta duran Match Scouter için aktif açılış ataması bulunmadı.

## 13. Turnuva ve lig

**Ekranda:** Kadıköy Sonbahar Kupası, fikstür, boş puan durumu ve DEMO analiz sekmesi açıldı. **Kodda:** oluşturma, kayıt/çekilme, kadro dondurma, mükerrer oyuncu engeli, planlama, doğrulanmış sonuçla sonraki tur ve lig puanı, bitirme/iptal var.

**Kalan:** formdaki her serbest format desteklenmiyor. Grup+eleme, İsviçre sistemi ve gelişmiş eşitlik bozma ayrıca uygulanmalı. Ödül açıklaması gerçek para/Token ödül dağıtımı değildir. E-spor platformu ve özel branş gibi ek bilgilerin ayrı yapılandırılmış alanlar olarak taşınması genişletilmeli. Canlı analiz kurallı özettir; kazanma tahmini servisi yok.

## 14. Ağ, topluluk, kampüs/şirket ve seyahat modu

**Kodda:** açık/onaylı/davetli üyelik, başvuru yönetimi, davet kabul/ret, davet gönderme/yönetici devri, üyeliğe göre özel maç erişimi ve seyahat tercihi var.

**Kalan:** topluluk oluşturma API'sinin kullanıcı formu ve geniş üye yönetim ekranı yok. Kampüs/şirket aidiyetinin kurumsal e-posta veya kuruluş sistemiyle doğrulanması yok. Yeni davet bildiriminin doğrudan ilgili topluluğa götürülmesi geliştirilmeli.

## 15. Tesis, müsait saat, rezervasyon ve iptal

**Kodda:** tesis/saat oluşturma yetkileri, çakışma/çift rezervasyon engeli, sunucudan toplam tutar, iptal sınırı, işletmeci onayı, ödeme kaydı ve tamamlanma komutları var. Kullanıcı rezervasyonları oyun takviminde görülebiliyor. Tahsilat yöntemi tesiste ödeme.

**Kalan:** işletmeci paneli, tesis yorum/puan sistemi, gerçek kart tahsilatı/iade, ücret bölüşümünün katılımcılardan tahsilatı ve ödenmiş rezervasyon kapanışı yok. Kişi başı gösterim yalnız hesap önerisidir; sunucu toplam saha bedelini rezerve eder. Tesis kataloglarında coğrafi alanların genişletilmesi gerekir.

## 16. Sohbet ve fotoğraf iletme

**Kodda:** kalıcı DM/maç/takım sohbeti; üyelik ve iki yönlü engel kontrolü, metin/fotoğraf, okundu, sessiz, eski mesaj sayfalaması ve kaynak erişimi kontrollü fotoğraf iletme var. Aktif sohbette 3 sn, diğer aktif ekranlarda 20 sn sorgulama kullanılıyor.

**Kalan:** Realtime/WebSocket, yazıyor göstergesi, çevrimiçi durum, düzenleme/silme, ses/video mesaj ve çevrimdışı gönderim kuyruğu yok. Bunlar mevcut metin/fotoğraf mesajlaşmasının tamamlanmasıyla kendiliğinden gelmez.

## 17. Anı vitrini ve paylaşım

**Kodda:** özel medya yükleme, bekleyen/onaylı içerik, beğen/sil/bildir, kamera/galeri ve sohbetle paylaşma var.

**Bu incelemede düzeltildi:** vitrine tek başına paylaşımda desteklenmeyen `forwardId` gönderiliyordu. “Vitrin + sohbet” ise canlıda yalnız mesaj atıyordu. Artık tek işlem içinde incelemeye gönderilen anı ve sohbet fotoğrafı birlikte kaydediliyor; yetkisiz/kapalı sohbet tüm işlemi geri alıyor. Aynı isteği tekrar denemek çift paylaşım üretmiyor. Normal fotoğraf iletme ayrı kalıyor.

**Kalan:** skor etiketi ve sahne teması API kaydına taşınmıyor; paylaşım sonrası kalıcılığı eksik. Saha seçenekleri statik; gerçek katalog veya serbest yer girişiyle tamamlanmalı. Otomatik içerik tarama ve moderatör paneli yok. Onaylanmamış anı açık vitrine çıkmıyor; yazar kendi bekleyen paylaşımını görüyor.

## 18. Pasaport, sporlar, rozetler, görevler ve geçmiş

**Kodda:** profil/fotoğraf, branş/rol/seviye düzenleme, doğrulanmış kişisel maç/MMR/güvenilirlik geçmişi; gerçek kayıtlarla rozet, seri ve tekil görev ödülleri var. Profil sayfasına canlı geçmiş için boş liste dahil açık override gönderiliyor; mock geçmişe düşmüyor.

**Kalan:** görevlerin tamamını gösterme ve kazanıldı durumunu ayrıntılandırma, referans programı/kemer ve gelişmiş otomasyon. Yerel sıra profil becerisinde doldurulmuyor; sıralama sayfasındaki gerçek sıra ile aynı alana bağlanmalı. Başkasının ayrıntılı pasaport istatistikleri 10. maddede açık kaldı.

## 19. Cüzdan, Token, Plus ve mağaza

**Kodda:** sunucu defteri/kilitler; RevenueCat native katalog/fiyat, satın al/restore/yönet; kullanıcı kimliğine bağlama, doğrulanmış webhook/sync, tekrar işlem engeli, iade ve aylık hak pencereleri var. Plus pasaport çerçevesi aktif hakka bağlı.

**Kalan:** mağaza/RevenueCat yapılandırması ve satın alma/restore/iade/hesap değiştirme sandbox testleri. Tüm transfer/iade olayları otomatik çözülmüyor. VIP filtre/öncelik/komisyon avantajlarının tamamı uygulanmadı. `admin.feature.set(billing)` bayrağının mağaza kullanılabilirliğine etkisi bağlanmamış; acil satış kapatma davranışı tamamlanmalı. Önceden ödenmiş işlemlerin uzlaştırılması satış kapatmadan ayrı tutulmalı.

## 20. Bildirimler

**Kodda:** bildirim kutusu/okundu, tercihler, yaklaşan maç hatırlatması, kuyruk, sessiz saat/günlük sınır/yeniden deneme; APNs/FCM gönderimi ve native token/tıklama bağlantısı var.

**Kalan:** gerçek push anahtarları ve cihaz testi. Acil oyuncu eşleşmelerinin dağıtımı, haftalık özet, otomatik rematch ve kampanya üreticileri yok; tercih alanının saklanması bu işlerin mevcut olduğu anlamına gelmez. Tüm hedeflerde doğrudan ayrıntı ekranına geçiş ve sayfalanmış/eski içerik hedefleri sınanmalı.

## 21. Hesap, gizlilik, destek, rapor ve silme

**Kodda:** gizlilik/bildirim tercihi, engel kaldırma, destek/şikayet, operatör karar API'leri, kendi JSON verisini paylaşma, açık oturumları kapatma, silme talebi/geri çekme ve gecikmeli temizleme var. Takım/topluluk yöneticiliğinin devri ve medya silme kuyruğu mevcut.

**Kalan:** destek yanıtı bildirim olarak geliyor; talep listesindeki ayrıntılı yanıt/durum konuşması tamamlanmadı. Operatör paneli yok. Supabase Auth/Apple erişim iptali, RevenueCat/mağaza uzlaştırması, ücretli rezervasyon ve tesis sahipliği kapanışı tam değil. Toplu medya dışa aktarımı yok.

## 22. Privacy Policy, Terms of Use ve diğer bağlantılar

**Kodda:** Privacy Policy, Terms of Use, topluluk kuralları, hesap silme ve destek alanları tıklanabilir. URL yoksa açıklama açılıyor; sahte belge açılmıyor. Sürümlü belge yayını, ilk onay ve yeniden onay var; destek ve hesap kapatma güncel onay olmadan erişilebilir.

**Kalan:** gerçek metinler, HTTPS URL'ler, sürümler ve destek adresi. Bunları sen verdiğinde yapılandırmaya yerleştirmek gerekiyor. Mağaza gizlilik/veri güvenliği beyanı ve son yasal değerlendirme tamamlanmadı.

## 23. KMP cihaz ve izin kontrol listesi

| Kullanım | Android | iOS | Kalan doğrulama |
|---|---|---|---|
| Profil/sohbet/anı/doğrulama fotoğrafı | Kamera izni, TakePicture, Photo Picker, FileProvider | AVFoundation kamera izni, UIImagePicker, PHPicker | Gerçek kamera, iptal, ret ve ayarlardan geri dönüş |
| Bölge ve ilan noktası | Coarse/fine, LocationManager, geocoder | WhenInUse, CLLocationManager, CLGeocoder | Azaltılmış konum, kapalı servis, zaman aşımı |
| Bildirim | Android 13+ izin, kanal, FCM | Sistem izni, APNs/delegate | Gerçek push, ön/arka plan ve sonlandırılmış uygulama |
| Yol tarifi ve davet | Sistem harita/paylaşım intent'i | Harita ve share sheet | Uygun uygulama yok/iptal durumları |
| Cihaz takvimi | ACTION_INSERT sistem düzenleyicisi | iOS 17+ EventKitUI; iOS 16 izin/ret yolu | Minimum desteklenen sürüm ve cihaz |
| QR | QR üretme ve fotoğraftan çözümleme | QR üretme ve fotoğraftan çözümleme | Kamera fotoğrafıyla sahada deneme |
| Oturum ve veri dışa aktarımı | Keystore/AES-GCM, dosya paylaşımı | Keychain, dosya paylaşımı | Gerçek oturum yazma/yenileme, cihaz imzası |

Mikrofon, kişiler, Bluetooth ve arka plan konumu mevcut özellikler için istenmiyor. Galeri için tüm arşivi okuma izni şart koşulmuyor. Bu tablo kod kapsamıdır; tüm fiziksel cihaz senaryoları geçti iddiası değildir.

## 24. Sunucu, Supabase ve yayın

57 tablo ve 79 yetkili komut var; SQLite ve PostgreSQL adaptörleri, RLS SQL'i, özel Storage, iş kuyruğu, idempotency, rol/durum/sürüm kontrolleri, Docker/CI hazırlığı bulunuyor. [SQL Editor paketi](../backend/supabase/README.md) hazır.

**Kalan:** Supabase bağlantısı yanında Node API'nin HTTPS ortamında yayınlanması, gizli anahtarlar, sağlayıcılar, işler/izleme/uyarı, yedekten dönüş, performans/güvenlik ve mağaza dağıtımı gerekir. SQL Editor API'yi veya native sağlayıcıları kurmaz. PostgreSQL yazmaları global kilitle seri hale getiriliyor; üretim yükünde ölçek testi gerekir.

## Tamamlama önceliği

1. Canlıda bilgi kaybı/yanlış izlenim: diğer sporcu ayrıntıları, anı ek alanları, mağaza bayrağı, destek ayrıntısı ve eksik kataloglar.
2. Kullanıcıya sunulan yönetim: topluluk oluşturma, tesis işletmeci ve moderasyon panelleri.
3. Ürün kapsamı: turnuva formatları/ödüller, acil çağrı/özet/rematch işleri ve vaat edilen Plus avantajları; yayın kapsamına alınacaklar netleştirilmeli.
4. Supabase/API ve sağlayıcı kurulumu; gerçek iki hesapla tüm işlemleri yürütme.
5. Fiziksel Android/iPhone, mağaza sandbox, silme/iade, erişilebilirlik, minimum sürüm ve yayın kontrolleri.

Son test/build kanıtları ve genel kalan işler [BACKEND_DURUM_RAPORU.md](BACKEND_DURUM_RAPORU.md) ile [doğrulama klasöründe](validation/2026-11-05/) tutulur. Geçen otomatik test sayısı, bütün UI özelliklerinin tamamlandığı anlamına gelmez.
