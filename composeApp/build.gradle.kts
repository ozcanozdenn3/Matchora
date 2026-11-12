import java.net.URI
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            binaryOption("bundleId", "com.matchora.app.shared")
            isStatic = false
            freeCompilerArgs += "-Xoverride-konan-properties=minVersion.ios=16.0"
            // RevenueCat's prebuilt Swift objects retain the publishing machine's
            // toolchain path. Resolve compatibility libraries from the active Xcode.
            if (System.getProperty("os.name").startsWith("Mac")) {
                val developerPath = providers.exec { commandLine("xcode-select", "-p") }.standardOutput.asText.get().trim()
                val sdkName = if (iosTarget.name == "iosArm64") "iphoneos" else "iphonesimulator"
                linkerOpts("-L$developerPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$sdkName")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation("com.revenuecat.purchases:purchases-kmp-core:3.16.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            // Native Android map renderer; BSD-2-Clause. Version verified against the official release.
            implementation("org.maplibre.gl:android-sdk:13.7.1")
            implementation("com.google.firebase:firebase-messaging:25.0.0")
            implementation("com.google.zxing:core:3.5.3")
            implementation("androidx.credentials:credentials:1.3.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
        }
    }
}

android {
    namespace = "com.matchora.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.matchora.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

compose.resources {
    publicResClass = true
}

// Runtime mode is explicit. A release cannot silently ship the local demo or a loopback API.
val matchoraMode = providers.gradleProperty("matchora.mode").orElse(providers.environmentVariable("MATCHORA_MODE")).orElse("preview")
val matchoraApi = providers.gradleProperty("matchora.apiUrl").orElse(providers.environmentVariable("MATCHORA_API_URL")).orElse(providers.provider { if (matchoraMode.get() == "local") "http://127.0.0.1:8787" else "" })
val matchoraMapStyle = providers.gradleProperty("matchora.mapStyleUrl").orElse(providers.environmentVariable("MATCHORA_MAP_STYLE_URL")).orElse("")
val generateBackendConfig by tasks.registering {
    val output = layout.buildDirectory.dir("generated/backendConfig")
    inputs.property("mode", matchoraMode)
    inputs.property("apiUrl", matchoraApi)
    inputs.property("mapStyleUrl", matchoraMapStyle)
    val firebaseFile = layout.projectDirectory.file("google-services.json").asFile
    inputs.property("firebaseConfig", providers.provider { if (firebaseFile.exists()) firebaseFile.readText() else "" })
    outputs.dir(output)
    doLast {
        val mode = matchoraMode.get()
        require(mode in listOf("preview", "local", "production"))
        val endpoint = matchoraApi.get().trimEnd('/')
        val mapStyle = matchoraMapStyle.get().trim().ifEmpty { if (mode == "production") "" else "https://demotiles.maplibre.org/style.json" }
        require(mapStyle.isEmpty() || URI(mapStyle).let { it.scheme == "https" && it.host != null && it.userInfo == null && it.fragment == null })
        require(endpoint.isEmpty() || URI(endpoint).let { it.scheme in listOf("http", "https") && it.host != null && it.userInfo == null && it.query == null && it.fragment == null })
        val file = output.get().file("com/matchora/app/data/BackendBuildConfig.kt").asFile
        file.parentFile.mkdirs()
        val firebase = if (firebaseFile.exists()) groovy.json.JsonSlurper().parseText(firebaseFile.readText()) as Map<*, *> else emptyMap<Any, Any>()
        val project = firebase["project_info"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val client = (firebase["client"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.find {
            val info = it["client_info"] as? Map<*, *>
            (info?.get("android_client_info") as? Map<*, *>)?.get("package_name") == "com.matchora.app"
        } ?: emptyMap<Any, Any>()
        val info = client["client_info"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val apiKey = ((client["api_key"] as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("current_key")?.toString().orEmpty()
        fun literal(value: Any?) = "\"" + value?.toString().orEmpty().replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
        file.writeText("package com.matchora.app.data\nobject BackendBuildConfig { const val MODE = ${literal(mode)}; const val BASE_URL = ${literal(endpoint)}; " +
            "const val FIREBASE_APP_ID = ${literal(info["mobilesdk_app_id"])}; const val FIREBASE_API_KEY = ${literal(apiKey)}; " +
            "const val FIREBASE_PROJECT_ID = ${literal(project["project_id"])}; const val FIREBASE_SENDER_ID = ${literal(project["project_number"])}; const val MAP_STYLE_URL = ${literal(mapStyle)} }\n")
    }
}
kotlin.sourceSets.commonMain { kotlin.srcDir(layout.buildDirectory.dir("generated/backendConfig")) }
tasks.configureEach {
    if (name.startsWith("compileKotlin") || name.startsWith("compileDebugKotlin") || name.startsWith("compileReleaseKotlin")) dependsOn(generateBackendConfig)
}
val validateProductionConfiguration by tasks.registering {
    doLast {
        val uri = runCatching { URI(matchoraApi.get()) }.getOrNull()
        require(matchoraMode.get() == "production" && uri?.scheme == "https" && uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null && uri.host !in listOf("localhost", "127.0.0.1", "10.0.2.2")) {
            "Release requires -Pmatchora.mode=production -Pmatchora.apiUrl=https://YOUR_API. Preview/local builds cannot be published."
        }
        val mapStyle = runCatching { URI(matchoraMapStyle.get()) }.getOrNull()
        require(mapStyle?.scheme == "https" && mapStyle.host != null && mapStyle.userInfo == null && mapStyle.fragment == null && mapStyle.host != "demotiles.maplibre.org") {
            "Release requires -Pmatchora.mapStyleUrl=https://YOUR_MAP_PROVIDER/style.json. MapLibre demo tiles are for local validation."
        }
    }
}
tasks.configureEach {
    if ((name.contains("Release") && (name.startsWith("assemble") || name.startsWith("bundle") || name.startsWith("package"))) || name == "linkReleaseFrameworkIosArm64") dependsOn(validateProductionConfiguration)
}
