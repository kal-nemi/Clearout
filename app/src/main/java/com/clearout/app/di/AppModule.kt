package com.clearout.app.di

import android.content.Context
import com.clearout.app.data.repository.MediaStoreRepositoryImpl
import com.clearout.app.domain.repository.MediaStoreRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMediaStoreRepository(
        impl: MediaStoreRepositoryImpl
    ): MediaStoreRepository
}
