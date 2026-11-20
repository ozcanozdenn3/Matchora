# Onaylanan altı geliştirme — 18 Kasım 2026

Bu rapor son onaylanan altı geliştirmeyi kapsar. Mevcut premium bileşenler, renkler, ana gezinme ve sol üst pusula korunmuştur. `DemoData.kt` kataloğu değiştirilmemiştir. Canlı bağlantı modunda mevcut gerçek veritabanı kayıtları kullanılır.

## Ekranlar ve tamamlanan bağlantılar

| Geliştirme | Nerede kullanılır? | Tamamlanan işlev |
| --- | --- | --- |
| İlan taslağı ve yayın onayı | Şimdi Oyna / İlan Ver | Form alanları ve seçilen adım cihazda hesap bazında saklanır. Taslağı saklama, düzenlemeye devam etme ve silme seçenekleri vardır. Yayın düğmesi işlem boyunca kilitlenir; form sunucu onayından sonra kapanır. Aynı içerik yeniden gönderilirse aynı işlem kimliği kullanılır. |
| Adım doğrulaması | İlan formunun üç adımı | Başlık/branş/ağ, tarih/saat/bölge/tesis/harita noktası ve tercihler kendi adımlarında kontrol edilir. İleri düğmesi ve üstten adım atlama aynı kurallara bağlıdır. Hata ilgili adıma yönlendirir. |
| Fotoğraf ve kalıcı gönderim kuyruğu | Sohbet metni, sohbet fotoğrafı, Anı Vitrini yayını | Hazırlanıyor, gerçek aktarılan bayt yüzdesi, işleniyor ve gönderiliyor durumları bağlandı. Bekleyen/hatalı gönderimde tekrar deneme ve kaldırma vardır. Metin ve fotoğraf uygulama kapanınca kaybolmaz; aynı hesaba dönülünce devam eder. Yükleme onayı kaybolursa fotoğraf aynı anahtarla bulunur; mesaj onayı kaybolursa aynı komut tekrar edilir. |
| Anlık sohbet ve yazıyor bilgisi | Açık sohbet ve sohbet listesi | Kimliği doğrulanan SSE bağlantısı kurulur. Diğer katılımcıların değişiklikleri bildirildiğinde yetkili sohbet verisi yenilenir. Yazıyor bilgisi kısa ömürlüdür. Bağlantı kopunca tekrar bağlanır ve geçmiş yenilenir; düzenli yenileme korunur. Gönderen cihaz kendi başarılı işlemi için gereksiz ikinci yenileme yapmaz; aynı hesabın diğer cihazları güncellenir. |
| Tüm sporcularda arama | Sohbet listesindeki “Tüm sporcularda ara” | İsim, branş, il ve ilçe ile veritabanının tamamında sayfalı arama yapılır. Sonuçtan sporcu pasaportu ve birebir sohbet açılır. Türkçe harfler ve arama içindeki `%`, `_`, ters eğik çizgi güvenle işlenir. Aktif olmayan veya iki yönden engellenmiş hesaplar gösterilmez; özel iletişim bilgileri dönmez. Aynı isimdeki kişiler kullanıcı kimliğiyle ayrılır. |
| Tam kişisel geçmiş | Pasaport: maçlar/MMR/güven; Cüzdan: tüm hareketler | İlk açılış paketinin kayıt sınırından bağımsız sayfalı geçmiş eklendi. Maçlar, MMR, güvenilirlik ve cüzdan ayrı sekmelerde yüklenir. Eski tarihler yıl bilgisiyle gösterilir. Yükleme, boş durum, hata ve tekrar deneme vardır. |

## Gönderim güvenliği ve yaşam döngüsü

- Android kuyruğu cihaz anahtarıyla şifrelenir; iOS kuyruğu korumalı uygulama alanında tutulur. Yedeklemeye dahil edilmez.
- Her hesap ayrı kuyruk kullanır. Çıkış yapmak önceki hesabın metin ve fotoğraflarını diğer hesaba taşımaz.
- Sunucu onayı gelmeden gönderim kuyruktan silinmez. Geçici bağlantı hataları gecikmeli tekrar edilir; kalıcı ret kullanıcıya gösterilir.
- Sohbet silindiğinde o sohbete ait bekleyen doğrudan gönderimler temizlenir. Fotoğraf aktarımı sürerken silme yapılırsa aktarım sonrasında mesajın sohbeti yeniden açması engellenir.
- Hesap silme onaylandığında o hesabın cihazdaki bekleyen gönderimleri temizlenir. Başka hesapların dosyaları korunur.
- Kuyruk en fazla 50 gönderim ve toplam yaklaşık 28 MB kodlanmış fotoğraf saklar; dolduğunda kullanıcıya açıklama gösterilir.
- Bekleyen gönderimler uygulama çalışırken işlenir; uygulama yeniden açıldığında devam eder.

## Supabase ek SQL

Mevcut veritabanı için tek ek dosya: [06_feature_completion.sql](/Users/md/Desktop/Matchora/backend/supabase/06_feature_completion.sql).

Dosyanın tamamı SQL Editor'de tek seferde çalıştırılabilir. Tekrar çalıştırılabilir; mevcut kullanıcı, maç, sohbet ve cüzdan satırlarını değiştirmez. Arama/geçmiş indekslerini ve hesap bazlı `upload_receipts` tablosunu ekler. Yeni tablonun RLS korumasını etkinleştirir ve mobil rollere doğrudan erişim vermez.

Yerel sunucu yeniden başlatılırken bağlı Supabase üzerinde 12 ve 13 numaralı ekler uygulandı. Salt okunur kontrolde bu iki sürüm ve `receiptRls=true` doğrulandı. Dolayısıyla aynı dosyayı mevcut bağlantıda tekrar çalıştırmak da uygundur. Yeni kurulumlar için `01_schema.sql` paketi de güncellendi.

Anlık sohbet Matchora API üzerinden çalışır; bu özellik için Supabase Realtime publication ayarı gerekmez.

## Doğrulama kapsamı

Sunucu testleri: **246/246 başarılı**. Ortak KMP mantığı için Android/JVM testleri: **93/93 başarılı**. Android uygulama derlemesi başarılıdır.

Son doğrulama sonuçları [verification.json](/Users/md/Desktop/Matchora/docs/validation/2026-11-18/verification.json) dosyasındadır.

Sunucu testleri mevcut iş kurallarının yanında gerçek yerel HTTP üzerinden arama, özel geçmiş, SSE bağlantısı, yazıyor ve sohbet yenilenmesini sınar. Fotoğraf tekrarları eşzamanlı olarak sınanır. Ek SQL gerçek PostgreSQL motorunda iki kez çalıştırılır; RLS ve mobil rol kısıtları denetlenir. Mobil testler diskten geri yükleme, hesap ayrımı, dolu disk, kayıp yanıt, fotoğrafı ikinci kez yüklememe ve sohbet/hesap silme sırasında kuyruk temizliğini kapsar.

Android uygulaması ve iOS ortak kodu derlenmiştir. iOS uygulaması ayrıca Xcode ile derlenip açık iPhone 17 Pro simülatörüne kurulmuştur. Simülatörde açılış görünümü, pusula ve sunucu bağlantısı kontrol edilmiştir. Tüm yeni pencerelerin iki fiziksel cihazda elle dolaşılması bu doğrulamanın kapsamında değildir.

Mevcut anlık bildirim dağıtımı tek API sunucusu içindir. İleride birden fazla API kopyası çalıştırılırsa ortak bildirim dağıtımı eklenmelidir. Yeniden bağlantı ve düzenli yenileme mekanizmaları mevcut durumda korunur.
