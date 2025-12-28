package com.psyanidex.recipeflow.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

// Objeto anidado que contiene el nombre del ingrediente
data class IngredientDetails(
    val id: Int,
    val name: String
)

// Objeto que representa la relación entre receta e ingrediente (tabla join)
data class RecipeIngredient(
    val id: Int,
    val quantity: String,
    val unit: String,
    @SerializedName("ingredient") // Asegura que Gson mapee el objeto anidado correctamente
    val details: IngredientDetails
) {
    // Sobrescribimos toString para que la UI siga funcionando como antes
    override fun toString(): String {
        return "$quantity $unit de ${details.name}"
    }
}

data class Recipe(
    val id: Int, // El ID ahora es un Int
    val title: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>
)

data class PlannedRecipe(
    val date: LocalDate,
    val recipe: Recipe
)

data class ShoppingListItem(
    val ingredient: String, // Se mantiene como String, se rellena con RecipeIngredient.toString()
    val isChecked: Boolean = false
)

data class ImportRequest(val html: String)
