# Matchora arayüz ve akış düzenleme raporu

## Görsel kapsam

Ana akışın kartları, harita, alt navigasyon, mevcut renk paleti, yazı sistemi ve
spor ağı görünümü yeniden tasarlanmadı. Login ve ilk giriş pasaportu mevcut
mürekkep tonlu kartlar, kırık beyaz yüzeyler, altın detaylar ve turuncu aksiyonlarla
aynı bileşen dilinde düzenlendi. Yeni özelliklerin erişimleri ilgili mevcut
ekranlara eklendi.

## Login ve ilk pasaport

- Login penceresindeki tekrarlanan başlık, açıklama ve hediye alanları toparlandı.
- Apple, Google, telefon ve misafir seçenekleri aynı pencerede korundu.
- Üç adım: kimlik/konum/fotoğraf, branş/rol/seviye, oyun günleri ve tercihler.
- Uzun ek bilgiler isteğe bağlı açılır bölümde: biyografi, baskın el/ayak,
  forma numarası, iletişim, kampüs ve şirket.
- Tüm branş kataloğu arama ve seçim ekranından erişilebilir. Roller branşa göre
  değişir ve tam metinleriyle saklanır; rolün ilk sözcüğe kesilmesi kaldırıldı.
- Hazır gelen örnek isim, telefon, e-posta ve doğum yılı kaldırıldı.
- Ad, yaş, bölge, iletişim biçimi ve saat aralığı kontrolleri eklendi.
- Adımlar arasında ve login seçeneklerine geri dönerken form taslağı korunur.
- Fotoğraf galeriden seçilir, pasaport kartına taşınır ve profilden değiştirilebilir.
- İlk kayıtta doğrulanmış maç, rozet veya fotoğraf doğrulaması üretilmez.
- Hoş geldin ödülü aynı oturumda yalnızca bir defa verilir; cüzdanda bonus olarak görünür.
- Çıkıştan sonra yeniden girişte mevcut oturumun pasaportu tekrar doldurtulmaz.
- Üyelik isteyen aksiyon login sonrasında kaldığı yerden devam eder; misafir
  seçimi veya pencereyi kapatmak bekleyen işlemi iptal eder.

## Özelliklerin bulunduğu yerler

| Özellik | Erişim | Bu çalışmadaki durum |
| --- | --- | --- |
| Keşif, harita, branş ve ağ filtreleri | Akış / Harita | Mevcut tasarım ve akışlar korundu |
| Şimdi oyna, ilan oluşturma, eksik oyuncu ve kadro başvurusu | Orta aksiyon / maç detayı | Hesap kontrolü ve login sonrası devam eklendi |
| Başvuru yönetimi, bekleme listesi, check-in ve maç sohbeti | Maç detayı | Mevcut akışlar korundu; taahhüt iadesi maç bazında tek kullanımlı hale getirildi |
| Maçı başlat / tamamla / iptal et | Organizatörün maç detayı | Onay pencereleri ve yerel durum geçişleri eklendi |
| Skor ve fair play | Tamamlanan / devam eden maçın detayı | Sonuç bildirimi, özet, doğrulama bekleyen durum ve gerekçeli itiraz eklendi |
| Maçlar ve başvurular | Pasaport → Oyun takvimim | Katılım, organizatör maçları, bekleyen başvuru ve bekleme listesi tek yerde |
| Takım, meydan okuma, sıralama ve turnuva | Menü → Rekabet | Mevcut bileşenler korundu; üyelik isteyen veri değişikliklerine giriş kontrolü eklendi |
| Topluluk, kampüs, şirket, seyahat | Akış / Spor ağı | Mevcut yüzeyler korundu |
| Saha rezervasyonu | Spor ağı → Tesisler | TL ve tesiste ödeme modeli; dijital token bakiyesi kullanılmaz |
| Rezervasyon geçmişi ve iptali | Pasaport → Oyun takvimim → Sahalar | Bekleyen rezervasyonlar görüntülenir ve iptal edilebilir; slot yeniden açılır |
| Direkt mesaj ve anı paylaşımı | Sohbet | Gönderimlerde hesap kontrolü, boş arama durumu eklendi |
| Sohbet bildirme ve engelleme | Sohbet → Bilgi menüsü | Bildirme, direkt sohbeti engelleme ve engeli kaldırma eklendi |
| Gizlilik / mesaj tercihleri | Pasaport → Hesap ve tercihler | Görünürlük, sıralama, puan geçmişi, eşleşme sinyalleri ve DM tercihi |
| Bildirim tercihleri | Bildirim merkezi / Hesap ve tercihler | Her iki yerde aynı uygulama durumu kullanılır; pencere kapanınca sıfırlanmaz |
| Bildirim hedefleri | Bildirim merkezi | Maç, meydan okuma, sıralama, pasaport, hesap, oyun ve Plus bildirimleri ilgili yüzeye yönlenir |
| Token ve Plus | Cüzdan / token yükleme | Mevcut kart görünümü korundu; tüm girişler aynı bakiye ve Plus durumunu kullanır |
| Cüzdan hareketleri | Cüzdan → Tümü / Yükleme / Kilitler | Satın alma, bonus, kilit, iade ve harcamalar doğru işaretleriyle görüntülenir |
| Satın alma geri yükleme / üyelik yönetimi | Cüzdanın altı | Giriş noktaları ve bilgilendirme hazır; gerçek mağaza eylemi bağlantı bekler |
| İletişim ve fotoğraf doğrulama | Pasaport kartındaki doğrulama düğmeleri | Telefon/kod ve fotoğraf seçme/inceleme akışları; otomatik gerçek rozet verilmez |
| Destek, şikayet ve silme talebi | Hesap ve tercihler → Destek | Yerel talepler listelenir; hesap silme isteği onayla kaydedilir |

## Backend bağlantısı bekleyen noktalar

Bu değişiklikler arayüz ve yerel uygulama akışlarıdır. Apple/Google OAuth, gerçek
SMS, API, kalıcı oturum/veri, push, mağaza SDK'sı, makbuz doğrulama, sunucu cüzdanı,
moderasyon ve gerçek hesap silme işlemi eklenmedi. Önceden var olan demo token
satın alma ve Plus aktivasyonu yerel simülasyon olarak çalışır; gerçek ücret alınmaz.
Telefon önizleme akışında açıkça belirtilen 2026 kodu kullanılır.

`PassportDraft` profil oluşturma girdilerini doğrular. `MatchResultDraft`,
`SupportRequest`, `ModerationReport`, `NotificationDestination`, `PrivacyPrefs` ve
`NotificationPrefs` ilgili arayüzlerde kullanılır; servis bağlantılarının bu
modelleri ve mevcut callback'leri uygulama durumuna bağlaması gerekir.

Özellikle sunucu tarafında:

- Kimlik ve pasaport kullanıcı ID'si ile eşleştirilmeli; ilk giriş kontrolü ve
  hoş geldin ödülü sunucuda idempotent olmalı.
- Cüzdan, satın alma ve iadeler sunucuda doğrulanmalı. Mevcut maç bazlı yerel
  kilit kontrolü üretimdeki finansal defter yerine geçmez.
- Sonuç doğrulama, MMR ve güvenilirlik hesabı yetkili servis tarafından yapılmalı.
- Gizlilik, DM ve engelleme kuralları sunucu sorgularında ve mesaj tesliminde
  uygulanmalı; yerel tercih anahtarları erişim kontrolü yerine geçmez.
- Saha saatleri/iptal koşulları tesis servisiyle doğrulanmalı. Rezervasyon kaydı
  önizlemede bekleyen tesiste ödeme olarak oluşturulur.
- Destek, şikayet, fotoğraf inceleme ve hesap silme talepleri gerçek bir kuyruğa
  gönderilmeli. Buradaki talepler yalnızca uygulama belleğinde saklanır.
- Restore ve üyelik yönetimi mağaza SDK'sına bağlanmalı; bu düğmeler henüz
  gerçek bir mağaza isteği yapmaz.

Dolayısıyla bu çalışma mağaza yayınına veya tüm backend özelliklerinin üretimde
hazır olduğuna dair onay değildir. Yeni yerel durumlar uygulama süreci yeniden
başlatıldığında sıfırlanır.

## Doğrulama

- Android ve iOS Simulator Kotlin derlemeleri.
- 9 mevcut test + 7 pasaport doğrulama testi: 16 test.
- Yeni testler: kimlik/yaş, isteğe bağlı alanlar, telefon/e-posta, saat aralığı,
  gün sıralaması, sahte doğrulama geçmişi üretmeme, tüm branşların rolleri ve
  forma numarasının bağımsız saklanması.
- iPhone 17 Pro / iOS 26.5 simülatöründe login, üç pasaport adımı, profil kartı,
  hesap tercihleri, oyun takvimi ve cüzdan görsel olarak kontrol edildi.
- Boş kimlik formunun ilerlememesi; test adı/doğum yılıyla geçiş; rolün tam
  metinle son özete taşınması ve pasaportun oluşturulması denendi.
- Organizatör maçı başlatıldı, tamamlandı, 3–2 skor ve 5/5 fair play kaydı
  yapıldı; sonuç özeti doğrulama bekleyen durumda görüntülendi.
- Seçim rozetleri mevcut mürekkep/altın paletine uyarlandı; pasaport kısayollarının
  yükseklikleri eşitlendi.
- Gerçek cihaz, Android görsel testi, gerçek OAuth/SMS/push/ödeme testi yapılmadı.
