repositories {
    google()
    jcenter()
}

plugins {
    `kotlin-dsl`
}

val agpVersion = "3.4.0-beta02"
val javassistVersion = "3.24.0-GA"

dependencies {
    // Transform API is contained here
    implementation("com.android.tools.build:gradle-api:$agpVersion")

    // Bytecode manipulation API, similar to Java's Reflection & Lint's UAST
    implementation("org.javassist:javassist:$javassistVersion")
}
