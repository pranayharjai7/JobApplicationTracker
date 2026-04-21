package com.pranay.jobtracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pranay.jobtracker.MainActivity
import com.pranay.jobtracker.data.AccountRepository
import com.pranay.jobtracker.data.JobApplication
import com.pranay.jobtracker.data.JobApplicationRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class DashboardScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var appRepository: JobApplicationRepository

    @Inject
    lateinit var accountRepository: AccountRepository

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Seed some fake data
        runBlocking {
            val account = accountRepository.addOrUpdateAccount("test@gmail.com", "Test User", "")
            accountRepository.setActiveAccount(account.accountId)
            
            appRepository.insertApplications(listOf(
                JobApplication(
                    id = 1,
                    accountId = account.accountId,
                    companyName = "Google",
                    jobTitle = "Software Engineer",
                    status = "Applied",
                    stage = "APPLIED",
                    dateApplied = "Apr 21",
                    createdAt = System.currentTimeMillis()
                )
            ))
        }

        // Wait for splash screen to disappear
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("splash_screen").fetchSemanticsNodes().isEmpty()
        }
        
        // Ensure dashboard is ready
        composeTestRule.onNodeWithTag("smart_filters_button").assertIsDisplayed()
    }

    @Test
    fun dashboard_displaysJobApplications() {
        // Wait for dashboard to be fully ready
        composeTestRule.onNodeWithTag("smart_filters_button").assertIsDisplayed()
        
        // Check if the company name is displayed
        composeTestRule.onNodeWithText("Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("Software Engineer").assertIsDisplayed()
        
        // Check if the status badge is present (ApplicationCard uses label.uppercase())
        composeTestRule.onNodeWithText("APPLIED").assertIsDisplayed()
    }

    @Test
    fun dashboard_toggleFilterInteraction() {
        // Open filters
        composeTestRule.onNodeWithTag("smart_filters_button").performClick()
        
        // Wait for the bottom sheet content to appear
        // Using a more reliable waiter
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithTag("status_filter_header").fetchSemanticsNodes().isNotEmpty()
        }

        // Check if Status filter header exists
        composeTestRule.onNodeWithTag("status_filter_header").assertIsDisplayed()
        
        // Click on Interview filter chip
        composeTestRule.onNodeWithTag("filter_chip_INTERVIEW").performClick()
        
        // Verify it is displayed (means it was found in the hierarchy)
        composeTestRule.onNodeWithTag("filter_chip_INTERVIEW").assertIsDisplayed()
    }
}
