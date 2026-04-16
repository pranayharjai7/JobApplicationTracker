package com.pranay.jobtracker.di

import android.content.Context
import androidx.room.Room
import com.pranay.jobtracker.data.AppDatabase
import com.pranay.jobtracker.data.ApplicationDao
import com.pranay.jobtracker.data.JobApplicationRepository
import com.pranay.jobtracker.domain.EmailParser
import com.pranay.jobtracker.domain.GmailSyncManager
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "job_tracker_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideApplicationDao(db: AppDatabase): ApplicationDao {
        return db.applicationDao()
    }

    @Provides
    @Singleton
    fun provideEmailParser(): EmailParser {
        return RealEmailParserImpl()
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
}
