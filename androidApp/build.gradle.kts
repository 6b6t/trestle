plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val trestleVersion = providers.gradleProperty("trestle.version").orElse("0.1.0")
val trestleVersionCode = providers.gradleProperty("trestle.versionCode").map(String::toInt).orElse(1)

android {
    namespace = "net.blockhost.trestle"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "net.blockhost.trestle"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = trestleVersionCode.get()
        versionName = trestleVersion.get()
        manifestPlaceholders["curseForgeApiKey"] = providers.environmentVariable("TRESTLE_CURSEFORGE_API_KEY")
            .orElse(providers.gradleProperty("trestle.curseforge.apiKey"))
            .get()
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.core)
}
