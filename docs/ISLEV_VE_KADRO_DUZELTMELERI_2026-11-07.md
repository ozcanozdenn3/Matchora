# 7 Kasım — ilan, kadro ve uygulama işlev düzeltmeleri

Kullanıcının “İlan sahibi katılanı kadrodan çıkarabiliyor mu; uygulamanın diğer bölümlerinde de benzer boşluk var mı?” isteği üzerine mevcut işlemler kaynak ve test üzerinden yeniden incelendi. Gerçek eksikler bulundu ve aşağıdaki düzeltmeler uygulandı. Bu belge önceki rapora ektir; mock kataloğu ve önceki backend korunur.

## İlan ve maç kadrosu

- İlan oluşturma, başvuru, onay/ret, doğrudan katılım ve bekleme listesi vardı. Eksik `event.member.remove` eklendi ve oyuncu satırındaki çıkarma/onay işlemiyle bağlandı.
- Organizatör veya admin sıradan maçı başlamadan yönetebilir. Sürüm kontrolü vardır; organizatör kendini çıkaramaz. Resmi challenge/turnuva kadrosu serbestçe değiştirilemez.
- Çıkarılan katılımcının ilgili taahhüdü tamamen iade edilir; güvenilirlik/no-show cezası verilmez. Katılım/başvuru/sıra kapanır, sohbet erişimi kalkar, boş yer bekleme listesindeki sıradaki sporcuya sunulur.
- Çıkarılan oyuncu doğrudan katılma, başvuru veya sıra ile geri dönemez. “Çıkarılan katılımcılar” bölümündeki `event.member.restore` yalnız yeniden katılım izni verir; kendiliğinden kadroya almaz veya ücret kesmez.
- Başvurduktan sonra doğrudan katılınca başvurunun pending kalması düzeltildi. Başvuruyu iptal/ret ederek kadroda kalırken taahhüdü geri alma açığı kapatıldı.
- Aynı oyuncunun tam kadroya tekrar katıl isteği tek üyelik/tek taahhütle sonuçlanır. Ayrılıp yeniden katılımda yeni taahhüt nesli oluşur; eski iadeler tekrarlanmaz.
- Maç başladıktan sonra durum etiketi gecikmiş olsa bile sıradan ayrılma kapalıdır. Check-in sonrası kadro değişikliğini organizatör yönetir. Geç ayrılma düğmesi iade yapılmayacağını açıklar.
- Süresi biten maç tekrar başlatılamaz. Check-in yapmayan kişi tek skor kaydını tüketemez; sonuç bildirildikten sonra yeni check-in yapılamaz.
- Maç planı düzenleyicisinde kontenjan alanı bağlandı; kadro/ayrılmış yerlerin altına indirme sunucuda engellenir.

## Takım, topluluk ve turnuva

| Alan | Tamamlanan iş |
|---|---|
| Takım üyeleri | Kaptanın üyeyi çıkarma onayı; üyelik/sohbet erişiminin kapanması; çıkarılanın başvuruyla dönmesini engelleme; kaptanın yeniden davetiyle dönüş |
| Takımı kapatma | Tek üyeli kaptan da takımı kapatabilir. Davet/başvuru/üyelikler kapanır; bekleyen challenge taahhütleri asıl sahibine iade edilir. Arşivli takım yeni işlem alamaz. |
| Resmi kadro koruması | Aktif turnuva/fikstür taahhüdü varken ayrılma, çıkarma veya kapatma engellenir. Önce uygun kayıt geri çekilmeli ya da maç sonuçlanmalıdır. |
| Kaptanlık devri | Başka aktif üyeye devredilir; kendine veya askıdaki kullanıcıya devir ve üye olmayanın ayrılma isteği reddedilir. |
| Topluluktan ayrılma | Gelecekteki özel maç üyeliği/başvurusu/sırası ve taahhüdü temizlenir. Kullanıcı leave/join ile yöneticinin çıkarma kararını aşamaz. Yönetici daveti yeniden izin verir. |
| Topluluğu kapatma | Üyelik/başvurular kapanır, gelecekteki özel maçlar iptal ve taahhütler iade edilir. Devam eden özel maç varsa kapatma engellenir. |
| Topluluk daveti | Yöneticinin daveti onaylı toplulukta tekrar bekleyen başvuruya dönüşmeden kabul edilebilir. Askıdaki hesap onaylanamaz. |
| Turnuva takımı seçimi | Aynı branşta birden fazla kaptan takımı varsa kullanıcı takımını seçer. Çekilmede yalnız gerçekten kayıtlı takımlar gösterilir; yanlış takımın kaydı çekilmez. |
| Turnuva iptali | Hiç fikstür başlamamış running turnuva, bütün gelecekteki fikstürleriyle iptal edilebilir. Başlamış/sonuçlanmış fikstürler serbestçe silinmez. |
| Eleme beraberliği | Beraberlik skor kaydını tüketip turnuvayı kilitlemez; eşitlik bozucu dahil kazanan skor istenir. |

## Uygulamanın diğer bölümlerinde düzeltilenler

- **Form kapatma:** yönetim, tesis/saat oluşturma ve yorum penceresi kapanınca yerel coroutine'un iptal olması bulundu. Komutlar ana ekranın kalıcı yürütücüsüne taşındı; hata ve aynı anahtarla tekrar ana katmanda yaşar.
- **Bildirimden maç açma:** maç mevcut şehirde/sayfada olmasa da ayrıntı API'den alınır. Ayrıntı gerçek katılımcı profillerini de taşır; sonuç/geri bildirim penceresinde bu profiller korunur.
- **Aktif sohbet:** odada görülen yeni mesajın unread kalması düzeltildi. Okundu yalnız uygulama aktifken görünür ve kilitsiz odada ilerletilir.
- **Engelleme:** kullanıcının kendi koyduğu engel ile karşı tarafın koyduğu erişim engeli ayrıldı. “Engeli kaldır” kişinin kendi kaydı için gösterilir.
- **Özel fotoğraf:** sohbet fotoğrafının mesaj kimliğini `memory.like`/`memory.delete` komutuna yollaması düzeltildi. Bu işlemler yalnız gerçek vitrin anılarında vardır; özel fotoğraf görüntüleme/iletme sürer.
- **Üyelik ve push:** ayrılan/çıkarılan kişiye yeni özel sohbet bildirimi üretilmez. Kuyruktaki sohbet, katılım, plan değişikliği, hatırlatma ve sıra bildirimleri gönderimden önce güncel erişimi tekrar denetler.
- **Eski kayıtlar:** sahip/üye/davetli takımlar ve topluluklar, aktif kişisel turnuvalar ve başka şehirdeki aktif başvurular genel katalog sınırı nedeniyle kaybolmaz. Genel katalog/geçmiş sayfalaması hâlâ ayrı bir genişletmedir.
- **Şimdi oyna:** canlı öneriler seçili şehri filtreler; başka şehirdeki kendi maçları bu listeye karışmaz.
- **Profil fotoğrafı:** gerçek avatar yüklemesi diğer sporcu ayrıntısı ve maç/AutoSquad kadrosuna bağlandı. Fotoğraf yoksa mevcut baş harf görünümü kalır; preview değişmez.

## Kanıt ve sınır

- **149 backend testi geçti.** Yeni 23 regresyon; izin/yetki, iade, tekrar katılım, bekleme sırası, resmi kadro, takım/topluluk kapatma, KO eşitlik, eski katalog ve bildirim erişimi senaryolarını kapsar.
- **Android APK ve iOS framework derlemeleri başarılı; 32 istemci testi geçti.** Son preview paketinin kurulumu/başlatılması ve ana mock ekranı CUA ile doğrulandı.
- Mobil derleme ve istemci sonuçları [doğrulama klasöründe](validation/2026-11-07/). Testlerin geçtiği kaynak ile simülatöre kurulan paketin kapsamı ayrı kaydedilir.
- Preview simülatörde giriş/pasaport ve ilan formunun üç adımı gözlendi. Formun altına kaydırma CUA otomasyonunda ilerlemedi; bu tur yayınlama/oyuncu çıkarma bütünlüğü cihazda uçtan uca denenmiş sayılmaz. Kadro çıkarma/iade/sıra akışı backend testlerinde gerçek komutlarla doğrulandı.
- Geçici preview test profili ve form taslağı yeniden başlatmada sıfırlandı. `DemoData.kt` değiştirilmedi; ana mock ekran korunur.
- Supabase, sağlayıcı hesapları, imzalar ve gerçek cihaz/mağaza doğrulaması bekler. Bütün ekranların canlıda kusursuz çalıştığı iddia edilmez. Ayrıca kod/ürün kararı isteyen genişletmeler [önceki tamamlama raporunda](OZELLIK_TAMAMLAMA_2026-11-06.md#ayrıca-kod-veya-ürün-kararı-gereken-açıklar) ayrı durur.
