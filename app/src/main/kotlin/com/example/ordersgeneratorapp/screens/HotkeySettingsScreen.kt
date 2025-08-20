package com.example.ordersgeneratorapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ordersgeneratorapp.data.HotkeyPreset
import com.example.ordersgeneratorapp.data.HotkeySettings
import com.example.ordersgeneratorapp.data.BrokerAccount
import com.example.ordersgeneratorapp.data.ConnectionSettings
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotkeySettingsScreen(
    onBackClick: () -> Unit,
    hotkeySettings: HotkeySettings,
    connectionSettings: ConnectionSettings = ConnectionSettings(),
    onHotkeySettingsChanged: (HotkeySettings) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<HotkeyPreset?>(null) }

    if (showDialog || editingPreset != null) {
        HotkeyPresetDialog(
            preset = editingPreset,
            availableBrokers = connectionSettings.brokerAccounts,
            presetCount = hotkeySettings.presets.size,
            onDismiss = { 
                showDialog = false
                editingPreset = null
            },
            onSave = { preset ->
                val updatedPresets = if (editingPreset != null) {
                    hotkeySettings.presets.map { 
                        if (it.id == preset.id) preset else it
                    }
                } else {
                    hotkeySettings.presets + preset.copy(
                        id = UUID.randomUUID().toString(),
                        position = hotkeySettings.presets.size
                    )
                }
                onHotkeySettingsChanged(hotkeySettings.copy(presets = updatedPresets))
                showDialog = false
                editingPreset = null
            }
        )
    }

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
                    IconButton(onClick = { showDialog = true }) {
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
            if (connectionSettings.brokerAccounts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No broker accounts configured",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Please add broker accounts in Connection Settings first",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                // Group presets by symbol
                val groupedPresets = hotkeySettings.presets
                    .sortedBy { it.position }
                    .groupBy { it.symbol }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (groupedPresets.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No hotkeys configured",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { showDialog = true }) {
                                        Text("Add First Hotkey")
                                    }
                                }
                            }
                        }
                    } else {
                        groupedPresets.forEach { (symbol, presets) ->
                            item {
                                SymbolGroupCard(
                                    symbol = symbol,
                                    presets = presets,
                                    brokerAccounts = connectionSettings.brokerAccounts,
                                    onEditPreset = { editingPreset = it },
                                    onDeletePreset = { p ->
                                        val updated = hotkeySettings.presets.filterNot { it.id == p.id }
                                            .mapIndexed { idx, hk -> hk.copy(position = idx) }
                                        onHotkeySettingsChanged(hotkeySettings.copy(presets = updated))
                                    },
                                    onAddPresetForSymbol = {
                                        // Open dialog pre-filled with symbol
                                        editingPreset = HotkeyPreset(
                                            id = UUID.randomUUID().toString(),
                                            name = "",
                                            symbol = symbol,
                                            quantity = "",
                                            selectedBrokerIds = emptyList(),
                                            position = hotkeySettings.presets.size
                                        )
                                        showDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SymbolGroupCard(
    symbol: String,
    presets: List<HotkeyPreset>,
    brokerAccounts: List<BrokerAccount>,
    onEditPreset: (HotkeyPreset) -> Unit,
    onDeletePreset: (HotkeyPreset) -> Unit,
    onAddPresetForSymbol: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = symbol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row {
                    IconButton(onClick = onAddPresetForSymbol) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add preset for $symbol",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            presets.forEach { preset ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // SHOW TITLE ABOVE HOTKEY UI (use name; fallback)
                    Text(
                        text = preset.name.ifBlank { "Hotkey" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HotkeyPresetCard(
                        preset = preset,
                        brokerAccounts = brokerAccounts,
                        onEdit = { onEditPreset(preset) },
                        onDelete = { onDeletePreset(preset) }
                    )
                }
                if (preset != presets.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun HotkeyPresetCard(
    preset: HotkeyPreset,
    brokerAccounts: List<BrokerAccount>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name.ifEmpty { "Hotkey ${preset.position + 1}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${preset.orderType.uppercase()} • ${preset.quantity} shares • ${preset.timeInForce.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            if (preset.selectedBrokerIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Brokers:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    preset.selectedBrokerIds.forEach { brokerId ->
                        val brokerAccount = brokerAccounts.find { it.id == brokerId }
                        if (brokerAccount != null) {
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        text = brokerAccount.accountName.ifEmpty { 
                                            "${brokerAccount.brokerType} Account" 
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = when (brokerAccount.brokerType) {
                                        "Alpaca" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                        "IBKR" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotkeyPresetDialog(
    preset: HotkeyPreset?,
    availableBrokers: List<BrokerAccount>,
    presetCount: Int,
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
    var selectedBrokerIds by remember { mutableStateOf(preset?.selectedBrokerIds ?: emptyList()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (preset != null) "Edit Hotkey" else "Add Hotkey",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Basic Settings
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hotkey Name (Optional)") },
                    placeholder = { Text("e.g., TSLA Quick Trade") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Symbol") },
                    placeholder = { Text("e.g., TSLA") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    placeholder = { Text("100") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Order Type Selection
                val orderTypes = listOf("market", "limit", "stop", "stop_limit")
                var orderTypeExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = orderTypeExpanded,
                    onExpandedChange = { orderTypeExpanded = !orderTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = orderType.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Order Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderTypeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Time in Force Selection
                val timeInForceOptions = listOf("day", "gtc", "ioc")
                var tifExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = tifExpanded,
                    onExpandedChange = { tifExpanded = !tifExpanded }
                ) {
                    OutlinedTextField(
                        value = timeInForce.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time in Force") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tifExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
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
                
                // Price fields for limit/stop orders
                if (orderType in listOf("limit", "stop_limit")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = limitPrice,
                        onValueChange = { limitPrice = it },
                        label = { Text("Limit Price") },
                        placeholder = { Text("250.00") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (orderType in listOf("stop", "stop_limit")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stopPrice,
                        onValueChange = { stopPrice = it },
                        label = { Text("Stop Price") },
                        placeholder = { Text("245.00") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Broker Selection
                Text(
                    text = "Select Broker Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (availableBrokers.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "No broker accounts available. Please add accounts in Connection Settings.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    availableBrokers.forEach { broker ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = broker.id in selectedBrokerIds,
                                    onClick = {
                                        selectedBrokerIds = if (broker.id in selectedBrokerIds) {
                                            selectedBrokerIds - broker.id
                                        } else {
                                            selectedBrokerIds + broker.id
                                        }
                                    }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = broker.id in selectedBrokerIds,
                                onCheckedChange = {
                                    selectedBrokerIds = if (it) {
                                        selectedBrokerIds + broker.id
                                    } else {
                                        selectedBrokerIds - broker.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = broker.accountName.ifEmpty { "${broker.brokerType} Account" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${broker.brokerType} • ${if (broker.isEnabled) "Active" else "Disabled"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (symbol.isNotEmpty() && quantity.isNotEmpty() && selectedBrokerIds.isNotEmpty()) {
                                val newPreset = HotkeyPreset(
                                    id = preset?.id ?: UUID.randomUUID().toString(),
                                    position = preset?.position ?: presetCount,
                                    name = name,
                                    symbol = symbol.uppercase(),
                                    quantity = quantity,
                                    orderType = orderType,
                                    timeInForce = timeInForce,
                                    limitPrice = limitPrice,
                                    stopPrice = stopPrice,
                                    selectedBrokerIds = selectedBrokerIds
                                )
                                onSave(newPreset)
                            }
                        },
                        enabled = symbol.isNotEmpty() && quantity.isNotEmpty() && selectedBrokerIds.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}