package com.example.mobilnabanka.repository

import com.example.mobilnabanka.database.AppDatabase
import com.example.mobilnabanka.model.User
import com.example.mobilnabanka.model.UserTransaction

class Repository(private val db: AppDatabase) {
    suspend fun insertUser(user: User) = db.userDao().insert(user)
    suspend fun updateUser(user: User) = db.userDao().update(user)
    suspend fun deleteUser(user: User) = db.userDao().delete(user)
    suspend fun getUserById(userId: Int): User? = db.userDao().getUserById(userId)
    suspend fun getAllUsers(): List<User> = db.userDao().getAllUsers()

    suspend fun insertTransaction(transaction: UserTransaction) = db.transactionDao().insert(transaction)
    suspend fun updateTransaction(transaction: UserTransaction) = db.transactionDao().update(transaction)
    suspend fun deleteTransaction(transaction: UserTransaction) = db.transactionDao().delete(transaction)
    suspend fun getTransactionById(transactionId: Int): UserTransaction? = db.transactionDao().getTransactionById(transactionId)
    suspend fun getTransactionsByUserId(userId: Int): List<UserTransaction> = db.transactionDao().getTransactionsByUserId(userId)
    suspend fun getTotalAmountForUser(userId: Int): Double? = db.transactionDao().getTotalAmountForUser(userId)
}
