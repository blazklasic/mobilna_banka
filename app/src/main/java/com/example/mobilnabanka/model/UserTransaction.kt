package com.example.mobilnabanka.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "userTransactions",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class UserTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val timestamp: String,
    val type: String, // "Polog" ali "Dvig"
    val userId: Int
)
