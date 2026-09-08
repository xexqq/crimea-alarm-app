package com.xexqq.crimeaalarm

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(alert: AlertEntity)

    @Query("SELECT * FROM alerts ORDER BY id DESC LIMIT 100")
    suspend fun getRecent(): List<AlertEntity>

    @Query("SELECT MAX(postId) FROM alerts")
    suspend fun getLastPostId(): Long?
}
