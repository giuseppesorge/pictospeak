// Macrobenchmark module — test-only, never shipped. Performance sign-off happens on
// PHYSICAL devices exclusively (docs/perf-budgets.md); emulators are rejected.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "io.github.giuseppesorge.pictospeak.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Benchmarks always measure the foss flavor — the product that must hit the budget.
        missingDimensionStrategy("distribution", "foss")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        // Benchmarks run against a release-like build with debug signing.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.uiautomator)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.junit4)
}
