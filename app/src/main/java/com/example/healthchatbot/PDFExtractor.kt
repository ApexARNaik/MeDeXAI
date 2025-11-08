package com.example.healthchatbot

import android.content.Context
import android.net.Uri
import com.example.healthchatbot.models.TestResult
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

class PDFExtractor {

    private val skipKeywords = listOf(
        "TEST PARAMETER", "REFERENCE RANGE", "RESULT", "UNIT",
        "Page", "Report Status", "Collected On", "Reported On",
        "Method:", "Automated", "Lab ID", "Registered On"
    )

    fun extractFromPdf(context: Context, uri: Uri): List<TestResult> {
        PDFBoxResourceLoader.init(context)

        val results = mutableListOf<TestResult>()
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(uri)
            document = PDDocument.load(inputStream)

            val stripper = PDFTextStripper()
            val text = stripper.getText(document)

            results.addAll(parseTestResults(text))

        } catch (e: Exception) {
            throw Exception("Failed to extract PDF: ${e.message}")
        } finally {
            document?.close()
            inputStream?.close()
        }

        return results.distinct()
    }

    private fun parseTestResults(text: String): List<TestResult> {
        val results = mutableListOf<TestResult>()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            if (shouldSkipLine(line)) {
                i++
                continue
            }

            if (isPotentialTestName(line)) {
                val testName = line

                for (j in i + 1 until minOf(i + 7, lines.size)) {
                    val nextLine = lines[j]

                    if (isResultValue(nextLine)) {
                        results.add(TestResult(
                            testName = cleanTestName(testName),
                            value = nextLine
                        ))
                        i = j
                        break
                    }
                }
            }

            i++
        }

        return results
    }

    private fun shouldSkipLine(line: String): Boolean {
        return skipKeywords.any { line.contains(it, ignoreCase = true) } ||
                line.length <= 2 ||
                line.all { it in "-:/" }
    }

    private fun isPotentialTestName(line: String): Boolean {
        if (line.length < 3 || !line[0].isUpperCase()) return false
        if (shouldSkipLine(line)) return false

        val letters = line.filter { it.isLetter() }
        if (letters.isEmpty()) return false

        val uppercaseRatio = letters.count { it.isUpperCase() }.toFloat() / letters.length
        return uppercaseRatio >= 0.5
    }

    private fun isResultValue(line: String): Boolean {
        return line.matches(Regex("^[\\d.]+$"))
    }

    private fun cleanTestName(name: String): String {
        return name.replace(Regex("\\s+"), " ")
            .trimEnd(':')
            .trim()
    }
}