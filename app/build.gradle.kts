plugins {
        id("com.android.application")
        id("com.google.gms.google-services")

}

android {
    namespace = "com.photospot.fotospotapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.photospot.fotospotapp"
        minSdk = 31
        targetSdk = 35
        versionCode = 5
        versionName = "Alpha 3.0"

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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions:20.3.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.maps.android:android-maps-utils:2.2.5")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    implementation("com.tbuonomo:dotsindicator:4.2")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("androidx.preference:preference:1.2.1")



    implementation("com.android.billingclient:billing:6.1.0")


    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")



}