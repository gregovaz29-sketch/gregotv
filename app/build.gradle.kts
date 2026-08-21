plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.gregotv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gregotv"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // TMDB is optional. If no key is provided, the app runs without it.
        buildConfigField(
            "String",
            "TMDB_API_KEY",
            "\"${project.findProperty("TMDB_API_KEY") ?: ""}\""
        )
    }

    // A fixed signing key, on purpose. With no signingConfig, Gradle generates
    // a throwaway debug keystore on every CI runner, so each build carries a
    // different signature and Android refuses to install it over the previous
    // one. The only way through is to uninstall first, which wipes the
    // database — and that makes Room migrations impossible to ever test.
    // Proven: build-4 and the branch build ship different META-INF/CERT.RSA.
    //
    // Defaults to the checked-in debug keystore. Point GREGOTV_KEYSTORE at a
    // file written from a CI secret to sign with a private key instead;
    // nothing else needs to change.
    signingConfigs {
        create("shared") {
            storeFile = file(System.getenv("GREGOTV_KEYSTORE") ?: "gregotv-debug.keystore")
            storePassword = System.getenv("GREGOTV_KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("GREGOTV_KEY_ALIAS") ?: "gregotv"
            keyPassword = System.getenv("GREGOTV_KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose for TV
    implementation("androidx.tv:tv-material:1.0.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Media3 (ExoPlayer + HLS)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // SMB
    implementation("eu.agno3.jcifs:jcifs-ng:2.1.10")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore (settings)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Retrofit + serialization (optional TMDB, disabled without key)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
