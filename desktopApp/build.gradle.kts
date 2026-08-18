import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val trestleVersion = providers.gradleProperty("trestle.version").orElse("0.1.0")
val trestleVersionCode = providers.gradleProperty("trestle.versionCode").orElse("1")
val curseForgeApiKey = providers.gradleProperty("trestle.curseforge.apiKey").orElse("")

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.dbus.java.core)
    implementation(libs.dbus.java.transport.native.unixsocket)
    implementation(libs.filekit.dialogs.compose)
    implementation(libs.jna.platform)
    implementation(libs.java.objc.bridge)
    implementation(libs.kotlinx.coroutines.core)
    runtimeOnly(libs.slf4j.simple)
    testImplementation(kotlin("test"))
}

tasks.processResources {
    from(rootProject.file("licenses")) {
        into("licenses")
    }
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "net.blockhost.trestle.desktop.MainKt"
        jvmArgs += "-Djava.desktop.appName=trestle-Trestle.desktop"
        jvmArgs += "-Dtrestle.curseforge.apiKey=${curseForgeApiKey.get()}"

        nativeDistributions {
            modules(
                "java.instrument",
                "java.management",
                "java.net.http",
                "java.sql",
                "jdk.httpserver",
                "jdk.management",
                "jdk.security.auth",
                "jdk.unsupported",
            )
            targetFormats(
                TargetFormat.Deb,
                TargetFormat.Rpm,
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Msi,
                TargetFormat.Exe,
            )
            packageName = "Trestle"
            packageVersion = trestleVersion.get()
            description = "A cross-platform Minecraft Java Edition launcher"
            copyright = "Copyright 2026 Blockhost Network"
            vendor = "Blockhost Network"
            licenseFile.set(rootProject.file("LICENSE"))

            linux {
                iconFile.set(project.file("src/main/resources/trestle.png"))
                fileAssociation("application/java-archive", "jar", "Minecraft mod", iconFile.get().asFile)
                fileAssociation("application/zip", "zip", "Minecraft content pack", iconFile.get().asFile)
                fileAssociation("application/x-modrinth-modpack+zip", "mrpack", "Modrinth modpack", iconFile.get().asFile)
                packageName = "trestle"
                shortcut = true
                menuGroup = "Game"
                appCategory = "Game"
                appRelease = "1"
                debMaintainer = "40795980+AlexProgrammerDE@users.noreply.github.com"
                rpmLicenseType = "Apache-2.0"
            }

            macOS {
                iconFile.set(project.file("src/main/resources/trestle.icns"))
                fileAssociation("application/java-archive", "jar", "Minecraft mod", iconFile.get().asFile)
                fileAssociation("application/zip", "zip", "Minecraft content pack", iconFile.get().asFile)
                fileAssociation("application/x-modrinth-modpack+zip", "mrpack", "Modrinth modpack", iconFile.get().asFile)
                bundleID = "net.blockhost.trestle"
                dockName = "Trestle"
                appCategory = "public.app-category.games"
                minimumSystemVersion = "11.0"
                packageBuildVersion = trestleVersionCode.get()
                infoPlist {
                    extraKeysRawXml = """
                        <key>LSMultipleInstancesProhibited</key>
                        <true/>
                        <key>NSHighResolutionCapable</key>
                        <true/>
                        <key>NSSupportsAutomaticGraphicsSwitching</key>
                        <true/>
                        <key>CFBundleURLTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleURLName</key>
                                <string>net.blockhost.trestle</string>
                                <key>CFBundleURLSchemes</key>
                                <array>
                                    <string>trestle</string>
                                </array>
                            </dict>
                        </array>
                    """.trimIndent()
                }
            }

            windows {
                iconFile.set(project.file("src/main/resources/trestle.ico"))
                fileAssociation("application/java-archive", "jar", "Minecraft mod", iconFile.get().asFile)
                fileAssociation("application/zip", "zip", "Minecraft content pack", iconFile.get().asFile)
                fileAssociation("application/x-modrinth-modpack+zip", "mrpack", "Modrinth modpack", iconFile.get().asFile)
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
                menuGroup = "Trestle"
                upgradeUuid = "1fe5267d-ed22-5d63-8ce7-d0d6479c7a8b"
            }
        }
    }
}
