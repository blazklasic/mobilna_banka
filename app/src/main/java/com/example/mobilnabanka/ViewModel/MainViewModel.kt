package com.example.mobilnabanka.ViewModel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mobilnabanka.database.AppDatabase
import com.example.mobilnabanka.model.User
import com.example.mobilnabanka.model.UserTransaction
import com.example.mobilnabanka.repository.Repository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
    private val appDatabase = AppDatabase.getInstance(application)
    private val userDao = appDatabase.userDao()
    private val transactionDao = appDatabase.transactionDao()

    val isUserLoggedIn: Boolean
        get() = sharedPreferences.getString("account_number", null) != null

    private val repository: Repository = Repository(appDatabase)

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    private val _transactions = MutableLiveData<List<UserTransaction>>()
    val transactions: LiveData<List<UserTransaction>> get() = _transactions

    private val _showDialog = MutableLiveData(false)
    val showDialog: LiveData<Boolean> get() = _showDialog

    fun fetchUsers() {
        viewModelScope.launch {
            try {
                val userList = repository.getAllUsers()
                _users.postValue(userList)
            } catch (e: Exception) {
                _users.postValue(emptyList())
            }
        }
    }

    fun fetchUserTransactions(userId: Int) {
        if (userId == null) {
            Log.e("MainViewModel", "Cannot fetch transactions: userId is null.")
            return
        }
        viewModelScope.launch {
            try {
                val transactionList = repository.getTransactionsByUserId(userId)
                _transactions.postValue(transactionList)
            } catch (e: Exception) {
                _transactions.postValue(emptyList())
            }
        }
    }

    fun addUser(user: User) {
        viewModelScope.launch {
            repository.insertUser(user)
            fetchUsers() // Refresh the user list
        }
    }

    fun addTransaction(transaction: UserTransaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            fetchUserTransactions(transaction.userId)
        }
    }
    fun toggleDialog(show: Boolean) {
        _showDialog.value = show
    }
}
