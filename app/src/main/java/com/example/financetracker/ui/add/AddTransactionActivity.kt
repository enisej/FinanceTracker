package com.example.financetracker.ui.add

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.financetracker.data.database.DatabaseProvider
import com.example.financetracker.data.model.Transaction
import com.example.financetracker.data.repository.TransactionRepository
import com.example.financetracker.network.RetrofitClient
import com.example.financetracker.ui.theme.FinanceTrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FinanceTrackerTheme {
                val context = LocalContext.current
                val repository = remember {
                    TransactionRepository(DatabaseProvider.getDatabase(context).transactionDao())
                }

                var currentPhotoPath by remember { mutableStateOf<String?>(null) }

                // Camera launcher
                val takePictureLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.TakePicture()
                ) { success ->
                    if (!success) {
                        currentPhotoPath = null
                    }
                }

                // Function to launch camera
                fun launchCamera() {
                    val uri = createImageUri(context) { path ->
                        currentPhotoPath = path
                    }
                    takePictureLauncher.launch(uri)
                }

                // Permission launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        launchCamera()
                    }
                }



                AddTransactionScreen(
                    currentPhotoPath = currentPhotoPath,
                    onTakePhoto = {
                        if (checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onSave = { amount, type, description ->
                        val transaction = Transaction(
                            amount = amount,
                            type = type,
                            description = description,
                            imagePath = currentPhotoPath
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                RetrofitClient.api.sendTransaction(
                                    mapOf(
                                        "amount" to amount,
                                        "type" to type,
                                        "description" to description
                                    )
                                )
                            } catch (_: Exception) {}

                            repository.insert(transaction)
                            finish()
                        }
                    }
                )
            }
        }
    }

    private fun createImageUri(context: Context, onPathCreated: (String) -> Unit): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        val path = photoFile.absolutePath
        onPathCreated(path)

        return FileProvider.getUriForFile(
            context,
            "com.example.financetracker.provider",
            photoFile
        )
    }
}

@Composable
fun AddTransactionScreen(
    currentPhotoPath: String?,
    onTakePhoto: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("expense") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add New Transaction", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selectedType == "income", onClick = { selectedType = "income" })
            Text("Income")
            Spacer(Modifier.width(32.dp))
            RadioButton(selected = selectedType == "expense", onClick = { selectedType = "expense" })
            Text("Expense")
        }

        Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
            Text("Take Receipt Photo")
        }

        // Show photo preview if taken
        if (currentPhotoPath != null) {
            Text("Photo saved!", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Image(
                painter = rememberAsyncImagePainter(currentPhotoPath),
                contentDescription = "Receipt photo",
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount != null && description.isNotBlank()) {
                    onSave(amount, selectedType, description)
                }
            },
            enabled = amountText.toDoubleOrNull() != null && description.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Transaction")
        }

        Spacer(Modifier.height(32.dp))

        val context = LocalContext.current
        Button(onClick = { (context as? ComponentActivity)?.finish() }, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}