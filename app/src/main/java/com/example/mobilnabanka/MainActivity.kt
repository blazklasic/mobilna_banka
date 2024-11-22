package com.example.mobilnabanka

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.mobilnabanka.ui.theme.MobilnaBankaTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowBack // Correct import for ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    private val bankAccountViewModel: BankAccountViewModel by lazy {
        BankAccountViewModel(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobilnaBankaTheme {
                // Navigation Setup
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(navController = navController, startDestination = "bank_account_screen") {
                        composable("bank_account_screen") {
                            BankAccountScreen(innerPadding, bankAccountViewModel, navController)
                        }
                        composable("transaction_screen") {
                            // Pass navController to TransactionScreen
                            TransactionScreen(bankAccountViewModel, navController)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountScreen(innerPadding: PaddingValues, bankAccountViewModel: BankAccountViewModel, navController: NavHostController) {
    val stanje by bankAccountViewModel.balance
    val transactions by remember { mutableStateOf(bankAccountViewModel.transactions) }

    var userInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF003366), // Dark blue for top bar
                    titleContentColor = Color.White, // White for title
                ),
                title = {
                    Text(
                        "Mobilna Banka",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back button action */ }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack, // Correct ArrowBack icon
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle menu button action */ }) {
                        Icon(
                            imageVector = Icons.Filled.Menu, // Menu icon
                            contentDescription = "Menu"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        // The main content of the screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                    val amount = userInput.toDoubleOrNull()
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
                    val amount = userInput.toDoubleOrNull()
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
                onClick = { navController.navigate("transaction_screen") }, // Navigate to Transaction Screen
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Prikaži transakcije")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(bankAccountViewModel: BankAccountViewModel, navController: NavHostController) {
    val depositSum = bankAccountViewModel.getDepositSum()
    val withdrawalSum = bankAccountViewModel.getWithdrawalSum()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF003366),
                    titleContentColor = Color.White,
                ),
                title = {
                    Text(
                        "Zgodovina transakcij",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* tri crtice desno zgornji kot */ }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Zgodovina transakcij:",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Skupaj pologi: ${"%.2f".format(depositSum)}€",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Skupaj dvigi: ${"%.2f".format(withdrawalSum)}€",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BarChart(
                depositSum = depositSum,
                withdrawalSum = withdrawalSum,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(start = 65.dp)
            )
        }
    }
}


@Composable
fun BarChart(depositSum: Double, withdrawalSum: Double, modifier: Modifier = Modifier) {
    val data = listOf(
        BarData("Pologi", depositSum),
        BarData("Dvigi", withdrawalSum)
    )

    // Call the Canvas-based chart
    Canvas(modifier = modifier) {
        // Define max value for scaling the bars
        val maxValue = data.maxOfOrNull { it.value } ?: 1.0 // Ensure there's no divide by zero
        val barWidth = size.width / (data.size * 2) // Bar width based on the screen size and number of bars
        val spaceBetweenBars = barWidth // Space between bars

        // Draw bars
        data.forEachIndexed { index, item ->
            val barHeight = size.height * (item.value / maxValue) // Bar height proportional to value
            val barLeft = index * (barWidth * 2) // Position of the bar (left)
            val barTop = size.height - barHeight // Position of the bar (top)

            // Draw the bar using a rectangle
            drawRect(
                color = if (item.category == "Pologi") Color.Green else Color.Red,
                topLeft = Offset(barLeft, barTop.toFloat()),
                size = Size(barWidth, barHeight.toFloat())
            )

            // Draw the label under the bar
            drawIntoCanvas { canvas ->
                val paint = android.text.TextPaint().apply {
                    color = Color.Black.toArgb() // Convert Compose Color to Android Color
                    textSize = 40f // Text size in pixels
                }

                // Draw the category text below the bar
                canvas.nativeCanvas.drawText(
                    item.category,
                    barLeft + barWidth / 2,
                    size.height - 10f, // Position below the bar
                    paint
                )
            }
        }
    }
}

data class BarData(val category: String, val value: Double)

class BankAccountViewModel(private val context: Context) : ViewModel() {
    private val _balance = mutableStateOf(500.0)
    val balance: State<Double> get() = _balance

    private val _transactions = mutableStateListOf<String>()
    val transactions: List<String> get() = _transactions

    init {
        _balance.value = loadBalance()
        _transactions.addAll(loadTransactions())
    }

    private fun loadBalance(): Double {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        return sharedPreferences.getFloat("stanje", 500.0f).toDouble()
    }

    private fun saveBalance(stanje: Double) {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat("stanje", stanje.toFloat())
            apply()
        }
    }

    private fun loadTransactions(): List<String> {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        val savedTransactions = sharedPreferences.getStringSet("transakcije", emptySet())
        return savedTransactions?.toList() ?: emptyList()
    }

    private fun saveTransactions() {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("transakcije", _transactions.toSet())
            apply()
        }
    }

    fun updateBalance(newBalance: Double, transaction: String) {
        _balance.value = newBalance
        saveBalance(newBalance)
        addTransaction(transaction)
    }

    private fun addTransaction(transaction: String) {
        _transactions.add(transaction)
        saveTransactions()
    }
    fun getDepositSum(): Double {
        return _transactions.filter { it.startsWith("Polog") }
            .sumOf { transaction -> transaction.substringAfter(": +").removeSuffix("€").toDoubleOrNull() ?: 0.0 }
    }

    fun getWithdrawalSum(): Double {
        return _transactions.filter { it.startsWith("Dvig") }
            .sumOf { transaction -> transaction.substringAfter(": -").removeSuffix("€").toDoubleOrNull() ?: 0.0 }
    }
}
