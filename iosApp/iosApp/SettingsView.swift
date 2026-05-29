import SwiftUI

struct SettingsView: View {
    // Frost thresholds
    @AppStorage("tempThreshold") private var tempThreshold: Double = 1.0
    @AppStorage("humidityThreshold") private var humidityThreshold: Double = 80.0
    @AppStorage("precipitationThreshold") private var precipitationThreshold: Double = 1.0
    @AppStorage("sensitivity") private var sensitivity: Double = 1.0

    // App mode: 0 = auto/car, 1 = garden
    @AppStorage("appMode") private var appMode: Int = 0

    // Display
    @AppStorage("useFahrenheit") private var useFahrenheit: Bool = false

    // Subscription
    @StateObject private var subscriptionService = StoreKitSubscriptionService()

    var body: some View {
        NavigationStack {
            Form {
                thresholdsSection
                modeSection
                displaySection
                subscriptionSection
                aboutSection
            }
            .navigationTitle("Ustawienia")
        }
    }

    // MARK: - Sections

    private var thresholdsSection: some View {
        Section("Progi alertów") {
            VStack(alignment: .leading, spacing: 4) {
                Text("Próg temperatury: \(String(format: "%.1f°C", tempThreshold))")
                Slider(value: $tempThreshold, in: -5...5, step: 0.5)
                    .tint(.cyan)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("Minimalna wilgotność: \(Int(humidityThreshold))%")
                Slider(value: $humidityThreshold, in: 50...100, step: 5)
                    .tint(.cyan)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("Czułość: \(String(format: "%.1f×", sensitivity))")
                Slider(value: $sensitivity, in: 0.5...2.0, step: 0.1)
                    .tint(.cyan)
            }
        }
    }

    private var modeSection: some View {
        Section("Tryb aplikacji") {
            Picker("Tryb", selection: $appMode) {
                Text("Samochód / Auto").tag(0)
                Text("Ogród").tag(1)
            }
            .pickerStyle(.segmented)
        }
    }

    private var displaySection: some View {
        Section("Wyświetlanie") {
            Toggle("Temperatura w °F", isOn: $useFahrenheit)
        }
    }

    private var subscriptionSection: some View {
        Section("SzronAlert PRO") {
            if subscriptionService.isPro {
                Label("Aktywna subskrypcja PRO", systemImage: "checkmark.seal.fill")
                    .foregroundStyle(.green)
            } else {
                ForEach(subscriptionService.products, id: \.id) { product in
                    Button {
                        Task { await subscriptionService.purchase(product) }
                    } label: {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(product.displayName)
                                    .foregroundStyle(.primary)
                                Text(product.description)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text(product.displayPrice)
                                .foregroundStyle(.cyan)
                                .fontWeight(.semibold)
                        }
                    }
                }

                Button("Przywróć zakupy") {
                    Task { await subscriptionService.restore() }
                }
                .foregroundStyle(.secondary)
            }
        }
    }

    private var aboutSection: some View {
        Section("O aplikacji") {
            LabeledContent("Wersja", value: "1.0.0 (iOS MVP)")
            LabeledContent("Algorytm", value: "FrostCore KMP")
            Link("Polityka prywatności",
                 destination: URL(string: "https://okijeziorek.github.io/szronalert/privacy")!)
        }
    }
}
