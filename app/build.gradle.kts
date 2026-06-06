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
        versionName = "06.06.2026b"
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
    // --- ВНУТРЕННИЕ МОДУЛИ ПРОЕКТА ---
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:chats"))

    // --- ПЛАТФОРМЫ (BOM) ---
    // Firebase BOM (управляет версиями всех сервисов Firebase)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    // Compose BOM (управляет версиями Jetpack Compose)
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))

    // --- GOOGLE & FIREBASE ---
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // --- JETPACK COMPOSE & ANDROID UI ---
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.compose.material3:material3") // Версия подтянется из BOM автоматически
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- LIFECYCLE & CORE ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Сетевой клиент Ktor и Сериализация
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // --- COIL (ЗАГРУЗКА ИЗОБРАЖЕНИЙ) ---
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- LOCAL DATABASE (ROOM) ---
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:3.5.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.6.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.6.0")
    implementation("io.github.jan-tennert.supabase:functions-kt:3.6.0")
}





















