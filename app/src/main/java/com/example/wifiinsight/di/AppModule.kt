package com.example.wifiinsight.di

import android.content.Context
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.util.InternetChecker
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
    fun provideInternetChecker(): InternetChecker = InternetChecker()

    @Provides
    @Singleton
    fun provideWifiRepository(
        @ApplicationContext context: Context,
        internetChecker: InternetChecker
    ): WifiRepository = WifiRepositoryImpl(context, internetChecker)
}
