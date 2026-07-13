import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Offline content pipeline: core-vocabulary.csv + catalog snapshot -> bundled app assets.
// Run manually per release (the app itself NEVER touches the network):
//   ./gradlew :tools:arasaac-fetch:run
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
    mainClass.set("io.github.giuseppesorge.pictospeak.tools.arasaacfetch.MainKt")
}

dependencies {
    implementation(project(":nlg")) // PictogramToken + Board: single source of truth for asset formats
    implementation(libs.serialization.json)

    testImplementation(libs.junit4)
}
