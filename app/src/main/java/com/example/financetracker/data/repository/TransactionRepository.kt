package com.example.financetracker.data.repository

import com.example.financetracker.data.database.TransactionDao
import com.example.financetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {
    suspend fun insert(transaction: Transaction) = dao.insert(transaction)

    suspend fun delete(transaction: Transaction) = dao.delete(transaction)

    suspend fun deleteById(id: Int) = dao.deleteById(id)

    fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions()

    fun getBalance(): Flow<Double?> = dao.getBalance()
}