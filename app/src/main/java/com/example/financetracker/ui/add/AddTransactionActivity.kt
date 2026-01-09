package com.example.financetracker.ui.add

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import com.example.financetracker.data.database.DatabaseProvider
import com.example.financetracker.data.model.Transaction
import com.example.financetracker.data.repository.TransactionRepository
import com.example.financetracker.network.RetrofitClient
import com.example.financetracker.ui.theme.FinanceTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FinanceTrackerTheme {
                val context = LocalContext.current
                val repository = remember {
                    TransactionRepository(DatabaseProvider.getDatabase(context).transactionDao())
                }

                val categories by repository.getAllCategories()
                    .collectAsState(initial = emptyList())

                var currentPhotoPath by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(false) }
                var apiResponseData by remember { mutableStateOf<String?>(null) }

                val takePictureLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.TakePicture()
                ) { success -> if (!success) currentPhotoPath = null }

                fun launchCamera() {
                    val uri = createImageUri(context) { path -> currentPhotoPath = path }
                    takePictureLauncher.launch(uri)
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { if (it) launchCamera() }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text("New Transaction", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { innerPadding ->
                    AddTransactionScreen(
                        modifier = Modifier.padding(innerPadding),
                        categories = categories,
                        currentPhotoPath = currentPhotoPath,
                        isLoading = isLoading,
                        serverResponse = apiResponseData,
                        onDismissResponse = {
                            apiResponseData = null
                            finish()
                        },
                        onTakePhoto = {
                            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchCamera()
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onSave = { amount, type, description, category ->
                            isLoading = true
                            lifecycleScope.launch {
                                val transaction = Transaction(
                                    amount = amount,
                                    type = type,
                                    description = description,
                                    categoryName = category,
                                    imagePath = currentPhotoPath
                                )

                                withContext(Dispatchers.IO) { repository.insert(transaction) }

                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.api.sendTransaction(
                                            mapOf(
                                                "title" to description,
                                                "body" to "Category: $category, Type: $type, Amount: $amount EUR",
                                                "userId" to 1
                                            )
                                        )
                                    }
                                    apiResponseData = response.toString()
                                } catch (e: Exception) {
                                    apiResponseData = "Error: ${e.localizedMessage}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun createImageUri(context: Context, onPathCreated: (String) -> Unit): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        val path = photoFile.absolutePath
        onPathCreated(path)
        return FileProvider.getUriForFile(context, "com.example.financetracker.provider", photoFile)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    modifier: Modifier = Modifier,
    categories: List<com.example.financetracker.data.model.Category>,
    currentPhotoPath: String?,
    isLoading: Boolean,
    serverResponse: String?,
    onDismissResponse: () -> Unit,
    onTakePhoto: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("expense") }
    var selectedCategory by remember { mutableStateOf("Uncategorized") }
    var categoryExpanded by remember { mutableStateOf(false) }

    if (serverResponse != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissResponse,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("API Response Received", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = serverResponse,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onDismissResponse,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount (€)") },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = if (selectedType == "income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { categoryExpanded = true },
                shape = RoundedCornerShape(12.dp),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Box(Modifier
                .matchParentSize()
                .clickable { categoryExpanded = true })

            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                if (categories.isEmpty()) {
                    DropdownMenuItem(text = { Text("Uncategorized") }, onClick = {
                        selectedCategory = "Uncategorized"
                        categoryExpanded = false
                    })
                }
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text("${category.icon} ${category.name}") },
                        onClick = {
                            selectedCategory = category.name
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransactionTypeButton(
                label = "Income",
                isSelected = selectedType == "income",
                activeColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = { selectedType = "income" }
            )
            TransactionTypeButton(
                label = "Expense",
                isSelected = selectedType == "expense",
                activeColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
                onClick = { selectedType = "expense" }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentPhotoPath != null) {
                    Image(
                        painter = rememberAsyncImagePainter(currentPhotoPath),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    TextButton(onClick = onTakePhoto) { Text("Change Photo") }
                } else {
                    OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Receipt")
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount != null && description.isNotBlank()) {
                    onSave(amount, selectedType, description, selectedCategory)
                }
            },
            enabled = !isLoading && amountText.toDoubleOrNull() != null && description.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Confirm & Sync", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TransactionTypeButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}