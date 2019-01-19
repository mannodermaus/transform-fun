repositories {
    google()
    jcenter()
}

plugins {
    `kotlin-dsl`
}

val agp_version = "3.4.0-alpha10"
val javassist_version = "3.24.0-GA"

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle-api:$agp_version")
    implementation("com.android.tools.build:gradle:$agp_version")

    implementation("org.javassist:javassist:$javassist_version")
}
