package de.mannodermaus.transformtest

import com.android.build.api.transform.TransformInput
import javassist.ClassPool

class ManagedClassPool(inputs: Collection<TransformInput>, referencedInputs: Collection<TransformInput>) : ClassPool() {

    init {
        appendSystemPath()

        inputs.forEach { input ->
            input.directoryInputs.forEach {
                appendClassPath(it.file.absolutePath)
            }

            input.jarInputs.forEach {
                appendClassPath(it.file.absolutePath)
            }
        }

        referencedInputs.forEach { referencedInput ->
            referencedInput.directoryInputs.forEach {
                appendClassPath(it.file.absolutePath)
            }

            referencedInput.jarInputs.forEach {
                appendClassPath(it.file.absolutePath)
            }
        }
    }
}