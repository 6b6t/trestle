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
    is UserText.Resource -> formattedString(resource, *args.map { it.resolveArgument() }.toTypedArray())
    is UserText.Plural -> formattedPluralString(resource, quantity, *args.map { it.resolveArgument() }.toTypedArray())
}

@Composable
private fun Any.resolveArgument(): Any = if (this is UserText) resolve() else this

/**
 * Resolves text outside composition, for example for a system notification. [isolateArguments] is false for text
 * that is stored or printed to a terminal, where the invisible isolate characters do not belong.
 */
suspend fun UserText.load(isolateArguments: Boolean = true): String {
    if (this is UserText.Raw) return value
    val loaded = when (this) {
        is UserText.Resource -> args
        is UserText.Plural -> args
        is UserText.Raw -> emptyList()
    }.map { it.loadArgument(isolateArguments) }
    val formatArgs = if (isolateArguments) isolated(loaded) else loaded.toTypedArray()
    return when (this) {
        is UserText.Resource -> getString(resource, *formatArgs)
        is UserText.Plural -> getPluralString(resource, quantity, *formatArgs)
        is UserText.Raw -> value
    }
}

private suspend fun Any.loadArgument(isolateArguments: Boolean): Any = if (this is UserText) load(isolateArguments) else this

/** A string resource with its text arguments isolated, so names in another script cannot reorder the sentence. */
@Composable
fun formattedString(resource: StringResource, vararg args: Any): String = stringResource(resource, *isolated(args.asList()))

/** A plural resource with its text arguments isolated. See [formattedString]. */
@Composable
fun formattedPluralString(resource: PluralStringResource, quantity: Int, vararg args: Any): String =
    pluralStringResource(resource, quantity, *isolated(args.asList()))

/**
 * Wraps text arguments in Unicode first-strong isolates (U+2068 to U+2069). A Hebrew instance name inside an English
 * sentence then keeps its own direction without making the whole sentence right to left, and an English name inside
 * an Arabic sentence cannot pull the surrounding punctuation along. Numbers need no isolation.
 */
private fun isolated(args: List<Any>): Array<Any> =
    args.map { if (it is String) "$FIRST_STRONG_ISOLATE$it$POP_DIRECTIONAL_ISOLATE" else it }.toTypedArray()

private const val FIRST_STRONG_ISOLATE = '\u2068'
private const val POP_DIRECTIONAL_ISOLATE = '\u2069'
