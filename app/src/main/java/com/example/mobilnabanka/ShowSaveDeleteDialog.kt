package com.example.mobilnabanka

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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