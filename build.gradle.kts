// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
subprojects {
    configurations.configureEach {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("androidx.datastore:datastore-core-jvm"))
                    .using(module("androidx.datastore:datastore-core-android:1.1.1"))
            }
        }
    }
}
