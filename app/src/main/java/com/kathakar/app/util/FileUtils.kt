package com.kathakar.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

object FileUtils {

    const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024L  // 2 MB
    const val MAX_WORD_COUNT      = 10_000
    val SUPPORTED_MIME_TYPES      = arrayOf(
        "text/plain",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )

    data class FileReadResult(
        val text: String = "",
        val wordCount: Int = 0,
        val fileName: String = "",
        val error: String? = null
    ) {
        val isSuccess get() = error == null && text.isNotBlank()
    }

    // ── Main entry point ──────────────────────────────────────────────────────
    fun readFile(context: Context, uri: Uri): FileReadResult {
        return try {
            val fileName = getFileName(context, uri)
            val fileSize = getFileSize(context, uri)

            // Check file size first
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return FileReadResult(error = "File too large. Maximum size is 2 MB. " +
                    "Please split your chapter into smaller parts.")
            }

            val mimeType = context.contentResolver.getType(uri) ?: ""
            val text = when {
                mimeType == "text/plain" ||
                fileName.endsWith(".txt", ignoreCase = true) ->
                    readTxtFile(context, uri)

                mimeType.contains("wordprocessingml") ||
                fileName.endsWith(".docx", ignoreCase = true) ->
                    readDocxFile(context, uri)

                else -> return FileReadResult(
                    error = "Unsupported file type. Please use .docx or .txt files only.")
            }

            val cleaned   = cleanText(text)
            val wordCount = countWords(cleaned)

            if (wordCount > MAX_WORD_COUNT) {
                return FileReadResult(
                    error = "Chapter is too long ($wordCount words). " +
                        "Maximum is $MAX_WORD_COUNT words per chapter. " +
                        "Please divide your story into smaller chapters.")
            }

            if (cleaned.isBlank()) {
                return FileReadResult(error = "File appears to be empty. Please check the file and try again.")
            }

            FileReadResult(text = cleaned, wordCount = wordCount, fileName = fileName)

        } catch (e: Exception) {
            FileReadResult(error = "Could not read file: " + (e.localizedMessage ?: "Unknown error"))
        }
    }

    // ── .txt reader ───────────────────────────────────────────────────────────
    private fun readTxtFile(context: Context, uri: Uri): String {
        val stream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")
        return stream.use { it.bufferedReader(Charsets.UTF_8).readText() }
    }

    // ── .docx reader using Apache POI ─────────────────────────────────────────
    private fun readDocxFile(context: Context, uri: Uri): String {
        val stream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")
        return stream.use { inputStream ->
            val doc = XWPFDocument(inputStream)
            val sb  = StringBuilder()
            doc.paragraphs.forEach { para ->
                val text = para.text.trim()
                if (text.isNotEmpty()) {
                    sb.append(text)
                    sb.append("\n")
                }
            }
            doc.close()
            sb.toString()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun cleanText(raw: String): String {
        return raw
            .replace("\r\n", "\n")   // Windows line endings
            .replace("\r",   "\n")   // Old Mac line endings
            .replace("\u00A0", " ")  // Non-breaking spaces
            .lines()
            .joinToString("\n") { it.trim() }   // Trim each line
            .replace(Regex("\n{3,}"), "\n\n")   // Max 2 consecutive blank lines
            .trim()
    }

    fun countWords(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && idx >= 0) size = cursor.getLong(idx)
        }
        return size
    }
}
