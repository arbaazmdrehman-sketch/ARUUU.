package com.aruuu.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * ARUUU Room database.
 *
 * Encrypted via Android Keystore + SQLCipher is recommended for production.
 * Here we use WAL mode and standard Room encryption-at-rest from the OS.
 */
@Database(
    entities = [LockedAppEntity::class, IntruderEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ARUUUDatabase : RoomDatabase() {

    abstract fun lockedAppDao(): LockedAppDao
    abstract fun intruderDao(): IntruderDao

    companion object {
        private const val DATABASE_NAME = "aruuu.db"

        @Volatile
        private var INSTANCE: ARUUUDatabase? = null

        fun getInstance(context: Context): ARUUUDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): ARUUUDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ARUUUDatabase::class.java,
                DATABASE_NAME,
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Enable WAL for better write performance
                        db.execSQL("PRAGMA journal_mode=WAL")
                        // Enforce foreign keys
                        db.execSQL("PRAGMA foreign_keys=ON")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
    }
}
