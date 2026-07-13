plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "io.github.giuseppesorge.pictospeak"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.giuseppesorge.pictospeak"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("foss") {
            dimension = "distribution"
            isDefault = true
        }
        create("play") {
            dimension = "distribution"
        }
    }

    buildTypes {
        release {
            // R8 full mode is the AGP default; shrinking is mandatory from release one
            // (low-end performance budget, docs/perf-budgets.md).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

dependencies {
    implementation(project(":nlg"))
    implementation(project(":speech"))
    // The LLM module exists ONLY in the play flavor binary (CLAUDE.md hard rule 3).
    "playImplementation"(project(":llm"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.profileinstaller)

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}

// Baseline Profile: the androidx.baselineprofile plugin (1.4.1) does not yet support
// AGP 9 modules, so the profile is generated manually with :benchmark's
// BaselineProfileGenerator and committed to app/src/main/baseline-prof.txt, which AGP
// compiles into the APK natively. Procedure: docs/perf-budgets.md. Revisit when the
// plugin gains AGP 9 support.
