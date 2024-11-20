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
    // Observe the balance from the ViewModel
    val stanje by bankAccountViewModel.balance

    // Declare a state variable for the user input (text input as String)
    var userInput by remember { mutableStateOf(TextFieldValue("")) }

    // Declare a state variable to show an error message if input is invalid
    var errorMessage by remember { mutableStateOf("") }

    // Main layout of the screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // Top dark blue area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp) // Adjust the height as needed
                .background(Color(0xFF003366)) // Dark blue color
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

        Spacer(modifier = Modifier.height(16.dp)) // Space between the top section and the account info
        Text(
            text = "Številka računa: SI56192001234567892", // Example account number
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Display the current stanje (balance) from the ViewModel
        Text(
            text = "Trenutno stanje: ${"%.2f".format(stanje)}€",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // TextField to input amount (polog or dvig)
        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Vnesite vsoto") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Display error message if the input is invalid
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Button to polog money
        Button(
            onClick = {
                val amount = userInput.text.toDoubleOrNull() // Convert the input to Double
                if (amount != null && amount > 0) {
                    bankAccountViewModel.updateBalance(stanje + amount) // polog the amount
                    errorMessage = "" // Clear error message
                } else {
                    errorMessage = "Prosim vnesite veljavno pozitivno število." // Show error if input is invalid
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Polog")
        }

        // Button to dvig money
        Button(
            onClick = {
                val amount = userInput.text.toDoubleOrNull()
                if (amount != null && amount > 0 && amount <= stanje) {
                    bankAccountViewModel.updateBalance(stanje - amount) // dvig the amount
                    errorMessage = "" // Clear error message
                } else if (amount == null || amount <= 0) {
                    errorMessage = "Prosim vnesite veljavno pozitivno število."
                } else {
                    errorMessage = "Nezadostno stanje na računu"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Dvig")
        }
    }
}


class BankAccountViewModel(private val context: Context) : ViewModel() {

    private val _balance = mutableStateOf(500.0) // Default balance is 500.0
    val balance: State<Double> get() = _balance

    init {
        // Load the saved balance from SharedPreferences when the ViewModel is created
        _balance.value = getBalance()
    }

    // Retrieve the saved balance from SharedPreferences
    private fun getBalance(): Double {
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

    // Update balance and save it
    fun updateBalance(newBalance: Double) {
        _balance.value = newBalance
        saveBalance(newBalance)  // Save the new balance
    }
}

