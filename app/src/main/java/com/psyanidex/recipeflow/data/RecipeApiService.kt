package com.psyanidex.recipeflow.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RecipeApiService {

    @GET("recipes")
    suspend fun getRecipes(): List<Recipe>

    @POST("recipes/import")
    suspend fun importRecipe(@Body request: ImportRequest): Recipe

    @GET("recipes/{id}")
    suspend fun getRecipeById(@Path("id") id: Int): Recipe // Cambiado a Int

}
