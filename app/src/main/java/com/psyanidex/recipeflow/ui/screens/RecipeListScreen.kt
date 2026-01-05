package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.Category
import com.psyanidex.recipeflow.data.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    recipes: List<Recipe>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    onFavoriteClick: (Recipe) -> Unit,
    isNetworkAvailable: Boolean,
    onSyncFavorites: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecipes = recipes.filter { recipe ->
        recipe.title.contains(searchQuery, ignoreCase = true) ||
                (recipe.ingredients.any { it.details.name.contains(searchQuery, ignoreCase = true) })
    }

    val tabs = listOf("Todas", "Favoritas") + Category.values().map { it.name }

    fun getTabTitle(tab: String): String {
        return when (tab) {
            "MAIN" -> "Principal"
            "DESSERT" -> "Postre"
            "SOUP_CREAM" -> "Sopas"
            else -> tab
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mis Recetas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por nombre o ingrediente...") },
                modifier = Modifier.weight(1f)
            )
            if (selectedTab == "Favoritas") {
                IconButton(onClick = onSyncFavorites, enabled = isNetworkAvailable) {
                    Icon(Icons.Default.Sync, contentDescription = "Sincronizar favoritas")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            edgePadding = 0.dp
        ) {
            tabs.forEach { tab ->
                val isCategoryTab = tab != "Todas" && tab != "Favoritas"
                val isEnabled = !isCategoryTab || isNetworkAvailable
                Tab(
                    text = { Text(
                        text = getTabTitle(tab),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) },
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    enabled = isEnabled
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top=8.dp)) {
            items(filteredRecipes) { recipe ->
                val isCompleted = recipe.status == "COMPLETED"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isCompleted,
                    onClick = { onRecipeClick(recipe) }
                ) {
                    ListItem(
                        headlineContent = { Text(recipe.title) },
                        supportingContent = {
                            when (recipe.status) {
                                "COMPLETED" -> Text("${recipe.ingredients.size} ingredientes")
                                "FAILED" -> Text("Error en la importación", color = MaterialTheme.colorScheme.error)
                                else -> Text("Importando receta...", color = MaterialTheme.colorScheme.secondary)
                            }
                        },
                        trailingContent = {
                            when (recipe.status) {
                                "FAILED" -> {
                                    IconButton(onClick = { onDeleteRecipe(recipe) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar receta fallida")
                                    }
                                }
                                "COMPLETED" -> {
                                    IconButton(onClick = { onFavoriteClick(recipe) }, enabled = isNetworkAvailable) {
                                        Icon(
                                            imageVector = if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = if (recipe.isFavorite) "Quitar de favoritos" else "Añadir a favoritos"
                                        )
                                    }
                                }
                                else -> {
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