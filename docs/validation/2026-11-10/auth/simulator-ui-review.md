# Giriş simülatör kontrolü — 10 Kasım 2026

- Son Android APK, iOS framework ve Swift uygulaması derlendi. Ana iPhone 17 Pro simülatörüne preview modunda kuruldu/başlatıldı: FFD364D8-C8F8-4886-A74A-DCB4792DD958, PID87305. CLI sonucu ios-preview-build.txt içinde.
- CUA başlangıçta Simulator penceresine bağlandı. Yeni kurulumdan sonra gözlem `cgWindowNotFound` verdi. Pencereyi yeniden seçme girişiminde araç açıkça Mac’in kilitli olduğunu ve otomatik kilit açmanın başarısız olduğunu bildirdi.
- Son giriş ekranının görünümü, misafir dokunuşunun girişe dönmesi, belge kartları ve sistem tarayıcısı bu tur GUI ile doğrulanamadı. Mac elle açıldıktan sonra kontrol edilmeli. CUA dışında UI otomasyonu veya kilit atlatma denenmedi.
- Apple’ın Android’de gizlenmesi ortak platform politikasını kullanan otomatik testle; iki platform kodu derlemeyle doğrulandı. Fiziksel Android GUI testi yapılmadı.
- Gerçek Supabase/Apple/Google/SMTP anahtarları verilmedi. Canlı sosyal giriş, e-posta teslimi ve yeni/mevcut hesapların gerçek sağlayıcı uçtan uca testi yapılmadı. Sağlayıcı davranışları otomatik testte taklit edilir; bu canlı test değildir.
- DemoData.kt değişmedi. Önizlemede sahte hesap açma kapatıldı; gerçek doğrulanmış oturum olmadan özelliklerin açılması beklenmez.
