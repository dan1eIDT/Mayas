plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.dan1eidtj.mayas.feature.call"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    composeOptions {
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(project(":core:ui"))

    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.animation.core)
    implementation(libs.androidx.foundation)
    implementation(libs.firebase.auth)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.webrtc.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.supabase.bom))
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(libs.supabase.postgrest.kt)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.supabase.storage.kt)
    implementation(libs.supabase.realtime.kt)
    implementation(libs.supabase.functions.kt)
}
