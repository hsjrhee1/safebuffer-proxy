package com.safebuffer.app.di

import android.content.Context
import androidx.room.Room
import com.safebuffer.app.data.local.ChunkDatabase
import com.safebuffer.app.data.local.ChunkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChunkDatabase =
        Room.databaseBuilder(
            context,
            ChunkDatabase::class.java,
            ChunkDatabase.DATABASE_NAME
        )
        .addMigrations(
            ChunkDatabase.MIGRATION_1_2,
            ChunkDatabase.MIGRATION_2_3,
            ChunkDatabase.MIGRATION_3_4,
            ChunkDatabase.MIGRATION_4_5
        )
        .build()

    @Provides
    fun provideChunkDao(db: ChunkDatabase): ChunkDao = db.chunkDao()
}
