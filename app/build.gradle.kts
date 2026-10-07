import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

// Signature de la version de production : `keystore.properties` (à la racine, jamais versionné) donne le fichier de clé et ses
// mots de passe. Sans lui (autre machine, intégration continue), la version de production se construit simplement non signée.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "fr.conscience.numerique"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.conscience.numerique"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.2.1"
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
        // Les schémas JSON de Room servent aux tests de migration sur appareil.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    testOptions {
        unitTests.all {
            // StringResourcesTest lit les fichiers de traduction : sans ceci, Gradle ne relance pas les tests quand ils changent.
            it.inputs.dir("src/main/res")
        }
    }

    lint {
        // local.properties est propre à chaque machine et n'est pas versionné : ce contrôle n'a rien à vérifier dans le dépôt
        // (et il signale à tort un fichier déjà correctement échappé).
        disable += "PropertyEscape"
    }
}

// room-testing (tests de migration) lit les schémas JSON avec kotlinx-serialization 1.8 alors que le reste de l'app est épinglé en 1.7.3 :
// mélanger les deux versions plante (AbstractMethodError). On aligne tout sur 1.8.1, uniquement pour les tests sur appareil.
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
