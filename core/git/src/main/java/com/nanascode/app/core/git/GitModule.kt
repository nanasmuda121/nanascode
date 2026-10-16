package com.nanascode.app.core.git

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GitModule {

    @Binds
    @Singleton
    abstract fun bindGitManager(
        impl: GitManagerImpl
    ): GitManager
}
