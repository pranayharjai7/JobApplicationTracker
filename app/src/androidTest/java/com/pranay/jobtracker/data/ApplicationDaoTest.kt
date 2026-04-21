package com.pranay.jobtracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ApplicationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ApplicationDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.applicationDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetApplications() = runTest {
        val accountId = "test_account"
        val application = JobApplication(
            id = 1,
            accountId = accountId,
            companyName = "Google",
            jobTitle = "Software Engineer",
            status = "Applied",
            stage = "APPLIED",
            dateApplied = "2024-04-21",
            createdAt = System.currentTimeMillis()
        )
        
        dao.insertApplications(listOf(application))
        
        val allApps = dao.getAllApplications(accountId).first()
        assertThat(allApps).hasSize(1)
        assertThat(allApps[0].companyName).isEqualTo("Google")
    }

    @Test
    fun getDistinctCompaniesFiltersCorrectly() = runTest {
        val accountId = "account_1"
        val apps = listOf(
            JobApplication(id = 1, accountId = accountId, companyName = "Google", jobTitle = "A", status = "Applied"),
            JobApplication(id = 2, accountId = accountId, companyName = "Meta", jobTitle = "B", status = "Applied"),
            JobApplication(id = 3, accountId = accountId, companyName = "Google", jobTitle = "C", status = "Applied"),
            JobApplication(id = 4, accountId = "other_account", companyName = "Apple", jobTitle = "D", status = "Applied")
        )
        
        dao.insertApplications(apps)
        
        val companies = dao.getDistinctCompanies(accountId).first()
        assertThat(companies).hasSize(2)
        assertThat(companies).containsExactly("Google", "Meta").inOrder()
    }

    @Test
    fun findByNormalizedKeyMatchesCorrectly() = runTest {
        val accountId = "acc"
        val app = JobApplication(
            id = 1, 
            accountId = accountId, 
            companyName = "  Google Inc  ", 
            jobTitle = "Software Engineer ", 
            status = "Applied"
        )
        dao.insertApplications(listOf(app))
        
        // dao query uses lower(trim(companyName))
        val result = dao.findByNormalizedKey("google inc", "software engineer", accountId)
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(1)
    }

    @Test
    fun deleteByAccountClearsOnlyTargetedData() = runTest {
        val app1 = JobApplication(id = 1, accountId = "acc_1", companyName = "A", jobTitle = "T", status = "S")
        val app2 = JobApplication(id = 2, accountId = "acc_2", companyName = "B", jobTitle = "T", status = "S")
        
        dao.insertApplications(listOf(app1, app2))
        
        dao.deleteByAccount("acc_1")
        
        assertThat(dao.getAllApplications("acc_1").first()).isEmpty()
        assertThat(dao.getAllApplications("acc_2").first()).hasSize(1)
    }
}
