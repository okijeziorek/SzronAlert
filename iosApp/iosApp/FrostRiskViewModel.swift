import Foundation
import shared // KMP framework

// MARK: - Settings keys

private enum SettingsKey {
    static let tempThreshold = "tempThreshold"
    static let humidityThreshold = "humidityThreshold"
    static let precipitationThreshold = "precipitationThreshold"
    static let sensitivity = "sensitivity"
    static let appMode = "appMode"
    static let useFahrenheit = "useFahrenheit"
}

// MARK: - FrostRiskViewModel
// Consumes FrostCore from the shared KMP framework and coordinates
// weather fetching, location, and notification delivery.

@MainActor
final class FrostRiskViewModel: ObservableObject {

    // MARK: Published state

    @Published var frostProbability: Int = 0
    @Published var hasRisk: Bool = false
    @Published var currentTemp: Double = 0
    @Published var currentHumidity: Double = 0
    @Published var currentWindSpeed: Double = 0
    @Published var nightMinTemp: Double = 0
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var locationName: String = "Moja lokalizacja"

    // MARK: Dependencies

    private let locationService: CoreLocationService
    private let notificationService: LocalNotificationService

    // MARK: Settings (backed by UserDefaults, mirrors Android SettingsDataStore)
    // Use UserDefaults.object to distinguish "not set" from a legitimate 0.0 value.

    private func getSetting(_ key: String, default defaultValue: Double) -> Double {
        UserDefaults.standard.object(forKey: key) != nil
            ? UserDefaults.standard.double(forKey: key)
            : defaultValue
    }

    var tempThreshold: Double { getSetting(SettingsKey.tempThreshold, default: 1.0) }
    var humidityThreshold: Double { getSetting(SettingsKey.humidityThreshold, default: 80.0) }
    var precipitationThreshold: Double { getSetting(SettingsKey.precipitationThreshold, default: 1.0) }
    var sensitivity: Double { getSetting(SettingsKey.sensitivity, default: 1.0) }
    var appMode: Int {
        UserDefaults.standard.integer(forKey: SettingsKey.appMode)
    }
    var useFahrenheit: Bool {
        UserDefaults.standard.bool(forKey: SettingsKey.useFahrenheit)
    }

    var probabilityLevel: FrostProbabilityLevel {
        switch frostProbability {
        case 80...: return .veryHigh
        case 60..<80: return .high
        case 40..<60: return .moderate
        case 20..<40: return .low
        default: return .minimal
        }
    }

    var formattedTemp: String {
        formatTemp(currentTemp)
    }

    // MARK: Init

    init(locationService: CoreLocationService, notificationService: LocalNotificationService) {
        self.locationService = locationService
        self.notificationService = notificationService
    }

    // MARK: Public API

    func refresh() async {
        isLoading = true
        errorMessage = nil

        guard let location = await locationService.requestLocation() else {
            errorMessage = "Nie można pobrać lokalizacji. Sprawdź uprawnienia."
            isLoading = false
            return
        }

        do {
            let weather = try await fetchWeather(latitude: location.latitude, longitude: location.longitude)
            applyWeather(weather)
        } catch {
            errorMessage = error.localizedDescription
        }

        isLoading = false
    }

    // MARK: Private helpers

    private func applyWeather(_ weather: WeatherResponse) {
        let c = weather.current

        // Core calculations via KMP FrostCore – same algorithm as Android.
        let probability = Int(FrostCore.shared.calculateFrostProbability(
            temp: c.temperature,
            humidity: c.humidity,
            precip: c.precipitation,
            weatherCode: Int32(c.weatherCode),
            tempThreshold: tempThreshold,
            humidityThreshold: humidityThreshold,
            precipitationThreshold: precipitationThreshold,
            sensitivity: sensitivity,
            windSpeed: c.windSpeed,
            appMode: Int32(appMode)
        ))

        let risk = FrostCore.shared.hasFrostRisk(
            temp: c.temperature,
            humidity: c.humidity,
            precip: c.precipitation,
            weatherCode: Int32(c.weatherCode),
            tempThreshold: tempThreshold,
            humidityThreshold: humidityThreshold,
            precipitationThreshold: precipitationThreshold,
            sensitivity: sensitivity,
            windSpeed: c.windSpeed,
            appMode: Int32(appMode)
        )

        frostProbability = probability
        hasRisk = risk
        currentTemp = c.temperature
        currentHumidity = c.humidity
        currentWindSpeed = c.windSpeed
        // Prefix 12 = the 12-hour overnight window (matching Android's 20:00–08:00
        // nighttime check interval defined in SettingsDataStore.alertStartHour).
        nightMinTemp = weather.hourly.temperature.prefix(12).compactMap { $0 }.min() ?? c.temperature

        if risk {
            Task {
                await notificationService.scheduleFrostAlert(
                    title: "⚠️ Ryzyko szronu",
                    body: "Temperatura: \(formatTemp(c.temperature)) – przygotuj się!"
                )
            }
        }
    }

    private func fetchWeather(latitude: Double, longitude: Double) async throws -> WeatherResponse {
        // Uses OpenMeteoRequestBuilder from shared for the URL template.
        let urlStr = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=\(latitude)&longitude=\(longitude)" +
            "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,uv_index" +
            "&hourly=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m" +
            "&daily=temperature_2m_min,temperature_2m_max,weather_code" +
            "&forecast_days=7&timezone=auto"
        guard let url = URL(string: urlStr) else { throw URLError(.badURL) }
        let (data, _) = try await URLSession.shared.data(from: url)
        return try JSONDecoder().decode(WeatherResponse.self, from: data)
    }

    private func formatTemp(_ celsius: Double) -> String {
        if useFahrenheit {
            let f = celsius * 9 / 5 + 32
            return String(format: "%.1f°F", f)
        }
        return String(format: "%.1f°C", celsius)
    }
}
