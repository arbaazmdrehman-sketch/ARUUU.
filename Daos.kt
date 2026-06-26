package com.aruuu.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════════════════════════════
// LockedAppDao
// ═══════════════════════════════════════════════════════════════════════════

@Dao
interface LockedAppDao {

    @Query("SELECT * FROM locked_apps ORDER BY app_name ASC")
    fun observeAll(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps ORDER BY app_name ASC")
    suspend fun getAll(): List<LockedAppEntity>

    @Query("SELECT * FROM locked_apps WHERE package_name = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): LockedAppEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE package_name = :pkg)")
    suspend fun isLocked(pkg: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE package_name = :pkg AND is_hidden = 1)")
    suspend fun isHidden(pkg: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LockedAppEntity)

    @Update
    suspend fun update(entity: LockedAppEntity)

    @Query("DELETE FROM locked_apps WHERE package_name = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("DELETE FROM locked_apps")
    suspend fun deleteAll()

    @Query("UPDATE locked_apps SET last_accessed = :ts WHERE package_name = :pkg")
    suspend fun updateLastAccessed(pkg: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM locked_apps WHERE is_locked = 1")
    suspend fun lockedCount(): Int

    @Query("SELECT COUNT(*) FROM locked_apps WHERE is_hidden = 1")
    suspend fun hiddenCount(): Int
}

// ═══════════════════════════════════════════════════════════════════════════
// IntruderDao
// ═══════════════════════════════════════════════════════════════════════════

@Dao
interface IntruderDao {

    @Query("SELECT * FROM intruder_records ORDER BY timestamp_ms DESC")
    fun observeAll(): Flow<List<IntruderEntity>>

    @Query("SELECT * FROM intruder_records ORDER BY timestamp_ms DESC")
    suspend fun getAll(): List<IntruderEntity>

    @Query("SELECT * FROM intruder_records ORDER BY timestamp_ms DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<IntruderEntity>

    @Insert
    suspend fun insert(entity: IntruderEntity): Long

    @Query("DELETE FROM intruder_records WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM intruder_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM intruder_records")
    suspend fun count(): Int
}
