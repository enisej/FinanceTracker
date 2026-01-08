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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
                var snackbarHostState = remember { SnackbarHostState() }

                // Collect data in real-time
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
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                startActivity(Intent(this@MainActivity, AddTransactionActivity::class.java))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add transaction")
                        }
                    },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Finance Tracker") },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        BalanceCard(balance = balance)
                        Spacer(Modifier.height(24.dp))
                        TransactionList(
                            transactions = transactions,
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Current Balance",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format("%.2f €", balance),
                fontSize = 40.sp,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No transactions yet.\nTap + to add one!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Text(
            "Recent Transactions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Receipt image thumbnail
                if (transaction.imagePath != null) {
                    Image(
                        painter = rememberAsyncImagePainter(transaction.imagePath),
                        contentDescription = "Receipt thumbnail",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showImageDialog = true },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                }

                // Transaction details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            .format(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount
                Text(
                    text = if (transaction.type == "income")
                        "+${String.format("%.2f", transaction.amount)} €"
                    else
                        "-${String.format("%.2f", transaction.amount)} €",
                    color = if (transaction.type == "income")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.width(8.dp))

                // Delete button
                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete transaction",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Full image dialog
    if (showImageDialog && transaction.imagePath != null) {
        FullImageDialog(
            imagePath = transaction.imagePath,
            onDismiss = { showImageDialog = false }
        )
    }

    // Delete confirmation dialog
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
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        text = {
            Image(
                painter = rememberAsyncImagePainter(imagePath),
                contentDescription = "Full receipt image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .clip(RoundedCornerShape(8.dp)),
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
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Transaction?") },
        text = {
            Text("Are you sure you want to delete \"$transactionDescription\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}