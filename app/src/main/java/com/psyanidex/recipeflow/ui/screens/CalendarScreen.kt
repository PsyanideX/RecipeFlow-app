package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.Category
import com.psyanidex.recipeflow.data.MealType
import com.psyanidex.recipeflow.data.PlannedRecipe
import com.psyanidex.recipeflow.data.Recipe
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    allRecipes: List<Recipe>,
    plannedRecipes: List<PlannedRecipe>,
    plannedDesserts: List<Recipe>,
    onAddPlannedRecipe: (PlannedRecipe) -> Unit,
    onRemovePlannedRecipe: (PlannedRecipe) -> Unit,
    onAddPlannedDessert: (Recipe) -> Unit,
    onRemovePlannedDessert: (Recipe) -> Unit
) {
    val today = LocalDate.now()
    val futureDays = List(10) { today.plusDays(it.toLong()) }
    var showDialogFor by remember { mutableStateOf<Pair<LocalDate, MealType>?>(null) }
    var showDessertDialog by remember { mutableStateOf(false) }

    if (showDialogFor != null) {
        val (date, mealType) = showDialogFor!!
        AddRecipeDialog(
            date = date,
            mealType = mealType,
            allRecipes = allRecipes.filter { it.category != Category.DESSERT },
            onDismiss = { showDialogFor = null },
            onRecipeSelected = { recipe ->
                onAddPlannedRecipe(PlannedRecipe(date, recipe, mealType))
                showDialogFor = null
            }
        )
    }

    if (showDessertDialog) {
        AddDessertDialog(
            allDesserts = allRecipes.filter { it.category == Category.DESSERT },
            onDismiss = { showDessertDialog = false },
            onDessertSelected = {
                onAddPlannedDessert(it)
                showDessertDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Planificador Semanal", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(futureDays) { date ->
                DayCard(
                    date = date,
                    lunchRecipe = plannedRecipes.find { it.date == date && it.mealType == MealType.LUNCH }?.recipe,
                    dinnerRecipe = plannedRecipes.find { it.date == date && it.mealType == MealType.DINNER }?.recipe,
                    onAddRecipeClick = { mealType -> showDialogFor = date to mealType },
                    onRemoveRecipeClick = { mealType ->
                        plannedRecipes
                            .find { it.date == date && it.mealType == mealType }
                            ?.let { onRemovePlannedRecipe(it) }
                    }
                )
            }
            item {
                DessertSection(plannedDesserts, onAddDessertClick = { showDessertDialog = true }, onRemoveDessertClick = onRemovePlannedDessert)
            }
        }
    }
}

@Composable
fun DayCard(
    date: LocalDate,
    lunchRecipe: Recipe?,
    dinnerRecipe: Recipe?,
    onAddRecipeClick: (MealType) -> Unit,
    onRemoveRecipeClick: (MealType) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = date.format(formatter).replaceFirstChar { it.titlecase(Locale.ROOT) },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            MealRow(
                mealType = MealType.LUNCH,
                recipe = lunchRecipe,
                onAddClick = { onAddRecipeClick(MealType.LUNCH) },
                onRemoveClick = { onRemoveRecipeClick(MealType.LUNCH) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            MealRow(
                mealType = MealType.DINNER,
                recipe = dinnerRecipe,
                onAddClick = { onAddRecipeClick(MealType.DINNER) },
                onRemoveClick = { onRemoveRecipeClick(MealType.DINNER) }
            )
        }
    }
}

@Composable
private fun MealRow(
    mealType: MealType,
    recipe: Recipe?,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (mealType == MealType.LUNCH) "Comida" else "Cena",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = recipe?.title?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Sin planificar",
                style = MaterialTheme.typography.bodyMedium,
                color = if (recipe != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (recipe == null) {
            TextButton(onClick = onAddClick) {
                Text("Añadir")
            }
        } else {
            IconButton(onClick = onRemoveClick) {
                Icon(Icons.Default.Close, contentDescription = "Quitar receta")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecipeDialog(
    date: LocalDate,
    mealType: MealType,
    allRecipes: List<Recipe>,
    onDismiss: () -> Unit,
    onRecipeSelected: (Recipe) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredRecipes = allRecipes.filter { it.title.contains(searchQuery, ignoreCase = true) }
    val mealName = if (mealType == MealType.LUNCH) "comida" else "cena"

    AlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Añadir $mealName al ${date.dayOfMonth} de ${date.month.name.lowercase()}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar receta...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(filteredRecipes) { recipe ->
                        ListItem(
                            headlineContent = { Text(recipe.title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                            modifier = Modifier.clickable { onRecipeSelected(recipe) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DessertSection(
    desserts: List<Recipe>,
    onAddDessertClick: () -> Unit,
    onRemoveDessertClick: (Recipe) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Postres Planificados", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddDessertClick) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir postre")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (desserts.isEmpty()) {
                Text("No hay postres planificados", modifier = Modifier.padding(start = 16.dp))
            } else {
                desserts.forEach { dessert ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                    ) {
                        Text(
                            text = dessert.title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveDessertClick(dessert) }) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar postre")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDessertDialog(
    allDesserts: List<Recipe>,
    onDismiss: () -> Unit,
    onDessertSelected: (Recipe) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredDesserts = allDesserts.filter { it.title.contains(searchQuery, ignoreCase = true) }

    AlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Añadir Postre", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar postre...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(filteredDesserts) { dessert ->
                        ListItem(
                            headlineContent = { Text(dessert.title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                            modifier = Modifier.clickable { onDessertSelected(dessert) }
                        )
                    }
                }
            }
        }
    }
}