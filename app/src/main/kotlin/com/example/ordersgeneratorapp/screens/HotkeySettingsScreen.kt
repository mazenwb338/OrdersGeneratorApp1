package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ordersgeneratorapp.data.HotkeyPreset
import com.example.ordersgeneratorapp.data.HotkeySettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotkeySettingsScreen(
    onBackClick: () -> Unit,
    hotkeySettings: HotkeySettings,
    onHotkeySettingsChanged: (HotkeySettings) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<HotkeyPreset?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hotkey Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Hotkey")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Create hotkey presets for quick trading",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (hotkeySettings.hotkeyPresets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No hotkeys configured",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add hotkey presets to quickly place orders with predefined settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Your First Hotkey")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hotkeySettings.hotkeyPresets) { preset ->
                        HotkeyPresetCard(
                            preset = preset,
                            onEdit = { editingPreset = preset },
                            onDelete = {
                                val updatedPresets = hotkeySettings.hotkeyPresets.filter { it.id != preset.id }
                                onHotkeySettingsChanged(hotkeySettings.copy(hotkeyPresets = updatedPresets))
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog || editingPreset != null) {
        HotkeyPresetDialog(
            preset = editingPreset,
            onDismiss = {
                showAddDialog = false
                editingPreset = null
            },
            onSave = { preset ->
                val updatedPresets = if (editingPreset != null) {
                    hotkeySettings.hotkeyPresets.map { if (it.id == preset.id) preset else it }
                } else {
                    hotkeySettings.hotkeyPresets + preset
                }
                onHotkeySettingsChanged(hotkeySettings.copy(hotkeyPresets = updatedPresets))
                showAddDialog = false
                editingPreset = null
            }
        )
    }
}

@Composable
fun HotkeyPresetCard(
    preset: HotkeyPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Symbol: ${preset.symbol}", style = MaterialTheme.typography.bodyMedium)
                    Text("Quantity: ${preset.quantity}", style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Type: ${preset.orderType.uppercase()}", style = MaterialTheme.typography.bodyMedium)
                    Text("TIF: ${preset.timeInForce.uppercase()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            if (preset.limitPrice.isNotEmpty() || preset.stopPrice.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    if (preset.limitPrice.isNotEmpty()) {
                        Text("Limit: $${preset.limitPrice}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    if (preset.stopPrice.isNotEmpty()) {
                        Text("Stop: $${preset.stopPrice}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun HotkeyPresetDialog(
    preset: HotkeyPreset?,
    onDismiss: () -> Unit,
    onSave: (HotkeyPreset) -> Unit
) {
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var symbol by remember { mutableStateOf(preset?.symbol ?: "") }
    var quantity by remember { mutableStateOf(preset?.quantity ?: "") }
    var orderType by remember { mutableStateOf(preset?.orderType ?: "market") }
    var timeInForce by remember { mutableStateOf(preset?.timeInForce ?: "day") }
    var limitPrice by remember { mutableStateOf(preset?.limitPrice ?: "") }
    var stopPrice by remember { mutableStateOf(preset?.stopPrice ?: "") }
    
    val orderTypes = listOf("market", "limit", "stop", "stop_limit")
    val timeInForceOptions = listOf("day", "gtc", "ioc", "fok")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset != null) "Edit Hotkey" else "Add Hotkey") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hotkey Name") },
                    placeholder = { Text("e.g., Quick AAPL Buy") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Symbol") },
                    placeholder = { Text("AAPL") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    placeholder = { Text("10") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Order Type Dropdown
                var orderTypeExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = orderType.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Order Type") },
                        trailingIcon = {
                            IconButton(onClick = { orderTypeExpanded = !orderTypeExpanded }) {
                                Icon(
                                    if (orderTypeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = orderTypeExpanded,
                        onDismissRequest = { orderTypeExpanded = false }
                    ) {
                        orderTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.uppercase()) },
                                onClick = {
                                    orderType = type
                                    orderTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Time in Force Dropdown
                var tifExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = timeInForce.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time in Force") },
                        trailingIcon = {
                            IconButton(onClick = { tifExpanded = !tifExpanded }) {
                                Icon(
                                    if (tifExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = tifExpanded,
                        onDismissRequest = { tifExpanded = false }
                    ) {
                        timeInForceOptions.forEach { tif ->
                            DropdownMenuItem(
                                text = { Text(tif.uppercase()) },
                                onClick = {
                                    timeInForce = tif
                                    tifExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (orderType == "limit" || orderType == "stop_limit") {
                    OutlinedTextField(
                        value = limitPrice,
                        onValueChange = { limitPrice = it },
                        label = { Text("Limit Price") },
                        placeholder = { Text("150.00") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (orderType == "stop" || orderType == "stop_limit") {
                    OutlinedTextField(
                        value = stopPrice,
                        onValueChange = { stopPrice = it },
                        label = { Text("Stop Price") },
                        placeholder = { Text("145.00") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && symbol.isNotBlank() && quantity.isNotBlank()) {
                        val newPreset = HotkeyPreset(
                            id = preset?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            symbol = symbol.uppercase(),
                            quantity = quantity,
                            orderType = orderType,
                            timeInForce = timeInForce,
                            limitPrice = limitPrice,
                            stopPrice = stopPrice
                        )
                        onSave(newPreset)
                    }
                },
                enabled = name.isNotBlank() && symbol.isNotBlank() && quantity.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}