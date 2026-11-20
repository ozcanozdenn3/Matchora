# Supabase son bağlantı paketi

Bu dosyalar **mock verileri veya gerçek kullanıcıları eklemez**. Uygulamanın varsayılan `preview` görünümü `DemoData.kt` kataloğundan gelir; backend bağımsız olarak korunur.

SQL Editor içinde sırayla çalıştır:

1. `01_schema.sql`: işlemsel tablolar, indeksler, RLS ve değiştirilemeyen defter/denetim tetikleyicileri. Tek transaction içinde tekrar uygulanabilir. Mevcut, farklı bir şema üzerine taşımadan önce yedek ve staging denemesi gerekir.
2. `02_storage.sql`: `matchora-private` özel görsel bucket'ı, 5 MB ve MIME sınırı. Herkese açık okuma/yazma politikası oluşturmaz.
3. `03_verify.sql`: salt okunur kontrol. `denied_mobile_grants` sıfır olmalı; bucket `public=false` olmalı.

Kaynak SQL değişirse proje kökünden `cd backend` ve `npm run supabase:bundle` ile paketi yeniden üret.

## SQL dışında kalan zorunlu kurulum

SQL yalnız veritabanını/depo tanımını hazırlar. İş kurallarını işleten `backend/` **Node.js API'si ayrıca HTTPS üzerinde çalıştırılmalı**; Dockerfile hazırdır. SQL Editor, Node API'sini yayınlamaz.

- Sunucuya `DATABASE_URL`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` ve `.env.example` içindeki gerekli değerleri ver. Sunucu anahtarı mobil uygulamaya konmaz. Veritabanı bağlantısı mobil `anon/authenticated` rolleriyle yapılmaz.
- Supabase Auth: Apple/Google sağlayıcılarını etkinleştir, sağlayıcı kimlik bilgilerini ekle, redirect allowlist'e `matchora://auth/callback**` ekle. `AUTH_USERINFO_URL=https://PROJECT.supabase.co/auth/v1/user` ve publishable/anon `AUTH_PUBLIC_KEY` sunucuda tanımlanır.
- Telefon için Twilio Messaging Service veya mevcut SMS köprüsünü yapılandır. Sadece SQL çalıştırmak SMS gönderimini açmaz.
- Android `composeApp/google-services.json`, iOS APNs entitlement/provisioning ve sunucu FCM/APNs anahtarları ayrıca sağlanır.
- RevenueCat'te mağazaları, ürünleri, private webhook Authorization değerini ve ürün ID eşlemelerini tanımla. Public SDK anahtarları ile sunucu secret API anahtarını ayır. Restore transfer davranışını **orijinal Matchora hesabında tutma** politikasıyla yapılandır; başka hesaba otomatik token aktarımı yapılmaz.
- Gerçek yasal URL'leri/sürümleri, destek adresini ve mağaza imzalarını ekle. `npm run audit:release` eksik ortam değişkenlerini değerlerini ifşa etmeden listeler.

Şema, PGlite'ın yerel PostgreSQL motorunda yeniden uygulama, RLS, mobil rol reddi, defter değişmezliği ve gerçek giriş/profil/maç işlemleriyle sınanır. Bu test gerçek Supabase pooler, ağ, yedekleme ve yük testi yerine geçmez.

7 Kasım paketi 59 tablo ve `006_push_delivery.sql` dahil güncel migrationları içerir. Outbox sağlayıcı kabul zamanı cihaz teslim makbuzu değildir.

## 18 Kasım ek özellik paketi

Mevcut şema zaten kuruluysa yalnız `06_feature_completion.sql` dosyasının tamamını SQL Editor'de çalıştır. Arama/geçmiş indekslerini ve fotoğraf tekrar denemelerini tek kayıtta tutan özel tabloyu ekler. Kullanıcı, maç, sohbet ve cüzdan kayıtlarını değiştirmez; tekrar çalıştırılabilir. Son kontrolde sürümler `12`, `13` ve `receipt_rls=true` görünmeli.

Anlık sohbet Matchora API üzerinde kimliği doğrulanan SSE bağlantısıyla çalışır. Bu paket için Supabase Realtime publication açmak gerekmez. Mevcut API tek sunucu olarak çalıştırılır; birden fazla API kopyasına geçerken ortak bildirim dağıtımı eklenmelidir. Düzenli yenileme ve yeniden bağlanma kontrolü de korunur.

## 18 Kasım — seçilen 4–9. maddeler

Mevcut kurulum için tek ek dosya `07_account_cleanup_history.sql` dosyasıdır. Tamamını SQL Editor'de tek seferde çalıştır. Son kontrolde `version=14` ve `identity_cleanup_rls=true` beklenir. Dosya tekrar çalıştırılabilir; mevcut maç, sohbet, cüzdan veya kullanıcı satırlarını değiştirmez.

Yeni `identity_deletions` tablosu yalnız API sunucusu içindir. Hesap sahibinin süresi dolan silme talebi işlenirken Supabase kullanıcı kimliği, uygulama kaydı anonimleştirilmeden önce buraya alınır. Yerel temizlik tamamlandıktan sonra worker Auth admin API'sini çağırır. Bağlantı/izin hataları yeniden denenir ve Operasyon merkezi → Hesap temizliği ekranında görünür. Tamamlanınca eski sağlayıcı kimliği kuyruktan temizlenir.

Sunucu `SUPABASE_AUTH_ADMIN_KEY` varsa onu, yoksa mevcut `SUPABASE_SERVICE_ROLE_KEY` değerini kullanır. Bu anahtar mobil uygulamaya verilmez. `SUPABASE_URL` ve `AUTH_USERINFO_URL` aynı Supabase projesini göstermeli; farklı projede işlem yapılmaz.

Önceki sürümde zaten anonimleştirilmiş ve sağlayıcı kimliği kaybedilmiş hesaplar otomatik eşleştirilmez. Böyle eski Auth kayıtları varsa ayrıca incelenmelidir. Yeni ve henüz işlenmemiş silme talepleri yeni akışı kullanır.
