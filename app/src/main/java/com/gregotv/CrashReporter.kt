package com.gregotv

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Writes the stack trace of any uncaught exception to a file the user can read
 * from Settings, then hands control back to the platform's default handler so
 * the process still dies and the system crash dialog still shows. On an Android
 * TV box with no attached computer, this is the only way to see why the app
 * closed itself.
 */
object CrashReporter {

    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, thread, error) }
            // Chain to the original handler so behaviour is otherwise unchanged.
            previous?.uncaughtException(thread, error)
        }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
        val text = buildString {
            appendLine("GregoTV crash")
            appendLine("when: ${System.currentTimeMillis()}")
            appendLine("thread: ${thread.name}")
            appendLine("device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("---")
            append(trace)
        }
        File(context.filesDir, FILE).writeText(text)
    }

    /** The last recorded crash, or null if there is none. */
    fun lastCrash(context: Context): String? =
        File(context.applicationContext.filesDir, FILE)
            .takeIf { it.exists() }
            ?.readText()

    fun clear(context: Context) {
        File(context.applicationContext.filesDir, FILE).delete()
    }
}
