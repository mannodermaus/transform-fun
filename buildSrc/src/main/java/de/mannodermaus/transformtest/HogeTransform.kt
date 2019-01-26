package de.mannodermaus.transformtest

import com.android.SdkConstants.DOT_CLASS
import com.android.build.api.transform.Format.DIRECTORY
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.gradle.api.Project
import java.io.File

class HogeTransform(project: Project) : Transform() {

    private val logger = project.logger

    /* Transform API */

    override fun getName() = "hoge"

    override fun getInputTypes() = setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun isIncremental() = false

    override fun getScopes() = mutableSetOf(QualifiedContent.Scope.PROJECT)

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        // Aggregate ALL class files that are eligible for replacements
        val allClassFiles = findClasses(transformInvocation)

        // Find output directory
        val outputDir = transformInvocation.outputProvider.getContentLocation(name, outputTypes, scopes, DIRECTORY)
        log("Output Directory: ${outputDir.absolutePath}")

        // Transform all classes that utilize "List.of()";
        // even if they don't, copy them over to the output folder.
        // Otherwise, the files would be lost!
        allClassFiles.forEach { clazz ->
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
                        val replacement = when {
                            paramCount == 1 && method.parameterTypes[0].name == "java.lang.Object[]" -> {
                                // static <E> List<E> of(E... elements)
                                StringBuilder().apply {
                                    append("\$_ = new java.util.ArrayList();")
                                    append("\$_.addAll(java.util.Arrays.asList(\$1));")
                                    append("\$_ = java.util.Collections.unmodifiableList(\$_);")
                                }.toString()
                            }

                            paramCount == 0 -> {
                                // static <E> List<E> of()
                                "\$_ = java.util.Collections.emptyList();"
                            }

                            else -> {
                                // static <E> List<E> of(E e1)
                                // static <E> List<E> of(E e1, E e2)
                                // static <E> List<E> of(E e1, E e2, E e3)
                                // static <E> List<E> of(E e1, E e2, E e3, E e4)
                                // static <E> List<E> of(E e1, E e2, E e3, E e4, E e5)
                                // static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6)
                                // static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7)
                                // static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8)
                                // static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9)
                                // static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10)
                                StringBuilder().apply {
                                    append("\$_ = new java.util.ArrayList($paramCount);")
                                    for (i in 1..paramCount) {
                                        append("\$_.add(\$$i);")
                                    }
                                    append("\$_ = java.util.Collections.unmodifiableList(\$_);")
                                }.toString()
                            }
                        }
                        
                        m.replace(replacement)
                        log("${m.enclosingClass.name}:L${m.lineNumber}: List.of()")
                    }
                }
            })

            clazz.writeFile(outputDir.canonicalPath)
        }
    }

    /* Private */
    
    private fun log(message: String) = logger.info("[HogeTransform] $message")

    private fun createClassPool(transformInvocation: TransformInvocation) =
        ManagedClassPool(transformInvocation.inputs, transformInvocation.referencedInputs)

    private fun findClasses(transformInvocation: TransformInvocation): List<CtClass> {
        // Aggregate ALL class files that are eligible for replacements
        val allClassFiles = mutableListOf<CtClass>()

        // Javassist Bridge; used to look up classes & modify bytecode
        val classPool = createClassPool(transformInvocation)

        // Iterate over all inputs
        for (input in transformInvocation.inputs) {
            for (directory in input.directoryInputs) {
                val inputPath = directory.file.absolutePath
                log("Input Directory: $inputPath")

                // TODO Incremental
//                if (directory.changedFiles.isNotEmpty()) {
//                    println("|__ Changed Files:")
//                    for (changedFile in directory.changedFiles) {
//                        log("|_____ (${changedFile.value}) ${changedFile.key}")
//                    }
//                }

                directory.file.walkTopDown()
                    .forEach {
                        if (it.absolutePath.endsWith(DOT_CLASS)) {
                            // Keep track of the fully qualified class name for this file
                            // (subtract the ".class" suffix)
                            val fqcn = it.absolutePath
                                .substring(inputPath.length + 1, it.absolutePath.length - DOT_CLASS.length)
                                .replace(File.separator, ".")
                            allClassFiles += classPool.get(fqcn)
                        }
                    }
            }

            // TODO JAR Inputs
//            for (jar in input.jarInputs) {
//                log("|__ (${jar.status}) Scanning JAR: ${jar.file.absolutePath}")
//            }
        }

        return allClassFiles.toList()
    }
}
