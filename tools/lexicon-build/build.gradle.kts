import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Offline content pipeline: Morph-it! + curated vocabulary -> bundled lexicon asset.
// Run manually per release (the app itself NEVER touches the network):
//   ./gradlew :tools:lexicon-build:run
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

application {
    mainClass.set("io.github.giuseppesorge.pictospeak.tools.lexiconbuild.MainKt")
}

dependencies {
    implementation(project(":nlg")) // Lexicon model: single source of truth for the asset format
    implementation(libs.serialization.json)

    testImplementation(libs.junit4)
}
