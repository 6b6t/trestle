import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ktlint) apply false
}

val trestleVersion = providers.gradleProperty("trestle.version").orElse("0.1.0")

// Version catalog accessors are not available inside allprojects or subprojects blocks.
val ktlintVersion = libs.versions.ktlint.asProvider()
val composeRulesDetekt = libs.compose.rules.detekt

allprojects {
    group = "net.blockhost"
    version = trestleVersion.get()

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<KtlintExtension> {
        version = ktlintVersion
        coloredOutput = false
        // Compose resources add generated sources under build/.
        val buildDirectory = layout.buildDirectory.get().asFile
        filter {
            exclude { it.file.startsWith(buildDirectory) }
        }
    }
}

subprojects {
    apply(plugin = "dev.detekt")
    configure<DetektExtension> {
        // Kotlin Multiplatform source sets are not covered by default. Analyze every source set without type resolution.
        source.setFrom(layout.projectDirectory.dir("src"))
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        baseline = file("detekt-baseline.xml")
        parallel = true
    }
    dependencies {
        "detektPlugins"(composeRulesDetekt)
    }
}
