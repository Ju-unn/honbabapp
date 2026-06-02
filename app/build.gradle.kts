import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.honbabapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.honbabapp"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties().apply {
            load(project.rootProject.file("local.properties").inputStream())
        }

        buildConfigField(
            "String",
            "NAVER_CLIENT_ID",
            "\"${properties.getProperty("NAVER_CLIENT_ID")}\""
        )

        buildConfigField(
            "String",
            "NAVER_CLIENT_SECRET",
            "\"${properties.getProperty("NAVER_CLIENT_SECRET")}\""
        )

        buildConfigField(
            "String",
            "NAVER_MAPS_CLIENT_ID",
            "\"${properties.getProperty("NAVER_MAPS_CLIENT_ID", "")}\""
        )

        buildConfigField(
            "String",
            "NAVER_MAPS_CLIENT_SECRET",
            "\"${properties.getProperty("NAVER_MAPS_CLIENT_SECRET", "")}\""
        )

        manifestPlaceholders["NAVER_MAPS_CLIENT_ID"] =
            properties.getProperty("NAVER_MAPS_CLIENT_ID", "")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

}

dependencies {
    val retrofitVersion = "2.9.0"
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.naver.maps:map-sdk:3.23.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.recyclerview)

    implementation("io.socket:socket.io-client:2.1.0")

    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-messaging:25.0.1")
}