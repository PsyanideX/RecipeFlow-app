package com.psyanidex.recipeflow

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.psyanidex.recipeflow.ui.screens.CalendarScreen
import com.psyanidex.recipeflow.ui.screens.RecipeDetailScreen
import com.psyanidex.recipeflow.ui.screens.RecipeListScreen
import com.psyanidex.recipeflow.ui.screens.ShoppingListScreen
import kotlinx.coroutines.Dispatchers
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
    val shoppingList = remember { mutableStateListOf<ShoppingListItem>() }

    // NUEVO: La lógica de la lista de la compra ahora está en una función reutilizable
    fun updateShoppingList() {
        val ingredients = plannedRecipes
            .flatMap { it.recipe.ingredients }
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
        shoppingList.clear()
        shoppingList.addAll(ingredients.map { ShoppingListItem(it) })
        scope.launch { storageManager.saveShoppingList(shoppingList) } // Guardar automáticamente
    }

    fun fetchRecipes() {
        scope.launch {
            isProcessing = true
            try {
                val fetchedRecipes = ApiClient.instance.getRecipes()
                recipes.clear()
                recipes.addAll(fetchedRecipes)
            } catch (e: Exception) {
                Log.e("MainScreen", "Error al obtener las recetas", e)
            } finally {
                isProcessing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        // Cargar datos persistidos al iniciar
        plannedRecipes.addAll(storageManager.plannedRecipesFlow.first())
        updateShoppingList() // Recalcular la lista al inicio
        fetchRecipes()
    }

    LaunchedEffect(initialUrl) {
        initialUrl?.let { url ->
            isProcessing = true
            scope.launch {
                try {
                    val doc = withContext(Dispatchers.IO) { Jsoup.connect(url).get() }
                    doc.select("script, style, header, footer, nav, aside, iframe, .ads, .advertisement, .ad-container, #comments, .comments-area").remove()
                    val cleanHtml = doc.body().html()
                    ApiClient.instance.importRecipe(ImportRequest(html = cleanHtml))
                    fetchRecipes()
                    navController.navigate("recipes")
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error al importar la receta", e)
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = navController, startDestination = "recipes") {
                composable("recipes") {
                    RecipeListScreen(recipes = recipes, onRecipeClick = { recipe ->
                        navController.navigate("recipeDetail/${recipe.id}")
                    })
                }
                composable(
                    route = "recipeDetail/{recipeId}",
                    arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
                ) {
                    backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getInt("recipeId")
                    val recipe = recipes.find { it.id == recipeId }
                    if (recipe != null) {
                        RecipeDetailScreen(recipe = recipe, onNavigateUp = { navController.navigateUp() })
                    }
                }
                composable("calendar") {
                    CalendarScreen(
                        allRecipes = recipes,
                        plannedRecipes = plannedRecipes,
                        onAddPlannedRecipe = { plannedRecipe ->
                            plannedRecipes.add(plannedRecipe)
                            scope.launch { storageManager.savePlannedRecipes(plannedRecipes) }
                            updateShoppingList() // Actualización en tiempo real
                        },
                        onRemovePlannedRecipe = { plannedRecipe ->
                            plannedRecipes.remove(plannedRecipe)
                            scope.launch { storageManager.savePlannedRecipes(plannedRecipes) }
                            updateShoppingList() // Actualización en tiempo real
                        }
                    )
                }
                composable("shopping") {
                    ShoppingListScreen(
                        items = shoppingList,
                        onItemCheckedChanged = { index, isChecked ->
                            shoppingList[index] = shoppingList[index].copy(isChecked = isChecked)
                            scope.launch { storageManager.saveShoppingList(shoppingList) }
                        },
                        onClearList = { 
                            plannedRecipes.clear() // Limpiar también los planes
                            updateShoppingList()
                            scope.launch { storageManager.savePlannedRecipes(plannedRecipes) }
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