package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.Recipe
import com.psyanidex.recipeflow.data.UpdateIngredientRequest
import com.psyanidex.recipeflow.data.UpdateRecipeRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecipeScreen(
    recipe: Recipe,
    onSave: (UpdateRecipeRequest) -> Unit,
    onNavigateUp: () -> Unit
) {
    // Estados para los campos editables
    var title by remember { mutableStateOf(recipe.title) }
    val ingredients = remember { 
        recipe.ingredients.map { 
            mutableStateOf(UpdateIngredientRequest(it.details.name, it.quantity, it.unit))
        }.toMutableStateList() 
    }
    val steps = remember { recipe.steps.map { mutableStateOf(it) }.toMutableStateList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Receta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val updatedIngredients = ingredients.map { it.value }
                        val updatedSteps = steps.map { it.value }
                        onSave(UpdateRecipeRequest(title, updatedSteps, updatedIngredients))
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it).padding(16.dp)) {
            // --- TÍTULO ---
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- INGREDIENTES ---
            item {
                Text("Ingredientes", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
            }
            itemsIndexed(ingredients) { index, ingredientState ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ingredientState.value.quantity, onValueChange = { ingredientState.value = ingredientState.value.copy(quantity = it) }, label = { Text("Cant.") }, modifier = Modifier.weight(1.5f))
                    OutlinedTextField(value = ingredientState.value.unit, onValueChange = { ingredientState.value = ingredientState.value.copy(unit = it) }, label = { Text("Unidad") }, modifier = Modifier.weight(2f))
                    OutlinedTextField(value = ingredientState.value.name, onValueChange = { ingredientState.value = ingredientState.value.copy(name = it) }, label = { Text("Nombre") }, modifier = Modifier.weight(3f))
                    IconButton(onClick = { ingredients.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar ingrediente")
                    }
                }
            }
            item {
                Button(onClick = { ingredients.add(mutableStateOf(UpdateIngredientRequest("", "", ""))) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "")
                    Text("Añadir Ingrediente")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- PASOS ---
            item {
                Text("Pasos", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
            }
            itemsIndexed(steps) { index, stepState ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = stepState.value,
                        onValueChange = { stepState.value = it },
                        label = { Text("Paso ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { steps.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar paso")
                    }
                }
            }
            item {
                Button(onClick = { steps.add(mutableStateOf("")) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "")
                    Text("Añadir Paso")
                }
            }
        }
    }
}