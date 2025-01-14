package com.example.mobilnabanka.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Datoteka User.kt
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String
)
