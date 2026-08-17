package net.blockhost.trestle.domain

sealed class LauncherException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(message: String, cause: Throwable? = null) : LauncherException(message, cause)
    class InvalidMetadata(message: String, cause: Throwable? = null) : LauncherException(message, cause)
    class UnsupportedRule(message: String) : LauncherException(message)
    class ChecksumMismatch(val artifact: String, val expected: String, val actual: String) :
        LauncherException("Checksum validation failed for $artifact.")

    class FileSystem(message: String, cause: Throwable? = null) : LauncherException(message, cause)
    class AuthenticationRequired : LauncherException("Sign in with a Microsoft account before launch.")
    class UnsupportedLoader(loader: ModLoader) :
        LauncherException("${loader.label} installation is not available in this milestone.")

    class RuntimeUnavailable(message: String) : LauncherException(message)
}
