import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :nlg is a PURE Kotlin JVM module — zero Android dependencies, ever (CLAUDE.md).
// That keeps the sentence engine unit-testable in milliseconds and portable.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
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

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
}
