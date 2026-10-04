// Deux apps ordinaires, sans code ni ressource, installées à côté de Red Flag par `scripts/test.sh` pendant les tests sur appareil :
// les écrans listent les apps non système, et un émulateur de base (surtout ancien) n'en a aucune. Jamais publiées.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "fr.conscience.numerique.fixture"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.conscience.numerique.fixture"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1"
    }

    flavorDimensions += "app"
    productFlavors {
        create("a") {
            dimension = "app"
            applicationIdSuffix = ".a"
            manifestPlaceholders["label"] = "Fixture A"
        }
        create("b") {
            dimension = "app"
            applicationIdSuffix = ".b"
            manifestPlaceholders["label"] = "Fixture B"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
