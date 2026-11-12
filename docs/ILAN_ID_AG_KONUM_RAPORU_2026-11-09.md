# İlan ID, ağlar, radar ve uygulama işlev raporu — 9 Kasım 2026

Bu çalışma önceki backend’i korur. `DemoData.kt` değiştirilmedi; açık simülatör için `preview` derlenir. Premium tema ve giriş öncesi/sonrası Explore pusulası korunur. **205 backend testi ve 62 ortak istemci testi başarılıdır; Android APK ve iOS Simulator framework derlenmiştir.** Canlı sağlayıcılarla yayın onayı anlamına gelmez.

## Bu istekte değişenler

| Alan | Son davranış |
|---|---|
| Spor radarı / ID ile bul | İl ve branş satırının üstünde yeni alan. UUID veya `matchora://event/...` bağlantısı girilince ilan açılır. Keşif şehrinin dışında kalan ilanlar da bulunur. Hatalı/olmayan ID hata gösterir; giriş, onay, kapasite, bekleme ve taahhüt kuralları detay ekranında yürür. |
| Benzersiz kimlik | Yeni canlı ilan sunucuda `crypto.randomUUID()` ile kimlik alır. Aynı işlem anahtarının tekrarında aynı kimlik döner. Kimlik kalıcıdır; ayrı bir mock kimliği sunucu kaydı yerine kullanılmaz. |
| Paylaşım | Yayın sonrası ilan detayı açılır. ID seçilebilir metin olarak görünür. Paylaş düğmesi iOS/Android sistem paylaşımını aynı ID, davet bağlantısı ve “Spor radarı → ID ile bul” tarifiyle açar. Alıcıyı kullanıcı seçer. |
| Saha / Kampüs / Şirket / Seyahat | Dört seçim ilan formunun **ilk adımında** görünür. Radarda seçilen ağ başlangıç değeridir. Kampüs/şirket için gerçek onaylı topluluk seçilir; üyelik yoksa form bunu açıklayan hata verir. Seyahat hedefi seçilen gerçek şehir/bölgedir. |
| Ağ backend’i | Üyeliğin yanı sıra topluluğun aktifliği ve türünün CAMPUS/COMPANY ile eşleşmesi doğrulanır. İsim topluluk kaydından gelir; başka tür topluluk ID’siyle kurum ilanı açılamaz. Saha/seyahatte ilgisiz communityId saklanmaz. Paylaşılan ID özel ağ üyeliğini atlamaz. |
| İl / ilçe ve harita | Mini radar ilk ilanın pinine kilitlenmez; elle seçilen bölgeye native geocoder ile gider. Büyük haritada bölge değişince eski pin odağı temizlenir. Önizlemede de bölge çözümleme çalışır. Gelen eski bootstrap veya GPS cevabı elle yapılan tercihi geri almaz. Geocoder bağlantısı yoksa kesin bir bölge bulunduğu varsayılmaz. |
| Mock görünümü | Saha mockları şehir seçimiyle kaybolmaz. Kampüs/şirket mockları yeni preview pasaportunda kurum alanı boş diye süzülmez. Harita kamerası başka şehre giderken İstanbul fixture pinleri kendi gerçek örnek koordinatlarında kalır; taşınmış sahte pin üretilmez. |
| Eski sohbetler | `GET /v1/threads` sayfaları ve “Daha eski sohbetleri göster”. Kullanıcıya bağlı süreli/şifreli cursor; kişisel gizleme sayfa öncesi uygulanır; hesap değişiminde durum temizlenir. Yenileme erişimi tekrar doğrular. |
| Sohbet bildirimi / okunmamış rozet | `GET /v1/threads/:id` ilk 100 dışındaki hedefi açar ve aktif sohbeti yeniler. Gizli veya üyelik dışındaki sohbet dönmez. Engellenen/erişimi kalkan sohbetin mesajları boşaltılır. Okunmamış konuşma sayısı tüm kayıtlardan hesaplanır; yalnız yüklenen sayfadan sayılmaz. |
| Anı düzenleme | Düzenleyici paylaşımın kendi şehir/ilçesini korur. Ayrı il/ilçe seçimi eklendi; bölge değişirse eski tesis/nokta temizlenir. Tema değişimi seçilen fotoğrafı kaldırmaz. Fotoğraf kaynağı ortak kamera/galeri penceresidir. |
| Vitrin konumu | Create/update ham koordinatı aralık ve sayı türüyle doğrular; iki ondalık saklar. Bootstrap ve global feed eski hassas payload’ları da yuvarlar. Bölge/tesis değişip yeni nokta verilmezse eski mesafe noktası temizlenir. |

**ID araması otomatik ücret/Token harcatan katılım değildir.** İlan doğrudan açılır; kullanıcı detaydan katılır veya organizatöre başvurur. Böylece onaylı ilan ve özel ağ kuralları paylaşımla delinmez. Önizlemede yeni kayıtlar oturumluk demo kaydıdır; cihazlar arası kalıcılık canlı sunucu gerektirir.

## Uygulama genelinde mevcut zincirler ve sınırlar

| Bölüm / pencereler | Mevcut bağlantı | Kalan sınır / dış kurulum |
|---|---|---|
| Giriş, ilk pasaport, belge onayı, OTP | OAuth/PKCE callback, oturum, profile.save, medya, sürümlü legal.accept ve SMS adaptörleri | Apple/Google/SMS hesapları ve gerçek yasal belgeler |
| Akış, radar, acil oyuncu, hızlı eşleş, harita, ilan detayı | Şehir/branş/ağ filtreleri, uygunluk skoru, yaklaşık GPS, ID araması; oluştur/düzenle/katıl/başvur/geri çek/kabul/ret/bekleme | Üretim harita servisi, fiziksel konum testi; öğrenilmiş AI değildir |
| Kadro, organizatör, maç sohbeti, check-in, sonuç, itiraz | Yetki/sürüm/kapasite, çıkar/yeniden izin ver, Token iadesi, QR süresi, sonuç doğrulama ve uyuşmazlık | Gerçek kamera/QR ve insan moderatör turu |
| Pasaport: branş kartları, son maçlar, güvenilirlik, rozet, görev, QR | Sunucuda MMR/Elo ve güvenilirlik; doğrulanmış sonuçlar; 8 gerçek rozet, haftalık tekil ödüller; profil/branş/fotoğraf/telefon işlemleri | Bazı kişisel geçmişler ilk 100/200 kayıtla sınırlı; bunların tümünde sayfalama henüz tamam değil |
| Yarış: sıralama, takım, davet, challenge, turnuva | Sıralama sayfalı/filtreli; kaptan işlemleri, doğru kaynak takım, talep/karar/ayrıl/çıkar/devir/arşiv; iki turnuva formatı, fikstür ve sonuç | Genel sporcu araması ve ileri format/ödül fonlama tamam değil; resmi kayıt kadrosu rastgele değiştirilemez |
| Ağ: topluluk kartları, üyelik ve yönetim | Oluştur/OPEN/APPROVAL/INVITE, yönetici kuyruğu, bildirim, davet, üye çıkar/devir/kapama, özel maç temizliği | Kurum kimliğini doğrulayacak dış sağlayıcı/operasyon |
| Ağ: tesis başvuru, işletmeci, saat, rezervasyon ve yorum | Başvuru/operatör onayı; adres ve saha sayısı; çakışmasız saat CRUD; gerçek rezervasyon durumları, tesiste ödeme kaydı, tamamlanmış rezervasyon yorumu | Gerçek işletme/saat verileri ve insan doğrulama; tekrarlayan toplu takvim ve kişi bazlı tahsilat/iade sağlayıcısı yok |
| Mesajlar: detay, bilgi, fotoğraf, yanıt, mesaj menüsü, sohbet menüsü | Gönder/oku/yanıt/düzenle/benden sil/herkesten sil, kişisel sohbet sil/temizle, engelle/mute/bildir; eski mesaj ve sohbet sayfaları | Yenileme polling; WebSocket/Realtime ve kalıcı çevrimdışı gönderim kuyruğu yok; arama yüklenen sohbetler üzerinde |
| Anı Vitrini: keşfet/yakın/en beğenilen/yeni/benim, oluştur/detay/düzenle | Tüm uygun kayıtlar üzerinde sunucu sıralama/cursor, kararlı random seed, konum mesafesi, beğen/paylaş/sil/bildir ve moderasyon | Canlı Storage ve içerik inceleyen operatör; otomatik içerik taraması yok |
| Bildirimler, tercih, cihaz kaydı | Kategorili sayfalama, tüm kayıtlardan okunmamış, oku/temizle, sessiz saat/kota/km, outbox ve APNs/FCM adaptörleri | Gerçek anahtarlar ve worker; OS teslim garantisi verilemez |
| Token, Plus, satın al/restore, defter, ödeme pencereleri | RevenueCat/StoreKit/Play Billing istemcisi ve doğrulanmış sunucu defteri, webhook/idempotency, abonelik hakları | Mağaza ürün/anahtar/webhook kurulumu ve sandbox testleri; tüm transfer/iade uzlaştırması otomasyonu tamam değil |
| Hesap, gizlilik, blok listesi, oturum, export/sil, destek ve operasyon | Tercihler, gerçek kullanıcı engeli, oturum iptali, dışa aktarma, gecikmeli silme/redaksiyon, destek/şikayet/moderasyon/rol kuyrukları | Dış sağlayıcı hesap birleştirme/silme uzlaştırması ve bazı operasyon geçmişlerinin sayfalaması |
| Privacy Policy, Terms ve destek bağlantıları | Tıklanabilir yapılandırma alanları ve sürüm denetimi; adres yokken yanlış başarı göstermez | Gerçek URL/metin/sürüm ve destek e-postası kullanıcıdan beklenir |

386 benzersiz özellik satırının tamamı: [ekran/pencere envanteri](TUM_EKRANLAR_BACKEND_ENVANTERI.md). Kod sayımı davranış testi sayısı değildir.

## Test ve kurulum kanıtları

- **205/205 backend testi**, atlanan yok. Bu turda ID’nin tekilliği/yeniden deneme/şehirden bağımsız bulunması, dört ağın tür/üyelik doğrulaması, kapalı/dolu/onaylı ilan; 100 dışı sohbet/ACL/cursor/okunmamış; vitrin hassas koordinat ve bölge değişimi eklenmiştir. PostgreSQL motoru/PGlite, RLS ve HTTP testleri de geçmiştir.
- **62/62 ortak istemci testi**, atlanan yok. ID/bağlantı ayrıştırma, native paylaşım metni, şehirden bağımsız sorgu; eski sohbet sayfası yenileme ve aktif sohbet erişim değişimi dahil.
- Android debug APK ve iOS Simulator framework başarılı. Swift uygulaması ve görsel kontrolün sonucu [kanıt klasöründe](validation/2026-11-09/ilan-id-ag/) kaydedilir.
- Supabase SQL Editor paketleri yeniden üretildi. Kalıcı UUID mevcut `events.id` sütunudur; bu özellik için yeni tablo/migration gerekmedi. Canlı Supabase üzerinde SQL çalıştırılmadı.

**Yalnız Google/Apple bağlantısı ve SQL Editor kaldı denemez.** API/worker yayını, Supabase Storage/Auth/RLS kurulumu, SMS/RevenueCat/APNs/FCM ve harita yapılandırmaları, gerçek yasal belgeler/dağıtım imzaları, yukarıdaki kalan geliştirmeler ve gerçek cihaz/mağaza testleri de duruyor. Bunlar tamamlanmadan “kusursuz backend” veya “yayın hazır” iddiası verilmez.
