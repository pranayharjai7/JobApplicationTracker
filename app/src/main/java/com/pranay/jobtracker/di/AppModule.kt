package com.pranay.jobtracker.di

import android.content.Context
import androidx.room.Room
import com.pranay.jobtracker.data.AppDatabase
import com.pranay.jobtracker.data.ApplicationDao
import com.pranay.jobtracker.data.EmailEventDao
import com.pranay.jobtracker.data.EmailEventRepository
import com.pranay.jobtracker.data.JobApplicationRepository
import com.pranay.jobtracker.data.SyncMetadataDao
import com.pranay.jobtracker.data.SyncMetadataRepository
import com.pranay.jobtracker.domain.AddEmailEventUseCase
import com.pranay.jobtracker.domain.EmailParser
import com.pranay.jobtracker.domain.EmailPreprocessor
import com.pranay.jobtracker.domain.GenerateJobSummaryUseCase
import com.pranay.jobtracker.domain.GmailSyncManager
import com.pranay.jobtracker.domain.MatchOrCreateJobApplicationUseCase
import com.pranay.jobtracker.domain.SyncEmailsUseCase
import com.pranay.jobtracker.domain.RealEmailParserImpl
import com.pranay.jobtracker.domain.RealGmailSyncManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        securityManager: com.pranay.jobtracker.security.SecurityManager
    ): AppDatabase {
        val passphrase = securityManager.getDatabasePassphrase()
        val factory = net.sqlcipher.database.SupportFactory(passphrase.toByteArray())

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "job_tracker_db"
        ).openHelperFactory(factory)
        .addMigrations(
            AppDatabase.MIGRATION_2_3, 
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6
        ).build()
    }

    @Provides
    fun provideApplicationDao(db: AppDatabase): ApplicationDao {
        return db.applicationDao()
    }

    @Provides
    fun provideAccountInfoDao(db: AppDatabase): com.pranay.jobtracker.data.AccountInfoDao {
        return db.accountInfoDao()
    }

    @Provides
    fun provideSyncMetadataDao(db: AppDatabase): SyncMetadataDao {
        return db.syncMetadataDao()
    }

    @Provides
    fun provideEmailEventDao(db: AppDatabase): EmailEventDao {
        return db.emailEventDao()
    }

    @Provides
    @Singleton
    fun provideEmailEventRepository(dao: EmailEventDao): EmailEventRepository {
        return EmailEventRepository(dao)
    }

    @Provides
    @Singleton
    fun provideSyncMetadataRepository(dao: SyncMetadataDao): SyncMetadataRepository {
        return SyncMetadataRepository(dao)
    }

    @Provides
    @Singleton
    fun provideEmailParser(
        aiProviderFactory: com.pranay.jobtracker.domain.ai.AIProviderFactory
    ): EmailParser {
        return RealEmailParserImpl(aiProviderFactory)
    }

    @Provides
    @Singleton
    fun provideEmailPreprocessor(): EmailPreprocessor {
        return EmailPreprocessor()
    }

    @Provides
    @Singleton
    fun provideGmailSyncManager(
        @ApplicationContext context: Context,
        repository: JobApplicationRepository,
        emailParser: EmailParser
    ): GmailSyncManager {
        return RealGmailSyncManagerImpl(context, repository, emailParser)
    }

    @Provides
    @Singleton
    fun provideMatchOrCreateJobApplicationUseCase(
        appRepository: JobApplicationRepository
    ): MatchOrCreateJobApplicationUseCase = MatchOrCreateJobApplicationUseCase(appRepository)

    @Provides
    @Singleton
    fun provideAddEmailEventUseCase(
        eventRepository: EmailEventRepository
    ): AddEmailEventUseCase = AddEmailEventUseCase(eventRepository)

    @Provides
    @Singleton
    fun provideGenerateJobSummaryUseCase(
        eventRepository: EmailEventRepository,
        appRepository: JobApplicationRepository,
        aiProviderFactory: com.pranay.jobtracker.domain.ai.AIProviderFactory
    ): GenerateJobSummaryUseCase = GenerateJobSummaryUseCase(eventRepository, appRepository, aiProviderFactory)

    @Provides
    @Singleton
    fun provideSyncEmailsUseCase(
        @ApplicationContext context: Context,
        appRepository: JobApplicationRepository,
        metaRepository: SyncMetadataRepository,
        emailParser: EmailParser,
        preprocessor: EmailPreprocessor,
        matchOrCreate: MatchOrCreateJobApplicationUseCase,
        addEmailEvent: AddEmailEventUseCase,
        generateSummary: GenerateJobSummaryUseCase
    ): SyncEmailsUseCase = SyncEmailsUseCase(
        context, appRepository, metaRepository, emailParser, preprocessor,
        matchOrCreate, addEmailEvent, generateSummary
    )
}
