package com.pranay.jobtracker.domain

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pranay.jobtracker.BuildConfig
import java.text.SimpleDateFormat
import java.util.Locale

class RealEmailParserImpl : EmailParser {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val gson = GsonBuilder().setLenient().create()

    // RFC 2822 date format patterns used by Gmail headers
    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "d MMM yyyy HH:mm:ss Z"
    )

    override suspend fun parseEmailBatch(emails: List<RawEmailData>): List<ParsedEmailInfo> {
        if (emails.isEmpty()) return emptyList()

        val emailsText = emails.mapIndexed { index, email ->
            val actualDate = email.date
            val emailId = email.emailId
            """
            --- EMAIL ${index + 1} ---
            Email ID: $emailId
            Date: $actualDate
            From: ${email.from}
            Subject: ${email.subject}
            Body:
            ${email.body.take(500)}
            """.trimIndent()
        }.joinToString("\n\n")

        val prompt = """
            You are a helpful assistant that analyzes emails to track job applications.
            Extract the structured information from the following batch of emails.
            Respond ONLY with a valid JSON Array [] of objects matching this exact structure:
            [
              {
                "sourceEmailId": "The 'Email ID' specified for this email",
                "companyName": "Example Co",
                "jobTitle": "Engineer",
                "status": "Applied",
                "dateApplied": "Wed, 8 Apr",
                "recruiterEmail": "hr@example.com",
                "notes": "They received the application."
              }
            ]
            Do not include any markdown formatting like ```json or anything else. Just the raw JSON block.
            If an email is definitively NOT about a job application, strictly ignore it and do NOT include it in the array.

            EMAILS TO PROCESS:
            $emailsText
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )

            val text = response.text ?: return emptyList()
            val startIndex = text.indexOf("[")
            val endIndex = text.lastIndexOf("]")
            if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) return emptyList()

            val jsonText = text.substring(startIndex, endIndex + 1)
            if (jsonText == "[]") return emptyList()

            val listType = object : TypeToken<List<JobApplicationDto>>() {}.type
            val parsedList: List<JobApplicationDto> = gson.fromJson(jsonText, listType)

            parsedList.mapNotNull { dto ->
                if (dto.companyName.isNullOrEmpty() && dto.jobTitle.isNullOrEmpty()) return@mapNotNull null
                val emailData = emails.firstOrNull { it.emailId == dto.sourceEmailId } ?: emails.first()
                val dateStr = dto.dateApplied ?: emailData.date

                ParsedEmailInfo(
                    sourceEmailId  = dto.sourceEmailId ?: emailData.emailId,
                    companyName    = dto.companyName ?: "Unknown Company",
                    jobTitle       = dto.jobTitle ?: "Unknown Role",
                    status         = dto.status ?: "Unknown",
                    dateStr        = dateStr,
                    dateEpochMs    = parseRfc2822ToEpoch(dateStr),
                    recruiterEmail = dto.recruiterEmail,
                    snippet        = emailData.body
                )
            }
        } catch (e: com.google.ai.client.generativeai.type.QuotaExceededException) {
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseRfc2822ToEpoch(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        for (pattern in dateFormats) {
            runCatching {
                SimpleDateFormat(pattern, Locale.ENGLISH).parse(dateStr.trim())?.time
            }.getOrNull()?.let { return it }
        }
        return 0L
    }

    private data class JobApplicationDto(
        val sourceEmailId: String?,
        val companyName: String?,
        val jobTitle: String?,
        val status: String?,
        val dateApplied: String?,
        val recruiterEmail: String?,
        val notes: String?
    )
}
