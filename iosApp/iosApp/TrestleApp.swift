import SwiftUI
import TrestleShared

@main
struct TrestleIosApp: App {
    var body: some Scene {
        WindowGroup {
            TrestleRootView()
                .ignoresSafeArea(.keyboard)
        }
    }
}

private struct TrestleRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.TrestleViewController(runtimeBridge: AmethystRuntimeBridge())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
