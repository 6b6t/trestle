package net.blockhost.trestle.runtime

import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.MemorySettings
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_memory_leaves_too_little_ram
import net.blockhost.trestle.resources.msg_mobile_memory_too_high
import net.blockhost.trestle.resources.msg_modded_needs_more_memory
import net.blockhost.trestle.text.UserText
import net.blockhost.trestle.text.text

data class SystemProfile(val totalMemoryMiB: Int, val availableProcessors: Int, val isMobile: Boolean) {
    init {
        require(totalMemoryMiB > 0)
        require(availableProcessors > 0)
    }
}

data class LaunchRecommendation(val memory: MemorySettings, val warnings: List<UserText>)

object LaunchTuningAdvisor {
    fun recommend(instance: GameInstance, system: SystemProfile): LaunchRecommendation {
        val recommendedMemory = recommendMemory(instance.modLoader, system)
        val safeMaximum = safeMaximum(system)
        val warnings = buildList {
            if (instance.memory.maximumMiB > safeMaximum) {
                add(text(Res.string.msg_memory_leaves_too_little_ram))
            }
            if (system.isMobile && instance.memory.maximumMiB > system.totalMemoryMiB / 2) {
                add(text(Res.string.msg_mobile_memory_too_high))
            }
            if (instance.memory.maximumMiB < 1_024 && instance.modLoader != ModLoader.VANILLA) {
                add(text(Res.string.msg_modded_needs_more_memory))
            }
        }
        return LaunchRecommendation(memory = recommendedMemory, warnings = warnings)
    }

    fun recommendMemory(loader: ModLoader, system: SystemProfile): MemorySettings {
        val safeMaximum = safeMaximum(system)
        val workloadTarget = when (loader) {
            ModLoader.VANILLA -> if (system.isMobile) 2_048 else 3_072
            ModLoader.FABRIC, ModLoader.QUILT -> if (system.isMobile) 3_072 else 4_096
            ModLoader.FORGE, ModLoader.NEOFORGE -> if (system.isMobile) 3_584 else 6_144
        }
        val recommendedMaximum = minOf(safeMaximum, workloadTarget).coerceAtLeast(512)
        return MemorySettings(minOf(512, recommendedMaximum), recommendedMaximum)
    }

    private fun safeMaximum(system: SystemProfile): Int {
        val reserve = if (system.isMobile) {
            if (system.totalMemoryMiB < 3_072) 800 else 1_024
        } else {
            maxOf(1_536, system.totalMemoryMiB / 4)
        }
        return (system.totalMemoryMiB - reserve).coerceAtLeast(512)
    }
}

data class JvmArgumentReview(val accepted: List<String>, val ignored: List<String>)

object JvmArgumentPolicy {
    fun review(arguments: List<String>): JvmArgumentReview {
        val accepted = mutableListOf<String>()
        val ignored = mutableListOf<String>()
        var skipNext = false
        arguments.forEach { argument ->
            if (skipNext) {
                ignored += argument
                skipNext = false
            } else if (argument == "-cp" || argument == "-classpath") {
                ignored += argument
                skipNext = true
            } else if (RESERVED_PREFIXES.any(argument::startsWith)) {
                ignored += argument
            } else {
                accepted += argument
            }
        }
        return JvmArgumentReview(accepted, ignored)
    }

    private val RESERVED_PREFIXES = listOf(
        "-Xms",
        "-Xmx",
        "-XX:InitialHeapSize",
        "-XX:MaxHeapSize",
        "-Djava.library.path=",
        "-Dorg.lwjgl.librarypath=",
        "-version:",
        "-d32",
        "-d64",
    )
}
