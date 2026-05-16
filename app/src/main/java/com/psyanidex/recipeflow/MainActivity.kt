package com.psyanidex.recipeflow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.psyanidex.recipeflow.data.*
import com.psyanidex.recipeflow.ui.navigation.BottomNavigationBar
import com.psyanidex.recipeflow.ui.screens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            MaterialTheme {
                MainScreen(sharedUrl)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(initialUrl: String?) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context) }

    val recipes = remember { mutableStateListOf<Recipe>() }
    val plannedRecipes = remember { mutableStateListOf<PlannedRecipe>() }
    val plannedDesserts = remember { mutableStateListOf<Recipe>() }
    val shoppingList = remember { mutableStateListOf<ShoppingListItem>() }
    var selectedTab by remember { mutableStateOf("Todas") }
    var isNetworkAvailable by remember { mutableStateOf(true) }

    fun updateIngredientsInShoppingList() {
        val dailyIngredients = plannedRecipes.flatMap { it.recipe.ingredients }
        val dessertIngredients = plannedDesserts.flatMap { it.ingredients }
        val allIngredients = dailyIngredients + dessertIngredients

        val newIngredientItems = allIngredients
            .groupBy { it.details.name.lowercase(Locale.getDefault()) }
            .map { (name, entries) ->
                val processedQuantities = entries
                    .groupBy { it.unit.lowercase(Locale.getDefault()) }
                    .map { (unit, unitEntries) ->
                        val quantitiesAsDouble = unitEntries.map { it.quantity.toDoubleOrNull() }
                        if (quantitiesAsDouble.any { it == null }) {
                            unitEntries.joinToString(", ") { "${it.quantity} ${it.unit}" }
                        } else {
                            val sum = quantitiesAsDouble.sumOf { it!! }
                            val formattedSum = if (sum % 1.0 == 0.0) sum.toInt().toString() else sum.toString()
                            "$formattedSum ${unitEntries.first().unit}"
                        }
                    }
                    .joinToString(", ")
                "${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}: $processedQuantities"
            }
            .map { ShoppingListItem(text = it, isCustom = false) }

        val customItems = shoppingList.filter { it.isCustom }
        shoppingList.clear()
        shoppingList.addAll(newIngredientItems)
        shoppingList.addAll(customItems)
        scope.launch { storageManager.saveShoppingList(shoppingList) }
    }

    fun fetchRecipes() {
        scope.launch {
            isProcessing = true
            try {
                val fetchedRecipes = ApiClient.instance.getRecipes()
                recipes.clear()
                recipes.addAll(fetchedRecipes)
                isNetworkAvailable = true
            } catch (e: Exception) {
                isNetworkAvailable = false
                Log.e("MainScreen", "Error al obtener las recetas. Mostrando datos locales.", e)
                val favoriteRecipes = storageManager.favoriteRecipesFlow.first()
                recipes.clear()
                recipes.addAll(favoriteRecipes)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sin conexión. Mostrando favoritas.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isProcessing = false
            }
        }
    }

    fun syncFavoriteRecipes() {
        scope.launch {
            isProcessing = true
            try {
                val favoriteRecipes = ApiClient.instance.getRecipes(favorite = true)
                storageManager.saveFavoriteRecipes(favoriteRecipes)
                
                // Update favorite status in the local list
                val favoriteIds = favoriteRecipes.map { it.id }.toSet()
                recipes.indices.forEach { i ->
                    val recipe = recipes[i]
                    if (recipe.id in favoriteIds) {
                        recipes[i] = recipe.copy(isFavorite = true)
                    } else if (recipe.isFavorite) {
                        recipes[i] = recipe.copy(isFavorite = false)
                    }
                }
                
                isNetworkAvailable = true
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Favoritas sincronizadas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isNetworkAvailable = false
                Log.e("MainScreen", "Error al sincronizar las favoritas", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error de sincronización. Comprueba la conexión.", Toast.LENGTH_LONG).show()
                }
            } finally {
                isProcessing = false
            }
        }
    }

    fun pollRecipeStatus(recipeId: Int) {
        scope.launch {
            var currentStatus = ""
            var attempts = 0
            val maxAttempts = 20 // Approx 5 minutes total
            
            while (currentStatus != "COMPLETED" && currentStatus != "FAILED" && attempts < maxAttempts) {
                try {
                    val recipeState = ApiClient.instance.getRecipeById(recipeId)
                    currentStatus = recipeState.status ?: ""
                } catch (e: Exception) {
                    currentStatus = "FAILED"
                    Log.e("MainScreen", "Error durante el sondeo", e)
                }
                if (currentStatus != "COMPLETED" && currentStatus != "FAILED") {
                    delay(15000)
                    attempts++
                }
            }
            withContext(Dispatchers.Main) {
                if (currentStatus == "COMPLETED") {
                    Toast.makeText(context, "Receta importada con éxito", Toast.LENGTH_SHORT).show()
                    fetchRecipes()
                    navController.navigate("recipeDetail/$recipeId")
                } else {
                    Toast.makeText(context, "La importación ha fallado", Toast.LENGTH_LONG).show()
                    fetchRecipes()
                }
            }
        }
    }

    fun importFromText(text: String) {
        scope.launch {
            isProcessing = true
            try {
                val importResponse = ApiClient.instance.importRecipeFromText(ImportTextRequest(text = text))
                isNetworkAvailable = true
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, importResponse.message, Toast.LENGTH_SHORT).show()
                }
                pollRecipeStatus(importResponse.id)
            } catch (e: Exception) {
                isNetworkAvailable = false
                Log.e("MainScreen", "Error al importar texto", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al iniciar la importación", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isProcessing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        plannedRecipes.addAll(storageManager.plannedRecipesFlow.first())
        plannedDesserts.addAll(storageManager.plannedDessertsFlow.first())
        shoppingList.addAll(storageManager.shoppingListFlow.first())
        updateIngredientsInShoppingList()
        fetchRecipes()
    }

    LaunchedEffect(initialUrl) {
        initialUrl?.let { url ->
            isProcessing = true
            try {
                val doc = withContext(Dispatchers.IO) {
                    Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                        .referrer(url)
                        .get()
                }
                doc.head().remove()
                val allSelectors = HtmlCleanerSelectors.aevitar + HtmlCleanerSelectors.especificas
                doc.select(allSelectors.joinToString(", ")).remove()
                doc.select("a").unwrap()
                var changed = true
                while (changed) {
                    changed = false
                    for (el in doc.body().select("div, section")) {
                        if (el.childrenSize() == 1 && el.ownText().isBlank()) {
                            el.child(0).unwrap()
                            changed = true
                            break
                        }
                    }
                }
                doc.body().select("*").reversed().forEach { element ->
                    if (!element.hasText() && element.children().isEmpty()) {
                        element.remove()
                    }
                }
                doc.body().select("*").forEach { element ->
                    element.removeAttr("class")
                    element.removeAttr("id")
                    element.removeAttr("style")
                }
                val cleanHtml = doc.body().html()
                val importResponse = ApiClient.instance.importRecipe(ImportRequest(html = cleanHtml))
                isNetworkAvailable = true
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, importResponse.message, Toast.LENGTH_SHORT).show()
                }
                pollRecipeStatus(importResponse.id)
            } catch (e: Exception) {
                isNetworkAvailable = false
                Log.e("MainScreen", "Error al iniciar la importación", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al iniciar la importación", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = navController, startDestination = "recipes") {
                composable("recipes") {
                    RecipeListScreen(
                        recipes = recipes,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onRecipeClick = { recipe ->
                            navController.navigate("recipeDetail/${recipe.id}")
                        },
                        onDeleteRecipe = { recipe ->
                            scope.launch {
                                isProcessing = true
                                try {
                                    ApiClient.instance.deleteRecipe(recipe.id)
                                    recipes.remove(recipe)
                                    isNetworkAvailable = true
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Receta eliminada", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isNetworkAvailable = false
                                    Log.e("MainScreen", "Error al eliminar la receta", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        onFavoriteClick = { recipe ->
                            scope.launch {
                                val updatedRecipe = recipe.copy(isFavorite = !recipe.isFavorite)
                                try {
                                    val savedRecipe = ApiClient.instance.updateRecipe(recipe.id,
                                        UpdateRecipeRequest(
                                            title = updatedRecipe.title,
                                            steps = updatedRecipe.steps,
                                            ingredients = updatedRecipe.ingredients.map {
                                                UpdateIngredientRequest(it.details.name, it.quantity, it.unit)
                                            },
                                            category = updatedRecipe.category,
                                            isFavorite = updatedRecipe.isFavorite
                                        )
                                    )
                                    isNetworkAvailable = true
                                    val index = recipes.indexOf(recipe)
                                    if (index != -1) {
                                        recipes[index] = savedRecipe
                                    }
                                    val favoriteRecipes = storageManager.favoriteRecipesFlow.first().toMutableList()
                                    if (savedRecipe.isFavorite) {
                                        if (favoriteRecipes.find { it.id == savedRecipe.id } == null) {
                                            favoriteRecipes.add(savedRecipe)
                                        }
                                    } else {
                                        favoriteRecipes.removeAll { it.id == savedRecipe.id }
                                    }
                                    storageManager.saveFavoriteRecipes(favoriteRecipes)
                                } catch (e: Exception) {
                                    isNetworkAvailable = false
                                    Log.e("MainScreen", "Error al actualizar la receta", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onAddRecipeClick = { navController.navigate("createRecipe") },
                        onImportRecipeText = { text -> importFromText(text) },
                        isNetworkAvailable = isNetworkAvailable,
                        onSyncFavorites = { syncFavoriteRecipes() }
                    )
                }
                composable("createRecipe") {
                    CreateRecipeScreen(
                        onNavigateUp = { navController.navigateUp() },
                        onSave = { newRecipe ->
                            scope.launch {
                                isProcessing = true
                                try {
                                    val savedRecipe = ApiClient.instance.createRecipe(newRecipe)
                                    recipes.add(savedRecipe)
                                    isNetworkAvailable = true
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Receta creada", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    isNetworkAvailable = false
                                    Log.e("MainScreen", "Error al crear la receta", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error al crear", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                    )
                }
                composable(
                    route = "recipeDetail/{recipeId}",
                    arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
                ) {
                    backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getInt("recipeId")
                    val recipe = recipes.find { it.id == recipeId }
                    if (recipe != null) {
                        RecipeDetailScreen(
                            recipe = recipe,
                            onNavigateUp = { navController.navigateUp() },
                            onEditClick = { navController.navigate("editRecipe/${recipe.id}") },
                            onDeleteConfirm = {
                                scope.launch {
                                    isProcessing = true
                                    try {
                                        ApiClient.instance.deleteRecipe(recipe.id)
                                        recipes.remove(recipe)
                                        plannedRecipes.removeAll { it.recipe.id == recipe.id }
                                        updateIngredientsInShoppingList()
                                        storageManager.savePlannedRecipes(plannedRecipes)
                                        isNetworkAvailable = true
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Receta eliminada", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    } catch (e: Exception) {
                                        isNetworkAvailable = false
                                        Log.e("MainScreen", "Error al eliminar la receta", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        )
                    }
                }
                composable(
                    route = "editRecipe/{recipeId}",
                    arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
                ) {
                    backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getInt("recipeId")
                    val recipe = recipes.find { it.id == recipeId }
                    if (recipe != null) {
                        EditRecipeScreen(
                            recipe = recipe,
                            onNavigateUp = { navController.navigateUp() },
                            onSave = { updatedRecipe ->
                                scope.launch {
                                    isProcessing = true
                                    try {
                                        val savedRecipe = ApiClient.instance.updateRecipe(recipe.id, updatedRecipe)
                                        val index = recipes.indexOf(recipe)
                                        if (index != -1) {
                                            recipes[index] = savedRecipe
                                        }
                                        plannedRecipes.replaceAll { if (it.recipe.id == recipe.id) it.copy(recipe = savedRecipe) else it }
                                        updateIngredientsInShoppingList()
                                        storageManager.savePlannedRecipes(plannedRecipes)
                                        isNetworkAvailable = true
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Receta guardada", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    } catch (e: Exception) {
                                        isNetworkAvailable = false
                                        Log.e("MainScreen", "Error al guardar la receta", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        )
                    }
                }
                composable("calendar") {
                    CalendarScreen(
                        allRecipes = recipes,
                        plannedRecipes = plannedRecipes,
                        plannedDesserts = plannedDesserts,
                        onAddPlannedRecipe = { plannedRecipe ->
                            plannedRecipes.add(plannedRecipe)
                            scope.launch { storageManager.savePlannedRecipes(plannedRecipes) }
                            updateIngredientsInShoppingList()
                        },
                        onRemovePlannedRecipe = { plannedRecipe ->
                            plannedRecipes.remove(plannedRecipe)
                            scope.launch { storageManager.savePlannedRecipes(plannedRecipes) }
                            updateIngredientsInShoppingList()
                        },
                        onAddPlannedDessert = { dessert ->
                            plannedDesserts.add(dessert)
                            scope.launch { storageManager.savePlannedDesserts(plannedDesserts) }
                            updateIngredientsInShoppingList()
                        },
                        onRemovePlannedDessert = { dessert ->
                            plannedDesserts.remove(dessert)
                            scope.launch { storageManager.savePlannedDesserts(plannedDesserts) }
                            updateIngredientsInShoppingList()
                        }
                    )
                }
                composable("shopping") {
                    ShoppingListScreen(
                        items = shoppingList,
                        onItemCheckedChanged = { item, isChecked ->
                            val index = shoppingList.indexOf(item)
                            if (index != -1) {
                                shoppingList[index] = item.copy(isChecked = isChecked)
                                scope.launch { storageManager.saveShoppingList(shoppingList) }
                            }
                        },
                        onAddItem = { text ->
                            shoppingList.add(ShoppingListItem(text = text, isCustom = true))
                            scope.launch { storageManager.saveShoppingList(shoppingList) }
                        },
                        onRemoveItem = { item ->
                            shoppingList.remove(item)
                            scope.launch { storageManager.saveShoppingList(shoppingList) }
                        },
                        onClearList = { 
                            plannedRecipes.clear()
                            plannedDesserts.clear()
                            shoppingList.clear()
                            scope.launch {
                                storageManager.savePlannedRecipes(plannedRecipes)
                                storageManager.savePlannedDesserts(plannedDesserts)
                                storageManager.saveShoppingList(shoppingList)
                            }
                        }
                    )
                }
            }

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}