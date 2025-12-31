package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit // NUEVO: Callback para eliminar la receta
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filtrar recetas basándose en la búsqueda (título o ingredientes)
    val filteredRecipes = recipes.filter { recipe ->
        recipe.title.contains(searchQuery, ignoreCase = true) ||
        (recipe.ingredients.any { it.details.name.contains(searchQuery, ignoreCase = true) })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mis Recetas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Campo de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar por nombre o ingrediente...") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Lista de recetas filtradas
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredRecipes) { recipe ->
                val isCompleted = recipe.status == "COMPLETED"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isCompleted, // Deshabilita el clic si no está completada
                    onClick = { onRecipeClick(recipe) }
                ) {
                    ListItem(
                        headlineContent = { Text(recipe.title) },
                        supportingContent = {
                            // Muestra un estado claro
                            when (recipe.status) {
                                "COMPLETED" -> Text("${recipe.ingredients.size} ingredientes")
                                "FAILED" -> Text("Error en la importación", color = MaterialTheme.colorScheme.error)
                                else -> Text("Importando receta...", color = MaterialTheme.colorScheme.secondary)
                            }
                        },
                        trailingContent = {
                            when (recipe.status) {
                                "FAILED" -> {
                                    // NUEVO: Botón para eliminar recetas fallidas
                                    IconButton(onClick = { onDeleteRecipe(recipe) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar receta fallida")
                                    }
                                }
                                "COMPLETED" -> { /* No mostrar nada */ }
                                else -> {
                                    // Muestra un spinner si la importación está en progreso
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}