package com.example.financetracker.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.financetracker.data.database.DatabaseProvider
import com.example.financetracker.data.model.Category
import com.example.financetracker.data.repository.TransactionRepository
import com.example.financetracker.ui.theme.FinanceTrackerTheme
import kotlinx.coroutines.launch

class CategoryActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanceTrackerTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val repository = remember {
                    TransactionRepository(DatabaseProvider.getDatabase(context).transactionDao())
                }

                val categories by repository.getAllCategories()
                    .collectAsState(initial = emptyList())
                var showAddDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Manage Categories") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                ) { padding ->
                    LazyColumn(modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()) {
                        items(categories) { category ->
                            ListItem(
                                headlineContent = { Text(category.name) },
                                leadingContent = {
                                    Text(
                                        category.icon,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                },
                                trailingContent = {
                                    if (category.name != "Uncategorized") {
                                        IconButton(onClick = {
                                            scope.launch { repository.deleteCategory(category) }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }

                    if (showAddDialog) {
                        AddCategoryDialog(
                            onDismiss = { showAddDialog = false },
                            onConfirm = { name, icon ->
                                scope.launch {
                                    repository.insertCategory(Category(name = name, icon = icon))
                                }
                                showAddDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("📁") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Icon (Emoji)") })
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, icon) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}