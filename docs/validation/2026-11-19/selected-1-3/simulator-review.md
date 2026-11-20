# Simülatör açılış kontrolü

- Son Swift/iOS paketi iPhone 17 Pro / iOS 26.5 üzerinde kuruldu ve başlatıldı.
- Açılışta Spor radarı, harita, ana gezinme ve sol üst pusula görüntülendi; sunucu erişim hatası penceresi görülmedi.
- Yerel API sağlık kontrolü HTTP 200 döndü. Yeni işlem kaydı yolu girişsiz isteği HTTP 401 ile reddetti.
- Görüntü, giriş yapılmamış gerçek backend görünümüdür; bu şehir/ağ için sıfır ilan gösterir. Bu çalışmada mock veri dosyası değiştirilmedi veya silinmedi.
- Bu görüntü yeni kuyruk ve ilan kurtarma pencerelerinin elle uçtan uca testi değildir. Bu akışların ortak mantığı 109 KMP testi ve ilgili sunucu/HTTP/PostgreSQL testleriyle doğrulandı; iki fiziksel cihazda elle test kalır.
