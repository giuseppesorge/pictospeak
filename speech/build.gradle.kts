plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "io.github.giuseppesorge.pictospeak.speech"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

dependencies {
    // :speech may see :nlg (candidate types in the confirm signature). The REVERSE edge is
    // forbidden — :nlg and :llm must never reach TTS types (INVARIANT-1, CLAUDE.md rule 2).
    implementation(project(":nlg"))
    implementation(libs.coroutines.android)

    testImplementation(libs.junit4)
}
