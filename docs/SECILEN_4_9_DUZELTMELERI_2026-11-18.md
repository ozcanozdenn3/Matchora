# Seçilen 4, 5, 6, 7, 8 ve 9. maddeler — 18 Kasım 2026

Bu tur, son incelemede seçtiğin altı maddeyi kapsar. Mevcut premium bileşenler, ana düzen, pusula ve `DemoData.kt` kataloğu korunmuştur.

| Madde | Tamamlanan iş | Kullanıldığı yer |
| --- | --- | --- |
| 4 · Grup sohbetinde kişi seçimi | Katılımcı artık mesajdaki kullanıcı kimliğiyle bulunur. Aynı isimde başka bir sporcu seçilmez. Kişi önbellekte yoksa doğru kimlikle pasaportu sunucudan yüklenir. | Sohbette katılımcının adı ve avatarı |
| 5 · Diğer sohbete gelen mesaj | Anlık bildirim hangi konuşmanın değiştiğini taşır. Açık olan sohbetten bağımsız olarak değişen sohbet ve okunmamış sohbet/bildirim sayıları yenilenir. Konuşmanın erişimi kaybedildiyse yetkili liste yeniden alınır. Sayfalama ve mevcut mesaj geçmişi korunur. | Açık sohbet, sohbet listesi, alt gezinme rozeti ve bildirim sayısı |
| 6 · Oluşturma formları | Takım, kupa ve topluluk formları sunucu onayı gelince kapanır. Hata aynı formda gösterilir. Alanlar hesap bazında cihazda saklanır; yeniden açınca geri yüklenir. Taslağı saklama/silme seçenekleri, kaydetme sırasında işlem kilidi ve aynı yayın anahtarıyla güvenli tekrar eklenmiştir. Son alan değeri kaydetme anında okunur. | Yarış → Kulüp kur / Turnuva oluştur; Ağ → Topluluk oluştur |
| 7 · Android galeri | Sistem seçicisi ve eski cihazlardaki belge seçicisi geniş depolama iznine bağlanmaz. `READ_MEDIA_IMAGES` ve `READ_EXTERNAL_STORAGE` bildirimleri kaldırılmıştır. Kamera için mevcut kamera izni devam eder. | Profil, sohbet, vitrin, tesis görsellerinde ortak fotoğraf seçicisi |
| 8 · Supabase Auth temizliği | Silme süresi dolan talepte sağlayıcı kimliği anonimleştirmeden önce özel kuyruğa kaydedilir. Uygulama verilerinin temizliği tamamlandıktan sonra worker ilgili Supabase Auth kaydını siler. Tekrar deneme, iş kilidi, yanlış proje engeli ve kayıp yanıt sonrası tamamlanmayı anlama bağlanmıştır. | Hesap silme; Operasyon merkezi → Hesap temizliği |
| 9 · Kalan kişisel geçmişler | Rezervasyon, destek ve şikâyet geçmişleri artık sayfalanır. 100 kayıt sınırının gerisindeki kayıtlar yüklenebilir. Destek yanıtları ve tam tarih görünür; tamamlanmış/ödenmiş eski rezervasyondan tesis değerlendirmesi açılır. | Oyun takvimim → Sahalar; Hesap ve tercihler → Destek |

## Supabase SQL Editor

Tek ek dosya: [07_account_cleanup_history.sql](/Users/md/Desktop/Matchora/backend/supabase/07_account_cleanup_history.sql).

Dosyanın tamamı tek seferde çalıştırılabilir; tekrar çalıştırılabilir. Yeni özel `identity_deletions` tablosunu, iş kuyruğu ve geçmiş indekslerini ekler. Mevcut kullanıcı/maç/sohbet/cüzdan satırlarını değiştirmez. Son kontrolde **version=14** ve **identity_cleanup_rls=true** beklenir. `01_schema.sql` yeni kurulum paketi de güncellenmiştir.

Yerel API sunucusu yeniden başlatıldığında mevcut Supabase bağlantısına 14 numaralı ek otomatik uygulanmıştır. Aynı SQL dosyasının SQL Editor'de tekrar çalıştırılması uygundur.

## Hesap temizliği ayrıntıları

- Varsayılan mevcut silme bekleme süresi korunur. Henüz süresi dolmamış talep için Auth hesabı silinmez.
- Kimlik eşlemesi, yerel temizlikle aynı veritabanı işlemi içinde kaydedilir; işlem geri alınırsa harici silme işi oluşmaz.
- Sunucu `SUPABASE_AUTH_ADMIN_KEY` veya mevcut `SUPABASE_SERVICE_ROLE_KEY` değerini kullanır. Bu anahtar mobil uygulamaya gönderilmez.
- Auth adresi, Supabase proje adresi ve işte saklanan proje eşleşmeden istek yapılmaz.
- Bir iş aynı anda iki worker tarafından işlenmez. Ağ kesintisi ve servis hatası gecikmeli tekrar edilir. Olmayan kullanıcı yanıtı, önceki silmenin tamamlandığı anlamına gelir; yanlış adresin genel 404 yanıtı başarı sayılmaz.
- Temizlik sürerken aynı eski sağlayıcı kimliğiyle yeni Matchora hesabı oluşturulması engellenir. Başarıdan sonra kuyruktaki sağlayıcı kimliği temizlenir.
- Operasyon merkezinde bekleyen işler, hata açıklaması ve yetkili yeniden deneme girişi vardır.
- Gerçek Supabase admin erişimi, rastgele ve mevcut olmayan kullanıcı için salt okunur istekle kontrol edildi. Yanıt `user_not_found` oldu. Canlı ortamda bu kontrol için herhangi bir Auth hesabı silinmedi; kontrol anında süresi dolmuş bekleyen sağlayıcı silme isteği yoktu.

**Eski kayıt sınırı:** Önceki sürümde zaten anonimleştirilmiş ve sağlayıcı kimliği kaybedilmiş hesaplar otomatik eşleştirilemez. Böyle eski Auth kayıtları varsa ayrıca incelenmelidir. Yeni ve henüz işlenmemiş talepler bu yeni akışı kullanır.

## Doğrulama

**252/252 sunucu testi** ve **99/99 ortak KMP testi (Android/JVM)** başarılıdır. Android uygulaması ve iOS framework derlemesi başarılıdır.

Son sonuçlar [verification.json](/Users/md/Desktop/Matchora/docs/validation/2026-11-18/selected-4-9/verification.json) dosyasındadır.

Sunucu testleri yanlış proje/eksik yetkide silme yapılmamasını, kayıp yanıtı, iki worker yarışını, geri alınan veritabanı işlemini, yönetici yetkisini ve 205 kayıtla geçmiş sayfalamasını kapsar. Ek SQL PostgreSQL motorunda iki kez uygulanmış; mobil rol erişimleri ve RLS doğrulanmıştır.

Ortak KMP testleri aynı isimli sporcuları, önbellekte olmayan kimliği, başka sohbet açıkken gelen mesajı/rozetleri, erişimi kaybolan sohbeti, form onayını bekleme, disk hatası, hesap değişimi ve taslak/işlem kimliğiyle tekrarı kapsar. Android birleştirilmiş manifestinde geniş medya/depolama izinlerinin kalmadığı kontrol edilmiştir.

Android uygulaması ile iOS ortak kodu ve Swift uygulaması derlenmiştir. Son sürüm iPhone 17 Pro / iOS 26.5 simülatörüne kurulmuştur. Bütün yeni pencerelerin iki fiziksel cihazda elle uçtan uca gezilmesi bu turun kapsamında değildir. Anlık bağlantı mevcut tek API sunucusu üzerinden çalışır.

Önceki incelemenin 1, 2 ve 3. maddeleri bu turda değiştirilmemiştir. Mağaza satış kurulumu, üretim HTTPS yayını ve önceki raporlardaki dış servis işleri de bu altı düzeltmeden ayrı kalır.

Teknik dayanaklar: [Supabase admin kullanıcı silme yöntemi](https://supabase.com/docs/reference/javascript/auth-admin-deleteuser), [Android sistem fotoğraf seçicisi](https://developer.android.com/training/data-storage/shared/photo-picker), [Google Play medya izinleri](https://support.google.com/googleplay/android-developer/answer/16935362?hl=en).
