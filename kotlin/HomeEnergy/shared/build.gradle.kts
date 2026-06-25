import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    // koin
    alias(libs.plugins.koin.compiler)

    //Room step2 -> plugins
    alias(libs.plugins.androidxRoom)
    alias(libs.plugins.ksp) //ksp for room annotation processing

    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    android {
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        namespace = "io.github.iandbrown.home_energy.shared"
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11 // Max supported version for Android
        }
        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.networking.kmp.core)

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
            implementation(libs.koin.annotations)  // For annotation support

            implementation(libs.kmp.logger)

            // nav 3 plus
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)

            // immutable collections
            implementation(libs.immutable.collections)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
    androidRuntimeClasspath(libs.compose.uiTooling)
}
