package com.example.mobilnabanka.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mobilnabanka.model.UserTransaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: UserTransaction)

    @Update
    suspend fun update(transaction: UserTransaction)

    @Delete
    suspend fun delete(transaction: UserTransaction)

    @Query("SELECT * FROM userTransactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Int): UserTransaction?

    @Query("SELECT * FROM userTransactions WHERE userId = :userId")
    suspend fun getTransactionsByUserId(userId: Int): List<UserTransaction>

    @Query("SELECT SUM(amount) FROM userTransactions WHERE userId = :userId")
    suspend fun getTotalAmountForUser(userId: Int): Double?
}