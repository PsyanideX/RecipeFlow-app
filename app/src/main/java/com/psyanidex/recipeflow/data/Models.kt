package com.psyanidex.recipeflow.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

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

data class PlannedRecipe(
    val date: LocalDate,
    val recipe: Recipe,
    val mealType: MealType
)

// CAMBIO: 'ingredient' ahora es 'text' y se añade 'isCustom'
data class ShoppingListItem(
    val text: String,
    val isChecked: Boolean = false,
    val isCustom: Boolean = false // Para diferenciar ingredientes de otros productos
)

data class ImportRequest(val html: String)
