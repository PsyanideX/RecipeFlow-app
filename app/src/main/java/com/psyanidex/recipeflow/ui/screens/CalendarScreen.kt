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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.PlannedRecipe
import com.psyanidex.recipeflow.data.Recipe
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    allRecipes: List<Recipe>,
    plannedRecipes: List<PlannedRecipe>,
    onAddPlannedRecipe: (PlannedRecipe) -> Unit,
    onGenerateShoppingList: (List<String>) -> Unit
) {
    val today = LocalDate.now()
    val weekDays = List(7) { today.plusDays(it.toLong()) }
    val selectedDays = remember { mutableStateMapOf<LocalDate, Boolean>() }
    var showDialogForDate by remember { mutableStateOf<LocalDate?>(null) }

    if (showDialogForDate != null) {
        AddRecipeDialog(
            date = showDialogForDate!!,
            allRecipes = allRecipes,
            onDismiss = { showDialogForDate = null },
            onRecipeSelected = { recipe ->
                onAddPlannedRecipe(PlannedRecipe(showDialogForDate!!, recipe))
                showDialogForDate = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Planificador Semanal", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(weekDays) { date ->
                DayCard(
                    date = date,
                    recipesForDay = plannedRecipes.filter { it.date == date }.map { it.recipe },
                    isSelected = selectedDays.getOrDefault(date, false),
                    onDateSelected = { isSelected -> selectedDays[date] = isSelected },
                    onAddRecipeClick = { showDialogForDate = date }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val ingredients = plannedRecipes
                    .filter { selectedDays.getOrDefault(it.date, false) }
                    .flatMap { it.recipe.ingredients.map { ingredient -> ingredient.toString() } } // Convertir Ingredient a String
                    .distinct()
                onGenerateShoppingList(ingredients)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDays.any { it.value }
        ) {
            Text("Generar Lista de la Compra")
        }
    }
}

@Composable
fun DayCard(
    date: LocalDate,
    recipesForDay: List<Recipe>,
    isSelected: Boolean,
    onDateSelected: (Boolean) -> Unit,
    onAddRecipeClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSelected, onCheckedChange = onDateSelected)
                    Text(
                        text = date.format(formatter).replaceFirstChar { it.titlecase(Locale.ROOT) },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                TextButton(onClick = onAddRecipeClick) {
                    Text("Añadir")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (recipesForDay.isNotEmpty()) {
                recipesForDay.forEach { recipe ->
                    Text(
                        text = "• ${recipe.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "No hay recetas planificadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecipeDialog(
    date: LocalDate,
    allRecipes: List<Recipe>,
    onDismiss: () -> Unit,
    onRecipeSelected: (Recipe) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredRecipes = allRecipes.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Añadir receta al ${date.dayOfMonth} de ${date.month.name.lowercase()}", style = MaterialTheme.typography.titleLarge)
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
                            headlineContent = { Text(recipe.title) },
                            modifier = Modifier.clickable { onRecipeSelected(recipe) }
                        )
                    }
                }
            }
        }
    }
}