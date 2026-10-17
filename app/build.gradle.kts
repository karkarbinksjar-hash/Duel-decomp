plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.yugioh.recomp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yugioh.recomp"
        minSdk = 26
        targetSdk = 34
        versionCode = 25
        versionName = "2.10.0"
        setProperty("archivesBaseName", "Duel-decomp")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val permanentKeystore = file("${System.getProperty("user.home")}/.android/duel_decomp_release.keystore")
            val fallbackKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "DuelDecomp2026SecureReleaseKey!"
                keyAlias = System.getenv("KEY_ALIAS") ?: "duel_decomp_release"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "DuelDecomp2026SecureReleaseKey!"
            } else if (permanentKeystore.exists()) {
                storeFile = permanentKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "DuelDecomp2026SecureReleaseKey!"
                keyAlias = System.getenv("KEY_ALIAS") ?: "duel_decomp_release"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "DuelDecomp2026SecureReleaseKey!"
            } else if (fallbackKeystore.exists()) {
                storeFile = fallbackKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.all {
            it.maxHeapSize = "384m"
            it.jvmArgs("-XX:+UseSerialGC")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
