import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    android {
        namespace = "net.blockhost.trestle.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
        withHostTest {}
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.adaptive.navigation.suite)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptive.layout)
            implementation(libs.compose.material3.adaptive.navigation)
            implementation(libs.compose.resources)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ksafe)
            implementation(libs.markdown.renderer)
            implementation(libs.markdown.renderer.coil3)
            implementation(libs.markdown.renderer.m3)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.okio)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okio.fake.file.system)
            implementation(libs.compose.ui.test)
        }

        val jvmMain = create("jvmMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.minecraft.auth)
            }
        }

        getByName("androidMain") {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.xz)
            }
        }

        getByName("desktopMain") {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.appdirs)
                implementation(libs.ktor.client.cio)
                implementation(libs.slf4j.api)
            }
        }

        getByName("desktopTest").dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

dependencies {
    add("androidRuntimeClasspath", libs.compose.ui.tooling)
}

compose.resources {
    packageOfResClass = "net.blockhost.trestle.resources"
    publicResClass = true
}
