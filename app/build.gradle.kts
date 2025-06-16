plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.schedo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.schedo"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES", // Konflik sebelumnya
                "META-INF/INDEX.LIST"    // Konflik baru
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.compose.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material:material:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Retrofit + GSON Converter
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutine untuk async request
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation(platform("androidx.compose:compose-bom:2024.03.00"))

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.navigation:navigation-compose:2.7.6")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation("androidx.compose.runtime:runtime")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("br.com.devsrsouza.compose.icons:font-awesome:1.1.0")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.30.1")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    implementation ("androidx.compose.ui:ui:1.5.0") // atau versi lebih baru
    implementation ("androidx.compose.material3:material3:1.2.0")
    implementation ("androidx.compose.foundation:foundation:1.5.0")
    implementation ("androidx.compose.runtime:runtime:1.5.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation ("com.google.code.gson:gson:2.10.1")

    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation ("com.google.http-client:google-http-client-android:1.43.3")
    implementation ("com.google.api-client:google-api-client-android:2.2.0")

    implementation ("com.google.auth:google-auth-library-oauth2-http:1.23.0")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("io.coil-kt:coil-compose:2.4.0")
    implementation ("com.google.firebase:firebase-auth:23.0.0")
    implementation ("com.google.android.gms:play-services-auth:21.0.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
    implementation ("androidx.compose.ui:ui:1.5.4")// Sesuaikan versi dengan yang terbaru
    implementation ("androidx.compose.material3:material3:1.2.0")// Untuk Material3 components
    implementation ("androidx.compose.runtime:runtime:1.5.4")
    implementation ("androidx.compose.foundation:foundation:1.5.4")
    implementation ("androidx.activity:activity-compose:1.8.2" )// Untuk NavHostController
    implementation ("androidx.navigation:navigation-compose:2.7.6")
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:2.0.4")
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:1.1.5")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp for multipart file uploads
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")

    // Optional: Logging interceptor for debugging API calls
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0") // Pastikan versi terbaru
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation ("androidx.compose.material3:material3:1.2.0")
    implementation ("androidx.core:core-ktx:1.12.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.9.0")
}
