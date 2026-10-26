/*
 * Copyright 2026 Pete Bentley <pete@sorted.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

spotless {
    val licenseHeader = rootProject.file("config/spotless/apache-2.0-header.txt")

    val ktlintConfig =
        mapOf(
            // AOSP / Android Kotlin style guide.
            "ktlint_code_style" to "android_studio",
            // Jetpack Compose @Composable functions are intentionally PascalCase.
            "ktlint_function_naming_ignore_when_annotated_with" to "Composable"
        )

    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(ktlintConfig)
        licenseHeaderFile(licenseHeader)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(ktlintConfig)
        licenseHeaderFile(
            licenseHeader,
            "(import |plugins |pluginManagement |dependencyResolutionManagement |rootProject|@file)"
        )
    }
    java {
        target("**/*.java")
        targetExclude("**/build/**")
        licenseHeaderFile(licenseHeader)
    }
}
