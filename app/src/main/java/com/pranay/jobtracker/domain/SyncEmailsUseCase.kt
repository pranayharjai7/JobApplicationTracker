package com.pranay.jobtracker.domain

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.pranay.jobtracker.data.JobApplicationRepository
import com.pranay.jobtracker.data.SyncMetadata
import com.pranay.jobtracker.data.SyncMetadataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WINDOW_MS   = 30L * 24 * 60 * 60 * 1000   // 30 days in milliseconds
private const val BATCH_SIZE  = 5
private const val MAX_RESULTS = 20L

@Singleton
class SyncEmailsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: JobApplicationRepository,
    private val metaRepository: SyncMetadataRepository,
    private val emailParser: EmailParser,
    private val preprocessor: EmailPreprocessor,
    private val matchOrCreate: MatchOrCreateJobApplicationUseCase,
    private val addEmailEvent: AddEmailEventUseCase,
    private val generateSummary: GenerateJobSummaryUseCase
) {

    suspend operator fun invoke(accountId: String) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return

        withContext(Dispatchers.IO) {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(GmailScopes.GMAIL_READONLY)
            ).apply { selectedAccount = account.account }

            val service = Gmail.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("JobApplicationTracker").build()

            val metadata = metaRepository.getMetadata(accountId)
            val (windowStart, windowEnd) = computeWindow(metadata)
            val query = buildQuery(windowStart, windowEnd)

            val response = service.users().messages().list("me")
                .setQ(query)
                .setMaxResults(MAX_RESULTS)
                .execute()

            val messages = response.messages ?: emptyList()

            // Advance the window pointer so each sync steps back in history.
            metaRepository.saveMetadata(SyncMetadata(accountId = accountId, oldestFetchedEpochMs = windowStart))

            if (messages.isEmpty()) return@withContext

            val rawBatch = mutableListOf<RawEmailData>()
            for (msg in messages) {
                if (!isActive) return@withContext
                val full = service.users().messages().get("me", msg.id).setFormat("full").execute()
                rawBatch.add(preprocessor.preprocess(full))
            }

            rawBatch.chunked(BATCH_SIZE).forEach { chunk ->
                if (!isActive) return@withContext
                val parsedList: List<ParsedEmailInfo> = emailParser.parseEmailBatch(chunk)
                for (parsed in parsedList) {
                    if (!isActive) return@withContext
                    val (jobApp, _) = matchOrCreate(parsed, accountId)
                    val added = addEmailEvent(parsed, jobApp.id, accountId)
                    if (added) {
                        appRepository.updateApplicationStatus(
                            id            = jobApp.id,
                            status        = parsed.status,
                            lastUpdatedAt = parsed.dateEpochMs,
                            lastUpdate    = parsed.dateStr,
                            summary       = null   // generateSummary fills this below
                        )
                        generateSummary(
                            jobApplicationId = jobApp.id,
                            companyName      = jobApp.companyName,
                            jobTitle         = jobApp.jobTitle,
                            currentStatus    = parsed.status,
                            lastUpdatedAt    = parsed.dateEpochMs,
                            lastUpdate       = parsed.dateStr
                        )
                    }
                }
            }
        }
    }

    /**
     * First sync: most-recent 30 days.
     * Subsequent syncs: 30-day window immediately before the last fetched window.
     */
    private fun computeWindow(metadata: SyncMetadata): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return if (metadata.oldestFetchedEpochMs == 0L) {
            Pair(now - WINDOW_MS, now)
        } else {
            Pair(metadata.oldestFetchedEpochMs - WINDOW_MS, metadata.oldestFetchedEpochMs)
        }
    }

    // Gmail before:/after: operators require epoch SECONDS, not milliseconds.
    private fun buildQuery(startMs: Long, endMs: Long): String {
        val after  = TimeUnit.MILLISECONDS.toSeconds(startMs)
        val before = TimeUnit.MILLISECONDS.toSeconds(endMs)
        return "(application OR interview OR offer OR recruited OR candidate OR resume) after:$after before:$before"
    }
}
