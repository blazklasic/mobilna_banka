package com.example.mobilnabanka.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.mobilnabanka.CurrencyApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Account(val name: String, val balance: Double)
data class Transaction(val type: String, val amount: Double, val timestamp: String, val category: String)
class BankAccountViewModel(private val context: Context) : ViewModel() {

    private val _balance = MutableStateFlow(500.0)
    val balance: StateFlow<Double> = _balance

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: List<Transaction>
        get() = _transactions.value

    init {
        _balance.value = loadBalance()
        _transactions.value = loadTransactions()
    }
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())

    init {
        _accounts.value = loadAccounts()
    }

    private fun loadAccounts(): List<Account> {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        val savedAccounts = sharedPreferences.getStringSet("accounts", emptySet())

        return savedAccounts?.mapNotNull {
            it.split("|").takeIf { it.size == 2 }?.let { parts ->
                Account(parts[0], parts[1].toDoubleOrNull() ?: 0.0)
            }
        } ?: emptyList()
    }


    private fun loadBalance(): Double {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        return sharedPreferences.getFloat("stanje", 500.0f).toDouble()
    }

    private fun loadTransactions(): List<Transaction> {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        val savedTransactions = sharedPreferences.getStringSet("transakcije", emptySet())
        return savedTransactions?.mapNotNull {
            it.split("|").takeIf { it.size == 4 }?.let { parts ->
                Transaction(parts[0], parts[1].toDoubleOrNull() ?: 0.0, parts[2], parts[3])
            }
        } ?: emptyList()
    }

    private fun addTransaction(transaction: Transaction) {
        _transactions.value = _transactions.value + transaction
        saveTransactions()
    }
    suspend fun convertEurToUsd(amountEur: Double, onConversionComplete: (Double?) -> Unit) {
        try {
            val response = CurrencyApiClient.service.getExchangeRate("51dd8350c106bb0f459783ab")
            val usdRate = response.rates["USD"]

            if (usdRate != null) {
                val usdAmount = amountEur * usdRate
                onConversionComplete(usdAmount)
            } else {
                onConversionComplete(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onConversionComplete(null)
        }
    }

    private fun saveTransactions() {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("transakcije", _transactions.value.map {
                "${it.type}|${it.amount}|${it.timestamp}|${it.category}"
            }.toSet())
            apply()
        }
    }

    private fun saveBalance(stanje: Double) {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat("stanje", stanje.toFloat())
            apply()
        }
    }

    fun updateBalance(newBalance: Double, type: String, amount: Double) {
        _balance.value = newBalance
        saveBalance(newBalance)

        val timestamp = System.currentTimeMillis().let {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(it)
        }

        val category = if (type == "Polog") "Income" else "Expense"
        val transaction = Transaction(type = type, amount = amount, timestamp = timestamp, category = category)
        addTransaction(transaction)
    }

    fun getDepositSum(): Double {
        return _transactions.value.filter { it.type == "Polog" }
            .sumOf { it.amount }
    }

    fun getWithdrawalSum(): Double {
        return _transactions.value.filter { it.type == "Dvig" }
            .sumOf { it.amount }
    }

    fun getDepositTransactions(): List<Transaction> {
        return _transactions.value.filter { it.type == "Polog" }
    }

    fun getWithdrawalTransactions(): List<Transaction> {
        return _transactions.value.filter { it.type == "Dvig" }
    }

    fun convertEuroToUSD(amountEur: Double, onConversionComplete: (Double?) -> Unit){
        try{
            if (amountEur != null){
                val usdAmount = amountEur * 1.03
                onConversionComplete(usdAmount)
            }
        }catch (e: Exception){
            e.printStackTrace()
            onConversionComplete(null)
        }
    }

    suspend fun callApi(){
        convertEurToUsd(0.12, onConversionComplete = {})
    }
}