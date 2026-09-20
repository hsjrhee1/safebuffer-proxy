package com.safebuffer.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChunkEntity::class, SavedRangeEntity::class],
    version = 5,
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

        // v2 → v3: 48시간 타임라인 파형용 진폭 요약
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE audio_chunks ADD COLUMN amplitudes TEXT")
            }
        }

        // v3 → v4: 저장 묶음 ID (저장 한 번 = 목록 항목 하나)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE audio_chunks ADD COLUMN saveGroupId INTEGER")
                // 기존에 저장된 청크들은 시작 시각을 묶음 ID로 삼아 각자 하나의 항목이 된다
                db.execSQL(
                    "UPDATE audio_chunks SET saveGroupId = startTimeMs WHERE isLocked = 1"
                )
            }
        }

        // v4 → v5: 사용자가 선택한 논리 시작/종료 경계 보존
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS saved_ranges (
                        saveGroupId INTEGER NOT NULL PRIMARY KEY,
                        requestedStartMs INTEGER NOT NULL,
                        requestedEndMs INTEGER NOT NULL
                    )""".trimIndent()
                )
            }
        }
    }
}
