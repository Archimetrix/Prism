package com.prism.music

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * If Prism crashes, what went wrong is written to a file on the phone (files/crashes), kept for the last few crashes,
 * so it can be read later: Settings → About → Diagnostics shows the last one and Share diagnostics includes them. A
 * crash's details otherwise only show over adb, and a crash that can't be reproduced can't be fixed (owner, 1.7.2: "it
 * crashed once while exiting the player"). Nothing is sent anywhere; the listener decides whether to share it.
 */
object CrashLog {
    private const val KEEP = 3
    private const val MAX_CHARS = 48_000

    private fun dir(context: Context) = File(context.filesDir, "crashes")

    /** Installed first thing at start-up, in front of Android's own handler (which still shows the crash dialog). */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val d = dir(context).apply { mkdirs() }
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val text = buildString {
            append("Prism ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.APPLICATION_ID}\n")
            append("Phone: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            append("When: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
            append("Thread: ${thread.name}\n\n")
            append(stack.take(MAX_CHARS))
        }
        File(d, "crash-${System.currentTimeMillis()}.txt").writeText(text)
        // Only the last few.
        d.listFiles()?.sortedByDescending { it.name }?.drop(KEEP)?.forEach { it.delete() }
    }

    /** The saved crashes, newest first. */
    fun all(context: Context): List<File> =
        dir(context).listFiles()?.filter { it.name.startsWith("crash-") }?.sortedByDescending { it.name }.orEmpty()

    /** When the newest one happened (ms), or null when there's none. */
    fun lastAt(context: Context): Long? = all(context).firstOrNull()?.name?.removePrefix("crash-")?.removeSuffix(".txt")?.toLongOrNull()

    /** Every saved crash, for Share diagnostics. */
    fun text(context: Context): String = all(context).joinToString("\n\n") { f -> "--- ${f.name} ---\n" + runCatching { f.readText() }.getOrDefault("") }

    fun clear(context: Context) { all(context).forEach { it.delete() } }
}
