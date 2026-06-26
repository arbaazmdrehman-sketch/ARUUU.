package com.aruuu.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// ═══════════════════════════════════════════════════════════════════════════
// LockedAppEntity — apps the user has added to the vault
// ═══════════════════════════════════════════════════════════════════════════

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    /** true  = app lock only (require auth to open)
     *  false = full vault hide (removed from launcher + recents) */
    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean = true,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = 0L,
)

// ═══════════════════════════════════════════════════════════════════════════
// IntruderEntity — one intruder capture event
// ═══════════════════════════════════════════════════════════════════════════

@Entity(tableName = "intruder_records")
data class IntruderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long,

    @ColumnInfo(name = "image_path")
    val imagePath: String,

    @ColumnInfo(name = "failed_attempts")
    val failedAttempts: Int,

    @ColumnInfo(name = "target_package")
    val targetPackage: String = "",
)
