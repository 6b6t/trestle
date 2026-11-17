package net.blockhost.trestle.text

import net.blockhost.trestle.app.InstanceSortMode
import net.blockhost.trestle.app.LauncherProxyType
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.instance.MinecraftNarratorMode
import net.blockhost.trestle.instance.MinecraftParticleSetting
import net.blockhost.trestle.resources.ReleaseChannel
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceSearchSort
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.label_auth_bedrock_credentials
import net.blockhost.trestle.resources.label_auth_bedrock_device_code
import net.blockhost.trestle.resources.label_auth_java_access_token
import net.blockhost.trestle.resources.label_auth_java_cookies
import net.blockhost.trestle.resources.label_auth_java_credentials
import net.blockhost.trestle.resources.label_auth_java_device_code
import net.blockhost.trestle.resources.label_auth_java_refresh_token
import net.blockhost.trestle.resources.label_auth_offline
import net.blockhost.trestle.resources.label_channel_alpha
import net.blockhost.trestle.resources.label_channel_beta
import net.blockhost.trestle.resources.label_channel_release
import net.blockhost.trestle.resources.label_channel_unknown
import net.blockhost.trestle.resources.label_narrator_all
import net.blockhost.trestle.resources.label_narrator_chat
import net.blockhost.trestle.resources.label_narrator_off
import net.blockhost.trestle.resources.label_narrator_system
import net.blockhost.trestle.resources.label_particles_all
import net.blockhost.trestle.resources.label_particles_decreased
import net.blockhost.trestle.resources.label_particles_minimal
import net.blockhost.trestle.resources.label_proxy_system
import net.blockhost.trestle.resources.label_sort_downloads
import net.blockhost.trestle.resources.label_sort_featured
import net.blockhost.trestle.resources.label_sort_last_launched
import net.blockhost.trestle.resources.label_sort_name
import net.blockhost.trestle.resources.label_sort_newest
import net.blockhost.trestle.resources.label_sort_relevance
import net.blockhost.trestle.resources.label_sort_updated
import net.blockhost.trestle.resources.label_theme_dark
import net.blockhost.trestle.resources.label_theme_light
import net.blockhost.trestle.resources.label_theme_system
import net.blockhost.trestle.resources.label_type_mod
import net.blockhost.trestle.resources.label_type_modpack
import net.blockhost.trestle.resources.label_type_modpacks
import net.blockhost.trestle.resources.label_type_modpacks_phrase
import net.blockhost.trestle.resources.label_type_mods
import net.blockhost.trestle.resources.label_type_mods_phrase
import net.blockhost.trestle.resources.label_type_resource_pack
import net.blockhost.trestle.resources.label_type_resource_packs
import net.blockhost.trestle.resources.label_type_resource_packs_phrase
import net.blockhost.trestle.resources.label_type_shader
import net.blockhost.trestle.resources.label_type_shaders
import net.blockhost.trestle.resources.label_type_shaders_phrase
import net.blockhost.trestle.resources.ui_none
import org.jetbrains.compose.resources.StringResource

/** Translated names for the launcher's choice lists. Product names, such as loaders and catalogs, are not translated. */
val ThemePreference.labelResource: StringResource
    get() = when (this) {
        ThemePreference.SYSTEM -> Res.string.label_theme_system
        ThemePreference.DARK -> Res.string.label_theme_dark
        ThemePreference.LIGHT -> Res.string.label_theme_light
    }

val InstanceSortMode.labelResource: StringResource
    get() = when (this) {
        InstanceSortMode.NAME -> Res.string.label_sort_name
        InstanceSortMode.LAST_LAUNCHED -> Res.string.label_sort_last_launched
    }

/** The proxy type as shown in settings. SOCKS5 and HTTP are protocol names and stay as they are. */
val LauncherProxyType.labelText: UserText
    get() = when (this) {
        LauncherProxyType.SYSTEM -> text(Res.string.label_proxy_system)
        LauncherProxyType.NONE -> text(Res.string.ui_none)
        LauncherProxyType.SOCKS5, LauncherProxyType.HTTP -> label.asUserText()
    }

val MinecraftNarratorMode.labelResource: StringResource
    get() = when (this) {
        MinecraftNarratorMode.OFF -> Res.string.label_narrator_off
        MinecraftNarratorMode.ALL -> Res.string.label_narrator_all
        MinecraftNarratorMode.CHAT -> Res.string.label_narrator_chat
        MinecraftNarratorMode.SYSTEM -> Res.string.label_narrator_system
    }

val MinecraftParticleSetting.labelResource: StringResource
    get() = when (this) {
        MinecraftParticleSetting.ALL -> Res.string.label_particles_all
        MinecraftParticleSetting.DECREASED -> Res.string.label_particles_decreased
        MinecraftParticleSetting.MINIMAL -> Res.string.label_particles_minimal
    }

val AccountAuthenticationMethod.labelResource: StringResource
    get() = when (this) {
        AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE -> Res.string.label_auth_java_device_code
        AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE -> Res.string.label_auth_bedrock_device_code
        AccountAuthenticationMethod.MICROSOFT_CREDENTIALS -> Res.string.label_auth_java_credentials
        AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS -> Res.string.label_auth_bedrock_credentials
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN -> Res.string.label_auth_java_refresh_token
        AccountAuthenticationMethod.MICROSOFT_COOKIES -> Res.string.label_auth_java_cookies
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> Res.string.label_auth_java_access_token
        AccountAuthenticationMethod.OFFLINE -> Res.string.label_auth_offline
    }

/** The content type as a title, for example "Resource packs". */
val ResourceType.labelResource: StringResource
    get() = when (this) {
        ResourceType.MOD -> Res.string.label_type_mods
        ResourceType.MODPACK -> Res.string.label_type_modpacks
        ResourceType.RESOURCE_PACK -> Res.string.label_type_resource_packs
        ResourceType.SHADER_PACK -> Res.string.label_type_shaders
    }

/** One item of the content type, for example "Resource pack". */
val ResourceType.singularResource: StringResource
    get() = when (this) {
        ResourceType.MOD -> Res.string.label_type_mod
        ResourceType.MODPACK -> Res.string.label_type_modpack
        ResourceType.RESOURCE_PACK -> Res.string.label_type_resource_pack
        ResourceType.SHADER_PACK -> Res.string.label_type_shader
    }

/** The content type inside a sentence, for example "resource packs" in English. */
val ResourceType.phraseResource: StringResource
    get() = when (this) {
        ResourceType.MOD -> Res.string.label_type_mods_phrase
        ResourceType.MODPACK -> Res.string.label_type_modpacks_phrase
        ResourceType.RESOURCE_PACK -> Res.string.label_type_resource_packs_phrase
        ResourceType.SHADER_PACK -> Res.string.label_type_shaders_phrase
    }

val ReleaseChannel.labelResource: StringResource
    get() = when (this) {
        ReleaseChannel.RELEASE -> Res.string.label_channel_release
        ReleaseChannel.BETA -> Res.string.label_channel_beta
        ReleaseChannel.ALPHA -> Res.string.label_channel_alpha
        ReleaseChannel.UNKNOWN -> Res.string.label_channel_unknown
    }

val ResourceSearchSort.labelResource: StringResource
    get() = when (this) {
        ResourceSearchSort.RELEVANCE -> Res.string.label_sort_relevance
        ResourceSearchSort.FEATURED -> Res.string.label_sort_featured
        ResourceSearchSort.DOWNLOADS -> Res.string.label_sort_downloads
        ResourceSearchSort.UPDATED -> Res.string.label_sort_updated
        ResourceSearchSort.NEWEST -> Res.string.label_sort_newest
    }
