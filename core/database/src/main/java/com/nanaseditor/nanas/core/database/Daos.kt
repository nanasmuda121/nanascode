package com.nanaseditor.nanas.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun getUserByLogin(login: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE login = :login")
    suspend fun deleteUser(login: String)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}

@Dao
interface RepoDao {
    @Query("SELECT * FROM repos ORDER BY updatedAt DESC")
    fun getAllReposFlow(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repos WHERE isPrivate = :isPrivate ORDER BY updatedAt DESC")
    fun getReposByVisibility(isPrivate: Boolean): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repos WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchRepos(query: String): Flow<List<RepoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepos(repos: List<RepoEntity>)

    @Query("DELETE FROM repos")
    suspend fun clearAll()
}
