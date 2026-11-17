package net.blockhost.trestle.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Text the player reads. Messages are kept as a resource and its arguments until they are drawn,
 * so they follow the interface language. [Raw] holds text that is already final, such as a file
 * name, a server message, or a message stored on disk.
 */
sealed interface UserText {
    data class Raw(val value: String) : UserText

    data class Resource(val resource: StringResource, val args: List<Any> = emptyList()) : UserText

    data class Plural(val resource: PluralStringResource, val quantity: Int, val args: List<Any> = emptyList()) : UserText

    /** A short form for logs and exception messages, which are not translated. */
    val debugText: String
        get() = when (this) {
            is Raw -> value
            is Resource -> if (args.isEmpty()) resource.key else "${resource.key}(${args.joinToString()})"
            is Plural -> "${resource.key}[$quantity](${args.joinToString()})"
        }
}

/** A resource message. Arguments may themselves be [UserText]; they are resolved first. */
fun text(resource: StringResource, vararg args: Any): UserText = UserText.Resource(resource, args.toList())

fun pluralText(resource: PluralStringResource, quantity: Int, vararg args: Any): UserText =
    UserText.Plural(resource, quantity, args.toList())

fun String.asUserText(): UserText = UserText.Raw(this)

@Composable
fun UserText.resolve(): String = when (this) {
    is UserText.Raw -> value
    is UserText.Resource -> stringResource(resource, *args.map { it.resolveArgument() }.toTypedArray())
    is UserText.Plural -> pluralStringResource(resource, quantity, *args.map { it.resolveArgument() }.toTypedArray())
}

@Composable
private fun Any.resolveArgument(): Any = if (this is UserText) resolve() else this

/** Resolves text outside composition, for example for the clipboard or a system notification. */
suspend fun UserText.load(): String = when (this) {
    is UserText.Raw -> value
    is UserText.Resource -> getString(resource, *args.map { it.loadArgument() }.toTypedArray())
    is UserText.Plural -> getPluralString(resource, quantity, *args.map { it.loadArgument() }.toTypedArray())
}

private suspend fun Any.loadArgument(): Any = if (this is UserText) load() else this
