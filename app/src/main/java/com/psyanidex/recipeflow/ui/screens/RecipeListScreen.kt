package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(recipes: List<Recipe>, onRecipeClick: (Recipe) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mis Recetas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recipes) { recipe ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onRecipeClick(recipe) }
                ) {
                    ListItem(
                        headlineContent = { Text(recipe.title) },
                        supportingContent = { Text("${recipe.ingredients.size} ingredientes") }
                    )
                }
            }
        }
    }
}