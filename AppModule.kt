package com.aruuu.app.di

import android.content.Context
import com.aruuu.app.data.local.*
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.service.IntruderCaptureService
import com.aruuu.app.service.ARUUUBiometricManager
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
    fun provideDatabase(@ApplicationContext context: Context): ARUUUDatabase =
        ARUUUDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideLockedAppDao(db: ARUUUDatabase): LockedAppDao = db.lockedAppDao()

    @Provides
    @Singleton
    fun provideIntruderDao(db: ARUUUDatabase): IntruderDao = db.intruderDao()

    @Provides
    @Singleton
    fun provideSecureCredentialManager(@ApplicationContext context: Context): SecureCredentialManager =
        SecureCredentialManager(context)

    @Provides
    @Singleton
    fun provideRepository(
        @ApplicationContext context: Context,
        lockedAppDao: LockedAppDao,
        intruderDao: IntruderDao,
        credentials: SecureCredentialManager,
    ): ARUUURepository = ARUUURepository(context, lockedAppDao, intruderDao, credentials)

    @Provides
    @Singleton
    fun provideBiometricManager(@ApplicationContext context: Context): ARUUUBiometricManager =
        ARUUUBiometricManager(context)

    @Provides
    @Singleton
    fun provideIntruderCaptureService(
        @ApplicationContext context: Context,
        repository: ARUUURepository,
    ): IntruderCaptureService = IntruderCaptureService(context, repository)
}
