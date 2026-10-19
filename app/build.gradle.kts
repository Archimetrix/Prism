import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

// Room writes each database version's schema here (checked in); MigrationTest builds old versions from them.
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.prism.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.prism.music"
        minSdk = 26
        targetSdk = 36
        versionCode = 38
        versionName = "1.7.0"
        vectorDrawables { useSupportLibrary = true }
        // The name under the launcher icon; a side-by-side build (below) is called "Prism Dev" so it can't be mistaken for Prism.
        manifestPlaceholders["appLabel"] = "@string/app_name"
        // Optional: `lastfm.apiKey=…` in local.properties lets genre detection ask Last.fm too.
        val localProps = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        buildConfigField("String", "LASTFM_API_KEY", "\"${localProps.getProperty("lastfm.apiKey", "")}\"")
    }

    // Releases are signed with the key in keystore.properties (gitignored, with the .jks beside it).
    // Without it — or with -PdebugSign — they fall back to the debug key so anyone can build.
    val keyProps = Properties().apply {
        rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    val useReleaseKey = keyProps.isNotEmpty() && !project.hasProperty("debugSign")
    signingConfigs {
        if (useReleaseKey) create("release") {
            storeFile = rootProject.file(keyProps.getProperty("storeFile"))
            storePassword = keyProps.getProperty("storePassword")
            keyAlias = keyProps.getProperty("keyAlias")
            keyPassword = keyProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // `-PsideBySide`: a debug Prism that installs next to the real one (for trying changes on your own phone), as
            // "Prism Dev". `-PsideBySide=name` installs as com.prism.music.name instead of .dev, beside an older dev copy.
            if (project.hasProperty("sideBySide")) {
                applicationIdSuffix = "." + (project.property("sideBySide") as String).ifBlank { "dev" }
                manifestPlaceholders["appLabel"] = "Prism Dev"
            }
        }
        release {
            // Kept unminified: NewPipeExtractor + Rhino rely on reflection.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (useReleaseKey) "release" else "debug")
            // `-PsideBySide` on a release build: the same "Prism Dev" test copy, but optimised like a release (debug
            // builds run Prism's own audio code slower, which matters when chasing stutters). Always the debug key, so
            // it can't be mistaken for (or installed over) the real Prism.
            if (project.hasProperty("sideBySide")) {
                applicationIdSuffix = "." + (project.property("sideBySide") as String).ifBlank { "dev" }
                manifestPlaceholders["appLabel"] = "Prism Dev"
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric tests can read the app's own resources (strings, the widgets' layouts).
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        // ONNX Runtime's engine (13+ MB per CPU type) comes with Sing's download (ModelStore.Model.SING_RUNTIME_*), not
        // in the APK. Its small Java bridge (~120 KB) stays: Android only finds that one in the APK (it has no soname).
        jniLibs {
            excludes += setOf("**/libonnxruntime.so")
        }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/CONTRIBUTORS.md",
                "META-INF/*.kotlin_module",
                "mozilla/public-suffix-list.txt",
                // Kuromoji's dictionary: downloaded when Japanese lyrics are first romanized (ModelStore.Model.JAPANESE).
                "com/atilika/kuromoji/ipadic/*.bin",
            )
        }
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    // Window size and fold posture (tablets, foldables, landscape): Apache-2.0, brings androidx.window.
    implementation("androidx.compose.material3.adaptive:adaptive:1.3.0")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    val media3 = "1.11.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    val room = "2.8.5"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.11.0")

    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
    // Runs yt-dlp's EJS solver (assets/ejs) on WebView's V8 to decipher signed-in stream URLs.
    implementation("androidx.javascriptengine:javascriptengine:1.1.1")
    implementation("org.jsoup:jsoup:1.23.2")
    // Japanese lyrics' kanji readings for romanization (Apache-2.0). Its 13 MB dictionary isn't packed into the APK
    // (see packaging below): it's downloaded on first use (ModelStore). Tests read it from the jar.
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    // Sing's vocal separation (Spleeter) runs on ONNX Runtime (MIT). Its native libraries (13+ MB per CPU type) aren't
    // packed into the APK (see packaging below): they're downloaded with Sing's model on first use (ModelStore).
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.31.0")
    // Blend: QR codes made and read (ZXing core, Apache-2.0), the camera for scanning one (CameraX, Apache-2.0).
    implementation("com.google.zxing:core:3.5.3")
    val camerax = "1.4.2"
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    // On the PC, tests run the same models with ONNX Runtime's desktop build.
    testImplementation("com.microsoft.onnxruntime:onnxruntime:1.31.0")

    implementation("dev.chrisbanes.haze:haze:2.0.1")
    implementation("dev.chrisbanes.haze:haze-blur:2.0.1")
    implementation("dev.chrisbanes.haze:haze-glass:2.0.1")

    testImplementation("junit:junit:4.13.2")
    // Renders the backgrounds to PNGs on the PC (BackdropRenderTest), with Robolectric's real graphics.
    testImplementation("org.robolectric:robolectric:4.14.1")
}

// The tests use ONNX Runtime's desktop build (with its PC libraries); the Android one's classes would clash with it.
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "com.microsoft.onnxruntime", module = "onnxruntime-android")
}

tasks.withType<Test>().configureEach {
    testLogging {
        showStandardStreams = true
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
