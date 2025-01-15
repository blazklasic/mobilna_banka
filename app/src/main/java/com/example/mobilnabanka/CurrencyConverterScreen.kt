package com.example.mobilnabanka

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mobilnabanka.ViewModel.BankAccountViewModel

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