import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

// Signing of the release build: `keystore.properties` (at the root, never committed) gives the key file and its passwords.
// Without it (another machine, continuous integration), the release build is simply built unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "app.redflag"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.redflag.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 14
        versionName = "0.5.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Experimental "usage access + overlay" variant: installable next to the accessibility version on the same phone.
            applicationIdSuffix = ".usage"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    sourceSets {
        // Room's JSON schemas are used by the migration tests on the device.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    testOptions {
        unitTests.all {
            // StringResourcesTest reads the translation files: without this, Gradle does not run the tests again when they change.
            it.inputs.dir("src/main/res")
        }
    }

    androidResources {
        // The app is only translated into English and French: the translations of the libraries (Material, AppCompat...) into
        // some 95 other languages would only weigh down the APK.
        localeFilters += listOf("en", "fr")
    }

    lint {
        // local.properties is specific to each machine and not committed: this check has nothing to verify in the repository
        // (and it wrongly flags a file that is already correctly escaped).
        disable += "PropertyEscape"
    }
}

// room-testing (migration tests) reads the JSON schemas with kotlinx-serialization 1.8 while the rest of the app is pinned to 1.7.3:
// mixing both versions crashes (AbstractMethodError). Everything is aligned on 1.8.1, for the device tests only.
configurations.matching { it.name.contains("AndroidTest", ignoreCase = true) }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization-")) {
            useVersion("1.8.1")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.android)
}
