package com.pranay.jobtracker.domain

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.pranay.jobtracker.data.JobApplicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class RealGmailSyncManagerImpl(
    private val context: Context,
    private val repository: JobApplicationRepository,
    private val emailParser: EmailParser
) : GmailSyncManager {

    override suspend fun syncRecentJobEmails() {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return
        
        withContext(Dispatchers.IO) {
            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context, listOf(GmailScopes.GMAIL_READONLY)
                ).apply {
                    selectedAccount = account.account
                }

                val service = Gmail.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                .setApplicationName("JobApplicationTracker")
                .build()

                // Broadened query to search entire email body/subject instead of just subject
                val query = "application OR interview OR offer OR candidate OR resume OR recruited newer_than:30d"
                val response = service.users().messages().list("me")
                    .setQ(query)
                    .setMaxResults(5)
                    .execute()

                val messages = response.messages ?: emptyList()
                
                val fetchedIds = messages.map { it.id }
                val knownIds = if (fetchedIds.isNotEmpty()) repository.getExistingEmailIds(fetchedIds) else emptyList()
                
                if (messages.isEmpty()) {
                    repository.insertApplications(listOf(
                        com.pranay.jobtracker.data.JobApplication(
                            companyName = "Debug: Empty",
                            jobTitle = "No Emails",
                            dateApplied = "Now",
                            status = "Empty",
                            lastUpdate = "Now",
                            recruiterEmail = null,
                            notes = "Your inbox query found zero results."
                        )
                    ))
                }

                val rawBatch = mutableListOf<RawEmailData>()

                for (msg in messages) {
                    if (!isActive) break // Halt loop securely if the Stop Button was pressed
                    if (msg.id in knownIds) continue // Skip already structurally saved emails!
                    
                    val fullMessage = service.users().messages().get("me", msg.id).setFormat("full").execute()
                    
                    val payload = fullMessage.payload
                    val headers = payload?.headers
                    val subject = headers?.find { it.name.equals("Subject", ignoreCase = true) }?.value ?: ""
                    val dateStr = headers?.find { it.name.equals("Date", ignoreCase = true) }?.value ?: ""
                    val from = headers?.find { it.name.equals("From", ignoreCase = true) }?.value ?: ""
                    val bodyStr = getPlainTextBody(payload)

                    rawBatch.add(RawEmailData(emailId = msg.id, subject = subject, body = bodyStr, date = dateStr, from = from))
                }

                if (rawBatch.isNotEmpty() && isActive) {
                    val parsed: List<ParsedEmailInfo> = emailParser.parseEmailBatch(rawBatch)
                    if (parsed.isNotEmpty()) {
                        repository.insertApplications(parsed.map { info ->
                            com.pranay.jobtracker.data.JobApplication(
                                companyName    = info.companyName,
                                jobTitle       = info.jobTitle,
                                dateApplied    = info.dateStr,
                                status         = info.status,
                                lastUpdate     = info.dateStr,
                                recruiterEmail = info.recruiterEmail,
                                notes          = null,
                                emailId        = info.sourceEmailId
                            )
                        })
                    } else {
                        repository.insertApplications(listOf(
                            com.pranay.jobtracker.data.JobApplication(
                                companyName = "No New Apps",
                                jobTitle = "Finished Scanning",
                                dateApplied = "Now",
                                status = "Done",
                                lastUpdate = "Now",
                                recruiterEmail = null,
                                notes = "Scanned ${rawBatch.size} emails, but Gemini didn't find any job applications in this batch."
                            )
                        ))
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e // Don't save manual cancellations!
                e.printStackTrace()
                repository.insertApplications(listOf(
                    com.pranay.jobtracker.data.JobApplication(
                        companyName = "Debug: Sync Crash",
                        jobTitle = e.javaClass.simpleName,
                        dateApplied = "Now",
                        status = "Exception",
                        lastUpdate = "Now",
                        recruiterEmail = null,
                        notes = e.message ?: e.toString()
                    )
                ))
            }
        }
    }

    private fun getPlainTextBody(payload: com.google.api.services.gmail.model.MessagePart?): String {
        if (payload == null) return ""
        
        // Sometimes simple payloads have body data right at the top
        if (payload.mimeType == "text/plain") {
            val data = payload.body?.data
            if (data != null) {
                return String(com.google.api.client.util.Base64.decodeBase64(data))
            }
        }
        
        if (payload.parts != null) {
            for (part in payload.parts) {
                val result = getPlainTextBody(part)
                if (result.isNotEmpty()) return result
            }
            
            // Fallback: If no plaintext found, try text/html
            for (part in payload.parts) {
                if (part.mimeType == "text/html") {
                    val data = part.body?.data
                    if (data != null) {
                        // Stripping HTML is rough, but Gemini handles raw HTML fine!
                        return String(com.google.api.client.util.Base64.decodeBase64(data))
                    }
                }
            }
        }
        
        // Final fallback if the topmost payload is HTML with no parts (uncommon but happens)
        if (payload.mimeType == "text/html" && payload.body?.data != null) {
            return String(com.google.api.client.util.Base64.decodeBase64(payload.body.data))
        }
        
        return ""
    }
}
