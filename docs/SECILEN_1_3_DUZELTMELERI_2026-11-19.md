# 1, 2 ve 3. maddelerin tamamlanması · 19 Kasım 2026

Bu çalışma önceki incelemede kalan üç gönderim/yayınlama sorununu kapsar. Önceki 4–9 düzeltmeleri korunmuştur.

| Madde | Uygulanan davranış | Uygulamada yeri |
| --- | --- | --- |
| 1 · Uzun süre bekleyen fotoğraf | Fotoğrafın cihazdaki kopyası yalnızca yükleme bitince silinmez; mesaj/paylaşım onayı gelene kadar hesap bazlı kalıcı kuyrukta tutulur. Sunucudaki sahipsiz yükleme 24 saat sonra temizlenmişse yeni yükleme anahtarıyla tekrar yüklenir; gönderimin asıl anahtarı korunur. | Sohbet fotoğrafları ve Anı Vitrini paylaşımı |
| 2 · Kaybolan ilan yayınlama yanıtı | Önce eski isteğin sonucu sunucudan doğrulanır. İlan zaten yayınlanmışsa Token veya tarih doğrulamasına takılmaz; mevcut ilan açılır ve cüzdan/ilan görünümü yenilenir. Aynı işlem yeniden ilan veya Token kilidi oluşturmaz. | İlan ver → taslağın herhangi bir adımındaki “Yayın durumunu kontrol et” |
| 3 · Erişimi kapanan sohbetin bekleyenleri | Kuyruk artık yalnızca açık sohbet içinden yönetilmez. Hesabın tüm bekleyen mesaj ve fotoğrafları, erişilebilir sohbet listesine bağlı olmadan görünür. Yeniden deneme ve onayla kaldırma kullanılabilir. | Mesajlar / Anı Vitrini → “Bekleyen gönderimler” |

## Kurtarma ayrıntıları

- Önceki isteğin tamamlandığı bilinmiyorsa içerik değiştirilmeden önce sunucudaki işlem kaydı sorgulanır. Tamamlanmış gönderim aynı anahtarla okunur; ikinci mesaj veya paylaşım yaratılmaz.
- Sunucu bir yüklemenin süresinin dolduğunu ayrı bir hata koduyla bildirir. Yetki/engelleme hatası fotoğraf süresinin dolmasıyla karıştırılmaz.
- Eski sürümün kuyruk kaydında fotoğrafın yerel kopyası zaten yoksa “Fotoğrafı yeniden seç” gösterilir. Kamera/galeri için mevcut ortak sistem seçicisi kullanılır. Yeni fotoğraf seçimi yeni bir mesaj kuyruğu yaratmaz; var olan gönderimi tamamlar. Bu sırada eski onay bulunursa yeniden fotoğraf yüklenmez.
- Yeni yükleme anahtarı da kalıcı kaydedilir. Uygulama yükleme sırasında kapatılırsa aynı anahtarla devam eder. Kuyruk sınırları 50 gönderim / 28 MB kodlanmış fotoğraf ve fotoğraf başına 5 MB olarak korunur; yeniden seçme bu sınırları aşamaz.
- İlanın ilk istek içeriği ve o andaki form değerleri taslakla saklanır. Yayından sonra form değiştirilmişse onay bulunduğunda sonraki değişiklikler taslakta tutulur. Eski sürümün karşılaştırılabilir form kaydı yoksa taslak ihtiyatlı olarak korunur.
- Doğrulama sırasında ağ yoksa yeni ilan denenmez, Token satın almaya yönlendirilmez; hata gösterilir ve taslak saklanır.
- İşlem kaydı sorgusu giriş gerektirir; yalnızca oturum sahibinin aynı anahtar ve aynı içerikle yaptığı işlem döner. Başka hesabın kaydı okunamaz. Sorgu devam eden sunucu işlemiyle aynı veritabanı kilidini kullanarak tamamlanmayı bekler.
- Hesap değiştirme, sohbet silme ve hesap silmede mevcut kuyruk ayrımı ve temizleme davranışı korunur.

## Supabase

**Bu üç madde için ek SQL çalıştırılması gerekmiyor.** Mevcut `commands` ve `upload_receipts` tabloları kullanılır; yeni tablo/kolon eklenmedi. Daha önce uygulanan 07 dosyası ve sürüm 14 geçerlidir. Güncellenmiş API ve mobil uygulama birlikte kullanılmalıdır. Yerel API yeni kodla yeniden başlatılmıştır.

## Doğrulama

**257/257 sunucu testi**, **109/109 ortak KMP testi (Android/JVM)** başarılıdır. Android uygulama paketi ve iOS ortak framework derlemesi başarılıdır. Swift iOS uygulaması da Xcode ile başarıyla derlenmiş ve açık iPhone 17 Pro / iOS 26.5 simülatörüne kurulmuştur. Ek olarak PostgreSQL motoru ve kurtarma HTTP kontrollerini içeren 6/6 hedefli test geçmiştir.

Doğrulama sonuçları son test ve derleme çıktılarıyla `docs/validation/2026-11-19/selected-1-3/verification.json` dosyasında tutulur.

Test kapsamı: 24 saati aşan gerçek sunucu temizliği sonrası sohbet/vitrin gönderimi, yükleme yanıtı ve mesaj yanıtı kaybı, aynı isteğin tekrarı, sıfır serbest Token ve geçmiş tarihli ilanın onayı, taslağın sonradan düzenlenmesi, başka hesaptan sorgu engeli, PostgreSQL sorguları, erişilemeyen sohbetin kuyrukta bulunması ve kaldırılması, eski fotoğrafın yeniden seçilmesi, disk sınırı ve hesap ayrımı.

Mevcut premium bileşenler kullanıldı. Mock veri kaynağı ve pusula görünümü değiştirilmedi. Kamera/galeri native entegrasyonları Android ve iOS derlemesinde kontrol edilir; yeni akışların tümünün iki fiziksel cihazda elle uçtan uca testi yapılmış sayılmaz.

## Bu çalışmanın dışında kalan yayın hazırlığı

Bu rapor üç somut düzeltmenin tamamlanma kaydıdır. Üretim HTTPS sunucusu, mağaza/RevenueCat ürün ve anahtar ayarları, gerçek cihaz/mağaza testleri önceki raporlardaki yayın adımları olarak kalır. Bütün uygulama için koşulsuz “hiç eksik yok / mağazaya hazır” sonucu çıkarılmaz.
