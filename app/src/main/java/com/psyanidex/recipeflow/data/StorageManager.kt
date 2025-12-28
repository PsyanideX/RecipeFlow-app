package com.psyanidex.recipeflow.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// Activa el DataStore para toda la app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recipe_flow_storage")

class StorageManager(private val context: Context) {

    // Gson customizado para que entienda el tipo LocalDate de Java
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()

    // Claves para guardar los datos
    private val plannedRecipesKey = stringPreferencesKey("planned_recipes_json")
    private val shoppingListKey = stringPreferencesKey("shopping_list_json")

    // --- Planificación ---

    suspend fun savePlannedRecipes(recipes: List<PlannedRecipe>) {
        val json = gson.toJson(recipes)
        context.dataStore.edit {
            it[plannedRecipesKey] = json
        }
    }

    val plannedRecipesFlow: Flow<List<PlannedRecipe>> = context.dataStore.data.map {
        val json = it[plannedRecipesKey] ?: "[]"
        val type = object : TypeToken<List<PlannedRecipe>>() {}.type
        gson.fromJson(json, type)
    }

    // --- Lista de la Compra ---

    suspend fun saveShoppingList(items: List<ShoppingListItem>) {
        val json = gson.toJson(items)
        context.dataStore.edit {
            it[shoppingListKey] = json
        }
    }

    val shoppingListFlow: Flow<List<ShoppingListItem>> = context.dataStore.data.map {
        val json = it[shoppingListKey] ?: "[]"
        val type = object : TypeToken<List<ShoppingListItem>>() {}.type
        gson.fromJson(json, type)
    }
}

// Adaptador para que Gson sepa convertir el tipo LocalDate
private class LocalDateAdapter : com.google.gson.TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate?) {
        out.value(value?.toString())
    }

    override fun read(input: JsonReader): LocalDate? {
        return if (input.peek() == JsonToken.NULL) {
            input.nextNull()
            null
        } else {
            LocalDate.parse(input.nextString())
        }
    }
}