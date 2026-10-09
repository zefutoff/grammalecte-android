import org.cyclonedx.gradle.CyclonedxDirectTask

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.cyclonedx)
}


allprojects {
    tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
        includeConfigs =
            listOf(
                "runtimeClasspath",
                "releaseRuntimeClasspath",
            )
        testConfigs = emptyList()
        includeMetadataResolution = true
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
