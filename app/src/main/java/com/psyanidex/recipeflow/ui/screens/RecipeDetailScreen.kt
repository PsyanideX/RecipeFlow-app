package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(recipe: Recipe, onNavigateUp: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Ingredientes
            Text("Ingredientes", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(recipe.ingredients.size) { index ->
                    Text(text = "• ${recipe.ingredients[index]}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pasos
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