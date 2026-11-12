# Yarış, ağ ve tesis — işlev tamamlama raporu

Bu devamda yalnız servis fonksiyonlarının varlığına bakılmadı: formun gönderdiği alanlar, sunucunun uyguladığı koşullar, dönen verinin kart/pencereye taşınması ve sonraki işlemin erişilebilirliği düzeltildi. Mock kataloglar ve premium tema korundu; `DemoData.kt` değiştirilmedi.

## Kapatılan somut eksikler

| Alan | Değişen davranış / backend |
|---|---|
| Topluluk ile ilan ayrımı | Topluluk oluşturma artık kalıcı **spor grubu, kampüs topluluğu, şirket spor topluluğu** seçer. Saha/Seyahat yeni topluluk türü değildir. İlan ise tek buluşmadır: halka açık şehir, üye olunan kampüs/şirket topluluğu veya seyahat hedefi. Mevcut eski mock topluluklar silinmedi. |
| Kupayı oluştur | Format/kapasite serbest metin yerine uygulanan tek eleme/lig seçicisidir. Takım başına maç kadrosu sayısı, bölge, MMR ve doğrulanmış iletişim şartı ayrı alanlardır. Yeni kupa sıfır kayıt ve boş fikstürle başlar; uydurma altı takım ve iki eşleşme eklenmez. Önceden var olan mock kupalar korunur. |
| Kayıt | Organizatör kendi takımıyla da kayıt olabilir. Kaptan takımını ve maçta oynayacak kişileri seçer. Seçilen sayı, aktif üyelik, branş/MMR/doğrulama ve aynı turnuvada başka takımda oynama yasağı sunucuda denetlenir. Kulübün bütün üyeleri otomatik fikstüre eklenmez. |
| Kayıtlar sekmesi | Kayıtlı takımlar, maç kadrosu sayısı ve durum gösterilir. Önceki demo AI sekmesinin yerine gerçek kayıt bilgisi gelir. Organizatör kayıt döneminde gerekçeli çıkarma yapar; kaptan ve kadro bilgilendirilir. Yeniden kayıt izni otomatik katılım yapmaz. Başlayan turnuva kadrosu bu işlemle değişmez. |
| Fikstür | Sunucu gerçek maç ID’si, takım isimleri, tarih/saat ve tesisi döner. Fikstür satırı gerçek maç detayını açar; check-in, sohbet, skor ve itiraz aynı maç üzerinde çalışır. Takımlar ilk katalog sayfasında olmasa da isimleri kaybolmaz. |
| Tur / sonuç / şampiyon | Kayıtlar dolmadan ilk tur, sonuçlar doğrulanmadan sonraki tur açılamaz. Finalden sonra şampiyon kimliği kalıcı kaydedilir, katılanlara bildirilir ve organizatör kilidi bir kez çözülür. Uygun finalde ayrıca anlamsız yeni tarih formu doldurmak gerekmez. |
| Kulübünü oluştur | Sabit İstanbul mahalleleri kaldırıldı; gerçek il/ilçe seçimi city/area olarak saklanır. Oluşan canlı takımın detayı açılır. Varsayılan başvuru MMR’ı yeni sporcuların başlangıcı olan 1000’dir. |
| Kadro / başvurular | Gerçek takım oluştur → başvur → kaptan kabul/ret → üyelik/sohbet → çıkar → davetle yeniden katıl zinciri test edildi. Turnuvaya seçilmiş resmi kadroların taahhüdü korunur. Başvuru/davet bildirimleri ilgili takım kartını açar. |
| İlk sayfa dışı bildirim hedefi | Takım/turnuva/topluluk/tesis için tek kayıt GET’i eklendi. Hedef ilk 100 kayıtta yoksa getirilir; sonraki yenilemede yeniden doğrulanır; arşivlenmiş kayıt eski önbellekten geri dönmez. Detay kapatılınca bildirim odağı tekrar tekrar pencere açmaz. |
| Tesisini ekle | Başvuru formu önizlemede de incelenebilir. Canlı başvuru yöneticiye gider; onaydan sonra tesis yayına ve işletmeci yönetimine açılır. Tesis adres/branş/saha sayısı/olanak/iptal alanları ve onay durumu önceki servislerle bağlıdır. |
| Müsaitlik | Tek saat ekle/düzenle/kapat/yeniden aç işlemlerine **haftalık plan** eklendi: tarih, günler, başlangıç, günlük aralık sayısı, süre, kort ve kuruş bedeli. Tek işlem 1–120 gerçek aralık üretir. Bir çakışma varsa hepsi geri alınır; önceki rezervasyon bozulmaz. Aynı işlem anahtarı aynı kimlikleri döner. |
| Tesis değerlendirme | Tesisin yorum penceresi artık kişinin tamamlanmış ve ödenmiş rezervasyonlarından doğrudan yazma girişi açar. Başkasının rezervasyonu ve henüz tamamlanmamış kayıt dönmez. Yorum düzenle/sil/bildir, sürüm ve moderasyon kuralları korunur. |
| Önizleme pasaportu | Yeni preview girişinden sonra demo istatistikleri ve diğer örnek branş kartları da korunur. Canlı yeni hesabın sıfır geçmiş kuralına dokunulmadı. |

## Otomatik doğrulama

- **213/213 backend testi** geçti. PostgreSQL motoru/PGlite, RLS ve HTTP regresyonları dahildir.
- Yeni testler yalnız satır varlığına bakmaz: gerçek seçilmiş kadro → fikstür → iki oyuncunun check-in’i → skor → rakip onayı → zaman penceresi/worker → kalıcı şampiyon zincirini yürütür. Kayıt çıkarma/yeniden izin, kulüp üyeliği ve haftalık tesis planında atomik geri alma da testlidir.
- Ortak istemciye fikstür/isim/tarih/hedef, takım bölgesi, çıkarılan kayıt durumu ve ilk sayfa dışı katalog hedefi için testler eklendi. Güncel mobil derleme ve test sonucu kanıt klasöründe tutulur.
- Yeni alanlar mevcut JSON payload ve durum kayıtlarını kullanır. Yeni tablo/migration gerekmez; Supabase SQL Editor paketi tekrar üretildi. Canlı Supabase üzerinde işlem yapılmadı.

Kaynak kapsamı: **56 ortak UI dosyası, 261 fonksiyon bildirimi, 95 pencere/menü çağrı noktası, 111 komut, 67 tablo**. Bunlar cihaz testlerinin sayısı değildir. [398 özellik satırı](TUM_EKRANLAR_BACKEND_ENVANTERI.md), [kaynak dizini](UI_KAYNAK_DIZINI.md).

## Hâlâ tamamlanmış sayılmayanlar

| Kalan | Açıklama |
|---|---|
| Canlı kurulum | Supabase DB/RLS/Storage/Auth, Node API HTTPS + worker, Apple/Google/SMS, RevenueCat mağaza ürünleri/webhookları, APNs/FCM, üretim harita adresi, dağıtım imzaları ve gerçek yasal metin/URL/sürümler. Yalnız SQL Editor ve iki sosyal giriş bağlantısı yeterli değildir. |
| Kalan geliştirme | Bazı kişisel/operasyon geçmişlerinde 100/200 kayıt sınırı; genel sporcu araması; sağlayıcı hesabı birleştirme/silme ve tüm mağaza transfer/iade uzlaştırması; kalıcı çevrimdışı gönderim kuyruğu. Mesajlar şu anda polling kullanır. |
| İleri ürünler | Otomatik içerik taraması, ileri turnuva formatları/ödül fonlama, kişi bazlı gerçek tahsilat/iade sağlayıcısı, plan bitince otomatik yeni tarih üretme, öğrenilmiş AI/otomatik dengeli kadro. Haftalık müsaitlik planı sonlu tarih aralığı üretir. |
| Yayın testleri | Gerçek mağaza satın alma/restore/iade, gerçek sağlayıcı girişleri/SMS ve fiziksel Android/iPhone konum/kamera/galeri/bildirim izinleri; erişilebilirlik/büyük yazı, üretim yükü ve yedekten dönüş. |

Önizleme, canlı veri kaydı değildir. Önizlemede tesis başvuru ve fikstür planı alanları görünür; gerçek yayın işlemi açıkça canlı hesap bekler. Bu rapor bütün uygulamanın kusursuz veya mağazaya yayına hazır olduğunu iddia etmez; önceki “her yer tamam” ifadesinin yerine test edilmiş davranışları ve kalanları belirtir.
