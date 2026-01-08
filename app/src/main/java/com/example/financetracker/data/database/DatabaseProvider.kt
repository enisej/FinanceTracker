package com.example.financetracker.data.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val appContext = context.applicationContext
            instance ?: Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                "finance_db"
            ).build().also { instance = it }
        }
    }
}