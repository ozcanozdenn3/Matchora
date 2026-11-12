# Matchora API

KMP uygulamasının iş kurallarını işleten Node.js 24 sunucusu. Yerelde SQLite; son bağlantı için PostgreSQL ve özel Supabase Storage adaptörleri vardır. Supabase'e henüz bağlanılmadı. Mobil uygulama service-role anahtarı taşımaz ve tablolara doğrudan yazmaz.

## Yerel kurulum

```sh
cd backend
npm ci --ignore-scripts
cp .env.example .env
# Yalnız yerelde DEV_AUTH_ENABLED=true yap.
npm run seed
npm run dev
```

Seed üretimde çalışmaz; yerel örnek hesap/maç/tesis ekler. Mobil preview kataloğundan bağımsızdır. `.env`, veritabanları, yüklemeler ve sağlayıcı dosyaları Git dışındadır.

```sh
npm run check
npm test
npm run audit:release
npm run supabase:bundle
```

Testlerde SQLite yanında PGlite'ın yerel PostgreSQL motoru, RLS, rol reddi ve değiştirilemeyen defter de sınanır. Bu gerçek Supabase ağı/pooler veya mağaza testi değildir. `audit:release` eksik yapılandırma adlarını değerlerini ifşa etmeden listeler; varsayılan ortamda başarısız olması beklenir.

## Mobil modlar

**Varsayılan `preview`**: `DemoData.kt` örnekleri korunur. Gerçek API, SMS veya satın alma başlatılmaz. `local` gerçek yerel API, `production` gerçek HTTPS API kullanır. API hatası demo bakiyesine dönüşmez.

Proje kökünde:

```sh
# Mock simülatör
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -Pmatchora.mode=preview
bash iosApp/build_and_run.sh

# Gerçek yerel API — iOS Simulator
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 \
  -Pmatchora.mode=local -Pmatchora.apiUrl=http://127.0.0.1:8787
MATCHORA_RUN_MODE=local MATCHORA_API_URL=http://127.0.0.1:8787 \
  MATCHORA_SIMULATOR_UDID=TEST_SIMULATOR_UDID MATCHORA_OPEN_SIMULATOR=false \
  bash iosApp/build_and_run.sh

# Android Emulator
./gradlew :composeApp:assembleDebug \
  -Pmatchora.mode=local -Pmatchora.apiUrl=http://10.0.2.2:8787
```

HTTP yalnız geliştirme ayarlarında açıktır. Release kapısı preview/local ve loopback adreslerini reddeder. Fiziksel cihaz için LAN adresi veya HTTPS test sunucusu gerekir.

Çalıştırma betiği **her zaman preview varsayar** ve önce o modun framework'ünü hazırlar. Yerel/production test için `MATCHORA_RUN_MODE`, API adresi ve ayrıca `MATCHORA_SIMULATOR_UDID` zorunludur; kullanıcının açık mock görünümü test sırasında değiştirilmez.

Simülatör Keychain yetkileri Xcode'un kullandığı Mach-O XML/DER bölümlerine gömülür; cihaz entitlements dosyası macOS host imzasına uygulanmaz. `MATCHORA_SIMULATOR_SIGN_IDENTITY` isteğe bağlıdır. Gerçek iOS dağıtımı `Matchora.entitlements`, geçerli provisioning ve cihaz testi gerektirir. iOS yayın bundle'ı `com.ozdentech.matchora`, simülatör/Android bundle'ı `com.matchora.app` olarak mevcut projede ayrıdır; servisleri doğru platform kimliğine bağla.

## API

- `GET /health`, `/v1/config`, `/v1/bootstrap?city=...`, `/v1/events?city=...&after=...&limit=100`, `/v1/events/:id`, `/v1/players/:id?sport=FOOTBALL` (gizlilik/engel kontrollü sporcu formu ve ortak maç/uyum).
- `POST /v1/auth/oauth/start`, `/oauth/finish`: Apple/Google sistem tarayıcısı + PKCE.
- `POST /v1/auth/exchange`, `/otp/start`, `/otp/verify`, `/contact/start`, `/contact/verify`, `/refresh`, `/logout`. Tüm auth yolları `/v1/auth` altındadır.
- `POST /v1/auth/dev`: yalnız açıkça etkinleştirilmiş yerel geliştirme.
- `POST /v1/commands/:operation`: Bearer oturumu, JSON ve `Idempotency-Key` gerekir.
- `GET /v1/threads/:id/messages?before=...&limit=50`: eski mesajlar.
- `POST /v1/media`: JPEG/PNG; sunucuda decode/resize/yeniden kodlama. `GET /v1/media/:id` her istekte erişimi kontrol eder.
- `GET /v1/passports/verify?code=...`: kısa süreli, imzalı sporcu kartı; özel iletişim alanı içermez.
- `GET /v1/account/export`, `/v1/account/sessions`: yalnız oturum sahibi.
- `GET /v1/admin/inbox`: güncel yönetici rolü; destek, şikayet bağlamı, fotoğraf/anı/sonuç/yorum, kullanıcı ve başarısız gönderim kuyrukları.
- `GET /v1/operator/venues`: güncel işletmeci/yönetici rolüne göre tesisler, saatler, rezervasyonlar.
- `GET /v1/venues/:id/reviews`: yayınlanmış ve kendi bekleyen yorumları; engeller uygulanır.
- `POST /v1/billing/revenuecat`: özel webhook Authorization. `/v1/billing/webhook`: zaman damgası + HMAC.
- `/openapi.json`: komut adları; gövde şemaları henüz ayrıntılı OpenAPI modelleri değildir. Alan doğrulaması `src/*.mjs` işleyicilerindedir.

Komut yanıtı `{ result, snapshot, replayed }`. Aynı anahtar/içerik eski sonucu getirir; aynı anahtarda farklı içerik 409 verir. İstemci yalnız commit sonrası sunucu durumunu uygular. Finans ve denetim kayıtları değiştirilemez. PostgreSQL adaptörünün genel advisory transaction lock'u doğruluk içindir; yüksek trafik için ölçülüp daraltılmalıdır.

## Kimlik ve SMS

`AUTH_USERINFO_URL=https://PROJECT.supabase.co/auth/v1/user` ve `AUTH_PUBLIC_KEY` kullanılır. Supabase Auth Apple/Google sağlayıcıları ve `matchora://auth/callback**` redirect allowlist'i hazırlanmalıdır. Sunucu PKCE verifier'ı tutar; mobil akış ayrıca API'den aldığı özel istek doğrulamasını taşır. Browser callback'i tek başına oturum açamaz. Girişten gelen güvenilir isim/iletişim ipucu onboarding'e aktarılır.

Doğrudan SMS için `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` ve Messaging Service SID veya gönderici tanımlanabilir. Alternatif köprü `{ to, code, requestId }` JSON ve Bearer sunucu anahtarı alır. Kod altı hane, beş dakika ve beş deneme sınırındadır. Numara mevcut hesaba doğrulanarak bağlanır; ayrı telefon/sosyal hesapları otomatik birleştirme yoktur. WhatsApp entegre değildir.

## Medya ve push

Sharp gerçek görseli çözümleyip metadata'yı temizler, yeniden boyutlandırır ve JPEG olarak kodlar. Yerel özel dizin veya `SUPABASE_URL` + `SUPABASE_SERVICE_ROLE_KEY` ile özel bucket kullanılır. İstemciye public nesne URL'si verilmez. Yetim yükleme ve hesap silme dosyaları tekrar deneyen silme kuyruğuyla temizlenir. Uygulamadaki yetkili moderasyon paneli destek/şikayet/anı/fotoğraf/sonuç/yorum incelemesine bağlıdır; otomatik içerik taraması ayrıca gerekir.

Android FCM SDK/servisi eklidir. `composeApp/google-services.json` dosyasını doğru Android paketine ekle; Gradle gerekli public tanımları üretir. Preview'da servis başlatılmaz. iOS token ve bildirim tıklaması Swift delegate üzerinden KMP'ye gelir; daha önce izin verildiyse açılışta APNs kaydı yenilenir. İmzalı APNs entitlement/provisioning gerekir.

Sunucu doğrudan APNs HTTP/2 ve FCM HTTP v1 gönderir veya `PUSH_GATEWAY_URL/TOKEN` köprüsünü kullanır. Outbox, tercih, sessiz saat, günlük sınır, tekrar deneme ve geçersiz cihaz tokenı temizliği vardır. Gerçek anahtarlarla cihaz testi yapılmadı.

## Mağaza

RevenueCat KMP SDK'sı native Apple/Google ürün, fiyat, purchase ve restore akışlarını açar. `app_user_id` API oturumundaki gerçek UUID'dir; `DemoData.ME` mağazaya verilmez. Mobil başarı callback'i bakiye yüklemez. Sunucunun doğruladığı webhook veya REST sync defteri günceller.

`.env.example` içinde public Apple/Google SDK anahtarları, ayrı server secret anahtarı, platform SKU -> store ID eşlemeleri ve store ID -> mantıksal ürün eşlemesi vardır. Android abonelik ID'si gerektiğinde `product:base-plan` biçimindedir. Dört token paketi ve aylık/yıllık Plus tanımlanır. Secret API anahtarı mobil config'e verilmez.

Abonelik hakları doğrulanmış ücretli dönemlerle kaydedilir. Plus ödülü İstanbul takvim ayında bir kez 100 TKN'dir; yıllık planda aylık işlenir. Restore/ürün değişimi tekrar ödül vermez; ödenmemiş dönem arası için ödül üretilmez. Bu kural gerçek mağaza ürün metniyle eşleştirilmelidir.

RevenueCat restore politikasını orijinal Matchora hesabında tutacak şekilde ayarla. Transfer ve bazı karmaşık iade/abonelik olayları operatör incelemesine alınır; tüm yaşam döngüleri otomatik değildir. Mağaza hesabı, gerçek ürün ve sandbox testleri gerekir. Fiziksel rezervasyon tesiste ödemedir; kişi başı hesap toplam saha borcunu azaltmaz ve token defterine girmez.

## Hukuki bağlantılar ve silme

Privacy/Terms/kurallar/silme/destek alanları tıklanabilir; URL yoksa açıklama çıkar. URL ve sürüm env'den veya `admin.legal.publish` ile yayınlanabilir. Güncel sürüm onayı kaydedilir; yeni sürümde yeniden onay gerekir. Destek, çıkış, hesap silme ve ayrılma işlemleri onay kapısının dışında kalır.

Hesap silme yerel API verisini, medyayı, oturumları ve üyelikleri temizler; takım/topluluk yöneticiliğini devreder. Supabase Auth / Apple sağlayıcı tokenı iptali, dış mağaza hakları ve ücretli fiziksel rezervasyon/tesis kapanışı için ayrıca uzlaştırma gerekir. Silme mağaza aboneliğini kendiliğinden iptal etmiş sayılmaz.

## Son bağlantı ve durum

SQL Editor için [Supabase kurulum paketi](supabase/README.md) hazırlanmıştır. **SQL Node.js API'sini yayınlamaz**; Dockerfile ile ayrıca HTTPS sunucu gerekir. Şema/RLS testi yerel PostgreSQL motorunda geçti; gerçek Supabase pooler/TLS ve canlı yük testi yapılmadı.

[Tam ekran/pencere envanteri](../docs/TUM_EKRANLAR_BACKEND_ENVANTERI.md), [kaynak dizini](../docs/UI_KAYNAK_DIZINI.md), [durum raporu](../docs/BACKEND_DURUM_RAPORU.md) ve [ekran / backend matrisi](../docs/EKRAN_BACKEND_MATRISI.md), dış kurulum ile ayrıca kod gerektiren açıkları ayrı listeler. Yalnız Supabase anahtarını eklemek, mağaza ve operasyon işlerini tamamlamaz.


### 6 Kasım ekran sözleşmeleri

`memory.create` isteği isteğe bağlı `matchScoreTag` (en fazla 80 karakter), `sceneTheme` (mobil enum) ve `shareThreadId` alanlarını kabul eder. Anı + sohbet tek transaction/idempotency anahtarıyla kaydedilir; anı moderasyon bekler. `team.create.colorHex` sekiz haneli ARGB rengidir. Turnuva açıklama metadatası `customSportName`, `organizerName`, `gamePlatform`, `serverRegion`, `city`, `area` ile korunur; serbest açıklamalar bir format/katılım/ödül motoru değildir.

Sporcu ayrıntısı yalnız görülebilir doğrulanmış maçlardan form üretir; kapalı geçmiş, iki yönlü engel ve kişisel eşleşme tercihleri uygulanır. İletişim/kimlik bilgisi dönmez. Destek yanıtları yalnız talep sahibinin snapshot'ına eklenir; kapanmış talebe ikinci karar reddedilir.

`admin.feature.set` içindeki `billing` satış bayrağı config'in mağaza kullanılabilirliğini değiştirir. Mobil satın alma öncesi config yeniden alınır; restore ve önceden ödenmiş webhook/sync uzlaştırması satış kapatmadan bağımsızdır.

### Uygulanabilir ürün iş akışları

- `rating.mjs`: başlangıç 1000, Elo K=24, kontrol edilmiş iki taraf, rakip çoğunluğu ve 24 saat itiraz süresi. Sosyal maç ve katılmayan sporcuya MMR yok. Profil branş arşivi puan sıfırlamayı önler. Resmi kadro sıradan katıl/ayrıl/başvuru/sıra yollarıyla değişmez.
- `tournament-rules.mjs`: KNOCKOUT veya ROUND_ROBIN; kapasite, kadrodaki her oyuncuya branş/minRating/BASIC doğrulaması. İleri formatlar geliştirilmiş sayılmaz.
- `community-management.mjs`: sürümlü güncelleme, üye çıkarma, gelecekteki özel maç üyelik/başvuru/sıra ve taahhüt temizliği.
- `operations.mjs`: işletmeci atama, tesis/saat/rezervasyon paneli, tamamlanmış rezervasyon yorumu, moderasyon, offline iade kaydı, başarısız gönderim tekrarı ve pazarlama iznine bağlı yönetici duyurusu. Offline kayıt para transferi yapmaz.
- `engagement.mjs`: tercih/engel/üyelik/süre kontrolüyle acil çağrı, haftalık özet ve karşılıklı tekrar oynama önerisi. Acil çağrı ilçe tabanlıdır; tam kilometre yarıçapı uygulanmaz.
- Keşif sayfalaması 1 saatlik AES-GCM cursor kullanır; özel maç ID'si cursor içeriğinden okunamaz. Her sayfa üyeliği yeniden doğrular. İstemci sayfaları yenilemede yeniden okur, sohbet gibi diğer alanlarda ek sayfaları bırakır.
- `005_product_workflows.sql`: `job_checkpoints` ve `venue_reviews`; `006_push_delivery.sql`: outbox claim tokenı ve sağlayıcı kabul zamanı. Güncel SQL Editor paketi ikisini de içerir.

Tüm ekran/özellik ve dış servis açıkları için [6 Kasım tamamlama raporu](../docs/OZELLIK_TAMAMLAMA_2026-11-06.md).

### 7 Kasım işlev denetimi

Organizatörün `event.member.remove/restore`, kaptanın `team.member.remove/archive`, yöneticinin `community.archive` işlemleri ilgili ekranlara bağlandı. Çıkarma taahhüt iadesi/sohbet yetkisi/sıra teklifini aynı transaction'da yürütür. Resmi kadrolar korunur. Başvuru/doğrudan katılım uzlaştırması, zaman/check-in korumaları, turnuva takım seçimi/gelecek fikstür iptali/KO beraberlik denetimi ve gönderim anında güncel bildirim erişimi eklendi. Yeni SQL tablosu gerekmedi. [Ayrıntı ve kanıt](../docs/ISLEV_VE_KADRO_DUZELTMELERI_2026-11-07.md).

## 8 Kasım işlev zincirleri

- İlan `approvalRequired` alanı doğrudan/onaylı katılımı ayırır. `event.join` onayı atlayamaz; bekleme teklifi onaylı ilanda başvuru üretir.
- Topluluk OPEN/APPROVAL/INVITE seçilir; başvuru sahibi/yöneticisi için doğru kuyruk ve NETWORK bildirim hedefleri vardır.
- `venue.application.create/withdraw` ve `admin.venue.application.review`: normal hesap başvurur; yönetici işletme yetkisini kontrol ederek onaylar. Onay aynı işlemde tesis ve işletmeci rolünü oluşturur. İşletmeci slot gün/saat/süre/kuruş ücretini yönetir. Kapasite/çakışma, sürüm ve aktif/ödenmiş rezervasyon guardları uygulanır. Bu akış karttan para çekmez.
- `/v1/rankings?sport=FOOTBALL&scope=district&city=...&district=...&after=...` filtreyi sayfa sınırından önce uygular; kişinin sıra konumunu ayrıca döndürür.
- `/v1/memories?mode=random|nearby|top|newest|mine&seed=...&after=...` tüm uygun paylaşımlar üzerinde sıralama yapar. Cursor kullanıcı/filtreye bağlı ve sürelidir.
- `discovery.location` koordinatları iki ondalığa yuvarlar,15 dakika geçerli tutar. Güncel nokta varsa acil çağrı yarıçapı ve radar uzaklık sinyali uygulanır; yoksa seçili bölge kullanılır. Arka plan takip izni istenmez.
- Sekiz rozet, gerçek maç geçmişi/sonuç ve güvenilirlik kaydı bağlıdır. Community Builder harici referral değil, yeni sporcuların ilk doğrulanmış maçını organize etme kuralıdır.
- 007/008/009 migrationları SQL Editor paketine dahildir. 009, farklı kortların aynı saate açılmasını engelleyen eski UNIQUE kısıtını kaldırır; yerel SQLite yükseltmesi eski rezervasyon ve ödeme FK'lerini korur.

Güncel sınırlar ve cihaz/sağlayıcı doğrulaması: `docs/KAPSAMLI_ISLEV_DENETIMI_2026-11-08.md`. Şema kurulumunun yanında API/worker ve sağlayıcı kurulumu gerekir.
