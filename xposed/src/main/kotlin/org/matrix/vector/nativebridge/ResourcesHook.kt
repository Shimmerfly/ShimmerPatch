package org.matrix.vector.nativebridge

import android.content.res.Resources

object ResourcesHook {
    @JvmStatic external fun initXResourcesNative(): Boolean

    @JvmStatic external fun makeInheritable(clazz: Class<*>): Boolean

    @JvmStatic
    external fun buildDummyClassLoader(
        parent: ClassLoader,
        resourceSuperClass: String,
        typedArraySuperClass: String,
    ): ClassLoader

    // Not @FastNative: the implementation walks a whole binary XML document and calls back into
    // Java once per attribute, and a fast transition leaves the thread runnable for all of it, so
    // anything waiting to suspend threads waits for the document.
    @JvmStatic
    external fun rewriteXmlReferencesNative(parserPtr: Long, origRes: Any, repRes: Resources)
}
