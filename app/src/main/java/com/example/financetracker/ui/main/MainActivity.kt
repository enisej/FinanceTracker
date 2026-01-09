package com.example.financetracker.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.financetracker.data.database.DatabaseProvider
import com.example.financetracker.data.model.Transaction
import com.example.financetracker.data.repository.TransactionRepository
import com.example.financetracker.ui.add.AddTransactionActivity
import com.example.financetracker.ui.theme.FinanceTrackerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            FinanceTrackerTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                val repository = remember {
                    TransactionRepository(DatabaseProvider.getDatabase(context).transactionDao())
                }

                var balance by remember { mutableDoubleStateOf(0.0) }
                var transactions by remember { mutableStateOf(emptyList<Transaction>()) }
                var selectedFilter by remember { mutableStateOf("All") }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    launch {
                        repository.getBalance().collect { newBalance ->
                            balance = newBalance ?: 0.0
                        }
                    }
                    launch {
                        repository.getAllTransactions().collect { list ->
                            transactions = list
                        }
                    }
                    launch {
                        repository.getAllCategories().collect { existingCategories ->
                            if (existingCategories.isEmpty()) {
                                repository.insertCategory(
                                    com.example.financetracker.data.model.Category(
                                        name = "Uncategorized",
                                        icon = "📁"
                                    )
                                )
                                repository.insertCategory(
                                    com.example.financetracker.data.model.Category(
                                        name = "Food",
                                        icon = "📁"
                                    )
                                )
                                repository.insertCategory(
                                    com.example.financetracker.data.model.Category(
                                        name = "Salary",
                                        icon = "📁"
                                    )
                                )
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        AddTransactionActivity::class.java
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add transaction")
                        }
                    },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Finance Tracker", fontWeight = FontWeight.Bold) },
                            actions = {
                                IconButton(onClick = {
                                    val intent =
                                        Intent(this@MainActivity, CategoryActivity::class.java)
                                    startActivity(intent)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Manage Categories"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                    ) {
                        BalanceCard(balance = balance)
                        Spacer(Modifier.height(24.dp))

                        Text(
                            "Filter by Type",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filterOptions = listOf("All", "income", "expense")
                            filterOptions.forEach { option ->
                                FilterChip(
                                    selected = selectedFilter == option,
                                    onClick = { selectedFilter = option },
                                    label = { Text(option.replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        val filteredTransactions = remember(transactions, selectedFilter) {
                            if (selectedFilter == "All") transactions
                            else transactions.filter { it.type == selectedFilter }
                        }

                        TransactionList(
                            transactions = filteredTransactions,
                            onDelete = { transaction ->
                                scope.launch {
                                    repository.delete(transaction)
                                    snackbarHostState.showSnackbar(
                                        message = "Transaction deleted",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Balance",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format("%.2f €", balance),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TransactionList(
    transactions: List<Transaction>,
    onDelete: (Transaction) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No transactions yet.\nTap + to add one!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    } else {
        Text(
            "Recent Transactions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(
                items = transactions,
                key = { it.id }
            ) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onDelete = { onDelete(transaction) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCard(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    var showImageDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (transaction.imagePath != null) {
                    Image(
                        painter = rememberAsyncImagePainter(transaction.imagePath),
                        contentDescription = "Receipt thumbnail",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showImageDialog = true }
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = transaction.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                            .format(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (transaction.type == "income")
                        "+${String.format("%.2f", transaction.amount)} €"
                    else
                        "-${String.format("%.2f", transaction.amount)} €",
                    color = if (transaction.type == "income")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    if (showImageDialog && transaction.imagePath != null) {
        FullImageDialog(
            imagePath = transaction.imagePath,
            onDismiss = { showImageDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            transactionDescription = transaction.description,
            onConfirm = {
                showDeleteDialog = false
                isVisible = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun FullImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Image(
                painter = rememberAsyncImagePainter(imagePath),
                contentDescription = "Full receipt",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    transactionDescription: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = { Text("Delete Transaction?", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Text(
                "Are you sure you want to delete \"$transactionDescription\"? This cannot be undone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}