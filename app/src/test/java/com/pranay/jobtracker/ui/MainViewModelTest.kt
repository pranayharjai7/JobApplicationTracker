package com.pranay.jobtracker.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pranay.jobtracker.data.AccountInfo
import com.pranay.jobtracker.data.AccountRepository
import com.pranay.jobtracker.data.EmailEventRepository
import com.pranay.jobtracker.data.JobApplicationRepository
import com.pranay.jobtracker.data.SyncMetadataRepository
import com.pranay.jobtracker.domain.GmailSyncManager
import com.pranay.jobtracker.domain.SyncEmailsUseCase
import com.pranay.jobtracker.domain.ai.AIProviderFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val repository = mockk<JobApplicationRepository>(relaxed = true)
    private val syncManager = mockk<GmailSyncManager>(relaxed = true)
    private val syncEmailsUseCase = mockk<SyncEmailsUseCase>(relaxed = true)
    private val metaRepository = mockk<SyncMetadataRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val eventRepository = mockk<EmailEventRepository>(relaxed = true)
    private val aiProviderFactory = mockk<AIProviderFactory>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks
        every { accountRepository.activeAccountIdFlow } returns flowOf("test_account")
        coEvery { accountRepository.getActiveAccountId() } returns "test_account"
        every { repository.getAllApplications(any()) } returns flowOf(emptyList())
        every { repository.getDistinctCompanies(any()) } returns flowOf(emptyList())
        every { eventRepository.getAllEventsForAccount(any()) } returns flowOf(emptyList())

        viewModel = MainViewModel(
            repository,
            syncManager,
            syncEmailsUseCase,
            metaRepository,
            accountRepository,
            eventRepository,
            aiProviderFactory
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isSyncing state transitions correctly during syncEmails`() = runTest {
        // Arrange
        coEvery { syncEmailsUseCase.invoke(any()) } coAnswers {
            // Simulate work
            kotlinx.coroutines.delay(100)
        }

        // Assert initial state
        assertThat(viewModel.isSyncing.value).isFalse()

        // Act & Assert intermediate/final states
        viewModel.isSyncing.test {
            assertThat(awaitItem()).isFalse() // Initial
            
            viewModel.syncEmails()
            assertThat(awaitItem()).isTrue() // Loading starts
            
            assertThat(awaitItem()).isFalse() // Loading ends
        }
    }

    @Test
    fun `stopSyncing cancels the sync job and resets state`() = runTest {
        // Arrange
        coEvery { syncEmailsUseCase.invoke(any()) } coAnswers {
            kotlinx.coroutines.delay(5000)
        }

        // Act
        viewModel.syncEmails()
        assertThat(viewModel.isSyncing.value).isTrue()
        
        viewModel.stopSyncing()
        
        // Assert
        assertThat(viewModel.isSyncing.value).isFalse()
    }

    @Test
    fun `toggleCompanyFilter updates selectedCompanies flow`() = runTest {
        viewModel.selectedCompanies.test {
            assertThat(awaitItem()).isEmpty()
            
            viewModel.toggleCompanyFilter("Google")
            assertThat(awaitItem()).containsExactly("Google")
            
            viewModel.toggleCompanyFilter("Google")
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `clearDatabase calls repositories correctly`() = runTest {
        // Act
        viewModel.clearDatabase()

        // Assert
        io.mockk.coVerify {
            repository.clearAccountData("test_account")
            metaRepository.clearMetadata("test_account")
        }
    }
}
