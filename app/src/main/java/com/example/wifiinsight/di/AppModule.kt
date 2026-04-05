package com.example.wifiinsight.di

import android.content.Context
import com.example.wifiinsight.data.repository.WifiRepository
import com.example.wifiinsight.data.repository.WifiRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para proveer dependencias de la capa de datos.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindWifiRepository(
        impl: WifiRepositoryImpl
    ): WifiRepository
}

/**
 * Módulo para proveer Context y dependencias de sistema
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}
