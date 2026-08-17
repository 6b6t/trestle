package net.blockhost.trestle.metadata

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import net.blockhost.trestle.domain.LauncherException

object MojangRuleEvaluator {
    fun allows(rules: List<MojangRule>, environment: PlatformEnvironment): Boolean {
        if (rules.isEmpty()) return true
        var allowed = false
        for (rule in rules) {
            if (matches(rule, environment)) allowed = rule.action == RuleAction.ALLOW
        }
        return allowed
    }

    private fun matches(rule: MojangRule, environment: PlatformEnvironment): Boolean {
        val os = rule.os
        if (os != null) {
            if (os.name != null && os.name != environment.operatingSystem.ruleName) return false
            if (os.arch != null && os.arch !in environment.architecture.aliases) return false
            if (os.version != null) {
                val matches = try {
                    Regex(os.version).containsMatchIn(environment.osVersion)
                } catch (error: IllegalArgumentException) {
                    throw LauncherException.UnsupportedRule("Invalid operating system rule: ${os.version}")
                }
                if (!matches) return false
            }
        }
        return rule.features.all { (name, required) -> (environment.features[name] ?: false) == required }
    }
}

data class MavenCoordinate(
    val group: String,
    val artifact: String,
    val version: String,
    val classifier: String? = null,
    val extension: String = "jar",
) {
    fun path(): String = buildString {
        append(group.replace('.', '/'))
        append('/')
        append(artifact)
        append('/')
        append(version)
        append('/')
        append(artifact)
        append('-')
        append(version)
        classifier?.let {
            append('-')
            append(it)
        }
        append('.')
        append(extension)
    }

    companion object {
        fun parse(value: String): MavenCoordinate {
            val coordinateAndExtension = value.split('@', limit = 2)
            val parts = coordinateAndExtension[0].split(':')
            if (parts.size !in 3..4 || parts.any { it.isBlank() }) {
                throw LauncherException.InvalidMetadata("Invalid Maven coordinate: $value")
            }
            return MavenCoordinate(
                group = parts[0],
                artifact = parts[1],
                version = parts[2],
                classifier = parts.getOrNull(3),
                extension = coordinateAndExtension.getOrNull(1) ?: "jar",
            )
        }
    }
}

object MojangArguments {
    fun resolve(
        values: List<kotlinx.serialization.json.JsonElement>,
        environment: PlatformEnvironment,
    ): List<String> = buildList {
        for (value in values) {
            when (value) {
                is JsonPrimitive -> value.contentOrNull?.let(::add)
                is JsonObject -> {
                    val rules = value["rules"]?.let(::decodeRules).orEmpty()
                    if (MojangRuleEvaluator.allows(rules, environment)) {
                        when (val argument = value["value"]) {
                            is JsonPrimitive -> argument.contentOrNull?.let(::add)
                            is JsonArray -> argument.forEach { item ->
                                (item as? JsonPrimitive)?.contentOrNull?.let(::add)
                                    ?: throw LauncherException.InvalidMetadata("Argument array contains a non-string value.")
                            }
                            else -> throw LauncherException.InvalidMetadata("Argument object has no string value.")
                        }
                    }
                }
                else -> throw LauncherException.InvalidMetadata("Unsupported argument value in version metadata.")
            }
        }
    }

    private fun decodeRules(element: kotlinx.serialization.json.JsonElement): List<MojangRule> {
        val array = element as? JsonArray
            ?: throw LauncherException.InvalidMetadata("Argument rules must be an array.")
        return array.map { item ->
            val rule = item as? JsonObject
                ?: throw LauncherException.InvalidMetadata("Argument rule must be an object.")
            val action = when ((rule["action"] as? JsonPrimitive)?.contentOrNull) {
                "allow" -> RuleAction.ALLOW
                "disallow" -> RuleAction.DISALLOW
                else -> throw LauncherException.UnsupportedRule("Argument rule has an unknown action.")
            }
            val os = (rule["os"] as? JsonObject)?.let { osObject ->
                RuleOs(
                    name = (osObject["name"] as? JsonPrimitive)?.contentOrNull,
                    arch = (osObject["arch"] as? JsonPrimitive)?.contentOrNull,
                    version = (osObject["version"] as? JsonPrimitive)?.contentOrNull,
                )
            }
            val features = (rule["features"] as? JsonObject).orEmpty().mapValues { (_, feature) ->
                (feature as? JsonPrimitive)?.booleanOrNull
                    ?: throw LauncherException.InvalidMetadata("Feature rule must contain a Boolean value.")
            }
            MojangRule(action, os, features)
        }
    }
}

fun parseLegacyArguments(value: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null
    var escaped = false
    for (character in value) {
        when {
            escaped -> {
                current.append(character)
                escaped = false
            }
            character == '\\' -> escaped = true
            quote != null && character == quote -> quote = null
            quote == null && (character == '\'' || character == '"') -> quote = character
            quote == null && character.isWhitespace() -> if (current.isNotEmpty()) {
                result += current.toString()
                current.clear()
            }
            else -> current.append(character)
        }
    }
    if (escaped || quote != null) throw LauncherException.InvalidMetadata("Legacy arguments contain an unfinished escape or quote.")
    if (current.isNotEmpty()) result += current.toString()
    return result
}
