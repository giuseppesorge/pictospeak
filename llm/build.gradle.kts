plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "io.github.giuseppesorge.pictospeak.llm"
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
    // :llm may see :nlg (it implements SentenceRefiner). It must NEVER see :speech
    // (INVARIANT-1) and no other module may import LiteRT-LM (docs/adr/0004).
    implementation(project(":nlg"))
    implementation(libs.coroutines.core)
    // PINNED runtime; 64-bit ABIs only (arm64-v8a + x86_64) — the runtime device gate in
    // :app checks Build.SUPPORTED_64_BIT_ABIS before this module is ever exercised.
    implementation(libs.litertlm.android)

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
}
