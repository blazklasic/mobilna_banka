package com.example.mobilnabanka

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.mobilnabanka.ui.theme.MobilnaBankaTheme
import com.example.mobilnabanka.ViewModel.BankAccountViewModel
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