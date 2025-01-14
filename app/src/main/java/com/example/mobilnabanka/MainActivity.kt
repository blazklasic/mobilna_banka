package com.example.mobilnabanka

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.mobilnabanka.ui.theme.MobilnaBankaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.api.IMapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class MainActivity : ComponentActivity() {
    private val bankAccountViewModel: BankAccountViewModel by lazy {
        BankAccountViewModel(applicationContext)
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var savedAccountNumber: String? = null
    var showDialog by mutableStateOf(false)

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MobilnaBankaTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (savedAccountNumber != null) "bank_account_screen" else "login_screen"
                    ) {
                        composable("login_screen") {
                            LoginScreen(navController)
                        }
                        composable("bank_account_screen") {
                            BankAccountScreen(innerPadding, bankAccountViewModel, navController)
                        }
                        composable("transaction_screen") {
                            TransactionScreen(bankAccountViewModel, navController)
                        }
                        composable("osm_map_screen") {
                            OpenStreetMapScreen(navController)
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
fun OpenStreetMapScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val mapView = MapView(context)
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                mapView.setMultiTouchControls(true)

                mapView.controller.setZoom(12)
                mapView.controller.setCenter(GeoPoint(46.0511, 14.5051))

                mapView
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Gumb za nazaj
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Nazaj")
        }
    }
}
@RequiresApi(Build.VERSION_CODES.GINGERBREAD)
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

@RequiresApi(Build.VERSION_CODES.GINGERBREAD)
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
            Button(
                onClick = { navController.navigate("osm_map_screen") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text(text = "Odpri OpenStreetMap")
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(bankAccountViewModel: BankAccountViewModel, navController: NavHostController) {
    val depositSum = bankAccountViewModel.getDepositSum()
    val withdrawalSum = bankAccountViewModel.getWithdrawalSum()
    val balance = bankAccountViewModel.balance.value

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
                .padding(16.dp)
        ) {
            // Display current balance
            Text(
                text = "Trenutno stanje: ${"%.2f".format(balance)}€",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Show deposit total
            Text(
                text = "Skupni znesek pologov: ${"%.2f".format(depositSum)}€",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = { showDeposits = !showDeposits },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showDeposits) "Skrij pologe" else "Pokaži pologe")
            }

            // Show deposits only if showDeposits is true
            if (showDeposits) {
                bankAccountViewModel.getDepositTransactions().reversed().forEach { transaction ->
                    Text(
                        text = "Datum: ${transaction.timestamp}, Znesek: ${"%.2f".format(transaction.amount)}€",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show withdrawal total
            Text(
                text = "Skupni znesek dvigov: ${"%.2f".format(withdrawalSum)}€",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = { showWithdrawals = !showWithdrawals },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showWithdrawals) "Skrij dvige" else "Pokaži dvige")
            }

            // Show withdrawals only if showWithdrawals is true
            if (showWithdrawals) {
                bankAccountViewModel.getWithdrawalTransactions().reversed().forEach { transaction ->
                    Text(
                        text = "Datum: ${transaction.timestamp}, Znesek: ${"%.2f".format(transaction.amount)}€",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
data class BarData(val category: String, val value: Double)

data class Transaction(val type: String, val amount: Double, val timestamp: String, val category: String)
data class Account(val name: String, val balance: Double)

class BankAccountViewModel(private val context: Context) : ViewModel() {

    private val _balance = MutableStateFlow(500.0) // Default balance
    val balance: StateFlow<Double> = _balance

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: List<Transaction>
        get() = _transactions.value

    init {
        _balance.value = loadBalance()
        _transactions.value = loadTransactions()
    }
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    init {
        _accounts.value = loadAccounts()  // Load saved accounts on initialization
    }

    // Load accounts from SharedPreferences
    private fun loadAccounts(): List<Account> {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        val savedAccounts = sharedPreferences.getStringSet("accounts", emptySet())

        return savedAccounts?.mapNotNull {
            it.split("|").takeIf { it.size == 2 }?.let { parts ->
                Account(parts[0], parts[1].toDoubleOrNull() ?: 0.0)
            }
        } ?: emptyList()
    }

    // Save accounts to SharedPreferences
    private fun saveAccounts() {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("accounts", _accounts.value.map {
                "${it.name}|${it.balance}"
            }.toSet())
            apply()
        }
    }

    // Add a new account
    fun addAccount(name: String, balance: Double) {
        val newAccount = Account(name, balance)
        _accounts.value = _accounts.value + newAccount
        saveAccounts()  // Save after adding the account
    }

    // Update the balance of an existing account
    fun updateAccountBalance(name: String, newBalance: Double) {
        _accounts.value = _accounts.value.map {
            if (it.name == name) {
                it.copy(balance = newBalance)  // Update balance using copy
            } else {
                it
            }
        }
        saveAccounts()  // Save after updating the balance
    }

    // Get account by name
    fun getAccountByName(name: String): Account? {
        return _accounts.value.find { it.name == name }
    }

    // Add deposit to a specific account
    fun addDepositToAccount(name: String, amount: Double) {
        val account = getAccountByName(name)
        if (account != null) {
            val updatedBalance = account.balance + amount
            updateAccountBalance(name, updatedBalance)  // Update balance
        }
    }

    // Add withdrawal from a specific account
    fun addWithdrawalFromAccount(name: String, amount: Double) {
        val account = getAccountByName(name)
        if (account != null) {
            val updatedBalance = account.balance - amount
            if (updatedBalance >= 0) {
                updateAccountBalance(name, updatedBalance)  // Update balance
            } else {
                // Handle insufficient balance case
                println("Insufficient balance for withdrawal!")
            }
        }
    }
    // Load the initial balance from SharedPreferences
    private fun loadBalance(): Double {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        return sharedPreferences.getFloat("stanje", 500.0f).toDouble()
    }

    // Load transactions from SharedPreferences
    private fun loadTransactions(): List<Transaction> {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        val savedTransactions = sharedPreferences.getStringSet("transakcije", emptySet())
        return savedTransactions?.mapNotNull {
            it.split("|").takeIf { it.size == 4 }?.let { parts ->
                Transaction(parts[0], parts[1].toDoubleOrNull() ?: 0.0, parts[2], parts[3])
            }
        } ?: emptyList()
    }

    // Add a deposit transaction and update the balance
    fun addDeposit(amount: Double, timestamp: String) {
        val transaction = Transaction("Polog", amount, timestamp, "Income")
        addTransaction(transaction)
        _balance.value += amount
        saveBalance(_balance.value)  // Save balance after update
    }

    // Add a withdrawal transaction and update the balance
    fun addWithdrawal(amount: Double, timestamp: String) {
        val transaction = Transaction("Dvig", amount, timestamp, "Expense")
        addTransaction(transaction)
        _balance.value -= amount
        saveBalance(_balance.value)  // Save balance after update
    }

    // Save a transaction to the list
    private fun addTransaction(transaction: Transaction) {
        _transactions.value = _transactions.value + transaction
        saveTransactions()
    }

    // Save transactions to SharedPreferences
    private fun saveTransactions() {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("transakcije", _transactions.value.map {
                "${it.type}|${it.amount}|${it.timestamp}|${it.category}"
            }.toSet())
            apply()
        }
    }

    // Save the current balance to SharedPreferences
    private fun saveBalance(stanje: Double) {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat("stanje", stanje.toFloat())
            apply()
        }
    }

    // Update balance and transactions (for external calls)
    fun updateBalance(newBalance: Double, type: String, amount: Double) {
        _balance.value = newBalance
        saveBalance(newBalance)  // Save new balance

        val timestamp = System.currentTimeMillis().let {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(it)
        }

        val category = if (type == "Polog") "Income" else "Expense"
        val transaction = Transaction(type = type, amount = amount, timestamp = timestamp, category = category)
        addTransaction(transaction)
    }

    // Get transactions by category (Income or Expense)
    fun getTransactionsByCategory(category: String): List<Transaction> {
        return _transactions.value.filter { it.category == category }
    }

    // Get the sum of all deposits
    fun getDepositSum(): Double {
        return _transactions.value.filter { it.type == "Polog" }
            .sumOf { it.amount }
    }

    // Get the sum of all withdrawals
    fun getWithdrawalSum(): Double {
        return _transactions.value.filter { it.type == "Dvig" }
            .sumOf { it.amount }
    }

    // Get all deposit transactions
    fun getDepositTransactions(): List<Transaction> {
        return _transactions.value.filter { it.type == "Polog" }
    }

    // Get all withdrawal transactions
    fun getWithdrawalTransactions(): List<Transaction> {
        return _transactions.value.filter { it.type == "Dvig" }
    }
}