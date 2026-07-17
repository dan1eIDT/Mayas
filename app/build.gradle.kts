import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dan1eidtj.mayas"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.dan1eidtj.mayas"
        minSdk = 24
        versionCode = 1
        versionName = "14.07.2026b"
        multiDexEnabled = true
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        sourceSets {
            getByName("main") {
                @Suppress("DEPRECATION")
                java.srcDirs("src/main/java", "src/main/kotlin")
            }
        }
        buildConfigField("String", "SUPABASE_URL", "\"${properties.getProperty("supabaseUrlP")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${properties.getProperty("supabaseKeyP")}\"")
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

}
dependencies {
    // Хуйня подзаборная
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:chats"))
    implementation(project(":feature:call"))

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))

    // гугл а вы когда признаетесь что вы у меня удалили уже 2 акка? еще и деньги просили. Уроды
    implementation(platform(libs.androidx.compose.bom))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // джетпак джоурайд
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Сетевой клиент Ktor и Сериализация
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Коил коил коил сфчик
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")


    // РЕКЛАМА)))
    implementation("com.yandex.android:mobileads:7.16.1") // Яндех
    implementation("com.google.android.gms:play-services-ads:24.7.0") // гугле

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.6.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.6.0")
    implementation("io.github.jan-tennert.supabase:functions-kt:3.6.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // webrtc блять
    implementation("io.github.webrtc-sdk:android:144.7559.05")

    implementation("androidx.multidex:multidex:2.0.1")

    implementation("androidx.fragment:fragment:1.8.2")

}





















