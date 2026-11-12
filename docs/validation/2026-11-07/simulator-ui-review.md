# 7 Kasım 2026 — iOS preview görsel kontrolü

Cihaz: iPhone 17 Pro, iOS 26.5. Mod: preview. Son paket çalıştırma PID: 50514.

CUA ile gözlemlenenler:

- Ana akışta 65 örnek oyun, acil maç, Challenge ve sıralama kartları mevcut.
- Sıralamada örnek sporcular ve Onur Ak ayrıntısı mevcut; yeni “MMR nasıl değişir?” penceresi açıldı. Başlangıç 1000, denk rakipte +12/−12/0, check-in/karşı onay/24 saat açıklaması taşmadan görüldü.
- Topluluk oluşturma formunda ad, tür, katılım, branş, bölge, açıklama ve boş adda pasif oluşturma düğmesi görüldü. Kayıt yapılmadı.
- Login penceresi, Apple/Google/telefon/misafir ve Privacy/Terms bağlantıları görüldü. Privacy URL'si yokken açıklama penceresi açıldı.
- Preview Apple seçimiyle pasaportun üç adımı gezildi: kimlik, spor/rol/seviye, tercihler. Boş ad doğrulaması; test adı/doğum yılı girişi ve adımlar arası geçiş görüldü. Son oluşturma yapılmadı; test profili kaydedilmedi.
- Örnek tesislerde ücretler ₺ ile, dolu saatler pasif görünüyor. Tesis değerlendirme penceresinde açıkça “Önizleme değerlendirmesi” gösteriliyor.
- Tur sonunda ana akışa dönüldü; misafir/mock görünüm açık bırakıldı.

Bu tur bütün 296 envanter satırının cihaz testi değildir. Canlı işletmeci/moderasyon ve gerçek sağlayıcı işlemleri bu preview turunda denenmedi. Bunlar sunucu/istemci testleri ve ayrıca canlı staging/cihaz doğrulaması olarak ayrılır.

## İlan/kadro düzeltmelerinden sonraki tur

7 Kasım ikinci turda geçici preview profili oluşturularak Şimdi Oyna → İlan ver formunun üç adımı incelendi. Test adı, buluşma yeri ve tercih özetinin aktarımı görüldü. CUA scroll/drag formun altına ilerlemedi; yayınlama veya çıkarma/restore cihazda uçtan uca test edildi denmiyor. Bu iş kuralları backend komutlarıyla yeni testlerde doğrulandı.

Geçici profil/taslak uygulama yeniden başlatılarak temizlendi. Son avatar/ayrıntı bağlarını içeren paket tekrar kuruldu; PID74213. Ana ekranda Giriş düğmesi, 65 örnek oyun ve Challenge/sıralama kartları CUA ile gözlendi. Varsayılan preview/mock görünüm açık bırakıldı.
