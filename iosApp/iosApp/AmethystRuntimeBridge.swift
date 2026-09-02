import Foundation
import TrestleShared

final class AmethystRuntimeBridge: NSObject, IosRuntimeBridge {
    private let bundle = Bundle.main

    var availability: IosRuntimeAvailability {
        var reason: NSString?
        let available = TrestleNativeRuntime.isAvailable(reason: &reason)
        return IosRuntimeAvailability(
            available: available,
            reason: reason as String?,
            supportedJavaMajors: available ? Set([KotlinInt(int: 25)]) : Set()
        )
    }

    func runtime(javaMajor: Int32) -> IosRuntimeDescriptor? {
        guard javaMajor == 25, availability.available else { return nil }
        let javaHome = bundle.bundlePath + "/java_runtimes/java-25-openjdk"
        let nativeDirectory = bundle.bundlePath + "/Frameworks"
        let librariesDirectory = bundle.bundlePath + "/amethyst-libs"
        let libraries = (try? FileManager.default.contentsOfDirectory(atPath: librariesDirectory)) ?? []
        let classpath = libraries
            .filter { $0.hasSuffix(".jar") }
            .sorted()
            .map { librariesDirectory + "/" + $0 }
        return IosRuntimeDescriptor(
            javaHome: javaHome,
            nativeDirectory: nativeDirectory,
            classpathEntries: classpath
        )
    }

    func launch(request: IosJvmLaunchRequest, observer: IosJvmLaunchObserver) {
        TrestleNativeRuntime.launch(
            arguments: request.arguments,
            workingDirectory: request.workingDirectory,
            environment: request.environment,
            started: { observer.onStarted() },
            output: { observer.onOutput(line: $0) },
            completion: { exitCode, message in
                if let message {
                    observer.onFailed(message: message)
                } else {
                    observer.onExited(exitCode: exitCode)
                }
            }
        )
    }

    func cancel() {
        TrestleNativeRuntime.cancel()
    }
}
