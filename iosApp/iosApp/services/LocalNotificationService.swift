import UserNotifications

// MARK: - LocalNotificationService
// Implements the NotificationService concept from shared/platform/SystemServices.kt
// using UserNotifications framework (UNUserNotificationCenter).

@MainActor
final class LocalNotificationService: ObservableObject {

    private let center = UNUserNotificationCenter.current()

    // MARK: - Permission

    func requestPermission() async {
        _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
    }

    // MARK: - Scheduling

    /// Schedules a frost alert notification delivered after a short delay.
    /// If a notification with the same identifier already exists it is replaced.
    func scheduleFrostAlert(title: String, body: String) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.categoryIdentifier = "FROST_ALERT"

        // Deliver 10 seconds from now (adjust to specific evening hour in production).
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 10, repeats: false)
        let request = UNNotificationRequest(identifier: "frost_alert", content: content, trigger: trigger)

        try? await center.add(request)
    }

    /// Cancels all pending frost alert notifications.
    func cancelAllAlerts() {
        center.removePendingNotificationRequests(withIdentifiers: ["frost_alert"])
    }

    // MARK: - Morning brief notification

    /// Schedules a daily morning brief at the specified hour.
    func scheduleMorningBrief(hour: Int, title: String, body: String) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = 0

        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(identifier: "morning_brief", content: content, trigger: trigger)

        center.removePendingNotificationRequests(withIdentifiers: ["morning_brief"])
        try? await center.add(request)
    }
}
