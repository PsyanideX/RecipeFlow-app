package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    onAddRecipeClick: () -> Unit,
    onImportRecipeText: (String) -> Unit,
    isNetworkAvailable: Boolean,
    onSyncFavorites: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    val filteredRecipes = recipes.filter { recipe ->
        val matchesSearch = recipe.title.contains(searchQuery, ignoreCase = true) ||
                (recipe.ingredients.any { it.details.name.contains(searchQuery, ignoreCase = true) })
        
        val matchesTab = when (selectedTab) {
            "Todas" -> true
            "Favoritas" -> recipe.isFavorite
            else -> recipe.category.name == selectedTab
        }
        
        matchesSearch && matchesTab
    }

    val tabs = listOf("Todas", "Favoritas") + Category.values().map { it.name }

    fun getTabTitle(tab: String): String {
        return when (tab) {
            "MAIN" -> "Principal"
            "DESSERT" -> "Postre"
            "SOUP_CREAM" -> "Sopas"
            "ACOMPANANTES" -> "Acompañantes"
            "ENTRANTES" -> "Entrantes"
            else -> tab
        }
    }

    Scaffold(
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Opciones de receta")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Crear receta") },
                        onClick = {
                            showMenu = false
                            onAddRecipeClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Importar desde texto") },
                        onClick = {
                            showMenu = false
                            showImportDialog = true
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
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
                    Tab(
                        text = { Text(
                            text = getTabTitle(tab),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) },
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) }
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
                            headlineContent = { Text(
                                text = recipe.title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) },
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

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar Receta desde Texto") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Pega aquí el texto de la receta") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            onImportRecipeText(importText)
                            showImportDialog = false
                            importText = ""
                        }
                    },
                    enabled = importText.isNotBlank()
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
