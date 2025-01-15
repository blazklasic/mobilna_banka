package com.example.mobilnabanka

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mobilnabanka.ViewModel.BankAccountViewModel

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