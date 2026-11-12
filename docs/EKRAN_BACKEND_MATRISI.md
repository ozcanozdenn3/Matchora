# Matchora — ekran ve backend bağlantı matrisi
Son işlev düzeltmeleri: [ilan/kadro ve uygulama yönetimi — 7 Kasım](ISLEV_VE_KADRO_DUZELTMELERI_2026-11-07.md).


Güncelleme: 7 Kasım 2026. `preview` tüm örnek verileri korur; aşağıdaki API bağlantıları `local/production` içindir. “Bağlı” canlı sağlayıcı hesabıyla uçtan uca test yapıldığı anlamına gelmez. Her ekran/alt pencerenin güncel ayrıntısı [319 benzersiz satırlık tam envanterde](TUM_EKRANLAR_BACKEND_ENVANTERI.md); önceki 253 ID korunmuş, 66 yeni özellik/pencere eklenmiştir. Önceki görsel tur ayrı tarihli incelemededir.

| Ekran / alt akış | Veri / işlem bağlantısı | Durum / sınır |
|---|---|---|
| Splash / başlangıç | Yapılandırma, oturum, bootstrap, hata/yeniden deneme | Bağlı |
| Misafir keşfi | Kamuya açık bootstrap, kayıt gerektiren işlemde auth kapısı | Bağlı |
| Login Apple / Google | OAuth start/finish, PKCE, sistem tarayıcısı, callback, token exchange | Sağlayıcı kurulumu / canlı test gerekir |
| Telefon girişi | OTP start/verify, SMS adaptörü | Hesap ve canlı teslim gerekir; WhatsApp yok |
| Sporcu Pasaportu ilk giriş | `profile.save`, fotoğraf yükleme, `legal.accept` | Bağlı |
| Güncel belge onayı | Sürüme bağlı onay; mevcut kullanıcıda yeniden inceleme | Kod bağlı; gerçek metin/URL/sürüm bekliyor |
| Ana akış / radar | Canlı şehir sorgusu, maçlar, branş/ağ filtreleri, kurallı skor | Bağlı; özel ağ görünürlüğü onaylı sunucu üyeliğinden gelir |
| Akış / harita sonraki maç sayfası | `BackendMoreEventsButton.kt`, `nextEventCursor`, `loadMoreEvents`, yükleniyor/hata/tekrar | Bağlı; Home ve harita liste görünümünde; diğer kataloglar tam sayfalanmış değil |
| Challenge / sıralama özetleri | Gerçek takım, teklif, yerel sıralama ve haftalık fark | Bağlı; aynı kart yüksekliği |
| Akıllı eşleşme | Gerçek maç verisi, kurallı skor/filtre; kapalı sinyale uyma | Eğitilmiş AI veya otomatik organizasyon değildir |
| Maç kadrosu (AutoSquadSheet) | Gerçek katılımcılar, açık yerler, maç/sohbet geçişi | Bağlı; otomatik kadro kurma ayrı iş |
| Keşif haritası / filtre | Gerçek yaklaşık koordinat, canlı şehir filtresi ve native harita | Bağlı; canlıda demo koordinatı üretilmez; preview merkez/pinleri korunur |
| İl / ilçe seçici ve boş harita merkezi | Native GPS; izin istemeyen Android/iOS adres geocoding; `discovery.save` | Bağlı; manuel bölge → geocoder, uyumlu cihaz konumu veya Türkiye; boş şehirde ağ çağrısı yok; cihaz testi gerekir |
| Play Now / acil oyuncu / rakip bul | Maç keşfi, `event.create/join/request` | Bağlı; Play Now içinde ayrı sonsuz liste sayfalaması yok |
| Maç oluştur / yer / zaman seç | Server doğrulaması, ağ üyeliği, gerçek tarih, yer seçici | Bağlı |
| Maç detayı / katılım / ayrılma | `event.join/leave`, `request/cancel`, waitlist | Bağlı |
| Organizatör başvuru yönetimi | `event.request.review` | Bağlı |
| Maç düzenle / başlat / bitir / iptal | `event.update/transition`, yetki/sürüm ve bildirim | Bağlı |
| Check-in | Süreli imzalı kod, QR üretme/okuma, `event.checkin` | Fotoğraf/kod yolu bağlı; sürekli video/geofence yok |
| Maç sonucu / taraflar / onay / itiraz | `result.submit/confirm/dispute`; bireyselde ayrık taraflar, takım/turnuvada donmuş kadro | Bağlı; onayda check-in, rakip çoğunluğu ve 24 saatlik süre; operatör kararı var |
| MMR nasıl değişir? / sonuç MMR kartı | `RatingAndMissionSheets.kt`; config politikası ve kişi bazlı onay/delta/neden | Bağlı; 1000 başlangıç, rekabetçi maç/check-in/karşı katılım/branş; Elo K=24 ve taraf ortalaması; sosyal maç/görev/Token/Plus MMR kazandırmaz |
| Katılımcı değerlendirme | `feedback.submit` | Bağlı |
| Davet paylaş / yol tarifi / takvim | Native paylaşım, harita, takvim düzenleyicisi | Kod bağlı; fiziksel cihaz testi gerekir |
| Sıralama / sporcu detayı | Gerçek sıralama/haftalık fark, kapsam filtreleri, DM/davet | Gerçek form/MMR/rozet/ortak maç/uyum bağlı; anlık müsaitlik ve bireysel performans puanı yok |
| Takım oluştur / detay / ilan | `team.create/update`, gerçek kadro/istatistik | Bağlı |
| Takım başvurusu / geri çekme / inceleme | `team.request/cancel/review` | Bağlı |
| Takım daveti / kabul / ret / ayrıl / kaptan devri | `team.invite/respond/leave/transfer` | Bağlı |
| Challenge gönder / sözleşme / kabul / ret / geri çek | `challenge.create/respond/cancel`; maç ve sohbet oluşturma | Bağlı; bahis/tahsilat servisi yok |
| Challenge / anı gerçek saha seçimi | `VenueChoiceField.kt`; canlı tesis kataloğu veya elle yer, anıda şehir filtresi | Bağlı; preview önerileri korunur; yer metni rezervasyon veya venue ID bağlantısı oluşturmaz |
| Turnuva oluştur / kayıt / çekil / iptal | `tournament.create/register/withdraw/cancel` | Bağlı |
| Turnuva formatı / kapasitesi | Canlı yapılandırılmış `KNOCKOUT` veya `ROUND_ROBIN`; eleme 2/4/8/16/32, lig formunda 2/4/6/8/10/12/16 takım | Bağlı; İsviçre/grup+eleme preview örnekleri canlı motor sayılmaz |
| Turnuva MMR / doğrulama koşulları | `minRating`, `verifiedOnly`; her kadro üyesinin branş/MMR/BASIC şartı kayıtta ve fikstür öncesi doğrulanır | Bağlı; serbest metin tüm ek kuralları otomatik uygulamaz |
| Turnuva fikstürü / puan / sonraki tur | `tournament.schedule/complete`, doğrulanmış sonuçlar ve iki formatın ilerlemesi | Bağlı; ödül fonlama/dağıtım, ileri formatlar ve zaman planlayıcı yok |
| Turnuva analiz sekmesi | Gerçek puanların kurallı özeti | Canlıda kazanma tahmini veya AI olarak sunulmaz |
| Kaynakta kalan Match Scouter / kemer | Görsel simülasyon kaynakları | Tam ürün/servis akışı yok; preview kaynakları korunur |
| Spor ağı / seyahat | `discovery.save`, üyelikler | Bağlı |
| Topluluk oluştur | `CreateCommunitySheet.kt`, ad/tür/katılım/branş/konum/açıklama → `community.create` | Bağlı; preview yerel örnek ekler; kurumsal kimlik/SSO yok |
| Topluluk katıl / başvuru / incele / davet / devir | Community komutları, özel ağ kontrolü | Bağlı |
| Topluluk düzenle / üyeler / çıkarma onayı | `community.update/member.remove`, sahiplik/sürüm, gelecekteki özel maç üyeliği ve taahhüt temizliği | Bağlı; çok kademeli yardımcı yönetici/rol matrisi yok |
| Tesis / saat / rezervasyon | Gerçek venue/slot kataloğu, `booking.create/cancel`; toplam tutar sunucudan | Bağlı; tesiste ödeme; dış katalog/gerçek kart tahsilatı yok |
| Tesis yorum listesi / yaz / düzenle / kaldır / bildir | `VenueReviewSheets.kt`, tamamlanmış kendi rezervasyonu, tek yorum/sürüm ve yayın moderasyonu | Bağlı; published yorumlardan gerçek puan/sayı; yorum listesi son 50 kayıt, tam sayfalama yok |
| İşletmeci paneli / tesis ve saat ekleme | `OperationsSheets.kt` → `VenueOperatorSheet`, rol/sahiplik kontrollü `/v1/operator/venues` | Bağlı; temel tesis/slot formları var; ileri alan düzenleme/tekrarlı saat takvimi yok |
| İşletmeci onay / tesiste ödeme kaydı / tamamla | `booking.confirm/payment.record/complete`, `venue.slot.cancel`, onay ve referans pencereleri | Bağlı; karttan para çekmez; gerçek işletmeci verisi gerekir |
| Tesiste yapılmış tam iadeyi kaydet | `booking.refund.record`, referans/audit/bildirim, ödeme refunded ve rezervasyon cancelled | Bağlı; offline iade kaydıdır, para transferi veya kısmi iade yapmaz |
| Sohbet listesi / DM / maç / takım | Üyelik/engel kontrollü kalıcı threads/messages | Bağlı; polling, Realtime yok |
| Mesaj gönder / fotoğraf / ilet / eski mesajlar | `chat.send`, medya ACL, messages sayfalaması | Bağlı |
| Sohbet bilgisi / okundu / sessiz / engel / bildir | `chat.read/mute`, `user.block`, `report.create` | Bağlı |
| Anı vitrini / detay / oluştur / beğen / sil / bildir | Memory komutları, operasyon panelinde moderasyon; atomik vitrin+sohbet | Skor/tema, gerçek tesis seçimi ve atomik paylaşım bağlı; otomatik tarama yok |
| Kamera / galeri | Yerel kamera, Photo Picker / PHPicker, dosya hazırlama | Tüm çağrı noktaları ortak seçiciyi kullanır; gerçek cihaz testi gerekir |
| Bildirim kutusu / tercih / tıklama | Notification/outbox, maç hatırlatma, APNs/FCM adaptörleri, native izin | Kod bağlı; gerçek hesap/anahtar, worker yayını ve cihaz testi bekliyor |
| Acil çağrı / haftalık özet / rematch üreticileri | `engagement.mjs`; üyelik/engel/tercih, haftalık/olay tekilleştirme ve outbox | Bağlı; acil çağrı il/ilçe kullanır, tam km yarıçapı yok; kendiliğinden DM/katılım yapmaz |
| Yönetici duyuru oluştur / gözden geçir / gönder / geçmiş | `OperationsSheets.kt`, `admin.announcement.send`, isteğe bağlı il ve marketing=true, günlük rate limit/audit | Bağlı; kampanya takvimi/ileri segment editörü/teslim analitiği yok |
| Token paketleri / Plus / restore / yönet | RevenueCat native SDK, backend sync/webhook/ledger | Ürün/anahtar ve sandbox testi bekleniyor; satış kapatma bayrağı bağlı |
| Cüzdan hareketleri / kilitler | Sunucu defteri ve scoped holds | Bağlı |
| Pasaport / spor düzenleme / istatistik | Profil, doğrulanmış history/reliability/rating | Bağlı; sunucu puanını istemci değiştiremez |
| Tüm haftalık görevler / rozet / seri | `RatingAndMissionSheets.kt` → `MissionsSheet`; üç gerçek görev, ilerleme ve otomatik tekil TKN ödülü | Bağlı; 3 farklı rakip +15, tekrar oynama +10, iki check-in +10 TKN; referans/kemer ayrı |
| Dijital sporcu kartı | `passport.code` + süreli doğrulama API’si | Bağlı; kimlik belgesi değildir |
| Oyun takvimim / başvurular / sahalar / değerlendirme | Kendi maçları, talepler, fiziksel rezervasyonlar ve tamamlanmış rezervasyondan yorum editörü | Bağlı |
| Hesap / gizlilik / telefon / fotoğraf | Preferences, OTP, manuel operatör incelemesi | Kod bağlı; gerçek SMS ve cihaz testi gerekir; otomatik kimlik/canlılık kontrolü yok |
| Veri dışa aktarma / oturumlar | Kendi JSON verisi, native paylaşım, session revoke | Bağlı; toplu medya paketi yok |
| Hesap sil / geri çek | Gecikmeli silme, redaksiyon, medya kuyruğu, devirler | Yerel API tarafı bağlı; dış sağlayıcı/ücretli işlem kapanışı tamamlanmalı |
| Destek / rapor / operatör yanıtı | Support/report, admin karar komutları; yanıt/durum bildirimi | Bağlı; karşılıklı destek konuşması ve otomatik yaptırım yok |
| Operasyon merkezi / alt kuyruklar | `OperationsSheets.kt` → `ModerationSheet`; destek/şikayet/anı/fotoğraf/sonuç/yorum, bağlam ve gerekçeli karar | Bağlı; ilk admin hesabı ve yetkili personel gerekir; otomatik tarama/hakemlik yok |
| Operasyon kullanıcı / işletmeci rolü / askıya alma | Admin rol kontrollü kullanıcı arama, `admin.operator.assign`, `admin.user.status` | Bağlı; yeni admin oluşturma/çok kademeli rol editörü yok |
| Operasyon outbox hata / özellik anahtarları | `admin.outbox.retry`, `admin.feature.set`; işlem onay pencereleri | Bağlı; sağlayıcı kurulum hatalarını otomatik çözmez |
| Privacy / Terms / kurallar / silme / destek linki | Tıklanabilir boş durum, sürümlü yasal yayın | Gerçek URL’ler/metin/sürüm bekleniyor |
| Menü / tab geçişleri | Mevcut gezinme, auth kapıları ve deep link | Korundu; bazı hedef ayrıntıları genişletilmeli |

Yeni pencereler ve ortak bileşenler adlarıyla tam envanterde kayıtlı: `RatingAndMissionSheets.kt`, `CreateCommunitySheet.kt`, `VenueChoiceField.kt`, `OperationsSheets.kt`, `VenueReviewSheets.kt`, `BackendMoreEventsButton.kt`. Statik kaynak envanteri davranış veya gerçek cihaz testi değildir.

Supabase SQL/RLS/Storage kurulumu, Node API HTTPS ve worker yayını, Apple/Google, SMS, RevenueCat/mağaza ürünleri, APNs/FCM, yasal metin/URL ve dağıtım imzaları dış kurulum olarak bekliyor. Bu kod bağlantıları uygulamanın üretimde yayıma hazır olduğuna tek başına kanıt değildir; yalnız SQL Editor işlemleriyle bütün süreç tamamlanmaz. [Ayrıntılı durum ve kalan işler](BACKEND_DURUM_RAPORU.md), [Supabase kurulum paketi](../backend/supabase/README.md).
