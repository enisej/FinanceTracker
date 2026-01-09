package com.example.financetracker.data.repository

import com.example.financetracker.data.database.TransactionDao
import com.example.financetracker.data.model.Category
import com.example.financetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {

    suspend fun insert(transaction: Transaction) = dao.insert(transaction)
    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
    fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions()
    fun getBalance(): Flow<Double?> = dao.getBalance()

    suspend fun insertCategory(category: Category) = dao.insertCategory(category)
    fun getAllCategories(): Flow<List<Category>> = dao.getAllCategories()
    suspend fun deleteCategory(category: Category) = dao.deleteCategory(category)
}