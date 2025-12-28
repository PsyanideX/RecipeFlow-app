package com.psyanidex.recipeflow.data

import java.time.LocalDate

data class Recipe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val ingredients: List<String>,
    val steps: List<String>
)

data class PlannedRecipe(
    val date: LocalDate,
    val recipe: Recipe
)

data class ShoppingListItem(
    val ingredient: String,
    val isChecked: Boolean = false
)
