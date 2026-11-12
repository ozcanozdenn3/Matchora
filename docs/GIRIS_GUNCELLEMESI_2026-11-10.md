# Giriş güncellemesi — 10 Kasım 2026

Telefonla giriş seçeneği kaldırıldı. iOS’ta Apple, Google ve e-posta; Android’de Google ve e-posta var. Apple/Google düğmeleri gerçek Supabase PKCE akışına gider; preview/local sahte hesap veya doğrulamasız pasaport açma yolu artık yok. Telefonun profil içindeki iletişim doğrulaması bu değişikliğin dışındadır ve korunur.

## Arayüz ve davranış

- Mevcut premium renkler, çekmece, tipografi ve pusula korunur. Misafir önizlemesi renkli ve açıklamalı karttır. Privacy Policy ve Terms of Use, ikon ve Türkçe açıklama içeren ayrı büyük kartlardır.
- Telefon yerine şifresiz e-posta kodu: adres, kod gönderimi, 6 haneli doğrulama, yanlış/süresi dolmuş kod, yeniden gönderim ve 60 saniye sayaç. Yeni e-posta otomatik hesap oluşturabilir; mevcut e-posta aynı doğrulanmış Supabase kimliğiyle devam eder. [Supabase e-posta OTP belgeleri](https://supabase.com/docs/guides/auth/auth-email-passwordless).
- Uygulama ilk açılışta giriş penceresini gösterir. Misafir olarak kapatılırsa mock görünüm kalır; herhangi bir özellik/dock/üst çubuk dokunuşu girişe yönlendirir. Alt içeriğin erişilebilirlik eylemleri de kapatılır. Gelen ilan/bildirim/derin bağlantılar hesap kapısını atlamaz.
- Tamamlanmış pasaport ve gereken güncel belge onayı olmadan özellikler açılmaz. Çıkışta kilit hemen gelir. API oturumu yenilerken geçici bağlantı hatası sahte yeni hesap oluşturmaz.
- İlk pasaport gerçek `profile.save` ile saklanır. Görsel preview’da mevcut demo spor geçmişi korunur; canlı yeni hesaba sahte maç/MMR eklenmez. `DemoData.kt` değiştirilmedi.

## Yazılan backend

`POST /v1/auth/email/start` Supabase OTP servisine gönderir. Adres/IP sınırlaması kalıcı veritabanında tutulur. Yanıtta doğrulama kodu veya sahte oturum yoktur.

`POST /v1/auth/email/verify` sağlayıcıya kodu doğrulatır; dönen erişim belirteci güvenilen `/auth/v1/user` ile tekrar kontrol edilir. E-postanın doğrulanmış ve istenen adresle eşleşmiş olması zorunludur. Hatalı denemeler geri alınmaz; sınırlama sayacına işlenir. Oturum yalnız bundan sonra oluşturulur. Mobil depoya kod yazılmaz.

Sosyal girişte mevcut `/v1/auth/oauth/start` ve `/finish`, PKCE doğrulayıcısı, süreli/tek kullanımlık akış ve API’den alınmış ayrı istemci sırrı kullanılır. Mobil dönüş şeması ve akış ID’si kontrol edilir. iOS `ASWebAuthenticationSession`, Android HTTPS sistem tarayıcısı kullanır. Bu sürüm yerel Google/Apple SDK’sı yerine gerçek sağlayıcı tarayıcı girişidir.

## Son birlikte bağlanacaklar

| Alan | Yapılacak |
|---|---|
| API ve veritabanı | Supabase SQL/RLS/Storage, API/worker HTTPS yayını; mobil `MATCHORA_API_URL`. Kimlik akışı Matchora API üzerinden gider, doğrudan Supabase SDK eklenmedi. |
| Sunucu Auth | `AUTH_USERINFO_URL=https://PROJECT.supabase.co/auth/v1/user`, `AUTH_PUBLIC_KEY` (publishable/anon); gizli service-role anahtarı mobile konulmaz. |
| Apple | Supabase Apple sağlayıcısı ve Apple Developer Service ID/Team ID/key/secret/izinli dönüş adresi. Tarayıcı akışı için web OAuth ayarları gerekir. [Resmi Apple kurulum belgesi](https://supabase.com/docs/guides/auth/social-sign-in/auth-apple). |
| Google | Supabase Google sağlayıcısı, OAuth client ID/secret ve projenin Supabase callback adresi. [Resmi Google kurulum belgesi](https://supabase.com/docs/guides/auth/social-sign-in/auth-google). |
| Uygulamaya dönüş | Supabase Auth redirect allowlist, sunucunun ürettiği `matchora://auth/callback?flow=…` adresine izin vermeli. Ortak şema Android manifest/iOS plist’te mevcut. [Redirect URL kuralları](https://supabase.com/docs/guides/auth/redirect-urls). |
| E-posta | Email sağlayıcısı açık; Magic Link e-posta şablonunda `{{ .Token }}` kodu yer almalı. Kod uzunluğu mobilde 6 hanedir; sağlayıcı ayarı aynı tutulmalı. Üretimde SMTP ve gönderen alanı yapılandırılır. Supabase’in varsayılan servisi ekip dışı adresler için üretim teslimi sağlamaz. [SMTP belgesi](https://supabase.com/docs/guides/auth/auth-smtp). |
| Belgeler | Gerçek HTTPS Privacy Policy/Terms URL’leri, metin ve sürümleri. Boş bağlantı, sahte sayfa veya kabul edilmiş belge üretmez. |
| Canlı test | Gerçek Apple/Google/e-posta hesabı, yeni/mevcut hesap, tarayıcı iptali/dönüş, uygulama yeniden açma, ağ kesilmesi ve fiziksel Android/iPhone kontrolü. |

Bu hesap/anahtarlar çalışma alanına verilmediği için gerçek sağlayıcıda oturum açılıp doğrulanmış değildir. Yapılandırma yokken uygulama açık hata gösterir; sahte giriş yapmaz. Socket/Realtime bağlantısı bu giriş değişikliğinde eklenmedi. Tüm uygulamadaki diğer kalan işler [genel son durum raporunda](SON_DURUM_2026-11-10.md).

## Doğrulama

- Backend **220/220**, ortak istemci **71/71** başarılı; atlanan veya başarısız test yok. Yeni testler adres/kimlik eşleşmesi, kod reddi/tekrar kullanımı, kalıcı deneme sınırı, HTTP oturum geçişi, kodun depolanmaması, yanlış tarayıcı dönüşü, local modda sahte giriş olmaması ve Android Apple ayrımını kapsar.
- Son Android APK ve iOS framework derlendi. Swift uygulaması ana iPhone simülatörüne preview modunda kuruldu ve başlatıldı (PID87305).
- **Görsel kontrol yapılamadı:** CUA son kurulumdan sonra Mac’in kilitli olduğunu bildirdi. Yeni ekranın ve misafir dokunuşunun GUI kontrolü Mac elle açıldıktan sonra yapılmalı; bu tur görülmüş gibi raporlanmadı.
- Kaynak envanteri güncel: 56 UI dosyası,263 fonksiyon,95 popup/menü noktası,111 komut,67 tablo,406 benzersiz özellik satırı. Envanter ve diff whitespace kontrolü geçti.
- [Kanıt klasörü](validation/2026-11-10/auth/) ve [simülatör sınırlaması](validation/2026-11-10/auth/simulator-ui-review.md). Otomatik testler canlı sağlayıcı teslimi veya her pencerenin fiziksel cihaz testi sayılmaz.
