import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val trestleVersion = providers.gradleProperty("trestle.version").orElse("0.1.0")
val trestleVersionCode = providers.gradleProperty("trestle.versionCode").orElse("1")
val curseForgeApiKey = providers.environmentVariable("TRESTLE_CURSEFORGE_API_KEY")
    .orElse(providers.gradleProperty("trestle.curseforge.apiKey")).orElse("")
val broadFileAssociations = providers.gradleProperty("trestle.broadFileAssociations").map(String::toBoolean).orElse(false)

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.resources)
    implementation(libs.dbus.java.core)
    implementation(libs.dbus.java.transport.native.unixsocket)
    implementation(libs.filekit.dialogs.compose)
    implementation(libs.jna.platform)
    implementation(libs.java.objc.bridge)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okio)
    runtimeOnly(libs.slf4j.simple)
    testImplementation(kotlin("test"))
    testImplementation(libs.compose.ui.test)
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
        jvmArgs += "-Djava.desktop.appName=net.blockhost.trestle.desktop"
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
                if (broadFileAssociations.get()) {
                    fileAssociation("application/java-archive", "jar", "Minecraft mod", iconFile.get().asFile)
                    fileAssociation("application/zip", "zip", "Minecraft content pack", iconFile.get().asFile)
                }
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
                if (broadFileAssociations.get()) {
                    fileAssociation("application/java-archive", "jar", "Minecraft mod")
                    fileAssociation("application/zip", "zip", "Minecraft content pack")
                }
                fileAssociation("application/x-modrinth-modpack+zip", "mrpack", "Modrinth modpack")
                // Publisher certificates are not required. The packaging task below
                // applies ad hoc signatures on both Intel and Apple Silicon.
                signing { sign.set(false) }
                entitlementsFile.set(rootProject.file("packaging/macos/entitlements.plist"))
                runtimeEntitlementsFile.set(rootProject.file("packaging/macos/entitlements.plist"))
                bundleID = "net.blockhost.trestle"
                dockName = "Trestle"
                appCategory = "public.app-category.games"
                minimumSystemVersion = "11.0"
                // jpackage rejects zero-major versions on macOS. Native packages use
                // the increasing build number; the launcher keeps its release version.
                packageVersion = trestleVersionCode.get()
                packageBuildVersion = trestleVersionCode.get()
                infoPlist {
                    extraKeysRawXml = """
                        <key>TrestleVersion</key>
                        <string>${trestleVersion.get()}</string>
                        <key>NSHumanReadableCopyright</key>
                        <string>Copyright 2026 Blockhost Network</string>
                        <key>TrestleHomepage</key>
                        <string>https://github.com/6b6t/trestle</string>
                        <key>TrestleSupportURL</key>
                        <string>https://github.com/6b6t/trestle/issues</string>
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
                if (broadFileAssociations.get()) {
                    fileAssociation("application/java-archive", "jar", "Minecraft mod", iconFile.get().asFile)
                    fileAssociation("application/zip", "zip", "Minecraft content pack", iconFile.get().asFile)
                }
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

tasks.configureEach {
    if (name == "run") {
        outputs.upToDateWhen { false }
    }
}

// jpackage exposes installer URLs outside the Compose platform-specific DSL.
tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    if (targetFormat != TargetFormat.AppImage) {
        freeArgs.addAll("--about-url", "https://github.com/6b6t/trestle")
    }
    if (targetFormat == TargetFormat.Msi || targetFormat == TargetFormat.Exe) {
        freeArgs.addAll("--win-help-url", "https://github.com/6b6t/trestle/issues",
            "--win-update-url", "https://github.com/6b6t/trestle/releases/latest")
    }
}

if (System.getProperty("os.name") == "Mac OS X") {
    val signMacosApplication by tasks.registering(Exec::class) {
        dependsOn("createDistributable")
        commandLine(
            "python3",
            rootProject.file("scripts/sign-macos.py"),
            "--app", layout.buildDirectory.dir("compose/binaries/main/app/Trestle.app").get().asFile,
            "--entitlements", rootProject.file("packaging/macos/entitlements.plist"),
        )
    }
    tasks.matching { it.name == "packageDmg" || it.name == "packagePkg" }.configureEach {
        dependsOn(signMacosApplication)
    }
}
