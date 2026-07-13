// Root build file: declares plugin versions only. Each module applies what it needs —
// no allprojects/subprojects cross-configuration (kept boring and Gradle-isolation friendly).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}
