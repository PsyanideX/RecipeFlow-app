package com.psyanidex.recipeflow.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

// NUEVO: Enum para el tipo de comida
enum class MealType {
    LUNCH,
    DINNER
}

data class IngredientDetails(
    val id: Int,
    val name: String
)

data class RecipeIngredient(
    val id: Int,
    val quantity: String,
    val unit: String,
    @SerializedName("ingredient")
    val details: IngredientDetails
) {
    override fun toString(): String {
        return "$quantity $unit de ${details.name}"
    }
}

data class Recipe(
    val id: Int,
    val title: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>
)

// CAMBIO: PlannedRecipe ahora incluye el tipo de comida
data class PlannedRecipe(
    val date: LocalDate,
    val recipe: Recipe,
    val mealType: MealType
)

data class ShoppingListItem(
    val ingredient: String,
    val isChecked: Boolean = false
)

data class ImportRequest(val html: String)
