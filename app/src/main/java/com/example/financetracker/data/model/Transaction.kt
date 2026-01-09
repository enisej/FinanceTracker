package com.example.financetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val type: String,
    val description: String,
    val categoryName: String = "Uncategorized",
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable