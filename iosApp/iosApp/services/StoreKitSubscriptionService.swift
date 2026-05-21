import StoreKit

// MARK: - SubscriptionProduct wrapper
// Mirrors shared/platform/SystemServices.kt SubscriptionProduct

struct AppSubscriptionProduct: Identifiable {
    let id: String
    let displayName: String
    let description: String
    let displayPrice: String
    let trialAvailable: Bool

    init(from product: Product) {
        self.id = product.id
        self.displayName = product.displayName
        self.description = product.description
        self.displayPrice = product.displayPrice
        self.trialAvailable = product.subscription?.introductoryOffer != nil
    }
}

// MARK: - StoreKitSubscriptionService
// Implements SubscriptionService concept from shared/platform/SystemServices.kt
// using StoreKit 2. Manages the monthly / annual / lifetime PRO products.
// Product IDs must match those registered in App Store Connect.

@MainActor
final class StoreKitSubscriptionService: ObservableObject {

    // MARK: - Product IDs (must match App Store Connect)

    private enum ProductID {
        static let monthly  = "pl.oki.frostalert.ios.pro.monthly"
        static let annual   = "pl.oki.frostalert.ios.pro.annual"
        static let lifetime = "pl.oki.frostalert.ios.pro.lifetime"
        static let all = [monthly, annual, lifetime]
    }

    // MARK: - Published state

    @Published var isPro: Bool = false
    @Published var products: [AppSubscriptionProduct] = []
    @Published var errorMessage: String?

    private var updateListenerTask: Task<Void, Error>?

    // MARK: - Init / deinit

    init() {
        updateListenerTask = listenForTransactions()
        Task { await loadProducts() }
        Task { await refreshEntitlements() }
    }

    deinit {
        updateListenerTask?.cancel()
    }

    // MARK: - Public API

    func purchase(_ product: AppSubscriptionProduct) async {
        guard let skProduct = try? await Product.products(for: [product.id]).first else { return }

        do {
            let result = try await skProduct.purchase()
            switch result {
            case .success(let verification):
                if case .verified(let transaction) = verification {
                    await transaction.finish()
                    await refreshEntitlements()
                }
            case .userCancelled, .pending:
                break
            @unknown default:
                break
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func restore() async {
        do {
            try await AppStore.sync()
            await refreshEntitlements()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    // MARK: - Private helpers

    private func loadProducts() async {
        do {
            let skProducts = try await Product.products(for: ProductID.all)
            products = skProducts
                .sorted { $0.price < $1.price }
                .map { AppSubscriptionProduct(from: $0) }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func refreshEntitlements() async {
        var entitlementActive = false
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result,
               ProductID.all.contains(transaction.productID),
               transaction.revocationDate == nil {
                entitlementActive = true
                break
            }
        }
        isPro = entitlementActive
    }

    private func listenForTransactions() -> Task<Void, Error> {
        Task.detached {
            for await result in Transaction.updates {
                if case .verified(let transaction) = result {
                    await transaction.finish()
                    await self.refreshEntitlements()
                }
            }
        }
    }
}
