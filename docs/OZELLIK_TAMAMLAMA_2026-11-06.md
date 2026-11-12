# Matchora — özellik ve backend tamamlama raporu
Son işlev düzeltmeleri: [ilan/kadro ve uygulama yönetimi — 7 Kasım](ISLEV_VE_KADRO_DUZELTMELERI_2026-11-07.md).


Güncelleme: 7 Kasım 2026. Önceki backend ve `DemoData.kt` kataloğu korundu. Varsayılan uygulama `preview` modundadır. Canlı API hatası demo bakiyesine veya sahte işlem başarısına dönüşmez. Ana tasarım sistemi, renkler, kartlar ve gezinme korunur; yeni işlemler mevcut bileşenlerle ilgili ekranlarına eklenir.

## MMR hangi işlemle artar?

1. Her branş ilk kez 1000 MMR ile başlar. Seviye seçimi başlangıç puanını yükseltmez.
2. Maç rekabetçi olmalı; sporcu check-in yapmalı, taraflar belli olmalı ve karşı tarafta da check-in bulunmalı. Branş pasaportta bulunmalı.
3. Skoru bildirenin karşı tarafındaki kadronun çoğunluğu onaylar. Onay verenin check-in kaydı aranır. Yedi rakipte dört onay gerekir; takım arkadaşı onayları sayılmaz.
4. Skor bildiriminden başlayan 24 saatlik itiraz süresi ve maç bitişi geçince sunucu sonuçlandırır. İtirazlı veya eksik onaylı sonuç yönetici incelemesine kalır. Yönetici gerekçeli kararla süre/çoğunluk şartına istisna uygulayabilir; katılım ve MMR uygunluk kuralları korunur.
5. Elo K=24 kullanılır. İki taraf 1000 ise galibiyet +12, mağlubiyet −12, beraberlik 0. Takımda check-in yapan oyuncuların ortalaması karşılaştırılır; güçlü rakibi yenmenin getirisi artar. Puan 0 altına inmez, geçmişe gerçekten uygulanan fark yazılır.
6. Sosyal maç MMR değiştirmez. Check-in yapmayan sporcu maç sayısı/görev kazanmaz; güvenilirliği 10 azalır. Doğrulanmış katılım güvenilirliği üst sınır 100 olmak üzere 2 artırır.
7. Branşı kaldırıp geri eklemek puanı ve geçmişi sıfırlamaz. Sonuçlanmamış katılım varken branş kaldırılamaz. Resmi challenge/turnuva kadrosu sıradan katıl/ayrıl/başvuru/sıra yollarıyla değiştirilemez.

MMR açıklaması sporcu/pasaport ayrıntısındadır. Maç sonuç penceresi bekleme nedenini, rakip onay sayısını ve kesinleşen farkı gösterir. Token, haftalık görev ve Plus MMR satın almaz.

## Tamamlanan diğer akışlar

| Alan | Ürün ve backend davranışı |
|---|---|
| Görevler/rozetler | Bütün görevler tek pencerede; ilerleme ve yüklenmiş TKN durumu. No-show seri bozuyor. Pazar maçı Pazartesi doğrulansa da kendi haftasının ödülü bir kez veriliyor. Rakip görevleri takım arkadaşlarını saymıyor. |
| Turnuva/lig | Canlı form eleme veya tek devre lig, gerçek kapasite, asgari MMR ve telefon koşulunu seçiyor. Tüm dondurulmuş kadro kayıt ve ilk fikstür öncesi kontrol ediliyor. Eleme 2/4/8/16/32, lig 2–16. Oluşturma ücreti sunucudan 20 TKN; preview korunuyor. |
| Topluluk | Oluşturma, tür/branş/erişim/bölge/açıklama, sürümlü düzenleme, üye çıkarma, davet/başvuru ve yönetici devri. Çıkarılan kişinin gelecekteki özel maç üyeliği/başvuru/sırası/taahhüdü temizleniyor. |
| Kampüs/şirket | Sunucudaki gerçek üyelik esas alınıyor; serbest profil metni yetkili maçları tekrar gizlemiyor. Eksik üyelikten topluluk keşfine bağlantı var. |
| Bölge/harita | Elle seçilmiş bölge Android/iOS geocoder ile merkezleniyor; GPS izni gerektirmiyor. Canlı haritada uydurma demo koordinatı yok; bölge/GPS çözülemezse Türkiye merkezi. Preview haritası korunuyor. |
| Keşif sayfalaması | Seçili şehirde ilk 200 aktif maç ve kendi ilgili geçmişi; ana akış/harita listesinden daha fazlası. Şifreli, bütünlük kontrollü, 1 saatlik cursor. Her sayfa ve yenilemede özel ağ erişimi yeniden denetleniyor. |
| Gerçek yer seçimi | Anı/challenge için canlı katalog veya elle yer adı; örnek tesis listesi canlı katalog diye gösterilmiyor. |
| Tesis ücreti | Toplam ve kişi başı ücret kuruş hassasiyetinde; TL ücret TKN diye gösterilmiyor. Bölüşüm önerisi toplam borcu azaltmıyor. |
| Tesis yorumları | Kendi tamamlanmış rezervasyonuna puan/yorum, düzenleme/silme. İnceleme sonrası yayın; gerçek yayınlanmış yorumlardan tesis puanı; sahiplik/engel kontrolü. |
| İşletmeci | Güncel işletmeci/yönetici yetkisi; tesis/saat oluşturma-kapatma, rezervasyon onayı, tesiste ödeme kaydı, tamamlama, iptal ve offline iade kaydı. İade para transferi yapmaz; referansla gerçekleşmiş işlemi kaydeder. |
| Moderasyon | Destek yanıtı, şikayet bağlamı, anı/fotoğraf/sonuç/yorum inceleme, kullanıcı askıya alma/açma, işletmeci atama, özellik bayrağı ve başarısız gönderim tekrarı. Güncel rol kontrolü; şikayet bağlamında özel sohbet gövdesi yok. |
| Fotoğraf doğrulama | Aynı dosyaya açık talep çoğalmıyor; istemci gerçek ACK bekliyor. Eski avatar onaylanamıyor, sadece ilgili upload talebi kapanıyor. PHOTO resmi kimlik/canlılık doğrulaması değildir. |
| Bildirim üreticileri | Aynı ilçeye uygun acil çağrı, doğrulanmış haftalık özet, karşılıklı tekrar oynama önerisi. Özet Pazar maçlarının itiraz süresini bekliyor ve MMR'ı maç haftasına yazıyor. Pazarlama izinli aktif hesaplara şehirli yönetici duyurusu; 24 saatlik gönderim sınırı/idempotency. Otomatik DM/katılım yok. |
| Bildirim kuyruğu | Tercih, sessiz saat, süre, günlük kota, retry. Tek satır claim ve sahiplik tokenı; kota sağlayıcının kabul gününden ve aktif rezervasyonlardan hesaplanıyor; geçersiz cihazlar teslim edilmiş sayılmıyor. Cihaz teslim makbuzu izlenmiyor. Sağlayıcı teslimatı ile DB commit arasındaki ağ çökmesinde tekrar olasılığı için sağlayıcı dedupe/collapse davranışı hâlâ önemlidir. |

Önceden yazılmış giriş/OTP, mesajlaşma, medya, QR/check-in, cihaz izinleri, hesap silme/dışa aktarma, yasal bağlantılar, RevenueCat ve APNs/FCM adaptörleri duruyor. Her ekranın ve pencerenin sınırı [tam envanterde](TUM_EKRANLAR_BACKEND_ENVANTERI.md), dosya/fonksiyon/komut bağlantıları [kaynak dizininde](UI_KAYNAK_DIZINI.md).

## Son bağlantıda gerekenler

| İş | Hazır zemin | Kalan işlem |
|---|---|---|
| Supabase | PostgreSQL adaptörü, migrationlar, RLS/özel bucket ve SQL Editor paketi | [Kurulum sırası](../backend/supabase/README.md); gerçek bağlantı/TLS, yedek ve geri yükleme testi |
| API yayını | Node 24, Docker, health, doğrulama, CI | Ayrı HTTPS sunucu ve ortam değişkenleri. SQL Editor Node API'sini yayınlamaz. |
| Apple/Google/SMS | Sistem tarayıcısı/PKCE/callback ve OTP | Hesaplar, redirect allowlist, anahtarlar, gerçek giriş/teslim testi |
| Cihaz işlevleri | Android/iOS expect/actual; izin, ret, hata/ayar yolları | Fiziksel cihazda konum/kamera/QR/galeri/takvim/push, kısıtlı izin ve arka plandan dönüş testleri |
| Token/Plus | RevenueCat satın al/restore/yönet; server sync/webhook/defter | Mağaza ürün/ID/anahtarları, RevenueCat hesapları, sandbox satın alma/iade/yenileme testleri |
| Hukuki belgeler | Terms/Privacy/kurallar/silme/destek tıklanabilir; sürüm/onay | Gerçek metin, URL ve destek adresi. URL yokken açıklama var. |
| Operasyon | Moderasyon/işletmeci panelleri, audit | Gerçek yetkili kullanıcılar, tesis/saat kataloğu ve destek işleyişi |
| Yayın | Debug derlemeleri ve otomatik kontroller | İmzalı release AAB/IPA, cihaz/mağaza testleri, mağaza formları, yük/güvenlik/izleme |

## Ayrıca kod veya ürün kararı gereken açıklar

Bu işler yalnız dış anahtar ekleyerek kapanmaz:

- İsviçre, grup+eleme gibi ileri turnuva motorları ve karmaşık eşitlik/operasyon politikaları. Canlı form uygulanmış iki formatla sınırlıdır.
- Şehir kemeri/referans programı, preview'daki bütün VIP/öncelikli radar vaatleri ve öğrenilmiş AI. Canlı Plus metni uygulanmış hakları gösterir.
- Kesintisiz video QR tarayıcı, geofence ve cihaz bütünlüğü. Mevcut QR akışı fotoğraf veya elle kod kullanır.
- WebSocket/Supabase Realtime ve kalıcı çevrimdışı komut kuyruğu. Mevcut kalıcı sohbet aktifken periyodik yenilenir.
- Maç/mesaj dışındaki büyük katalog/geçmiş/yönetim listelerinin tam sayfalaması. İşletmeci verisi tesis başına 100 saat/rezervasyonla sınırlıdır; diğer limitler kaynakta bulunur.
- Acil çağrıda tam kilometre yarıçapı yerine aynı ilçe kuralı kullanılır.
- Otomatik içerik sınıflandırması ve bütün abonelik transfer/iade yaşam döngülerinin otomatik uzlaştırılması.
- Hesap silmede Supabase Auth/Apple dış sağlayıcı iptali; ücretli fiziksel rezervasyon/tesis sahipliğinin nihai kapanışı; medyanın toplu dışa aktarımı ve kesin saklama politikası.
- İstenirse gerçek kartla tesis tahsilatı/iade sağlayıcısı. Mevcut offline kayıtlar bankacılık işlemi yapmaz.

## Doğrulama

**149 backend ve 32 KMP/istemci testi geçti; hata/atlanan yok. Android APK ve iOS framework derlemeleri başarılı.** Son preview paketi simülatörde açıldı; login/üç pasaport adımı/MMR/topluluk/tesis yorumları incelendi, ana mock akış açık bırakıldı.

Son sonuçlar ve cihaz turu [durum raporunda](BACKEND_DURUM_RAPORU.md), ham kayıtlar [doğrulama klasöründe](validation/2026-11-07/). PostgreSQL motoru testi gerçek Supabase ağı değildir. Preview turu gerçek Apple/Google/SMS/mağaza işlemi değildir. `DemoData.kt` değişiklik kontrolü ve Android/iOS derlemeleri ayrıca kaydedilir.
