import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    //Room step2 -> plugins
    alias(libs.plugins.androidxRoom)
    alias(libs.plugins.ksp) //ksp for room annotation processing

    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            // IDB fix this, should be in libs.versions.toml
            //implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.navigation.compose)
            implementation(libs.coil.compose)


            //after compose multiplatform 1.6.10
            implementation(libs.lifecycle.viewmodel.compose)

            //Room step1
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled) //for sqlite drivers related

            implementation(libs.material.theme)
            implementation(libs.material.theme.prefs)
            implementation(libs.kotlin.serialization.json)

            // Koin
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)

            // ViewModel support in common code
            implementation(libs.androidx.lifecycle.viewmodel)

            implementation(libs.data.table)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing) //for v

        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.sunildhiman90.cmpwithroom"
            packageVersion = "1.0.0"
        }
    }
}

//Room step3: path where we want to generate the schemas
room {
    schemaDirectory("$projectDir/schemas")
}

//Room step5  KSP For processing Room annotations , Otherwise we will get Is Room annotation processor correctly configured? error
dependencies {
    // Update: https://issuetracker.google.com/u/0/issues/342905180
    listOf("kspDesktop").forEach {
        add(it, libs.androidx.room.compiler)
    }

}
