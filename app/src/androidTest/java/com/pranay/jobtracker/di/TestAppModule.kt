package com.pranay.jobtracker.di

import android.content.Context
import androidx.room.Room
import com.pranay.jobtracker.data.AppDatabase
import com.pranay.jobtracker.data.ApplicationDao
import com.pranay.jobtracker.data.EmailEventDao
import com.pranay.jobtracker.data.EmailEventRepository
import com.pranay.jobtracker.data.SyncMetadataDao
import com.pranay.jobtracker.data.SyncMetadataRepository
import com.pranay.jobtracker.data.JobApplicationRepository
import com.pranay.jobtracker.domain.EmailParser
import com.pranay.jobtracker.domain.EmailPreprocessor
import com.pranay.jobtracker.domain.ai.AIProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @Provides
    fun provideApplicationDao(db: AppDatabase): ApplicationDao = db.applicationDao()

    @Provides
    fun provideSyncMetadataDao(db: AppDatabase): SyncMetadataDao = db.syncMetadataDao()

    @Provides
    fun provideEmailEventDao(db: AppDatabase): EmailEventDao = db.emailEventDao()

    @Provides
    fun provideAccountInfoDao(db: AppDatabase): com.pranay.jobtracker.data.AccountInfoDao = db.accountInfoDao()

    @Provides
    @Singleton
    fun provideJobApplicationRepository(dao: ApplicationDao): JobApplicationRepository = JobApplicationRepository(dao)

    @Provides
    @Singleton
    fun provideEmailEventRepository(dao: EmailEventDao): EmailEventRepository = EmailEventRepository(dao)

    @Provides
    @Singleton
    fun provideSyncMetadataRepository(dao: SyncMetadataDao): SyncMetadataRepository = SyncMetadataRepository(dao)

    @Provides
    @Singleton
    fun provideEmailParser(aiProviderFactory: AIProviderFactory): EmailParser {
        return com.pranay.jobtracker.domain.MockEmailParserImpl()
    }

    @Provides
    @Singleton
    fun provideEmailPreprocessor(): EmailPreprocessor = EmailPreprocessor()

    @Provides
    @Singleton
    fun provideAIProviderFactory(): AIProviderFactory = AIProviderFactory(emptyList())

    @Provides
    @Singleton
    fun provideGmailSyncManager(
        repository: JobApplicationRepository,
        emailParser: EmailParser
    ): com.pranay.jobtracker.domain.GmailSyncManager {
        return com.pranay.jobtracker.domain.MockGmailSyncManagerImpl(repository, emailParser)
    }

}
