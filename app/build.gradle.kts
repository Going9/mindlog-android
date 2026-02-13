plugins {
    alias(libs.plugins.android.application)
}

val devBaseUrl = (project.findProperty("MINDLOG_DEV_BASE_URL") as String?) ?: "https://localhost:8443"

android {
    namespace = "com.mindlog.android"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.mindlog.android"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://www.mindlog.blog\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("dev.hotwire:core:1.2.5")
    implementation("dev.hotwire:navigation-fragments:1.2.5")
    implementation("androidx.browser:browser:1.8.0")
}
