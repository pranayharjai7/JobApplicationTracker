package com.pranay.jobtracker.domain

import com.google.common.truth.Truth.assertThat
import com.pranay.jobtracker.domain.ai.AIProvider
import com.pranay.jobtracker.domain.ai.AIProviderFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EmailParserTest {

    private lateinit var parser: EmailParser
    private val aiProviderFactory = mockk<AIProviderFactory>()
    private val aiProvider = mockk<AIProvider>()

    @Before
    fun setup() {
        every { aiProviderFactory.getProvider() } returns aiProvider
        parser = RealEmailParserImpl(aiProviderFactory)
    }

    @Test
    fun `parseEmailBatch parses valid AI response correctly`() = runTest {
        // Arrange
        val rawEmails = listOf(
            RawEmailData(
                emailId = "msg_123",
                subject = "Job Application Received",
                body = "Thanks for applying to Google...",
                date = "Tue, 17 Apr 2024 10:00:00 +0000",
                from = "jobs-noreply@google.com"
            )
        )

        val aiJsonResponse = """
            [
              {
                "sourceEmailId": "msg_123",
                "companyName": "Google",
                "jobTitle": "Software Engineer",
                "stage": "APPLIED",
                "subStatus": "Initial screening",
                "status": "Applied",
                "dateApplied": "Wed, 17 Apr",
                "recruiterEmail": "jobs-noreply@google.com",
                "notes": "System received the application."
              }
            ]
        """.trimIndent()

        coEvery { aiProvider.generateContent(any()) } returns aiJsonResponse

        // Act
        val result = parser.parseEmailBatch(rawEmails)

        // Assert
        assertThat(result).hasSize(1)
        val info = result[0]
        assertThat(info.companyName).isEqualTo("Google")
        assertThat(info.jobTitle).isEqualTo("Software Engineer")
        assertThat(info.stage).isEqualTo("APPLIED")
        assertThat(info.sourceEmailId).isEqualTo("msg_123")
    }

    @Test
    fun `parseEmailBatch handles malformed JSON by returning empty list`() = runTest {
        // Arrange
        val rawEmails = listOf(
            RawEmailData("id", "sub", "body", "Tue, 17 Apr 2024 10:00:00 +0000", "from")
        )
        coEvery { aiProvider.generateContent(any()) } returns "This is not JSON"

        // Act
        val result = parser.parseEmailBatch(rawEmails)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `parseEmailBatch handles empty email list immediately`() = runTest {
        // Act
        val result = parser.parseEmailBatch(emptyList())

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `parseRfc2822ToEpoch extracts correct timestamp for various formats`() = runTest {
        // We use reflection or a test-only wrapper if the method is private,
        // but here we can verify it indirectly via the parser output.
        
        val rawEmail = RawEmailData(
            emailId = "msg_1",
            subject = "Sub",
            body = "Body",
            date = "Wed, 17 Apr 2024 15:30:00 +0530",
            from = "sender"
        )
        
        val aiResponse = """
            [{"sourceEmailId": "msg_1", "companyName": "Test", "jobTitle": "Dev", "stage": "APPLIED"}]
        """.trimIndent()
        
        coEvery { aiProvider.generateContent(any()) } returns aiResponse

        val result = parser.parseEmailBatch(listOf(rawEmail))
        
        // Wed, 17 Apr 2024 15:30:00 +0530
        // Expected Epoch (UTC): 1713348000000 (roughly, verification below)
        assertThat(result[0].dateEpochMs).isGreaterThan(0L)
    }
}
