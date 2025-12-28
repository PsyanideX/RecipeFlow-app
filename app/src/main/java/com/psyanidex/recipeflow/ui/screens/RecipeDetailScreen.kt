package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onNavigateUp: () -> Unit,
    onDeleteConfirm: () -> Unit // NUEVO: Callback para confirmar el borrado
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeleteConfirm()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // NUEVO: Botón para iniciar el borrado
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar receta")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Ingredientes", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(recipe.ingredients.size) { index ->
                    Text(text = "• ${recipe.ingredients[index]}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Pasos", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                itemsIndexed(recipe.steps) { index, step ->
                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("${index + 1}. ", style = MaterialTheme.typography.bodyLarge)
                        Text(step, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// NUEVO: Diálogo de confirmación de borrado
@Composable
private fun DeleteConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar eliminación") },
        text = { Text("¿Estás seguro de que quieres eliminar esta receta? Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}