plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val uploadArtifactTasks = setOf("assembleRelease", "bundleRelease", "packageRelease")
val uploadTaskRequested = gradle.startParameter.taskNames.any { requestedTask ->
    uploadArtifactTasks.any { requestedTask.endsWith(it) }
}
val releaseCredentialNames = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD"
)
val releaseCredentials = if (uploadTaskRequested) {
    releaseCredentialNames.associateWith { name -> System.getenv(name).orEmpty() }
} else {
    emptyMap()
}
val missingReleaseCredentials = releaseCredentialNames.filter { name ->
    releaseCredentials[name].isNullOrBlank()
}

if (uploadTaskRequested && missingReleaseCredentials.isNotEmpty()) {
    throw GradleException(
        "Upload-ready release tasks require runtime-only signing inputs: " +
            missingReleaseCredentials.joinToString() + ". " +
            "Use :app:bundleReleaseVerification for unsigned-boundary CI verification."
    )
}

android {
    namespace = "com.wallpapercropfixer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wallpapercropfixer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // The bundled ML Kit native payload is 16 KB-aligned for these
            // 64-bit ABIs. Excluding 32-bit ABIs prevents shipping an AAB that
            // cannot satisfy the 16 KB native ELF release invariant.
            abiFilters += setOf("arm64-v8a", "x86_64")
        }
    }

    if (uploadTaskRequested) {
        signingConfigs {
            create("releaseUpload") {
                // Values are read only for an explicitly requested upload-semantic
                // task and are injected by the future dedicated release domain.
                storeFile = file(releaseCredentials.getValue("RELEASE_STORE_FILE"))
                storePassword = releaseCredentials.getValue("RELEASE_STORE_PASSWORD")
                keyAlias = releaseCredentials.getValue("RELEASE_KEY_ALIAS")
                keyPassword = releaseCredentials.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("releaseVerification") {
            // Explicit CI/dev verification artifact. It is not upload-ready and
            // is intentionally debug-signed so it can be installed locally.
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
        }
    }

    if (uploadTaskRequested) {
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("releaseUpload")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            keepDebugSymbols += listOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so",
                "**/libface_detector_v2_jni.so"
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // ML Kit Face Detection
    implementation(libs.mlkit.face.detection)

    // EXIF
    implementation(libs.androidx.exifinterface)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.ui.test.manifest)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
