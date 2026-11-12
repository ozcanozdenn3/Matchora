import SwiftUI
import ComposeApp
import UserNotifications

final class MatchoraAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UIWindow.appearance().overrideUserInterfaceStyle = .light
        UNUserNotificationCenter.current().delegate = self
        if DeviceEventBridge.shared.nativeServicesEnabled {
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
                if granted {
                    DispatchQueue.main.async { application.registerForRemoteNotifications() }
                }
            }
        }
        return true
    }
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        DeviceEventBridge.shared.updatePushToken(platform: "ios", token: deviceToken.map { String(format: "%02x", $0) }.joined())
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let data = response.notification.request.content.userInfo
        if let destination = data["destination"] as? String {
            DeviceEventBridge.shared.openNotification(destination: destination, targetId: data["targetId"] as? String ?? "")
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
                .onOpenURL { DeviceEventBridge.shared.openUrl(url: $0.absoluteString) }
        }
    }
}
