plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val trestleVersion = providers.gradleProperty("trestle.version").orElse("0.1.0")
val trestleVersionCode = providers.gradleProperty("trestle.versionCode").map(String::toInt).orElse(1)
val releaseSigningEnvironment = listOf(
    "TRESTLE_ANDROID_KEYSTORE_PATH",
    "TRESTLE_ANDROID_STORE_PASSWORD",
    "TRESTLE_ANDROID_KEY_ALIAS",
    "TRESTLE_ANDROID_KEY_PASSWORD",
).associateWith(providers::environmentVariable)

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

    signingConfigs {
        create("release") {
            storeFile = releaseSigningEnvironment.getValue("TRESTLE_ANDROID_KEYSTORE_PATH").orNull?.let(::file)
            storeType = "PKCS12"
            storePassword = releaseSigningEnvironment.getValue("TRESTLE_ANDROID_STORE_PASSWORD").orNull
            keyAlias = releaseSigningEnvironment.getValue("TRESTLE_ANDROID_KEY_ALIAS").orNull
            keyPassword = releaseSigningEnvironment.getValue("TRESTLE_ANDROID_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    val signingEnvironment = releaseSigningEnvironment
    doLast {
        val missing = signingEnvironment.filterValues { it.orNull.isNullOrBlank() }.keys
        check(missing.isEmpty()) {
            "Release signing requires these environment variables: ${missing.joinToString()}."
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseSigning)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.customview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.core)
}
