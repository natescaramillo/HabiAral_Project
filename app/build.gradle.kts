plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.habiaral"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.habiaral"
        minSdk = 29
        targetSdk = 35
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Firebase BoM (manages Firebase versions)
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))

    // Firebase modules
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-firestore:24.10.1")
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    implementation ("com.android.volley:volley:1.2.1")
    implementation("com.google.android.material:material:1.12.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // If you want OkHttp logging (optional, useful for debugging)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // JSON handling (if not already in your project)
    implementation ("org.json:json:20230227")

}
apply(plugin = "com.google.gms.google-services")
