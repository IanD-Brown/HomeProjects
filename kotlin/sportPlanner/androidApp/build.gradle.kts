import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "io.github.iandbrown.sportplanner.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        applicationId = "io.github.iandbrown.sportplanner"
        versionName = "1.0.0"
        versionCode = 1
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.material.theme)

    //Room step1
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled) //for sqlite drivers related
    // android startup (for room)
    implementation(libs.androidx.startup)

    implementation(libs.filekit.dialogs.compose)
}
