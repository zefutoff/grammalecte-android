import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

android {
    namespace = "fr.zefut.grammalecte"
    compileSdk = 36
    testBuildType = "dev"

    buildFeatures {
        compose = true
    }

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
        applicationId = "fr.zefut.grammalecte"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("dev") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            matchingFallbacks += listOf("debug")
        }

        release {
            isMinifyEnabled = true
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // Reuse debug-only test fixtures in the DEV variant.
    // These activities must never be packaged in RELEASE.
    sourceSets {
        getByName("dev") {
            kotlin.directories += "src/debug/kotlin"
            manifest.srcFile("src/debug/AndroidManifest.xml")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes +=
            setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    // Stable Compose 1.11 train: keep compileSdk 36 and Android 8 minimum.
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(project(":spellchecker"))
    implementation(project(":core"))
    implementation(project(":engine-grammalecte"))

    androidTestImplementation(project(":engine-grammalecte"))
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
