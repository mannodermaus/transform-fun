package de.mannodermaus.transformtest

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import java.io.File

fun Project.getBootClasspath(): List<File> {
    val android = extensions.getByName("android") as BaseExtension
    return android.bootClasspath
}
