package com.nyabi.dcremover.di

import com.nyabi.dcremover.data.repository.DcRepositoryImpl
import com.nyabi.dcremover.domain.repository.DcRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindDcRepository(impl: DcRepositoryImpl): DcRepository
}
