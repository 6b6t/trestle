package net.blockhost.trestle.domain

import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ui_checksum_validation_failed_for
import net.blockhost.trestle.resources.ui_loader_installation_not_available
import net.blockhost.trestle.resources.ui_sign_in_before_launch
import net.blockhost.trestle.text.UserFacingError
import net.blockhost.trestle.text.UserText
import net.blockhost.trestle.text.text

/** Launcher failures. Each carries the message the player sees; [message] is a short form for logs. */
sealed class LauncherException(override val userText: UserText, cause: Throwable? = null) :
    Exception(userText.debugText, cause),
    UserFacingError {
    class Network(text: UserText, cause: Throwable? = null) : LauncherException(text, cause)
    class InvalidMetadata(text: UserText, cause: Throwable? = null) : LauncherException(text, cause)
    class UnsupportedRule(text: UserText) : LauncherException(text)
    class ChecksumMismatch(val artifact: String, val expected: String, val actual: String) :
        LauncherException(text(Res.string.ui_checksum_validation_failed_for, artifact))

    class FileSystem(text: UserText, cause: Throwable? = null) : LauncherException(text, cause)
    class AuthenticationRequired : LauncherException(text(Res.string.ui_sign_in_before_launch))
    class UnsupportedLoader(loader: ModLoader) :
        LauncherException(text(Res.string.ui_loader_installation_not_available, loader.label))

    class RuntimeUnavailable(text: UserText) : LauncherException(text)
}
