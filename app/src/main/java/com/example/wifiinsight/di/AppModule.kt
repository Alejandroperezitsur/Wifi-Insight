package com.example.wifiinsight.di

import android.content.Context
import android.net.ConnectivityManager
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import com.example.wifiinsight.domain.usecase.ConnectToNetworkUseCase
import com.example.wifiinsight.domain.usecase.GetCurrentConnectionUseCase
import com.example.wifiinsight.domain.usecase.MonitorConnectionUseCase
import com.example.wifiinsight.domain.usecase.ScanNetworksUseCase
import com.example.wifiinsight.domain.util.InternetChecker
import com.example.wifiinsight.domain.util.ScanDataExporter
import com.example.wifiinsight.domain.util.WifiConnector
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
    fun provideWifiConnector(
        @ApplicationContext context: Context
    ): WifiConnector {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return WifiConnector(connectivityManager)
    }

    @Provides
    @Singleton
    fun provideWifiRepository(
        @ApplicationContext context: Context,
        internetChecker: InternetChecker,
        wifiConnector: WifiConnector
    ): WifiRepository = WifiRepositoryImpl(context, internetChecker, wifiConnector)

    @Provides
    @Singleton
    fun provideScanNetworksUseCase(
        @ApplicationContext context: Context
    ): ScanNetworksUseCase = ScanNetworksUseCase(context)

    @Provides
    @Singleton
    fun provideConnectToNetworkUseCase(
        wifiConnector: WifiConnector
    ): ConnectToNetworkUseCase = ConnectToNetworkUseCase(wifiConnector)

    @Provides
    @Singleton
    fun provideGetCurrentConnectionUseCase(
        @ApplicationContext context: Context,
        internetChecker: InternetChecker
    ): GetCurrentConnectionUseCase = GetCurrentConnectionUseCase(context, internetChecker)

    @Provides
    @Singleton
    fun provideMonitorConnectionUseCase(
        @ApplicationContext context: Context
    ): MonitorConnectionUseCase = MonitorConnectionUseCase(context)

    @Provides
    @Singleton
    fun provideScanDataExporter(
        @ApplicationContext context: Context
    ): ScanDataExporter = ScanDataExporter(context)
}
