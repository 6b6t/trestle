package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.category_adventure
import net.blockhost.trestle.resources.category_atmosphere
import net.blockhost.trestle.resources.category_audio
import net.blockhost.trestle.resources.category_blocks
import net.blockhost.trestle.resources.category_bloom
import net.blockhost.trestle.resources.category_cartoon
import net.blockhost.trestle.resources.category_challenging
import net.blockhost.trestle.resources.category_colored_lighting
import net.blockhost.trestle.resources.category_combat
import net.blockhost.trestle.resources.category_core_shaders
import net.blockhost.trestle.resources.category_cursed
import net.blockhost.trestle.resources.category_decoration
import net.blockhost.trestle.resources.category_economy
import net.blockhost.trestle.resources.category_entities
import net.blockhost.trestle.resources.category_environment
import net.blockhost.trestle.resources.category_equipment
import net.blockhost.trestle.resources.category_fantasy
import net.blockhost.trestle.resources.category_foliage
import net.blockhost.trestle.resources.category_fonts
import net.blockhost.trestle.resources.category_food
import net.blockhost.trestle.resources.category_game_mechanics
import net.blockhost.trestle.resources.category_gui
import net.blockhost.trestle.resources.category_high
import net.blockhost.trestle.resources.category_items
import net.blockhost.trestle.resources.category_kitchen_sink
import net.blockhost.trestle.resources.category_library
import net.blockhost.trestle.resources.category_lightweight
import net.blockhost.trestle.resources.category_locale
import net.blockhost.trestle.resources.category_low
import net.blockhost.trestle.resources.category_magic
import net.blockhost.trestle.resources.category_management
import net.blockhost.trestle.resources.category_medium
import net.blockhost.trestle.resources.category_minigame
import net.blockhost.trestle.resources.category_mobs
import net.blockhost.trestle.resources.category_modded
import net.blockhost.trestle.resources.category_models
import net.blockhost.trestle.resources.category_multiplayer
import net.blockhost.trestle.resources.category_optimization
import net.blockhost.trestle.resources.category_path_tracing
import net.blockhost.trestle.resources.category_pbr
import net.blockhost.trestle.resources.category_potato
import net.blockhost.trestle.resources.category_quests
import net.blockhost.trestle.resources.category_realistic
import net.blockhost.trestle.resources.category_reflections
import net.blockhost.trestle.resources.category_screenshot
import net.blockhost.trestle.resources.category_semi_realistic
import net.blockhost.trestle.resources.category_shadows
import net.blockhost.trestle.resources.category_simplistic
import net.blockhost.trestle.resources.category_social
import net.blockhost.trestle.resources.category_storage
import net.blockhost.trestle.resources.category_technology
import net.blockhost.trestle.resources.category_themed
import net.blockhost.trestle.resources.category_transportation
import net.blockhost.trestle.resources.category_tweaks
import net.blockhost.trestle.resources.category_utility
import net.blockhost.trestle.resources.category_vanilla_like
import net.blockhost.trestle.resources.category_worldgen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Modrinth's category tags for each content type, in the order Modrinth shows them. The category filter offers these
 * because Modrinth matches the tag name, which players should not have to type in English.
 */
internal fun resourceCategories(type: ResourceType): List<String> = when (type) {
    ResourceType.MOD -> modCategories
    ResourceType.MODPACK -> modpackCategories
    ResourceType.RESOURCE_PACK -> resourcePackCategories
    ResourceType.SHADER_PACK -> shaderCategories
}

/** The translated name of a category tag. Tags Trestle does not know, such as FTB pack tags, stay as they are. */
@Composable
internal fun categoryLabel(tag: String): String = categoryNames[tag.lowercase()]?.let { stringResource(it) } ?: tag

private val categoryNames: Map<String, StringResource> = mapOf(
    "adventure" to Res.string.category_adventure,
    "atmosphere" to Res.string.category_atmosphere,
    "audio" to Res.string.category_audio,
    "blocks" to Res.string.category_blocks,
    "bloom" to Res.string.category_bloom,
    "cartoon" to Res.string.category_cartoon,
    "challenging" to Res.string.category_challenging,
    "colored-lighting" to Res.string.category_colored_lighting,
    "combat" to Res.string.category_combat,
    "core-shaders" to Res.string.category_core_shaders,
    "cursed" to Res.string.category_cursed,
    "decoration" to Res.string.category_decoration,
    "economy" to Res.string.category_economy,
    "entities" to Res.string.category_entities,
    "environment" to Res.string.category_environment,
    "equipment" to Res.string.category_equipment,
    "fantasy" to Res.string.category_fantasy,
    "foliage" to Res.string.category_foliage,
    "fonts" to Res.string.category_fonts,
    "food" to Res.string.category_food,
    "game-mechanics" to Res.string.category_game_mechanics,
    "gui" to Res.string.category_gui,
    "high" to Res.string.category_high,
    "items" to Res.string.category_items,
    "kitchen-sink" to Res.string.category_kitchen_sink,
    "library" to Res.string.category_library,
    "lightweight" to Res.string.category_lightweight,
    "locale" to Res.string.category_locale,
    "low" to Res.string.category_low,
    "magic" to Res.string.category_magic,
    "management" to Res.string.category_management,
    "medium" to Res.string.category_medium,
    "minigame" to Res.string.category_minigame,
    "mobs" to Res.string.category_mobs,
    "modded" to Res.string.category_modded,
    "models" to Res.string.category_models,
    "multiplayer" to Res.string.category_multiplayer,
    "optimization" to Res.string.category_optimization,
    "path-tracing" to Res.string.category_path_tracing,
    "pbr" to Res.string.category_pbr,
    "potato" to Res.string.category_potato,
    "quests" to Res.string.category_quests,
    "realistic" to Res.string.category_realistic,
    "reflections" to Res.string.category_reflections,
    "screenshot" to Res.string.category_screenshot,
    "semi-realistic" to Res.string.category_semi_realistic,
    "shadows" to Res.string.category_shadows,
    "simplistic" to Res.string.category_simplistic,
    "social" to Res.string.category_social,
    "storage" to Res.string.category_storage,
    "technology" to Res.string.category_technology,
    "themed" to Res.string.category_themed,
    "transportation" to Res.string.category_transportation,
    "tweaks" to Res.string.category_tweaks,
    "utility" to Res.string.category_utility,
    "vanilla-like" to Res.string.category_vanilla_like,
    "worldgen" to Res.string.category_worldgen,
)

private val modCategories = listOf(
    "adventure",
    "cursed",
    "decoration",
    "economy",
    "equipment",
    "food",
    "game-mechanics",
    "library",
    "magic",
    "management",
    "minigame",
    "mobs",
    "optimization",
    "social",
    "storage",
    "technology",
    "transportation",
    "utility",
    "worldgen",
)

private val modpackCategories = listOf(
    "adventure",
    "challenging",
    "combat",
    "kitchen-sink",
    "lightweight",
    "magic",
    "multiplayer",
    "optimization",
    "quests",
    "technology",
)

private val resourcePackCategories = listOf(
    "audio",
    "blocks",
    "combat",
    "core-shaders",
    "cursed",
    "decoration",
    "entities",
    "environment",
    "equipment",
    "fonts",
    "gui",
    "items",
    "locale",
    "modded",
    "models",
    "realistic",
    "simplistic",
    "themed",
    "tweaks",
    "utility",
    "vanilla-like",
)

private val shaderCategories = listOf(
    "atmosphere",
    "bloom",
    "cartoon",
    "colored-lighting",
    "cursed",
    "fantasy",
    "foliage",
    "high",
    "low",
    "medium",
    "path-tracing",
    "pbr",
    "potato",
    "realistic",
    "reflections",
    "screenshot",
    "semi-realistic",
    "shadows",
    "vanilla-like",
)
