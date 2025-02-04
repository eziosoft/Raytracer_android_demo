package com.example.fps_raytrace.engine.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class LogFileHelper(private val context: Context) {

    private val logFile: File = File(context.filesDir, "app_logs.txt")
    private val bufferSize = 8 * 1024 // 8KB buffer size

    init {
        clearLogFile()
    }

    /**
     * Clears the log file.
     */
    private fun clearLogFile() {
        if (logFile.exists()) {
            logFile.delete()
        }
    }

    /**
     * Writes a log message to the file with buffered writing.
     */
    fun writeLog(message: String) {
        try {
            BufferedWriter(FileWriter(logFile, true), bufferSize).use { writer ->
                writer.appendLine(message)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Shares the log file using Android's share intent.
     */
    fun shareLogFile() {
        if (!logFile.exists()) {
            return // No file to share
        }

        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            logFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Log File"))
    }
}
