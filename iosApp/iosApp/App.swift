import SwiftUI

// MARK: - AppContainer
// Owns all services and the shared ViewModel so they are instantiated once
// and their lifetimes are tied to the App, not to individual Views.

final class AppContainer: ObservableObject {
    let locationService = CoreLocationService()
    let notificationService = LocalNotificationService()
    let storeKitService = StoreKitSubscriptionService()
    // ViewModel is created lazily so it can reference the already-created services.
    lazy var viewModel = FrostRiskViewModel(
        locationService: locationService,
        notificationService: notificationService
    )
}

@main
struct SzronAlertApp: App {
    @StateObject private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(container.locationService)
                .environmentObject(container.notificationService)
                .environmentObject(container.storeKitService)
                .environmentObject(container.viewModel)
        }
    }
}

struct RootView: View {
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
