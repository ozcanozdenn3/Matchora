import SwiftUI
import ComposeApp
import UserNotifications
import FirebaseCore
import FirebaseAuth

final class IosPhoneAuthDelegate: NSObject, NativePhoneAuthDelegate {
    func startVerification(phoneNumber: String, callback: PhoneVerificationCallback) {
        DispatchQueue.main.async {
            PhoneAuthProvider.provider().verifyPhoneNumber(phoneNumber, uiDelegate: nil) { verificationId, error in
                DispatchQueue.main.async {
                    if let error = error {
                        callback.onError(error: error.localizedDescription)
                        return
                    }
                    if let verificationId = verificationId {
                        callback.onCodeSent(verificationId: verificationId)
                    } else {
                        callback.onError(error: "Doğrulama kodu oluşturulamadı.")
                    }
                }
            }
        }
    }

    func verifyCode(verificationId: String, code: String, callback: PhoneAuthCallback) {
        let credential = PhoneAuthProvider.provider().credential(withVerificationID: verificationId, verificationCode: code)
        Auth.auth().signIn(with: credential) { authResult, error in
            if let error = error {
                DispatchQueue.main.async { callback.onError(error: error.localizedDescription) }
                return
            }
            guard let user = authResult?.user else {
                DispatchQueue.main.async { callback.onError(error: "Kullanıcı oturumu açılamadı.") }
                return
            }
            user.getIDTokenForcingRefresh(false) { idToken, tokenError in
                DispatchQueue.main.async {
                    if let tokenError = tokenError {
                        callback.onError(error: tokenError.localizedDescription)
                        return
                    }
                    guard let idToken = idToken else {
                        callback.onError(error: "Kimlik belirteci alınamadı.")
                        return
                    }
                    callback.onSuccess(idToken: idToken, phoneNumber: user.phoneNumber ?? "")
                }
            }
        }
    }
}

final class MatchoraAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private let phoneAuthDelegate = IosPhoneAuthDelegate()

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UIWindow.appearance().overrideUserInterfaceStyle = .light
        UNUserNotificationCenter.current().delegate = self

        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        DeviceEventBridge.shared.phoneAuthDelegate = phoneAuthDelegate

        if DeviceEventBridge.shared.nativeServicesEnabled {
            DispatchQueue.main.async { application.registerForRemoteNotifications() }
        }
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)
        DeviceEventBridge.shared.updatePushToken(platform: "ios", token: deviceToken.map { String(format: "%02x", $0) }.joined())
    }

    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        completionHandler(.noData)
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let data = response.notification.request.content.userInfo
        if let destination = data["destination"] as? String {
            DeviceEventBridge.shared.openPushNotification(
                destination: destination,
                targetId: data["targetId"] as? String ?? "",
                notificationId: data["notificationId"] as? String ?? ""
            )
        }
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(MatchoraAppDelegate.self) var appDelegate
    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.light)
                .ignoresSafeArea(.all)
                .onOpenURL { url in
                    if Auth.auth().canHandle(url) {
                        return
                    }
                    DeviceEventBridge.shared.openUrl(url: url.absoluteString)
                }
        }
    }
}
