package com.psyanidex.recipeflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.psyanidex.recipeflow.data.ShoppingListItem

@Composable
fun ShoppingListScreen(
    items: List<ShoppingListItem>,
    onItemCheckedChanged: (Int, Boolean) -> Unit,
    onClearList: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Lista de la Compra", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onClearList) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar lista")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tu lista de la compra está vacía.")
            }
        } else {
            LazyColumn {
                itemsIndexed(items) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemCheckedChanged(index, !item.isChecked) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { isChecked -> onItemCheckedChanged(index, isChecked) }
                        )
                        Text(
                            text = item.ingredient,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}