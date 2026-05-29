import Foundation

// MARK: - Swift-side weather models mirroring shared/WeatherModels.kt
// Used for decoding the Open-Meteo JSON response via URLSession.

struct WeatherResponse: Codable {
    let current: CurrentWeather
    let hourly: HourlyForecast
    let daily: DailyForecast?
}

struct CurrentWeather: Codable {
    let temperature: Double
    let humidity: Double
    let precipitation: Double
    let weatherCode: Int
    let windSpeed: Double
    let uvIndex: Double

    enum CodingKeys: String, CodingKey {
        case temperature = "temperature_2m"
        case humidity = "relative_humidity_2m"
        case precipitation
        case weatherCode = "weather_code"
        case windSpeed = "wind_speed_10m"
        case uvIndex = "uv_index"
    }
}

struct HourlyForecast: Codable {
    let time: [String]
    let temperature: [Double?]
    let humidity: [Double?]
    let precipitation: [Double?]
    let weatherCode: [Int?]
    let windSpeed: [Double?]

    enum CodingKeys: String, CodingKey {
        case time
        case temperature = "temperature_2m"
        case humidity = "relative_humidity_2m"
        case precipitation
        case weatherCode = "weather_code"
        case windSpeed = "wind_speed_10m"
    }
}

struct DailyForecast: Codable {
    let time: [String]
    let temperatureMin: [Double?]
    let temperatureMax: [Double?]
    let weatherCode: [Int?]

    enum CodingKeys: String, CodingKey {
        case time
        case temperatureMin = "temperature_2m_min"
        case temperatureMax = "temperature_2m_max"
        case weatherCode = "weather_code"
    }
}

// MARK: - Frost probability level (mirrors FrostCore.ProbabilityLevel)

enum FrostProbabilityLevel {
    case veryHigh, high, moderate, low, minimal

    var label: String {
        switch self {
        case .veryHigh: return "Bardzo wysokie"
        case .high:     return "Wysokie"
        case .moderate: return "Umiarkowane"
        case .low:      return "Niskie"
        case .minimal:  return "Minimalne"
        }
    }
}
