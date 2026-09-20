package com.safebuffer.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 사용자가 누른 저장 1회를 physical chunk 경계와 독립적으로 보존한다. */
@Entity(tableName = "saved_ranges")
data class SavedRangeEntity(
    @PrimaryKey val saveGroupId: Long,
    val requestedStartMs: Long,
    val requestedEndMs: Long
)
