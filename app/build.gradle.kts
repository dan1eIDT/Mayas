
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    namespace = "com.dan1eidtj.mayas"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.dan1eidtj.mayas"
        minSdk = 24
        versionCode = 1
        versionName = "01.06.2026b"
        multiDexEnabled = true
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    implementation ("androidx.compose.material3:material3:1.2.0")
    implementation ("io.coil-kt:coil-compose:2.5.0")
    implementation ("io.coil-kt:coil:2.6.0")
    implementation ("com.google.firebase:firebase-bom:32.7.0")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.firebase:firebase-firestore-ktx")
    implementation ("com.google.firebase:firebase-storage-ktx")
    implementation ("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:chats"))
    val room_version = "2.6.1" // или актуальная для твоего Gradle
    implementation  ("androidx.room:room-runtime:$room_version")
    implementation ("androidx.room:room-ktx:$room_version")



    // АЗАЗАХААЗАЗ СИКС СЕВЕН














    }


