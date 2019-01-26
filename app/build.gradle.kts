import de.mannodermaus.transformtest.HogeTransform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    // Using the hacked android.jar, which includes JDK 9 methods
    compileSdkVersion("android-1000")
    defaultConfig {
        applicationId = "de.mannodermaus.transformtest"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Register the transform with this API.
    // (if you distribute a custom Transform, this should be done in a Plugin class;
    // see Retrolambda or Realm for references)
    // If this line is commented out, the app will crash on startup with "NoSuchMethodError"!
    registerTransform(HogeTransform(project))
}

tasks.withType(KotlinCompile::class).configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk7", "1.3.20"))
}
