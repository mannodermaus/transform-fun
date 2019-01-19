package de.mannodermaus.transformtest

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class HogePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val android = project.extensions.findByName("android") as? BaseExtension
            ?: throw IllegalStateException("This plugin needs to be added after an `android` plugin!")
        android.registerTransform(HogeTransform(project))
    }
}
