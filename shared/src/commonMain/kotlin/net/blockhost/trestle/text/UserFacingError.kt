package net.blockhost.trestle.text

/** An error whose message is meant for the player. */
interface UserFacingError {
    val userText: UserText
}

class UserFacingArgumentException(override val userText: UserText, cause: Throwable? = null) :
    IllegalArgumentException(userText.debugText, cause),
    UserFacingError

class UserFacingStateException(override val userText: UserText, cause: Throwable? = null) :
    IllegalStateException(userText.debugText, cause),
    UserFacingError

/** Like [require], with a message for the player. */
inline fun requireUser(value: Boolean, message: () -> UserText) {
    if (!value) throw UserFacingArgumentException(message())
}

/** Like [requireNotNull], with a message for the player. */
inline fun <T : Any> requireUserNotNull(value: T?, message: () -> UserText): T = value ?: throw UserFacingArgumentException(message())

/** Like [check], with a message for the player. */
inline fun checkUser(value: Boolean, message: () -> UserText) {
    if (!value) throw UserFacingStateException(message())
}

/** Like [checkNotNull], with a message for the player. */
inline fun <T : Any> checkUserNotNull(value: T?, message: () -> UserText): T = value ?: throw UserFacingStateException(message())

/** Like [error], with a message for the player. */
fun userError(message: UserText): Nothing = throw UserFacingStateException(message)

/** The text to show for this failure: its player message when it has one, else its raw message, else [fallback]. */
fun Throwable.userText(fallback: UserText): UserText = when {
    this is UserFacingError -> userText
    else -> message?.takeIf(String::isNotBlank)?.let(UserText::Raw) ?: fallback
}
