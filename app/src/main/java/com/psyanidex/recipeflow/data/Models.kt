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
    val steps: List<String>,
    val status: String? // NUEVO: Para monitorear el estado ("COMPLETED", "FAILED", etc.)
)

data class PlannedRecipe(
    val date: LocalDate,
    val recipe: Recipe,
    val mealType: MealType
)

data class ShoppingListItem(
    val text: String,
    val isChecked: Boolean = false,
    val isCustom: Boolean = false
)

data class ImportRequest(val html: String)

// NUEVO: Respuesta inicial del endpoint de importación
data class ImportStatusResponse(
    val id: Int,
    val status: String,
    val message: String
)


// --- Modelos para la petición de actualización (existentes) ---
data class UpdateIngredientRequest(
    val name: String,
    val quantity: String,
    val unit: String
)

data class UpdateRecipeRequest(
    val title: String,
    val steps: List<String>,
    val ingredients: List<UpdateIngredientRequest>
)
