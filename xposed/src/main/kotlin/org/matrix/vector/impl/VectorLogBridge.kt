package org.matrix.vector.impl

import android.util.Log

/** Optional log sink supplied by the embedding runtime. */
object VectorLogBridge {

    fun interface Sink {
        fun log(priority: Int, tag: String, message: String, throwable: Throwable?)
    }

    @Volatile private var sink: Sink? = null

    @JvmStatic
    fun setSink(newSink: Sink?) {
        sink = newSink
    }

    fun log(
        priority: Int,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val logcatMessage =
            if (throwable == null) message
            else "$message\n${Log.getStackTraceString(throwable)}"
        Log.println(priority, tag, logcatMessage)
        runCatching { sink?.log(priority, tag, message, throwable) }
    }
}
