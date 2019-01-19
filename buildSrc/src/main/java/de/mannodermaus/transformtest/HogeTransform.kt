package de.mannodermaus.transformtest

import com.android.SdkConstants.DOT_CLASS
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import javassist.ClassPool
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.gradle.api.Project
import java.io.File

class HogeTransform(private val project: Project) : Transform() {
    override fun getName() = "hoge"

    override fun getInputTypes() = setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun isIncremental() = false

    override fun getScopes() = mutableSetOf(QualifiedContent.Scope.PROJECT)

//    override fun getReferencedScopes() = mutableSetOf(
//        QualifiedContent.Scope.EXTERNAL_LIBRARIES,
//        QualifiedContent.Scope.SUB_PROJECTS,
//        QualifiedContent.Scope.TESTED_CODE
//    )

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        // Javassist Bridge; used to look up classes & modify bytecode.
        // Provide it with all inputs to the Transform, and manually ppend android.jar to class pool.
        val classPool = ManagedClassPool(transformInvocation.inputs, transformInvocation.referencedInputs)
        addBootClassesToClassPool(classPool)

        val outputProvider = transformInvocation.outputProvider

        // Aggregate ALL class files that are eligible for replacements
        val allClassFiles = mutableListOf<String>()

        // Iterate over all inputs
        for (input in transformInvocation.inputs) {
            for (directory in input.directoryInputs) {
                val inputPath = directory.file.absolutePath
                project.logger.info("Scanning Directory: $inputPath")

                // TODO Incremental
//                if (directory.changedFiles.isNotEmpty()) {
//                    println("|__ Changed Files:")
//                    for (changedFile in directory.changedFiles) {
//                        project.logger.info("|_____ (${changedFile.value}) ${changedFile.key}")
//                    }
//                }

                directory.file.walkTopDown()
                    .forEach {
                        if (it.absolutePath.endsWith(DOT_CLASS)) {
                            // Keep track of the fully qualified class name for this file
                            val fqcn = it.absolutePath
                                .substring(inputPath.length + 1, it.absolutePath.length - DOT_CLASS.length)
                                .replace(File.separator, ".")
//                            println("|_____ $fqcn")
                            allClassFiles += fqcn
                        }
                    }
            }

            // TODO JAR Inputs
//            for (jar in input.jarInputs) {
//                println("|__ (${jar.status}) Scanning JAR: ${jar.file.absolutePath}")
//            }
        }

        // Get output directory for this input
        val outputDir = outputProvider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY)

        // Transform all classes that utilize "List.of()";
        // even if they don't, copy them over to the output folder.
        // Otherwise, the files would be lost!
        allClassFiles.map { classPool.get(it) }.forEach { clazz ->
            // Using javassist, there are a lot of possibilities to modify class files.
            // The API is quite similar to java.lang.reflect (i.e. getDeclaredFields(), getMethods(), ...),
            // but for our example, we need to instrument the entire class. Using an ExprEditor,
            // it's possible to intercept all expressions in each class, which allows us to find out
            // where exactly List.of() might be called! These visitors are similar to the JetBrains PSI API,
            // widely utilized in annotation processing.
            clazz.instrument(object : ExprEditor() {
                override fun edit(m: MethodCall) {
                    super.edit(m)

                    // Find call sites to List.of()
                    if (m.className == "java.util.List" && m.methodName == "of") {
                        // Found a match! Construct the bytecode to replace the original call site using the Javassist syntax.
                        val method = m.method
                        val paramCount = method.parameterTypes.size

                        // Special case handling for List.of(Object[]),
                        // because paramCount == 1, but it's actually more than 1 item in the list
                        val replacement: String =
                            if (paramCount == 1 && method.parameterTypes[0].name == "java.lang.Object[]") {
                                // List.of() with more than 10 parameters
                                StringBuilder().apply {
                                    append("\$_ = new java.util.ArrayList();")
                                    append("\$_.addAll(java.util.Arrays.asList(\$1));")
                                    append("\$_ = java.util.Collections.unmodifiableList(\$_);")
                                }.toString()

                            } else if (paramCount == 0) {
                                // List.of() with no argument (i.e. empty list)
                                "\$_ = java.util.Collections.emptyList();"

                            } else {
                                // Other usages of List.of()
                                StringBuilder().apply {
                                    append("\$_ = new java.util.ArrayList($paramCount);")
                                    for (i in 1..paramCount) {
                                        append("\$_.add(\$${i});")
                                    }
                                    append("\$_ = java.util.Collections.unmodifiableList(\$_);")
                                }.toString()
                            }
                        m.replace(replacement.toString())

                        project.logger.info("${m.enclosingClass.name}:L${m.lineNumber}: List.of()")
                    }
                }
            })
            clazz.writeFile(outputDir.canonicalPath)
        }
    }

    /**
     * There is no official way to get the path to android.jar for transform.
     * See https://code.google.com/p/android/issues/detail?id=209426
     */
    private fun addBootClassesToClassPool(classPool: ClassPool) {
        try {
            project.getBootClasspath().forEach {
                val path: String = it.absolutePath
                classPool.appendClassPath(path)
            }
        } catch (e: Exception) {
            // Just log it. It might not impact the transforming if the method which needs to be transformer doesn't
            // contain classes from android.jar.
            project.logger.debug("Cannot get bootClasspath caused by: ", e)
        }
    }
}
