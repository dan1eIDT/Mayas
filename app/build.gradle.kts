// app

import org.gradle.kotlin.dsl.implementation
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dan1eidtj.mayas"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.dan1eidtj.mayas"
        minSdk = 24
        versionCode = 4
        versionName = "1.0"
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
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // модули проекта
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:chats"))
    implementation(project(":feature:call"))
    implementation(project(":feature:settings"))

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // Compose — один BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.process)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // сетевой клиент и сериализация
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)

    // coil
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // реклама
    implementation(libs.yandex.mobileads)
    implementation(libs.play.services.ads)

    // room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.kotlinx.coroutines.play.services)

    // webrtc
    implementation(libs.webrtc.android)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.fragment)

    // тесты
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.core:core-splashscreen:1.0.1")
}