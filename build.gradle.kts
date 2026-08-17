plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
}

val trestleVersion = providers.gradleProperty("trestle.version").orElse("0.1.0")

allprojects {
    group = "net.blockhost"
    version = trestleVersion.get()
}
