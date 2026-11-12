# Matchora

Backend kurulumu: [API README](backend/README.md). Uygulama genelindeki bağlantılar ve yayın öncesi açıklar: [Backend durum raporu](docs/BACKEND_DURUM_RAPORU.md). Her ekran ve alt pencere için: [Tam backend envanteri](docs/TUM_EKRANLAR_BACKEND_ENVANTERI.md).

Son işlev düzeltmeleri: [ilan/kadro ve uygulama yönetimi](docs/ISLEV_VE_KADRO_DUZELTMELERI_2026-11-07.md).

Önceki uygulama ve doğrulama: [6 Kasım özellik tamamlama raporu](docs/OZELLIK_TAMAMLAMA_2026-11-06.md).

Kotlin Multiplatform + Compose Multiplatform ile gelistirilen, gercek spor baglantilarini kolaylastiran mobil uygulama temeli.

## Ilk ekran

Klarna'nin sade dashboard yaklasimindan ilham alan ana ekran:

- Play Now ana aksiyonu
- Yakindaki spor firsatlari
- Haftalik dogrulanmis spor hedefi
- Ana sayfa, Kesfet, Sohbet ve Profil navigasyonu

## Teknoloji

- Kotlin Multiplatform
- Compose Multiplatform
- Android ve iOS hedefleri
- Gradle Version Catalog

## Calistirma

Projeyi Android Studio ile `Desktop/Matchora` klasorunden ac. Gradle sync sonrasinda `composeApp` Android konfigurasyonunu calistirabilirsin.

iOS icin `MainViewController()` fonksiyonu `iosApp` Xcode host uygulamasina baglanmaya hazir birakildi.
