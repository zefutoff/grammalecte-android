import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "fr.grammalecteandroid.engine"
    compileSdk = 36

    androidResources {
        ignoreAssetsPatterns.addAll(
            listOf(
                "!.svn",
                "!.git",
                "!.ds_store",
                "!*.scc",
                ".*",
                "!CVS",
                "!thumbs.db",
                "!picasa.ini",
                "!*~",
            ),
        )
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

val quickJsVersion = libs.versions.quickjs.get()

configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("io.github.dokar3:quickjs-kt-android"))
            .using(module("io.github.dokar3:quickjs-kt-jvm:$quickJsVersion"))
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty(
        "grammalecte.perf",
        providers
            .gradleProperty("grammalectePerf")
            .orElse("0")
            .get(),
    )

    systemProperty(
        "grammalecte.stress",
        providers
            .gradleProperty("grammalecteStress")
            .orElse("0")
            .get(),
    )
}

dependencies {
    implementation(project(":core"))
    implementation(libs.quickjs)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.json)
}
