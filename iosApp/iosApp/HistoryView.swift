import SwiftUI

// MARK: - History item stored in UserDefaults (placeholder until SwiftData/SQLite is added)

private enum StorageKey {
    static let historyRecords = "historyRecords"
}

struct HistoryRecord: Identifiable, Codable {
    let id: UUID
    let timestamp: Date
    let minTemp: Double
    let frostProbability: Int
    let hasRisk: Bool

    init(minTemp: Double, frostProbability: Int, hasRisk: Bool) {
        self.id = UUID()
        self.timestamp = Date()
        self.minTemp = minTemp
        self.frostProbability = frostProbability
        self.hasRisk = hasRisk
    }
}

// MARK: - HistoryView
// Displays a chronological list of frost check records.
// Post-MVP: migrate to SwiftData for persistent local storage.

struct HistoryView: View {
    @State private var records: [HistoryRecord] = []

    var body: some View {
        NavigationStack {
            Group {
                if records.isEmpty {
                    ContentUnavailableView(
                        "Brak historii",
                        systemImage: "snowflake",
                        description: Text("Dane pojawią się po pierwszym sprawdzeniu szronu.")
                    )
                } else {
                    List(records) { record in
                        HistoryRowView(record: record)
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("Historia")
            .onAppear { loadRecords() }
        }
    }

    private func loadRecords() {
        guard let data = UserDefaults.standard.data(forKey: StorageKey.historyRecords) else { return }
        do {
            records = try JSONDecoder().decode([HistoryRecord].self, from: data)
                .sorted { $0.timestamp > $1.timestamp }
        } catch {
            // Corrupted history data — reset to empty to avoid a stuck error state.
            // Post-MVP: migrate to SwiftData which handles schema migrations properly.
            print("[HistoryView] Failed to decode history records: \(error)")
            records = []
        }
    }
}

struct HistoryRowView: View {
    let record: HistoryRecord

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: record.hasRisk ? "snowflake" : "checkmark.circle")
                .foregroundStyle(record.hasRisk ? .cyan : .green)
                .font(.title2)

            VStack(alignment: .leading, spacing: 2) {
                Text(record.timestamp.formatted(date: .abbreviated, time: .shortened))
                    .font(.subheadline.weight(.medium))
                Text("Min: \(String(format: "%.1f°C", record.minTemp))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text("\(record.frostProbability)%")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(probabilityColor(record.frostProbability))
                Text("ryzyko")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    private func probabilityColor(_ p: Int) -> Color {
        switch p {
        case 80...: return .red
        case 60..<80: return .orange
        case 40..<60: return .yellow
        default: return .green
        }
    }
}
