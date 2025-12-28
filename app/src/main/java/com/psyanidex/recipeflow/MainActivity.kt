package com.psyanidex.recipeflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.psyanidex.recipeflow.data.PlannedRecipe
import com.psyanidex.recipeflow.data.Recipe
import com.psyanidex.recipeflow.data.ShoppingListItem
import com.psyanidex.recipeflow.ui.navigation.BottomNavigationBar
import com.psyanidex.recipeflow.ui.screens.CalendarScreen
import com.psyanidex.recipeflow.ui.screens.RecipeDetailScreen
import com.psyanidex.recipeflow.ui.screens.RecipeListScreen
import com.psyanidex.recipeflow.ui.screens.ShoppingListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.time.LocalDate

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

    // --- ESTADO CENTRALIZADO DE LA APP ---
    val recipes = remember {
        mutableStateListOf(
            Recipe(title = "Tortilla de Patatas", ingredients = listOf("3 Patatas", "5 Huevos", "1 Cebolla", "Aceite de Oliva", "Sal"), steps = listOf("Pelar y cortar...", "Batir huevos...", "Mezclar y cuajar.")),
            Recipe(title = "Lentejas", ingredients = listOf("300g Lentejas", "1 Chorizo", "1 Pimiento", "1 Zanahoria", "Agua"), steps = listOf("Poner todo en la olla...", "Cocer 45 min."))
        )
    }

    val plannedRecipes = remember {
        mutableStateListOf(
            PlannedRecipe(LocalDate.now().plusDays(1), recipes.first()),
            PlannedRecipe(LocalDate.now().plusDays(3), recipes.last())
        )
    }

    val shoppingList = remember { mutableStateListOf<ShoppingListItem>() }


    // Procesamiento de URL compartida
    LaunchedEffect(initialUrl) {
        initialUrl?.let { url ->
            isProcessing = true
            scope.launch {
                val extractedRecipe = processUrlWithJsoup(url)
                recipes.add(extractedRecipe)
                isProcessing = false
                navController.navigate("recipes")
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = "recipes") {
                composable("recipes") {
                    RecipeListScreen(recipes = recipes, onRecipeClick = { recipe ->
                        navController.navigate("recipeDetail/${recipe.id}")
                    })
                }
                composable(
                    route = "recipeDetail/{recipeId}",
                    arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                ) {
                    backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("recipeId")
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
                        },
                        onGenerateShoppingList = { ingredients ->
                            shoppingList.clear()
                            shoppingList.addAll(ingredients.map { ShoppingListItem(it) })
                            navController.navigate("shopping") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("shopping") {
                    ShoppingListScreen(
                        items = shoppingList,
                        onItemCheckedChanged = { index, isChecked ->
                            shoppingList[index] = shoppingList[index].copy(isChecked = isChecked)
                        },
                        onClearList = { shoppingList.clear() }
                    )
                }
            }

            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// --- LÓGICA DE EXTRACCIÓN (MOCK + JSOUP) ---
suspend fun processUrlWithJsoup(url: String): Recipe = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(url).get()
        doc.select("script, style, header, footer, nav, aside, iframe, .ads, .advertisement, .ad-container, #comments, .comments-area").remove()
        Recipe(
            title = doc.title().take(30) ?: "Receta Importada",
            ingredients = listOf("Ingrediente Extraído 1", "Ingrediente Extraído 2"),
            steps = listOf("Paso 1 detectado en el HTML", "Paso 2 detectado en el HTML")
        )
    } catch (e: Exception) {
        Recipe(title = "Error al importar", ingredients = emptyList(), steps = emptyList())
    }
}