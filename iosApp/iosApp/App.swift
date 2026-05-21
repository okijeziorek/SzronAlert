import SwiftUI

@main
struct SzronAlertApp: App {
    @StateObject private var locationService = CoreLocationService()
    @StateObject private var notificationService = LocalNotificationService()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(locationService)
                .environmentObject(notificationService)
        }
    }
}

struct RootView: View {
    @EnvironmentObject var locationService: CoreLocationService
    @EnvironmentObject var notificationService: LocalNotificationService

    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Label("Szron", systemImage: "snowflake")
                }

            HistoryView()
                .tabItem {
                    Label("Historia", systemImage: "chart.line.uptrend.xyaxis")
                }

            SettingsView()
                .tabItem {
                    Label("Ustawienia", systemImage: "gearshape")
                }
        }
        .accentColor(.cyan)
        .task {
            await notificationService.requestPermission()
        }
    }
}
