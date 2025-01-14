package com.example.mobilnabanka

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.mobilnabanka.ui.theme.MobilnaBankaTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface CurrencyApiService {
    @GET("v4/latest/EUR")
    suspend fun getExchangeRate(@Query("apikey") apiKey: String): CurrencyExchangeResponse
}

data class CurrencyExchangeResponse(
    val rates: Map<String, Double>
)

object CurrencyApiClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.exchangerate-api.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: CurrencyApiService = retrofit.create(CurrencyApiService::class.java)
}

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
                        composable("currency_converter"){
                            CurrencyConverterScreen(navController, bankAccountViewModel)
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
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val currentLocation = remember { mutableStateOf(GeoPoint(46.0511, 14.5051)) }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation.value = GeoPoint(location.latitude, location.longitude)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val mapView = MapView(context)
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                mapView.setMultiTouchControls(true)

                mapView.controller.setZoom(12)
                mapView.controller.setCenter(currentLocation.value)

                mapView
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

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
    var userInputEur by remember { mutableStateOf("") }

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
            Button(
                onClick = {navController.navigate("currency_converter")} ,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ){
                Text(text = "Odpri menjalnico")
            }
        }
    }
}
@Composable
fun CurrencyConverterScreen(navController: NavHostController, bankAccountViewModel: BankAccountViewModel) {
    var userInputEur by remember { mutableStateOf("") }
    var userInputUsd by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold() { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = userInputEur,
                onValueChange = { userInputEur = it },
                label = { Text("Vnesite vsoto v EUR") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = userInputUsd,
                onValueChange = { },
                label = { Text("Vrednost v USD") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            Button(
                onClick = {
                    val amountEur = userInputEur.toDoubleOrNull()
                    if (amountEur != null && amountEur > 0) {

                        bankAccountViewModel.convertEuroToUSD(amountEur) { usdAmount ->
                            if (usdAmount != null) {
                                userInputUsd = "%.2f".format(usdAmount)
                            } else {
                                errorMessage = "Napaka pri pridobivanju tečajev."
                            }
                        }
                    } else {
                        errorMessage = "Prosim vnesite veljavno število v EUR."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = "Pretvori v USD")
            }
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Nazaj")
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
                    IconButton(onClick = {  }) {
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
            Text(
                text = "Trenutno stanje: ${"%.2f".format(balance)}€",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

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

            if (showWithdrawals) {
                bankAccountViewModel.getWithdrawalTransactions().reversed().forEach { transaction ->
                    Text(
                        text = "Datum: ${transaction.timestamp}, Znesek: ${"%.2f".format(transaction.amount)}€",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
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
fun TransactionList(transactions: List<Transaction>, filterCategory: String? = null) {
    val filteredTransactions = filterCategory?.let {
        transactions.filter { it.type == filterCategory }
    } ?: transactions

    Column(modifier = Modifier.fillMaxWidth()) {
        filteredTransactions.forEach { transaction ->
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
                val paint = android.text.TextPaint().apply {
                    color = Color.Black.toArgb()
                    textSize = 40f
                }

                canvas.nativeCanvas.drawText(
                    item.category,
                    barLeft + barWidth / 2,
                    size.height - 10f,
                    paint
                )
            }
        }
    }
}

data class BarData(val category: String, val value: Double)

data class Transaction(val type: String, val amount: Double, val timestamp: String, val category: String)
data class Account(val name: String, val balance: Double)

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
    val accounts: StateFlow<List<Account>> = _accounts

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

    private fun saveAccounts() {
        val sharedPreferences = context.getSharedPreferences("MobilnaBanka", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("accounts", _accounts.value.map {
                "${it.name}|${it.balance}"
            }.toSet())
            apply()
        }
    }

    fun addAccount(name: String, balance: Double) {
        val newAccount = Account(name, balance)
        _accounts.value = _accounts.value + newAccount
        saveAccounts()
    }

    fun updateAccountBalance(name: String, newBalance: Double) {
        _accounts.value = _accounts.value.map {
            if (it.name == name) {
                it.copy(balance = newBalance)
            } else {
                it
            }
        }
        saveAccounts()
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

    fun getAccountByName(name: String): Account? {
        return _accounts.value.find { it.name == name }
    }

    fun addDepositToAccount(name: String, amount: Double) {
        val account = getAccountByName(name)
        if (account != null) {
            val updatedBalance = account.balance + amount
            updateAccountBalance(name, updatedBalance)
        }
    }

    fun addWithdrawalFromAccount(name: String, amount: Double) {
        val account = getAccountByName(name)
        if (account != null) {
            val updatedBalance = account.balance - amount
            if (updatedBalance >= 0) {
                updateAccountBalance(name, updatedBalance)
            } else {
                println("Insufficient balance for withdrawal!")
            }
        }
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

    fun addDeposit(amount: Double, timestamp: String) {
        val transaction = Transaction("Polog", amount, timestamp, "Income")
        addTransaction(transaction)
        _balance.value += amount
        saveBalance(_balance.value)
    }

    fun addWithdrawal(amount: Double, timestamp: String) {
        val transaction = Transaction("Dvig", amount, timestamp, "Expense")
        addTransaction(transaction)
        _balance.value -= amount
        saveBalance(_balance.value)
    }

    private fun addTransaction(transaction: Transaction) {
        _transactions.value = _transactions.value + transaction
        saveTransactions()
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

    fun getTransactionsByCategory(category: String): List<Transaction> {
        return _transactions.value.filter { it.category == category }
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
}
