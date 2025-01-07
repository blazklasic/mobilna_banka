package com.example.mobilnabanka

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.mobilnabanka.ui.theme.MobilnaBankaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    private val bankAccountViewModel: BankAccountViewModel by lazy {
        BankAccountViewModel(applicationContext)
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var savedAccountNumber: String? = null
    var showDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MobilnaBankaTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(navController = navController, startDestination = if (savedAccountNumber != null) "bank_account_screen" else "login_screen") {
                        composable("login_screen") {
                            LoginScreen(navController)
                        }
                        composable("bank_account_screen") {
                            BankAccountScreen(innerPadding, bankAccountViewModel, navController)
                        }
                        composable("transaction_screen") {
                            TransactionScreen(bankAccountViewModel, navController)
                        }
                    }
                }
                if (showDialog) {
                    ShowSaveDeleteDialog(onDismiss = { showDialog = false })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sharedPreferences = getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        savedAccountNumber = sharedPreferences.getString("account_number", null)
    }

    override fun onStop() {
        super.onStop()
        if (savedAccountNumber != null) {
            showDialog = true
        }
    }
}

@Composable
fun ShowSaveDeleteDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Želite shraniti račun?") },
        text = { Text("Ali želite shraniti podatke o računa za lažjo ponovno prijavo?") },
        confirmButton = {
            Button(onClick = {
                Toast.makeText(context, "Podatki računa shranjeni.", Toast.LENGTH_SHORT).show()
                onDismiss()
                (context as? Activity)?.finish()
            }) {
                Text("Shrani")
            }
        },
        dismissButton = {
            Button(onClick = {
                val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
                sharedPreferences.edit().remove("account_number").apply()
                Toast.makeText(context, "Izpis uspešen.", Toast.LENGTH_SHORT).show()
                onDismiss()
                (context as? Activity)?.finish()
            }) {
                Text("Izpis")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    var accountNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF003366),
                    titleContentColor = Color.White,
                ),
                title = {
                    Text("Vpis v Mobilno Banko")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vnesite številko svojega bančnega računa za vpis:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Številka računa") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.isNotEmpty()
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (accountNumber.isNotEmpty()) {
                        with(sharedPreferences.edit()) {
                            putString("account_number", accountNumber)
                            apply()
                        }
                        navController.navigate("bank_account_screen")
                    } else {
                        errorMessage = "Vnesti morate številko bančnega računa."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Vpis")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountScreen(innerPadding: PaddingValues, bankAccountViewModel: BankAccountViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val stanje by bankAccountViewModel.balance.collectAsState()
    val transactions by remember { mutableStateOf(bankAccountViewModel.transactions) }

    val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
    val accountNumber = sharedPreferences.getString("account_number", "Unknown Account") ?: "Unknown Account"

    var userInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF003366),
                    titleContentColor = Color.White,
                ),
                title = {
                    Text(
                        "Mobilna Banka",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? MainActivity)?.let { activity ->
                            activity.showDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu action */ }) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Številka računa: $accountNumber",
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
                        bankAccountViewModel.updateBalance(stanje + amount, "Polog", amount)
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
                        bankAccountViewModel.updateBalance(stanje - amount, "Dvig", amount)
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
                onClick = { navController.navigate("transaction_screen") },
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

    var showDeposits by remember { mutableStateOf(false) }
    var showWithdrawals by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { /* Menu action */ }) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { showDeposits = true; showWithdrawals = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Pologi")
                }

                Button(
                    onClick = { showWithdrawals = true; showDeposits = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Dvigi")
                }
            }

            if (showDeposits) {
                TransactionList(
                    transactions = bankAccountViewModel.transactions
                        .filter { it.type == "Polog" }
                        .reversed()
                )
            } else if (showWithdrawals) {
                TransactionList(
                    transactions = bankAccountViewModel.transactions
                        .filter { it.type == "Dvig" }
                        .reversed()
                )
            } else {
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
}

@Composable
fun TransactionList(transactions: List<Transaction>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        transactions.forEach { transaction ->
            Text(
                text = "${transaction.type}: ${"%.2f".format(transaction.amount)}€ - ${transaction.timestamp}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
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

    Canvas(modifier = modifier) {
        val maxValue = data.maxOfOrNull { it.value } ?: 1.0
        val barWidth = size.width / (data.size * 2)
        val spaceBetweenBars = barWidth

        // Draw bars
        data.forEachIndexed { index, item ->
            val barHeight = size.height * (item.value / maxValue)
            val barLeft = index * (barWidth * 2)
            val barTop = size.height - barHeight

            drawRect(
                color = if (item.category == "Pologi") Color.Green else Color.Red,
                topLeft = Offset(barLeft, barTop.toFloat()),
                size = Size(barWidth, barHeight.toFloat())
            )

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    item.category,
                    barLeft + barWidth / 2,
                    size.height - 10f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

data class BarData(val category: String, val value: Double)

data class Transaction(val type: String, val amount: Double, val timestamp: String)

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

    private fun loadBalance(): Double {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        return sharedPreferences.getFloat("stanje", 500.0f).toDouble()
    }

    private fun loadTransactions(): List<Transaction> {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        val savedTransactions = sharedPreferences.getStringSet("transakcije", emptySet())
        return savedTransactions?.mapNotNull {
            it.split("|").takeIf { it.size == 3 }?.let { parts ->
                Transaction(parts[0], parts[1].toDoubleOrNull() ?: 0.0, parts[2])
            }
        } ?: emptyList()
    }

    fun saveBalance(stanje: Double) {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat("stanje", stanje.toFloat())
            apply()
        }
        _balance.value = stanje
    }

    fun updateBalance(newBalance: Double, type: String, amount: Double) {
        _balance.value = newBalance
        saveBalance(newBalance)

        val timestamp = System.currentTimeMillis().let {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(it)
        }
        val transaction = Transaction(type, amount, timestamp)
        addTransaction(transaction)
    }

    private fun addTransaction(transaction: Transaction) {
        _transactions.value = _transactions.value + transaction
        saveTransactions()
    }

    private fun saveTransactions() {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("transakcije", _transactions.value.map {
                "${it.type}|${it.amount}|${it.timestamp}"
            }.toSet())
            apply()
        }
    }

    fun getDepositSum(): Double {
        return _transactions.value.filter { it.type == "Polog" }
            .sumOf { it.amount }
    }

    fun getWithdrawalSum(): Double {
        return _transactions.value.filter { it.type == "Dvig" }
            .sumOf { it.amount }
    }
}
