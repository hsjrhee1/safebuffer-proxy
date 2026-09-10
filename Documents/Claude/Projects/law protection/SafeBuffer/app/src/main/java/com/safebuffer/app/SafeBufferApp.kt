package com.safebuffer.app

import android.app.Application
import androidx.room.Room
import com.safebuffer.app.data.local.ChunkDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SafeBufferApp : Application()
