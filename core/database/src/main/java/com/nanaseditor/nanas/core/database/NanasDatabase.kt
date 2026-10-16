package com.nanaseditor.nanas.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [UserEntity::class, RepoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NanasDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun repoDao(): RepoDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NanasDatabase {
        return Room.databaseBuilder(
            context,
            NanasDatabase::class.java,
            "nanascode_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUserDao(db: NanasDatabase): UserDao = db.userDao()

    @Provides
    fun provideRepoDao(db: NanasDatabase): RepoDao = db.repoDao()
}
