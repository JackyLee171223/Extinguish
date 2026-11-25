plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    //alias(libs.plugins.parcelize)
    id(libs.plugins.parcelize.get().pluginId)
}

android {
    namespace = "extinguish.shizuku_service"
    compileSdk = 37
    compileSdkMinor = 0

    buildFeatures {
        buildConfig = true
        aidl = true
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(project(":hidden-api"))
    implementation(project(":ipc"))
    // Reuse the single canonical AIDL interface set from root-service so that
    // the app layer uses one binder type regardless of the privileged method.
    implementation(project(":root-service"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

}
