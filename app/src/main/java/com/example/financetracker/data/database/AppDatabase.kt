package com.example.financetracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.financetracker.data.model.Transaction

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}