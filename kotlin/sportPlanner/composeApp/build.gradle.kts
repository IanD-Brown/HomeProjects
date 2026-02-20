import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.androidLibrary)

    //Room step2 -> plugins
    alias(libs.plugins.androidxRoom)
    alias(libs.plugins.ksp) //ksp for room annotation processing

    alias(libs.plugins.kotlinSerialization)

    alias(libs.plugins.kotest.plugin)

    alias(libs.plugins.kover)
    alias(libs.plugins.stability.analyzer)

    alias(libs.plugins.mockkery)
}

kover {
    reports {
        filters {
            excludes {
                // Entry Points
                classes("MainKt") // Desktop
                classes("*.MainActivity") // Android
                classes("App", "JVMPlatform", "Platform_jvmKt")

                // Generated Classes & Resources
                packages("*.generated.*", "*.database", "*.ui", "*.di")

                // Compose Related
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }

        verify {
            rule {
                minBound(80)
            }
        }
    }
}

kotlin {
    androidLibrary {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        namespace = "io.github.iandbrown.sportplanner"
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11 // Max supported version for Android
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.startup)
        }

        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // navigation
            implementation(libs.navigation.compose)

            // theme prefs
            implementation(libs.material.theme)
            implementation(libs.material.theme.prefs)
            implementation(libs.kotlin.serialization.json)

            //Room step1
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled) //for sqlite drivers related

            // Koin
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)

            // Enables FileKit dialogs without Compose dependencies
            implementation(libs.filekit.dialogs)

            // Enables FileKit dialogs with Composable utilities
            implementation(libs.filekit.dialogs.compose)

            implementation(libs.navigationevent.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.core)
            implementation(libs.kotest.framework)
        }

        jvmTest {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.iandbrown.sportplanner.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sportplanner"
            packageVersion = "1.0.0"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
}
