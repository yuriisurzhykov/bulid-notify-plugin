package me.yuriisoft.buildnotify.mobile

import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS entry point consumed by the Xcode project's [ContentView].
 *
 * Usage in Swift:
 * ```swift
 * struct ContentView: View {
 *     var body: some View {
 *         ComposeView().ignoresSafeArea(.keyboard)
 *     }
 * }
 * class ComposeView: UIViewControllerRepresentable {
 *     func makeUIViewController(context: Context) -> UIViewController {
 *         MainViewControllerKt.MainViewController()
 *     }
 *     func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
 * }
 * ```
 */
fun MainViewController() = ComposeUIViewController { App() }
