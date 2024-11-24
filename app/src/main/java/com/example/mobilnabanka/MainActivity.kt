package com.example.mobilnabanka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.mobilnabanka.ui.theme.MobilnaBankaTheme
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State


class MainActivity : ComponentActivity() {
    private val bankAccountViewModel: BankAccountViewModel by lazy {
        BankAccountViewModel(applicationContext)  // Initialize ViewModel with application context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobilnaBankaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BankAccountScreen(innerPadding, bankAccountViewModel) // Pass the ViewModel to the screen
                }
            }
        }
    }
}


@Composable
fun BankAccountScreen(innerPadding: PaddingValues, bankAccountViewModel: BankAccountViewModel) {
    val stanje by bankAccountViewModel.balance
    val transactions by remember { mutableStateOf(bankAccountViewModel.transactions) }

    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf("") }
    var showTransactions by remember { mutableStateOf(false) } // State to toggle transaction visibility

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .background(Color(0xFF003366))
        ) {
            Text(
                text = "Mobilna Banka",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Številka računa: SI56192001234567892",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Trenutno stanje: ${"%.2f".format(stanje)}€",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Vnesite vsoto") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                val amount = userInput.text.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    bankAccountViewModel.updateBalance(stanje + amount, "Polog: +${"%.2f".format(amount)}€")
                    errorMessage = ""
                } else {
                    errorMessage = "Prosim vnesite veljavno pozitivno število."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Polog")
        }

        Button(
            onClick = {
                val amount = userInput.text.toDoubleOrNull()
                if (amount != null && amount > 0 && amount <= stanje) {
                    bankAccountViewModel.updateBalance(stanje - amount, "Dvig: -${"%.2f".format(amount)}€")
                    errorMessage = ""
                } else if (amount == null || amount <= 0) {
                    errorMessage = "Prosim vnesite veljavno pozitivno število."
                } else {
                    errorMessage = "Nezadostno stanje na računu"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Dvig")
        }

        Button(
            onClick = { showTransactions = !showTransactions }, // Toggle visibility
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (showTransactions) "Skrij transakcije" else "Prikaži transakcije")
        }

        if (showTransactions) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Zadnjih 5 transakcij:", style = MaterialTheme.typography.headlineMedium)
            for (transaction in transactions.reversed()) {
                Text(text = transaction, modifier = Modifier.padding(4.dp))
            }
        }
    }
}



class BankAccountViewModel(private val context: Context) : ViewModel() {

    private val _balance = mutableStateOf(500.0) // Default balance is 500.0
    val balance: State<Double> get() = _balance

    private val _transactions = mutableStateListOf<String>() // List to store transaction history
    val transactions: List<String> get() = _transactions

    init {
        // Load the saved balance and transactions when the ViewModel is created
        _balance.value = loadBalance()
        _transactions.addAll(loadTransactions())
    }

    // Retrieve the saved balance from SharedPreferences
    private fun loadBalance(): Double {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        return sharedPreferences.getFloat("stanje", 500.0f).toDouble()  // Default balance is 500.0
    }

    // Save the balance to SharedPreferences
    private fun saveBalance(stanje: Double) {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat("stanje", stanje.toFloat())  // Store balance as a float
            apply()  // Apply changes asynchronously
        }
    }

    // Retrieve transaction history from SharedPreferences
    private fun loadTransactions(): List<String> {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        val savedTransactions = sharedPreferences.getStringSet("transakcije", emptySet())
        return savedTransactions?.toList() ?: emptyList()
    }

    // Save transaction history to SharedPreferences
    private fun saveTransactions() {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("transakcije", _transactions.toSet())
            apply()
        }
    }

    // Update balance and save it
    fun updateBalance(newBalance: Double, transaction: String) {
        _balance.value = newBalance
        saveBalance(newBalance)  // Save the new balance
        addTransaction(transaction)  // Add the transaction
    }

    // Add a new transaction and save the history
    private fun addTransaction(transaction: String) {
        if (_transactions.size >= 5) {
            _transactions.removeFirst() // Remove the oldest transaction if we exceed 5
        }
        _transactions.add(transaction)
        saveTransactions() // Save updated transactions
    }
}


