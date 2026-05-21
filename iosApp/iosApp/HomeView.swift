import SwiftUI

struct HomeView: View {
    @EnvironmentObject var viewModel: FrostRiskViewModel

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    riskGaugeSection
                    weatherDetailsSection
                    actionSection
                }
                .padding()
            }
            .navigationTitle("SzronAlert")
            .navigationBarTitleDisplayMode(.large)
            .refreshable { await viewModel.refresh() }
            .task { await viewModel.refresh() }
            .overlay {
                if viewModel.isLoading {
                    ProgressView("Pobieranie danych…")
                        .padding()
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // MARK: - Sections

    private var riskGaugeSection: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .stroke(riskColor.opacity(0.2), lineWidth: 16)
                Circle()
                    .trim(from: 0, to: CGFloat(viewModel.frostProbability) / 100)
                    .stroke(riskColor, style: StrokeStyle(lineWidth: 16, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 0.8), value: viewModel.frostProbability)

                VStack(spacing: 4) {
                    Text("\(viewModel.frostProbability)%")
                        .font(.system(size: 48, weight: .bold, design: .rounded))
                        .foregroundStyle(riskColor)
                    Text("ryzyko szronu")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 200, height: 200)
            .padding(.top, 8)

            Text(viewModel.probabilityLevel.label)
                .font(.title2.weight(.semibold))
                .foregroundStyle(riskColor)

            if viewModel.hasRisk {
                Label("Przygotuj się na szron!", systemImage: "exclamationmark.triangle.fill")
                    .foregroundStyle(.orange)
                    .font(.callout.weight(.medium))
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.background.secondary, in: RoundedRectangle(cornerRadius: 16))
    }

    private var weatherDetailsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Aktualne warunki")
                .font(.headline)

            HStack(spacing: 16) {
                weatherTile(icon: "thermometer.medium", label: "Temperatura", value: viewModel.formattedTemp)
                weatherTile(icon: "humidity", label: "Wilgotność", value: "\(Int(viewModel.currentHumidity))%")
                weatherTile(icon: "wind", label: "Wiatr", value: "\(Int(viewModel.currentWindSpeed)) km/h")
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.background.secondary, in: RoundedRectangle(cornerRadius: 16))
    }

    private var actionSection: some View {
        VStack(spacing: 12) {
            if let error = viewModel.errorMessage {
                Label(error, systemImage: "xmark.circle")
                    .foregroundStyle(.red)
                    .font(.callout)
                    .multilineTextAlignment(.center)
            }

            Button {
                Task { await viewModel.refresh() }
            } label: {
                Label("Odśwież", systemImage: "arrow.clockwise")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(.cyan)
            .disabled(viewModel.isLoading)
        }
    }

    // MARK: - Helpers

    private var riskColor: Color {
        switch viewModel.frostProbability {
        case 80...: return .red
        case 60..<80: return .orange
        case 40..<60: return .yellow
        case 20..<40: return .cyan
        default: return .green
        }
    }

    private func weatherTile(icon: String, label: String, value: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(.cyan)
            Text(value)
                .font(.callout.weight(.semibold))
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}
