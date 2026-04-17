package com.harsh.attandancesystem.util

import android.content.ContentResolver
import android.net.Uri
import com.harsh.attandancesystem.data.local.Student
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object SpreadsheetImportUtils {
    private val nameAliases = listOf("name", "studentname", "fullname", "studentfullname")
    private val classAliases = listOf("classname", "class", "classsection", "section", "standard", "std", "semester", "sem")
    private val emailAliases = listOf("email", "emailid", "mail", "mailid", "userid", "username")
    private val passwordAliases = listOf("password", "pass", "passwd", "pwd", "loginpassword")

    @JvmStatic
    fun importStudents(contentResolver: ContentResolver, uri: Uri): List<Student> {
        return importStudents(contentResolver, uri, "")
    }

    @JvmStatic
    fun importStudents(contentResolver: ContentResolver, uri: Uri, forcedClassName: String): List<Student> {
        val originalFileName = resolveFileName(contentResolver, uri)
        val fileName = originalFileName.lowercase(Locale.getDefault())
        val fallbackClassName = forcedClassName.trim().ifBlank { buildFallbackClassName(originalFileName) }
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            if (fileName.endsWith(".csv")) {
                parseCsv(inputStream.bufferedReader(), fallbackClassName)
            } else {
                parseXlsx(inputStream.readBytes(), fallbackClassName)
            }
        } ?: emptyList()
    }

    private fun resolveFileName(contentResolver: ContentResolver, uri: Uri): String {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index) ?: ""
                    }
                }
            }
        return uri.lastPathSegment ?: ""
    }

    private fun parseCsv(reader: BufferedReader, fallbackClassName: String): List<Student> {
        val rows = reader.readLines().filter { it.isNotBlank() }.map { parseCsvLine(it) }
        if (rows.isEmpty()) return emptyList()
        return buildStudents(rows, fallbackClassName)
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(char)
            }
            i++
        }
        values.add(current.toString().trim())
        return values
    }

    private fun parseXlsx(bytes: ByteArray, fallbackClassName: String): List<Student> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetPath = entries.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sorted()
            .firstOrNull()
            ?: return emptyList()

        val rows = parseSheet(entries[sheetPath] ?: return emptyList(), sharedStrings)
        return buildStudents(rows, fallbackClassName)
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val values = mutableListOf<String>()
        try {
            val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = builder.parse(bytes.inputStream())
            val nodes = document.getElementsByTagName("si")
            for (i in 0 until nodes.length) {
                val item = nodes.item(i)
                values.add(item.textContent.orEmpty())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return values
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val parser = android.util.Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")

        val rows = mutableListOf<List<String>>()
        var eventType = parser.eventType
        var currentRow = mutableMapOf<Int, String>()
        var currentCellType: String? = null
        var currentCellColumn = -1
        var insideValue = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableMapOf()
                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t")
                            currentCellColumn = columnIndex(parser.getAttributeValue(null, "r"))
                        }
                        "v", "t" -> insideValue = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideValue && currentCellColumn >= 0) {
                        val rawValue = parser.text.orEmpty()
                        val value = if (currentCellType == "s") {
                            sharedStrings.getOrNull(rawValue.toIntOrNull() ?: -1).orEmpty()
                        } else {
                            rawValue
                        }
                        currentRow[currentCellColumn] = value
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                val maxIndex = currentRow.keys.maxOrNull() ?: -1
                                rows.add(List(maxIndex + 1) { index -> currentRow[index].orEmpty() })
                            }
                        }
                        "c" -> {
                            currentCellType = null
                            currentCellColumn = -1
                        }
                        "v", "t" -> insideValue = false
                    }
                }
            }
            eventType = parser.next()
        }

        return rows
    }

    private fun columnIndex(cellRef: String?): Int {
        if (cellRef.isNullOrBlank()) return -1
        var index = 0
        for (char in cellRef) {
            if (!char.isLetter()) break
            index = index * 26 + (char.uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }

    private fun buildStudents(rows: List<List<String>>, fallbackClassName: String): List<Student> {
        if (rows.isEmpty()) return emptyList()
        
        val headerRow = rows.first()
        val headerMap = mutableMapOf<String, Int>()
        
        headerRow.forEachIndexed { index, header ->
            val normalized = normalizeHeader(header)
            when {
                nameAliases.contains(normalized) -> headerMap["name"] = index
                classAliases.contains(normalized) -> headerMap["classname"] = index
                emailAliases.contains(normalized) -> headerMap["email"] = index
                passwordAliases.contains(normalized) -> headerMap["password"] = index
                normalized.contains("roll") -> headerMap["rollno"] = index
                normalized.contains("div") -> headerMap["division"] = index
                normalized.contains("branch") -> headerMap["branch"] = index
                normalized.contains("enroll") -> headerMap["enrollmentno"] = index
                normalized.contains("mobile") || normalized.contains("phone") -> headerMap["mobileno"] = index
                normalized.contains("abc") -> headerMap["abcid"] = index
            }
        }

        val missing = mutableListOf<String>()
        if (!headerMap.containsKey("name")) missing.add("Name")
        if (!headerMap.containsKey("classname") && fallbackClassName.isBlank()) missing.add("Class/Section")

        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("Sheet is missing columns: ${missing.joinToString(", ")}")
        }

        return rows.drop(1)
            .mapNotNull { row ->
                val name = getCell(row, headerMap, "name")
                val className = if (fallbackClassName.isNotBlank()) {
                    fallbackClassName
                } else {
                    getCell(row, headerMap, "classname")
                }
                val email = resolveEmail(row, headerMap, name)
                val password = resolvePassword(row, headerMap, name, email)

                if (name.isBlank() || className.isBlank()) {
                    null
                } else {
                    Student(
                        name = name,
                        className = className,
                        email = email,
                        password = password,
                        rollNo = getCell(row, headerMap, "rollno"),
                        division = getCell(row, headerMap, "division"),
                        branch = getCell(row, headerMap, "branch"),
                        enrollmentNo = getCell(row, headerMap, "enrollmentno"),
                        mobileNo = getCell(row, headerMap, "mobileno"),
                        abcId = getCell(row, headerMap, "abcid")
                    )
                }
            }
    }

    private fun getCell(row: List<String>, headerMap: Map<String, Int>, key: String): String {
        val index = headerMap[key] ?: return ""
        return row.getOrNull(index)?.trim().orEmpty()
    }

    private fun resolvePassword(
        row: List<String>,
        headerMap: Map<String, Int>,
        name: String,
        email: String
    ): String {
        val explicitPassword = getCell(row, headerMap, "password")
        if (explicitPassword.isNotBlank()) {
            return explicitPassword
        }

        val mobile = getCell(row, headerMap, "mobileno")
        if (mobile.isNotBlank()) {
            return mobile.takeLast(6)
        }

        val enrollment = getCell(row, headerMap, "enrollmentno")
        if (enrollment.isNotBlank()) {
            return enrollment.takeLast(6)
        }

        val emailPrefix = email.substringBefore("@").trim()
        if (emailPrefix.isNotBlank()) {
            return emailPrefix
        }

        return name.lowercase(Locale.getDefault()).replace(" ", "")
    }

    private fun resolveEmail(
        row: List<String>,
        headerMap: Map<String, Int>,
        name: String
    ): String {
        val explicitEmail = getCell(row, headerMap, "email")
        if (explicitEmail.isNotBlank()) {
            return explicitEmail
        }

        val enrollment = getCell(row, headerMap, "enrollmentno")
        if (enrollment.isNotBlank()) {
            return "${sanitizeToken(enrollment)}@student.local"
        }

        val mobile = getCell(row, headerMap, "mobileno")
        if (mobile.isNotBlank()) {
            return "${sanitizeToken(mobile)}@student.local"
        }

        return "${sanitizeToken(name)}@student.local"
    }

    private fun sanitizeToken(value: String): String {
        val cleaned = value.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9]+"), "")
        return if (cleaned.isBlank()) "student" else cleaned
    }

    private fun normalizeHeader(header: String): String {
        return header.lowercase(Locale.getDefault())
            .replace(" ", "")
            .replace("_", "")
            .replace("/", "")
            .replace(".", "")
    }

    private fun buildFallbackClassName(fileName: String): String {
        val baseName = fileName.substringBeforeLast(".")
        return baseName
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
