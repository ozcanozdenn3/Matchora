Son doğrulama ve açık işler: [10 Kasım son durum](SON_DURUM_2026-11-10.md).

# Matchora — backend ve yayın hazırlığı raporu

**Son durum ve kalan işler: [9 Kasım tamamlama raporu](TAMAMLAMA_RAPORU_2026-11-09.md).**

Güncel kapsam: [8 Kasım, her UI dosyası ve işlev zinciri](KAPSAMLI_ISLEV_DENETIMI_2026-11-08.md). İlan onayı/bekleme, gerçek sıralama, topluluk başvuru bildirimi, işletmeci başvurusu/tesis-saat yönetimi, kilometreli acil çağrı, sekiz rozet ve geçmiş düzeltmeleri bu tur eklendi. 007/008/009/010 SQL paketi hazır.

Son işlev düzeltmeleri: [ilan/kadro ve uygulama yönetimi — 7 Kasım](ISLEV_VE_KADRO_DUZELTMELERI_2026-11-07.md).


Güncelleme: **9 Kasım 2026**. Bu rapor kodu, doğrulanan davranışları, dış servis kurulumunu ve ayrıca geliştirme isteyen işleri ayrı gösterir. **Canlı servisler ve mağaza testleri tamamlanmadan uygulama yayınlanmaya hazır sayılmamalıdır.**

## Görünüm ve mock veriler

- `DemoData.kt` değiştirilmedi; maç, takım, sohbet, vitrin, pasaport, turnuva, sıralama ve cüzdan örnekleri duruyor.
- Çalıştırma betiği önce `preview` framework hazırlar; backend modu için ayrıca test cihazı UDID seçilmesi gerekir. Böylece yerel test paketi kullanıcının açık mock simülatöründe yanlışlıkla kalmaz.
- Varsayılan mod `preview`. Bu mod tasarım incelemesi için örnek verileri gösterir. `local` gerçek yerel API'yi, `production` HTTPS API'yi kullanır. Backend kodu korunur.
- Challenge ve sıralama kartları aynı satır yüksekliğini doldurur. Renk, kart, ikon ve ana gezinme sistemi korunmuştur.
- Backend modunda başarısız işlem demo bakiyesine veya sahte başarıya dönüşmez. Canlı mağaza fiyatları SDK'dan gelir; yapılandırılmamış ürün gerçek fiyatmış gibi gösterilmez.
- Mevcut ekranların ve alt akışların envanteri: [ekran / backend matrisi](EKRAN_BACKEND_MATRISI.md). Son gezintinin bulguları ve ekran/kod ayrımı: [adım adım inceleme](UYGULAMA_ADIM_ADIM_INCELEME.md).

## Güncel tam ekran envanteri

[398 satırlık tam ekran/pencere envanteri](TUM_EKRANLAR_BACKEND_ENVANTERI.md), eski 253 satırın üzerine eklenen yönetim ve açıklama pencerelerini de kapsar. [Kaynak dizini](UI_KAYNAK_DIZINI.md), 56 ortak UI dosyası, 261 fonksiyon bildirimi, 95 pencere/menü açılış noktası, 21 native Kotlin dosyası ve 111 komutu kapsar. Sayılar otomatik kaynak taramasıdır; cihaz testi sayısı değildir.

6 Kasım tamamlaması: MMR politikası ve maç bazında bekleme nedeni, puan sıfırlamaya karşı branş arşivi, karşı taraf çoğunluğu/check-in, no-show ve haftalık ödül düzeltmeleri; bütün görevlerin penceresi; uygulanabilir turnuva format/kapasite/MMR/telefon koşulları; topluluk oluşturma/düzenleme/üye çıkarma; gerçek tesis seçimi, kuruş hassasiyetli ücret, müşteri yorumu; tesis işletmeci ve moderasyon merkezleri; acil çağrı/haftalık özet/rematch/izinli duyuru üreticileri; şehirli, şifreli cursor ile maç sayfalama ve manuel bölge merkezleme.

Güncel ayrıntı ve kalan geliştirmeler: [özellik tamamlama raporu](OZELLIK_TAMAMLAMA_2026-11-06.md). Dış servis hesapları, gerçek yasal metinler, fiziksel cihaz ve mağaza testleri tamamlanmış gösterilmez. Büyük listelerden maç ve mesaj sayfalaması bağlıdır; konuşma sayfalaması da bağlıdır; bazı kişisel/operasyon geçmişlerinde sınırlar vardır. Gelişmiş turnuva formatı, şehir kemeri/referans, öğrenilmiş AI, gerçek zamanlı bağlantı ve dış sağlayıcı hesap kapanışı ayrıca geliştirme gerektirir.

Görsel kontrol kapsamı ve test kanıtları aşağıdadır. Kaynak envanteri, bütün pencerelerin cihazda uçtan uca gezildiği anlamına gelmez.

## Altyapı

`backend/` içinde Node.js 24 API'si, **67 tablo**, **108 yetkili komut**, SQL işlemleri, idempotency, rol kontrolleri ve iş kuyrukları bulunur. Yerelde SQLite, son bağlantı için PostgreSQL adaptörü hazırlanmıştır. Mobil istemci veritabanına doğrudan yazmaz. Supabase'e canlı bağlantı veya canlı şema değişikliği yapılmadı.

KMP ortak repository; Android HTTP/Keystore ve iOS NSURLSession/Keychain uygulamalarını kullanır. Oturum yenileme, dönen refresh tokenını saklama, yetkisi iptal edilen oturumda özel veriyi temizleme, hata ve aynı anahtarla yeniden deneme yolları vardır. Profil düzenleme, açıldığı sürümü korur; arka plan yenilemesi formu sıfırlamaz. Yoğun okumalarda aynı sporcu/profil tekrarları istek içinde önbelleklenir, sıralama değişimleri toplu hesaplanır.

RevenueCat KMP **3.16.0**, uyumlu Kotlin **2.3.20**, Android FCM, QR çözümleme ve sunucuda Sharp görsel yeniden kodlama bağımlılıkları eklendi. Swift uyumluluk kitaplıklarının yolu aktif Xcode'dan çözülür.

## Uygulama boyunca yazılanlar

| Alan | Kod ve istemci bağlantısı | Sınır / dış kurulum |
|---|---|---|
| Misafir keşfi | Harita, maç, takım, turnuva ve açık vitrin; işlem sırasında hesap kapısı | Canlı veri için API adresi gerekir |
| Apple / Google giriş | Sistem tarayıcısı; iOS ASWebAuthenticationSession; Supabase PKCE, kısa süreli tek kullanımlık akış, callback doğrulaması ve sunucuda kimlik doğrulama | Sağlayıcı hesapları, redirect allowlist ve gerçek giriş testi bekliyor. Ayrı hesapları otomatik birleştirme yok |
| Oturum | Kısa erişim tokenı, rotasyonlu yenileme, tekrar kullanımında aile iptali, çıkış, oturum listeleme/kapatma | Son cihaz imzasıyla Keychain doğrulaması gerekir |
| Telefon | Süreli altı haneli OTP, gönderim/deneme sınırı, mevcut hesaba bağlı doğrulama; doğrudan Twilio veya SMS köprüsü | SMS hesabı/anahtarı ve gerçek teslim testi. WhatsApp ayrı entegrasyondur |
| Pasaport | Üç adımlı giriş, sağlayıcıdan gelen isim/iletişim ipucu, branş/rol/seviye, profil fotoğrafı, alan/sürüm doğrulama, bir defalık ödüller | Apple profil fotoğrafı sağlıyormuş gibi davranılmaz. Fotoğraf kullanıcı tarafından seçilir |
| Doğrulama | Telefonun formdan doğrulamasız değiştirilmesini engelleme; profil fotoğrafını operatör incelemesine gönderme | PHOTO rozeti canlılık testi veya resmi kimlik doğrulaması değildir |
| Dijital sporcu kartı | Süreli imzalı QR, doğrulama API'si; özel telefon/e-posta içermez | Kimlik belgesi veya maç bileti yerine geçmez |
| Konum | Özellik anında ön plan izni, gerçek konum, il/ilçe çözümleme, elle seçim, yaklaşık mesafe; canlı veride uydurma harita pini yok | Fiziksel cihazda doğruluk, azaltılmış izin ve servis kapalı durumları sınanmalı |
| Keşif / eşleşme | Branş, bölge, seviye, güvenilirlik ve müsaitlikten açıklanabilir skor; kampüs/şirket erişim kontrolü; seyahat tercihi | Eğitilmiş AI servisi değildir; gelişmiş otomasyon ayrı iştir |
| Maçlar | Oluşturma, katılım/ayrılma, başvuru/geri çekme/onay/ret, kapasite yarışı, sıra ve süreli kontenjan teklifi | Ana akış/harita liste görünümü daha fazla maç yükler; başka katalog/geçmiş listeleri hâlâ sınırlıdır |
| Organizatör | Gelecek maçın planını sürüm kontrolüyle düzenleme, katılımcıya değişiklik bildirimi; başlat/bitir/iptal | Resmi challenge/turnuva kadrosu sıradan maç düzenlemesiyle değiştirilemez |
| Check-in | Maç bazlı taahhüt, tekil iade, süreli HMAC kodu, gerçek QR üretme; kamera/galeri fotoğrafından QR okuma ve elle giriş | Sürekli video tarayıcı ve geofence/cihaz bütünlüğü yok |
| Süre sonları | Geçmiş maçı keşiften çıkarma; kendi geçmişini koruma; terk edilen doğrulanmamış taahhüdü 72 saat sonra çözme | İtirazlı sonuçlar operatöre kalır |
| Sonuç / MMR | Skor, taraflar, check-in, rakip çoğunluğu, 24 saat itiraz; K=24 Elo, gerçek sınırlandırılmış delta, no-show −10 güvenilirlik; branş sil/ekle puanı sıfırlamaz | Başlangıç 1000, denk rakipte ±12; aykırı/sorunlu sonuç yönetici incelemesine kalır |
| Geri bildirim | Gerçek katılımcı kontrolü, kendini puanlamayı engelleme, fair play / tekrar oynama tercihi | Doğrulanmadan toplu puan üretmez |
| Takım | Kurma, ilan düzenleme, başvuru/geri çekme, inceleme, davet/kabul/ret, ayrılma, kaptan devri, üyeliğe bağlı sohbet | İstatistikler doğrulanmış maçlardan gelir |
| Challenge | Tarih/saat, kaptan yetkisi, teklif taahhüdü, gönderilmiş teklifi geri çekme, kabul/ret, kalıcı maç/kadro/sohbet | Bahis, ödül havuzu ve hakem tahsilatı servisi yok |
| Turnuva / lig | Kadro dondurma, mükerrer oyuncu kontrolü, kayıt/çekilme, fikstür, sonraki tur, gerçek sonuç/puan tablosu, lig kapanışı, iptal | Canlı form KNOCKOUT / ROUND_ROBIN, gerçek kapasite, tüm kadroya min MMR/telefon kontrolü; İsviçre/grup+eleme gibi gelişmiş motorlar ayrıca gerekir |
| Topluluk | Açık/onaylı/davetli üyelik, başvuru yönetimi, davet kabul/ret, davet gönderme, yönetici devri ve özel ağ erişimi | Oluşturma/düzenleme ve üye çıkarma formları bağlı; gelecekteki özel maç üyeliği/taahhüdü temizlenir |
| Tesis | Kullanıcı başvurusu/admin onayı; tesis yayınlama; tesis/saat düzenle-kapat-yeniden aç; kort sayısı, çakışma ve çift rezervasyon denetimi | Başvuruda insan yetki incelemesi; işletmeci gerçek gün/saat/süre/ücret girer |
| Rezervasyon | Sunucudan toplam saha ücreti, kişi başı hesap önerisi, iptal son tarihi, tesis onayı, ödeme kaydı ve tamamlanma komutları | Şu an tesiste ödeme. Bölüşüm seçimi toplam borcu azaltmaz. Kuruş hassasiyeti, tamamlanan rezervasyona yorum/moderasyon, offline iade kaydı bağlı. Gerçek kart tahsilatı/iadesi yok |
| Mesajlaşma | Kalıcı DM/maç/takım sohbeti, metin/fotoğraf, üyelik, iki yönlü engel, okundu, sessize alma, eski mesajları yükleme | Aktif sohbette 3 sn, diğer aktif ekranlarda 20 sn sorgulama. WebSocket/Supabase Realtime yok |
| Fotoğraf iletme | Kaynak sohbete erişim veya onaylı açık anı kontrolü; başka özel dosyayı ID ile iletme engeli | Sürekli bağlantı/delta senkronizasyonu ayrıca geliştirilebilir |
| Saha anısı | Fotoğraf yükleme, skor/tema, atomik vitrin+sohbet, inceleme, moderatör erişimi, onay/ret, beğeni, silme/bildirme | Onay/ret ve fotoğraf talepleri moderasyon merkezine bağlı; otomatik içerik taraması yok |
| Kamera / galeri | Ortak kaynak seçici; profil, sohbet, anı ve doğrulamada yerel kamera / sistem fotoğraf seçici; ret/hata/ayar yolları | Fiziksel kamera testi bekliyor; tüm galeri erişimi şart koşulmaz |
| Dosyalar | Gerçek görsel çözümleme, yeniden boyutlama/JPEG kodlama, metadata temizleme, boyut/kota, özel Supabase Storage adaptörü, yetim dosya ve yeniden denenen silme kuyruğu | Özel bucket ve sunucu anahtarı bekliyor. Gerçek bulut testi yapılmadı |
| Bildirim | Kalıcı kutu, tercihler, okundu, yeniden planlanan maç hatırlatmaları, outbox, sessiz saat, sınır/backoff, APNs/FCM sunucu gönderimi, Android FCM token/callback, iOS delegate/deep link | Firebase dosyası, APNs/FCM anahtarları, imza/provisioning ve gerçek cihaz testi gerekir |
| Token / Plus | RevenueCat SDK ürün/fiyat yükleme, native satın alma, geri yükleme, mağazada yönetim; gerçek kullanıcı ID'sine bağlama; sunucu sync/webhook ve defter | Mağaza/RevenueCat hesapları, ürünler, anahtarlar ve sandbox testleri bekliyor |
| Abonelik ödülleri | Doğrulanmış ücretli dönem pencereleri; yıllık planın aylık hakları; İstanbul takvim ayında tek 100 TKN; restore/ürün değişiminde tekrar yüklemeyi önleme; ödenmemiş aralığa ödül vermeme | Transfer ve bazı iade/ürün yaşam döngüleri inceleme kuyruğuna düşer; tam otomasyon değildir |
| Plus sunumu | Aktif hakla pasaportta altın çerçeve; canlı ekranda yazılmış avantajların gösterimi | Preview'daki VIP radar/ilan vaatlerinin tamamı canlı ürün olarak uygulanmadı; canlı metin bunları vaat etmez |
| Diğer sporcu ayrıntısı | Gizlilik/engel kontrollü gerçek son form, MMR farkı, kazanılmış rozetler, ortak maç ve geri bildirim uyumu | Anlık müsaitlik ve bireysel performans notu servisi yok |
| Ödül / rozet | Sekiz rozetin tamamı: check-in/seri/fair play/ilk maç organizasyonu/şampiyonluk/acil katılım/maç/şehir/hafta; bütün görevler, hafta devrinde de tekil TKN ödülü | Referans programı ve şehir kemeri için tam iş akışı yok |
| Cihaz uygulamaları | Maç davetini native paylaşma, haritada yol tarifi, cihaz takviminde kullanıcı onayıyla etkinlik oluşturma, hesap verisini native paylaşma | Fiziksel cihaz / eski desteklenen iOS sürümüyle test bekliyor |
| Hesap / gizlilik | Tercihler, rapor/destek kaydı, operatör yanıtı, kendi verisini JSON dışa aktarma, oturum kapatma, silme ve geri çekme | Medyanın toplu dışa aktarımı, nihai saklama politikası ve destek operasyonu ayrıca ele alınmalı |
| Hesap silme | Özel veriyi temizleme, oturum/medya/üyelik silme, canlı maç/taahhüt kapatma, takım/toplulukta yöneticiliği devretme | Supabase Auth/Apple sağlayıcı hesabı-token iptali, RevenueCat/mağaza uzlaştırması, ücretli fiziksel rezervasyon ve tesis sahipliği kapanışı ayrıca tamamlanmalı |
| Yasal bağlantılar | Privacy Policy, Terms of Use, kurallar, silme ve destek tıklanabilir; URL yoksa açıklama; sürümlü yayın/onay; yeni sürümde yeniden onay; onaysızken destek/silme açık | Gerçek metin ve URL'ler bekleniyor |
| Güvenlik / operasyon | İşlemsel yazma, yetki ve durum kontrolü, rate limit, idempotency, değiştirilemeyen finans/denetim, özellik bayrağı, health, Docker ve CI | Canlı yük/güvenlik, izleme/uyarı, yedek/geri yükleme ve son yayın kontrolleri gerekiyor |

## İzin ve cihaz yaklaşımı

- Konum yalnız ilgili özellik kullanıldığında istenir. Arka plan konumu istenmez; yaklaşık konum/elle seçim desteklenir.
- Kamera fotoğraf ve QR fotoğrafı içindir. Android Photo Picker / iOS PHPicker yalnız seçilen görsele erişir; tüm galeri izni zorunlu değildir.
- Bildirim izni sistemin sürümüne göre istenir. Android kanal/FCM servisi ve iOS APNs kaydı hazırlanmıştır.
- Android takvim akışı sistem düzenleyicisini açar. iOS 17+ EventKitUI kullanıcı onayıyla ekler; iOS 16 için izin ve ret/ayar yolu vardır. Uygulama takvimi sessizce okumaz.
- Paylaşım ve yol tarifi sistem arayüzlerini açar. Mağazanın Face ID/parmak izi onayı SDK/mağaza tarafından yönetilir; uygulamaya gereksiz biyometri izni eklenmez.
- Kullanılmayan mikrofon, kişiler, Bluetooth ve arka plan konum izinleri eklenmedi. Metin olarak girilen ses kanalı, uygulama içi sesli görüşme servisi değildir.

## Supabase'de çalıştırılacaklar

[Kurulum paketi](../backend/supabase/README.md):

1. [01_schema.sql](../backend/supabase/01_schema.sql): şema, indeks, RLS, defter/denetim koruması.
2. [02_storage.sql](../backend/supabase/02_storage.sql): özel `matchora-private` bucket.
3. [03_verify.sql](../backend/supabase/03_verify.sql): salt okunur kontroller.

Bu paket demo kullanıcı/işlem veya gizli anahtar eklemez. **SQL Editor Node.js API'sini yayınlamaz.** API ayrıca HTTPS üzerinde çalıştırılır; Dockerfile hazırdır. `DATABASE_URL`, özel Storage anahtarı, Auth sağlayıcıları ve mobil API adresi kurulumda tanımlanır. Canlı şemaya uygulanmadan staging/yedek kontrolü gerekir.

## Kalan işler

**Hesap, URL ve canlı kurulum bekleyenler:** Supabase projesi/DB/Storage/Auth, API barındırma ve HTTPS, Apple/Google sağlayıcı bilgileri, SMS, `google-services.json`, APNs/FCM, RevenueCat/mağaza ürünleri ve anahtarları, gerçek yasal URL'ler/sürümler, destek adresi, dağıtım imzaları.

**Ayrıca kod/ürün/operasyon gerektirenler:** güvenli hesap birleştirme ve sağlayıcı token iptali; dış servislerde hesap silme uzlaştırması; tüm RevenueCat transfer/iade senaryolarının otomasyonu; gerçek kartla fiziksel rezervasyon iadesi/işletmeci devri; tekrarlı toplu saat takvimi; konuşma listesi ve tüm kişisel/operasyon geçmişinde tam sayfalama; Realtime; sürekli QR/geofence; gelişmiş lig formatları; rematch/Auto-Squad otomasyonu, referans/kemer ve gerçek AI servisi; tam telemetry/analitik ve erişilebilirlik/yerelleştirme kapsamı. Bunlar yalnız anahtar eklenince tamamlanmış olmaz.

**Yayın doğrulaması:** gerçek Apple/Google, SMS, satın alma/restore/iade/hesap değiştirme; fiziksel Android/iPhone izinleri; minimum iOS/Android ve 16 KB Android cihazı; çevrimdışı/hatalı ağ, büyük yazı/erişilebilirlik; Supabase pooler/TLS ve yük; yedekten dönüş; mağaza veri güvenliği/gizlilik beyanı ve inceleme hesabı.

## Doğrulama kayıtları

- Son doğrulanmış backend çalıştırması: **196 test geçti**, hata/atlanan yok. PostgreSQL motorunda (PGlite) şemanın tekrar kurulumu, RLS, mobil rollerin reddi, defter değişmezliği ve gerçek profil/maç işlemleri dahil. Gerçek Supabase ağı/pooler testi değildir.
- Android debug APK ve iOS Simulator framework üretimi son kaynaklarla **başarılı**. **57 KMP/istemci testi geçti**; hata/atlanan yok. Swift uyumluluk yolu ve Kotlin test bağımlılığı sorunları giderildi. [Mobil test özeti](validation/2026-11-09/mobile-tests.json).
- MapLibre dahil son APK: 12 native kütüphane; arm64-v8a/x86_64 yükleme hizaları 16 KB ve APK `zipalign -P 16` kontrolü geçti. 32 bit MapLibre ELF hizaları 4 KB; 64 bit sonucu ile karıştırılmamalı. Bu statik kontroldür; gerçek 16 KB cihaz/release AAB testi değildir. [Kanıt](validation/2026-11-08/android-native-alignment.json).
- Simülatör başlatmasında cihaz entitlements dosyasını host imzasına koymanın provisioning hatası oluşturduğu doğrulandı. Betik Xcode gibi simülatöre özel XML/DER Mach-O yetkilerini kullanacak şekilde düzeltildi; yerel API sürümü açıldı. Keychain oturum yazma/yenileme ve gerçek cihaz imzası uçtan uca test edilmiş sayılmaz. 7 Kasım preview turunda ana mock akış, sıralama/sporcu ve MMR açıklaması, topluluk oluşturma, login/üç pasaport adımı, yasal URL açıklaması, tesis ücretleri ve yorum penceresi gözlendi. [Tur kapsamı](validation/2026-11-07/simulator-ui-review.md). Bütün ekranlar veya canlı işlemler cihazda test edilmiş sayılmaz.
- Mock kataloğunun korunduğu ve `git diff --check` kontrolünün geçtiği doğrulandı. Plist/entitlement ve shell söz dizimi kontrolleri geçti.
- Canlı sağlayıcı, ödeme veya push işlemi yapılmadı. Supabase'e bağlanılmadı; mağazaya yükleme/yayın yapılmadı.

Son kanıtlar: [doğrulama klasörü](validation/2026-11-07/). Kurulum/API sözleşmesi: [Backend README](../backend/README.md).

Teknik kaynaklar: [Supabase PKCE](https://supabase.com/docs/guides/auth/sessions/pkce-flow), [RevenueCat KMP](https://www.revenuecat.com/docs/getting-started/installation/kotlin-multiplatform), [Android FCM](https://firebase.google.com/docs/cloud-messaging/android/client), [Apple takvim](https://developer.apple.com/documentation/eventkit/accessing-calendar-using-eventkit-and-eventkitui).

8 Kasım son simülatör turu: preview kurulumu ve üç pasaport adımı başarılı; giriş öncesi/sonrası aynı pusula gözlendi. Ağ girişinde otomasyon bağlantısı kapandığı için yeni sunucu işlemlerinin UI uçtan uca testi bu tur tamamlanmadı. [Görsel kontrol sınırları](validation/2026-11-08/simulator-ui-review.md).

Son kurulum: 9 Kasım preview simülatörde çalışıyor (PID58525); CUA ana mock akışı ve pusula tekrar gözlendi. [Son kanıt](validation/2026-11-09/simulator-ui-review.md).
