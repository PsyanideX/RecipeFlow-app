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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onAddPlannedRecipe: (PlannedRecipe) -> Unit,
    onGenerateShoppingList: (List<String>) -> Unit
) {
    val today = LocalDate.now()
    val futureDays = List(10) { today.plusDays(it.toLong()) }
    val selectedDays = remember { mutableStateMapOf<LocalDate, Boolean>() }
    var showDialogFor by remember { mutableStateOf<Pair<LocalDate, MealType>?>(null) }

    showDialogFor?.let { (date, mealType) ->
        AddRecipeDialog(
            date = date,
            mealType = mealType,
            allRecipes = allRecipes,
            onDismiss = { showDialogFor = null },
            onRecipeSelected = { recipe ->
                onAddPlannedRecipe(PlannedRecipe(date, recipe, mealType))
                showDialogFor = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Planificador Semanal", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(futureDays) { date ->
                DayCard(
                    date = date,
                    lunchRecipe = plannedRecipes.find { it.date == date && it.mealType == MealType.LUNCH }?.recipe,
                    dinnerRecipe = plannedRecipes.find { it.date == date && it.mealType == MealType.DINNER }?.recipe,
                    isSelected = selectedDays.getOrDefault(date, false),
                    onDateSelected = { isSelected -> selectedDays[date] = isSelected },
                    onAddRecipeClick = { mealType -> showDialogFor = date to mealType }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // LÓGICA DE AGRUPACIÓN Y SUMA DE INGREDIENTES
                val ingredients = plannedRecipes
                    .filter { selectedDays.getOrDefault(it.date, false) }
                    .flatMap { it.recipe.ingredients }
                    .groupBy { it.details.name.lowercase() } // 1. Agrupar por nombre de ingrediente
                    .map { (name, entries) ->
                        val processedQuantities = entries
                            .groupBy { it.unit.lowercase() } // 2. Agrupar por unidad
                            .map { (unit, unitEntries) ->
                                val quantitiesAsDouble = unitEntries.map { it.quantity.toDoubleOrNull() }

                                if (quantitiesAsDouble.any { it == null }) {
                                    // 3a. Si alguna cantidad no es un número, listar por separado
                                    unitEntries.joinToString(", ") { "${it.quantity} ${it.unit}" }
                                } else {
                                    // 3b. Si todas son números, sumarlas
                                    val sum = quantitiesAsDouble.sumOf { it!! }
                                    val formattedSum = if (sum % 1.0 == 0.0) sum.toInt().toString() else sum.toString()
                                    "$formattedSum ${unitEntries.first().unit}"
                                }
                            }
                            .joinToString(", ")

                        // 4. Formatear el resultado final
                        "${name.replaceFirstChar { it.titlecase(Locale.getDefault()) }}: $processedQuantities"
                    }
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
    lunchRecipe: Recipe?,
    dinnerRecipe: Recipe?,
    isSelected: Boolean,
    onDateSelected: (Boolean) -> Unit,
    onAddRecipeClick: (MealType) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = isSelected, onCheckedChange = onDateSelected)
                Text(
                    text = date.format(formatter).replaceFirstChar { it.titlecase(Locale.ROOT) },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            MealRow(
                mealType = MealType.LUNCH,
                recipe = lunchRecipe,
                onAddClick = { onAddRecipeClick(MealType.LUNCH) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            MealRow(
                mealType = MealType.DINNER,
                recipe = dinnerRecipe,
                onAddClick = { onAddRecipeClick(MealType.DINNER) }
            )
        }
    }
}

@Composable
private fun MealRow(
    mealType: MealType,
    recipe: Recipe?,
    onAddClick: () -> Unit
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
                text = recipe?.title ?: "Sin planificar",
                style = MaterialTheme.typography.bodyMedium,
                color = if (recipe != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (recipe == null) {
            TextButton(onClick = onAddClick) {
                Text("Añadir")
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
                            headlineContent = { Text(recipe.title) },
                            modifier = Modifier.clickable { onRecipeSelected(recipe) }
                        )
                    }
                }
            }
        }
    }
}