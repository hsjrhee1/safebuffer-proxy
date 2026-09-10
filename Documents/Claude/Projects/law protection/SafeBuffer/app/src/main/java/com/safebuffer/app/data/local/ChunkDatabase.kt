package com.safebuffer.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChunkEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ChunkDatabase : RoomDatabase() {
    abstract fun chunkDao(): ChunkDao

    companion object {
        const val DATABASE_NAME = "safebuffer_chunks.db"

        // v1 → v2: 원본 증명 필드 3개 추가
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE audio_chunks ADD COLUMN fileHash TEXT")
                db.execSQL("ALTER TABLE audio_chunks ADD COLUMN serverTimestamp TEXT")
                db.execSQL("ALTER TABLE audio_chunks ADD COLUMN serverToken TEXT")
            }
        }
    }
}
