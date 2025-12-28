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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.ShoppingListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    items: List<ShoppingListItem>,
    onItemCheckedChanged: (ShoppingListItem, Boolean) -> Unit,
    onAddItem: (String) -> Unit,
    onRemoveItem: (ShoppingListItem) -> Unit,
    onClearList: () -> Unit
) {
    val (customItems, ingredientItems) = items.partition { it.isCustom }
    var newItemText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Lista de la Compra", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onClearList) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar toda la planificación y lista")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                label = { Text("Añadir otro producto...") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (newItemText.isNotBlank()) {
                    onAddItem(newItemText)
                    newItemText = ""
                }
            }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Añadir producto", modifier = Modifier.padding(start = 8.dp))
            }
        }

        LazyColumn {
            if (items.isEmpty()) {
                item {
                    Text("Tu lista de la compra está vacía.", modifier = Modifier.padding(top = 24.dp))
                }
            }

            if (ingredientItems.isNotEmpty()) {
                item {
                    Text("Ingredientes de Recetas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }
                items(ingredientItems) { item ->
                    ShoppingItemRow(item = item, onCheckedChange = { isChecked -> onItemCheckedChanged(item, isChecked) })
                }
            }

            if (customItems.isNotEmpty()) {
                item {
                    Text("Otros Productos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }
                items(customItems) { item ->
                    ShoppingItemRow(item = item, onCheckedChange = { isChecked -> onItemCheckedChanged(item, isChecked) }) {
                        IconButton(onClick = { onRemoveItem(item) }) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar producto")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(item: ShoppingListItem, onCheckedChange: (Boolean) -> Unit, trailingContent: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!item.isChecked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = item.text,
            modifier = Modifier.weight(1f),
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
        )
        trailingContent?.invoke()
    }
}