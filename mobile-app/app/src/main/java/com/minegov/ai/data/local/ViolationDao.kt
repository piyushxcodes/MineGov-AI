package com.minegov.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(violation: ViolationEntity)

    @Update
    suspend fun update(violation: ViolationEntity)

    @Query("SELECT * FROM violations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ViolationEntity>>

    @Query(
        "SELECT * FROM violations " +
                "WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED' " +
                "ORDER BY createdAt ASC"
    )
    suspend fun getPendingViolations(): List<ViolationEntity>

    @Query("SELECT * FROM violations WHERE localId = :id LIMIT 1")
    suspend fun getById(id: String): ViolationEntity?

    @Query("DELETE FROM violations WHERE localId = :id")
    suspend fun deleteById(id: String)
}