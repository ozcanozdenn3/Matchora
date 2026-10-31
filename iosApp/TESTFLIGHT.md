# Matchora → TestFlight

- Bundle ID: `com.ozdentech.matchora`
- Apple Developer Team: `XV6VPKTQAS`
- Version: `1.1`, Build: `2` (Distribution-Info.plist)
- Xcode project: `iosApp/Matchora.xcodeproj`, scheme: `Matchora`

## IPA oluşturma

Xcode → Settings → Accounts içinde ilgili Apple Developer hesabı açık olmalı.

```sh
./iosApp/build_ipa.sh
```

Çıktılar:

- Arşiv: `iosApp/build/distribution/Matchora.xcarchive`
- App Store Connect için imzalı IPA: `iosApp/build/distribution/export/Matchora.ipa`

Betiğin imzalama veya dışa aktarma aşaması hata verirse IPA yüklemeye hazır değildir.
Geliştirme/yerel imzalı paketler TestFlight yerine geçmez.
Yeni bir build yüklerken Distribution-Info.plist içindeki CFBundleVersion artırılmalıdır.

## App Store Connect'te görünmesi

1. https://appstoreconnect.apple.com → Apps → + → New App.
2. Platform: iOS. Ad: Matchora. Dil: Turkish. Bundle ID: `com.ozdentech.matchora`.
3. SKU: `matchora-ios-001` (hesabında benzersiz bir iç referans).
4. Bundle ID listede yoksa https://developer.apple.com/account/resources/identifiers/list adresinde
   App IDs → App → Explicit Bundle ID ile `com.ozdentech.matchora` kaydının aynı takımda olduğunu kontrol et.
5. İmzalı IPA'yı Transporter ile yükle; alternatif olarak Xcode'da arşivi açıp
   Distribute App → App Store Connect → Upload yolunu kullan.
6. Apple işlemesi tamamlanınca Matchora → TestFlight → iOS Builds altında build görünür.
7. Gerekiyorsa export compliance sorularını uygulamanın gerçek şifreleme kullanımına göre yanıtla.
   Internal Testing grubuna build ve test kullanıcıları ekle.
   External Testing için beta incelemesi gerekebilir.

Uygulama kaydının görünmesi ile TestFlight build'inin görünmesi farklı adımlardır.
IPA'yı bilgisayarda oluşturmak otomatik olarak App Store Connect'e yüklemez.

Kaynaklar:
- https://developer.apple.com/help/app-store-connect/create-an-app-record/add-a-new-app/
- https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds
- https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases
