import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled) //for sqlite drivers related

    implementation(libs.kmp.logger)
}

compose.desktop {
    application {
        mainClass = "io.github.iandbrown.home_energy.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.iandbrown.home_energy"
            packageVersion = "1.0.0"
        }
    }
}
