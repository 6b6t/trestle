import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val trestleVersion = providers.gradleProperty("trestle.version").orElse("0.1.0")

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    runtimeOnly(libs.slf4j.simple)
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "net.blockhost.trestle.desktop.MainKt"

        nativeDistributions {
            modules("jdk.unsupported")
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "Trestle"
            packageVersion = trestleVersion.get()

            linux {
                iconFile.set(project.file("src/main/resources/trestle.png"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/trestle.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/trestle.ico"))
            }
        }
    }
}
