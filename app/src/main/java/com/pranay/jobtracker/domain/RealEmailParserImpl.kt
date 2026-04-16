package com.pranay.jobtracker.domain

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pranay.jobtracker.BuildConfig
import com.pranay.jobtracker.data.JobApplication

class RealEmailParserImpl : EmailParser {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val gson = GsonBuilder().setLenient().create()

    override suspend fun parseEmailBatch(emails: List<RawEmailData>): List<JobApplication> {
        if (emails.isEmpty()) return emptyList()

        val emailsText = emails.mapIndexed { index, email ->
            val actualDate = email.date.substringBefore("||")
            val emailId = email.date.substringAfter("||")
            """
            --- EMAIL ${index + 1} ---
            Email ID: $emailId
            Date: $actualDate
            Subject: ${email.subject}
            Body:
            ${email.body.take(2000)}
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
                
                JobApplication(
                    companyName = dto.companyName ?: "Unknown Company",
                    jobTitle = dto.jobTitle ?: "Unknown Role",
                    dateApplied = dto.dateApplied ?: "Recent",
                    status = dto.status ?: "Unknown",
                    lastUpdate = dto.dateApplied ?: "Recent",
                    recruiterEmail = dto.recruiterEmail,
                    notes = dto.notes,
                    emailId = dto.sourceEmailId
                )
            }
        } catch (e: com.google.ai.client.generativeai.type.QuotaExceededException) {
            e.printStackTrace()
            listOf(JobApplication(
                companyName = "Sync Paused",
                jobTitle = "Quota Exceeded",
                dateApplied = "Now",
                status = "Limit Hit",
                lastUpdate = "Now",
                recruiterEmail = null,
                notes = "Google Gemini Free Tier limit reached. Please wait a minute and try again."
            ))
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(JobApplication(
                companyName = "Debug: Gemini Crash",
                jobTitle = e.javaClass.simpleName,
                dateApplied = "Now",
                status = "Error",
                lastUpdate = "Now",
                recruiterEmail = null,
                notes = e.message ?: e.toString()
            ))
        }
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
