package com.example.financetracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.financetracker.data.model.Transaction
import com.example.financetracker.data.model.Category

@Database(entities = [Transaction::class, Category::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}