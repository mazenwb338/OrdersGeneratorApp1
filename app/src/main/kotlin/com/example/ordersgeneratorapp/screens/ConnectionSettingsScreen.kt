package com.example.ordersgeneratorapp.screens

import android.util.Log  // ✅ Add this missing import
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.data.AlpacaSettings
import com.example.ordersgeneratorapp.data.IbkrSettings
import com.example.ordersgeneratorapp.data.BrokerAccount
import com.example.ordersgeneratorapp.repository.AlpacaRepository
import com.example.ordersgeneratorapp.repository.IbkrRepository
import com.example.ordersgeneratorapp.util.SettingsManager
import com.example.ordersgeneratorapp.util.ErrorHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsScreen(
    onBackClick: () -> Unit,
    connectionSettings: ConnectionSettings,
    onSettingsChanged: (ConnectionSettings) -> Unit,
    onNavigateToHotkeys: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val alpacaRepository = remember { AlpacaRepository(settingsManager) }
    val ibkrRepo = remember { IbkrRepository() }
    val scope = rememberCoroutineScope()

    var showApiKeyPassword by remember { mutableStateOf(false) }
    var showSecretKeyPassword by remember { mutableStateOf(false) }
    var isTestingAlpaca by remember { mutableStateOf(false) }
    var isTestingIBKR by remember { mutableStateOf(false) }
    var testConnectionMessage by remember { mutableStateOf<String?>(null) }
    var ibkrTestStatus by remember { mutableStateOf<String?>(null) }

    // Helper function to convert old settings to new broker accounts
    fun updateBrokerAccounts(newConnectionSettings: ConnectionSettings): ConnectionSettings {
        val brokerAccounts = mutableListOf<BrokerAccount>()
        
        Log.d("ConnectionSettings", "Updating broker accounts - Alpaca: ${newConnectionSettings.alpaca.apiKey.isNotEmpty()}, IBKR: ${newConnectionSettings.ibkr.host.isNotEmpty()}")
        
        // Convert Alpaca settings to BrokerAccount if configured
        if (newConnectionSettings.alpaca.apiKey.isNotEmpty() && newConnectionSettings.alpaca.secretKey.isNotEmpty()) {
            val alpacaAccount = BrokerAccount(
                id = "alpaca-main",
                brokerType = "Alpaca",
                accountName = "Alpaca Main Account",
                isEnabled = true,
                alpacaApiKey = newConnectionSettings.alpaca.apiKey,
                alpacaSecretKey = newConnectionSettings.alpaca.secretKey,
                alpacaBaseUrl = newConnectionSettings.alpaca.baseUrl
            )
            brokerAccounts.add(alpacaAccount)
            Log.d("ConnectionSettings", "Created Alpaca broker account")
        }
        
        // Convert IBKR settings to BrokerAccount if configured
        if (newConnectionSettings.ibkr.host.isNotEmpty() && newConnectionSettings.ibkr.port.isNotEmpty()) {
            val ibkrAccount = BrokerAccount(
                id = "ibkr-main",
                brokerType = "IBKR", 
                accountName = "IBKR Main Account",
                isEnabled = true,
                ibkrHost = newConnectionSettings.ibkr.host,
                ibkrPort = newConnectionSettings.ibkr.port,
                ibkrClientId = newConnectionSettings.ibkr.clientId,
                ibkrAccountId = newConnectionSettings.ibkr.accountId
            )
            brokerAccounts.add(ibkrAccount)
            Log.d("ConnectionSettings", "Created IBKR broker account")
        }
        
        val result = newConnectionSettings.copy(brokerAccounts = brokerAccounts)
        Log.d("ConnectionSettings", "Final broker accounts count: ${result.brokerAccounts.size}")
        return result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHotkeys) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Hotkey Settings")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Navigation to Hotkeys Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHotkeys() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚡ Hotkey Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Configure trading hotkeys and presets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Go to Hotkeys",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            BrokerAccountsSection(
                brokerAccounts = connectionSettings.brokerAccounts,
                onChange = { updatedList ->
                    val updated = connectionSettings.copy(brokerAccounts = updatedList)
                    onSettingsChanged(updated)
                }
            )

            // Alpaca Settings Card
            AlpacaSettingsCard(
                settings = connectionSettings.alpaca,
                onSettingsChanged = { newAlpacaSettings ->
                    val newConnectionSettings = connectionSettings.copy(alpaca = newAlpacaSettings)
                    val updatedConnectionSettings = updateBrokerAccounts(newConnectionSettings)
                    onSettingsChanged(updatedConnectionSettings)
                },
                showApiKeyPassword = showApiKeyPassword,
                onToggleApiKeyVisibility = { showApiKeyPassword = !showApiKeyPassword },
                showSecretKeyPassword = showSecretKeyPassword,
                onToggleSecretKeyVisibility = { showSecretKeyPassword = !showSecretKeyPassword },
                isTestingConnection = isTestingAlpaca,
                onTestConnection = {
                    scope.launch {
                        isTestingAlpaca = true
                        testConnectionMessage = "Testing Alpaca connection..."

                        val result = alpacaRepository.testConnection()
                        testConnectionMessage = if (result.isSuccess) {
                            result.getOrNull() ?: "Connection successful"
                        } else {
                            "Connection failed: ${result.exceptionOrNull()?.message}"
                        }

                        isTestingAlpaca = false

                        delay(3000)
                        testConnectionMessage = null
                    }
                }
            )

            // IBKR Settings Card
            IBKRSettingsCard(
                settings = connectionSettings.ibkr,
                onSettingsChanged = { newIbkrSettings ->
                    val newConnectionSettings = connectionSettings.copy(ibkr = newIbkrSettings)
                    val updatedConnectionSettings = updateBrokerAccounts(newConnectionSettings)
                    onSettingsChanged(updatedConnectionSettings)
                },
                isTestingConnection = isTestingIBKR,
                onTestConnection = {
                    scope.launch {
                        isTestingIBKR = true
                        testConnectionMessage = "Testing IBKR connection..."

                        // TODO: Implement IBKR connection test
                        delay(2000)
                        testConnectionMessage = "IBKR connection test not implemented yet"

                        isTestingIBKR = false

                        delay(3000)
                        testConnectionMessage = null
                    }
                }
            )

            // ✅ Manual Test Broker Creation (for debugging)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🧪 Testing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            val testBrokerAccount = BrokerAccount(
                                id = "test-alpaca-${System.currentTimeMillis()}",
                                brokerType = "Alpaca",
                                accountName = "Test Alpaca Account",
                                isEnabled = true,
                                alpacaApiKey = "test-api-key",
                                alpacaSecretKey = "test-secret-key",
                                alpacaBaseUrl = "https://paper-api.alpaca.markets"
                            )
                            
                            val updatedSettings = connectionSettings.copy(
                                brokerAccounts = connectionSettings.brokerAccounts + testBrokerAccount
                            )
                            
                            Log.d("ConnectionSettings", "Adding test broker account. Total: ${updatedSettings.brokerAccounts.size}")
                            onSettingsChanged(updatedSettings)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Test Broker Account")
                    }
                    
                    if (connectionSettings.brokerAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val updatedSettings = connectionSettings.copy(brokerAccounts = emptyList())
                                Log.d("ConnectionSettings", "Cleared all broker accounts")
                                onSettingsChanged(updatedSettings)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Clear All Broker Accounts")
                        }
                    }
                }
            }

            // Test Connection Message
            testConnectionMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.contains("successful") || message.contains("✅")) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = if (message.contains("successful") || message.contains("✅")) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            // IBKR Test Connection Button
            Button(onClick = {
                scope.launch {
                    ibkrTestStatus = "Testing..."
                    val r = ibkrRepo.testConnection(
                        apiKey = connectionSettings.ibkr.apiKey,
                        baseUrl = connectionSettings.ibkr.baseUrl
                    )
                    ibkrTestStatus = if (r.isSuccess) "IBKR OK" else "IBKR FAILED: ${r.exceptionOrNull()?.message}"
                }
            }) { Text("Test IBKR Connection") }

            ibkrTestStatus?.let { Text(it) }
        }
    }
}

@Composable
private fun AlpacaSettingsCard(
    settings: AlpacaSettings,
    onSettingsChanged: (AlpacaSettings) -> Unit,
    showApiKeyPassword: Boolean,
    onToggleApiKeyVisibility: () -> Unit,
    showSecretKeyPassword: Boolean,
    onToggleSecretKeyVisibility: () -> Unit,
    isTestingConnection: Boolean,
    onTestConnection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Alpaca Trading API",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paper Trading")
                Switch(
                    checked = settings.isPaper,
                    onCheckedChange = {
                        onSettingsChanged(settings.copy(isPaper = it))
                    }
                )
            }

            OutlinedTextField(
                value = settings.baseUrl,
                onValueChange = {
                    onSettingsChanged(settings.copy(baseUrl = it))
                },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = {
                    onSettingsChanged(settings.copy(apiKey = it))
                },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showApiKeyPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = onToggleApiKeyVisibility) {
                        Icon(
                            imageVector = if (showApiKeyPassword) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = if (showApiKeyPassword) {
                                "Hide API key"
                            } else {
                                "Show API key"
                            }
                        )
                    }
                },
                singleLine = true
            )

            OutlinedTextField(
                value = settings.secretKey,
                onValueChange = {
                    onSettingsChanged(settings.copy(secretKey = it))
                },
                label = { Text("Secret Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showSecretKeyPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = onToggleSecretKeyVisibility) {
                        Icon(
                            imageVector = if (showSecretKeyPassword) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = if (showSecretKeyPassword) {
                                "Hide secret key"
                            } else {
                                "Show secret key"
                            }
                        )
                    }
                },
                singleLine = true
            )

            Button(
                onClick = onTestConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTestingConnection && settings.apiKey.isNotEmpty() && settings.secretKey.isNotEmpty()
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Text("Test Connection")
                }
            }
        }
    }
}

@Composable
private fun IBKRSettingsCard(
    settings: IbkrSettings,
    onSettingsChanged: (IbkrSettings) -> Unit,
    isTestingConnection: Boolean,
    onTestConnection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Interactive Brokers API",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = settings.host,
                onValueChange = { onSettingsChanged(settings.copy(host = it)) },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.port,
                onValueChange = { onSettingsChanged(settings.copy(port = it)) },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.clientId,
                onValueChange = { onSettingsChanged(settings.copy(clientId = it)) },
                label = { Text("Client ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.accountId,
                onValueChange = { onSettingsChanged(settings.copy(accountId = it)) },
                label = { Text("Account ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = { onSettingsChanged(settings.copy(apiKey = it)) },
                label = { Text("API Key (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.baseUrl,
                onValueChange = { onSettingsChanged(settings.copy(baseUrl = it)) },
                label = { Text("Base URL (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = onTestConnection,
                enabled = !isTestingConnection &&
                        settings.host.isNotEmpty() &&
                        settings.port.isNotEmpty() &&
                        settings.clientId.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Text("Test Connection")
                }
            }
        }
    }
}

@Composable
private fun BrokerAccountsSection(
    brokerAccounts: List<BrokerAccount>,
    onChange: (List<BrokerAccount>) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val alpacaRepo = remember { AlpacaRepository(settingsManager) }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var addType by remember { mutableStateOf("Alpaca") }
    var editingAccount: BrokerAccount? by remember { mutableStateOf(null) }

    // test status per account id
    var testStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    fun setStatus(id: String, status: String) {
        testStatuses = testStatuses.toMutableMap().apply { put(id, status) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Broker Accounts", style = MaterialTheme.typography.headlineSmall)

            brokerAccounts.forEach { acct ->
                BrokerAccountRow(
                    account = acct,
                    status = testStatuses[acct.id],
                    onToggleEnabled = {
                        onChange(brokerAccounts.map {
                            if (it.id == acct.id) it.copy(isEnabled = !it.isEnabled) else it
                        })
                    },
                    onEdit = { editingAccount = acct },
                    onDelete = {
                        onChange(brokerAccounts.filterNot { it.id == acct.id })
                    },
                    onTest = {
                        if (acct.brokerType == "Alpaca") {
                            scope.launch {
                                setStatus(acct.id, "Testing...")
                                val result = alpacaRepo.testCredentials(
                                    apiKey = acct.alpacaApiKey,
                                    secretKey = acct.alpacaSecretKey,
                                    isPaper = acct.alpacaIsPaper
                                )
                                setStatus(
                                    acct.id,
                                    result.getOrElse { "❌ ${it.message}" }
                                )
                            }
                        } else {
                            setStatus(acct.id, "Test not implemented")
                        }
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { addType = "Alpaca"; showAddDialog = true }) { Text("Add Alpaca") }
                Button(onClick = { addType = "IBKR"; showAddDialog = true }) { Text("Add IBKR") }
            }
        }
    }

    if (showAddDialog) {
        BrokerAccountDialog(
            initial = null,
            brokerType = addType,
            onDismiss = { showAddDialog = false },
            onSave = { newAcct ->
                onChange(brokerAccounts + newAcct)
                showAddDialog = false
            }
        )
    }

    editingAccount?.let { acct ->
        BrokerAccountDialog(
            initial = acct,
            brokerType = acct.brokerType,
            onDismiss = { editingAccount = null },
            onSave = { updated ->
                onChange(brokerAccounts.map { if (it.id == acct.id) updated else it })
                editingAccount = null
            }
        )
    }
}

@Composable
private fun BrokerAccountRow(
    account: BrokerAccount,
    status: String?,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isEnabled)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${account.accountName} (${account.brokerType})", fontWeight = FontWeight.Bold)
                    val cred = when (account.brokerType) {
                        "Alpaca" -> if (account.alpacaApiKey.isNotBlank()) account.alpacaApiKey.take(6) + "…" else ""
                        "IBKR" -> listOf(account.ibkrHost, account.ibkrPort).filter { it.isNotBlank() }.joinToString(":")
                        else -> ""
                    }
                    if (cred.isNotBlank()) {
                        Text(cred, style = MaterialTheme.typography.bodySmall)
                    }
                    if (status != null) {
                        Text(status, style = MaterialTheme.typography.bodySmall,
                            color = if (status.startsWith("✅")) MaterialTheme.colorScheme.primary
                            else if (status.startsWith("❌")) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (account.brokerType == "Alpaca") {
                        TextButton(onClick = onTest, enabled = status != "Testing...") {
                            Text(if (status == "Testing...") "Testing..." else "Test")
                        }
                    }
                    IconButton(onClick = onToggleEnabled) {
                        Icon(
                            imageVector = if (account.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Enable/Disable"
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun BrokerAccountDialog(
    initial: BrokerAccount?,
    brokerType: String,
    onDismiss: () -> Unit,
    onSave: (BrokerAccount) -> Unit
) {
    var accountName by remember { mutableStateOf(initial?.accountName ?: "${brokerType} Account") }

    // Alpaca fields
    var alpacaApiKey by remember { mutableStateOf(initial?.alpacaApiKey ?: "") }
    var alpacaSecret by remember { mutableStateOf(initial?.alpacaSecretKey ?: "") }
    var alpacaBaseUrl by remember { mutableStateOf(initial?.alpacaBaseUrl ?: "https://paper-api.alpaca.markets") }
    var alpacaIsPaper by remember { mutableStateOf(initial?.alpacaIsPaper ?: true) }

    // IBKR fields
    var ibkrHost by remember { mutableStateOf(initial?.ibkrHost ?: "127.0.0.1") }
    var ibkrPort by remember { mutableStateOf(initial?.ibkrPort ?: "7497") }
    var ibkrClientId by remember { mutableStateOf(initial?.ibkrClientId ?: "1") }
    var ibkrAccountId by remember { mutableStateOf(initial?.ibkrAccountId ?: "") }
    var ibkrGatewayBaseUrl by remember { mutableStateOf(initial?.ibkrGatewayBaseUrl ?: "") }
    var ibkrApiKey by remember { mutableStateOf(initial?.ibkrApiKey ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add $brokerType Account" else "Edit $brokerType Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Name") },
                    singleLine = true
                )
                if (brokerType == "Alpaca") {
                    OutlinedTextField(alpacaApiKey, { alpacaApiKey = it }, label = { Text("API Key") }, singleLine = true)
                    OutlinedTextField(alpacaSecret, { alpacaSecret = it }, label = { Text("Secret Key") }, singleLine = true)
                    OutlinedTextField(alpacaBaseUrl, { alpacaBaseUrl = it }, label = { Text("Base URL") }, singleLine = true)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paper Trading")
                        Switch(checked = alpacaIsPaper, onCheckedChange = { alpacaIsPaper = it })
                    }
                } else if (brokerType == "IBKR") {
                    OutlinedTextField(ibkrHost, { ibkrHost = it }, label = { Text("Host") }, singleLine = true)
                    OutlinedTextField(ibkrPort, { ibkrPort = it }, label = { Text("Port") }, singleLine = true)
                    OutlinedTextField(ibkrClientId, { ibkrClientId = it }, label = { Text("Client ID") }, singleLine = true)
                    OutlinedTextField(ibkrAccountId, { ibkrAccountId = it }, label = { Text("Account ID") }, singleLine = true)
                    OutlinedTextField(ibkrGatewayBaseUrl, { ibkrGatewayBaseUrl = it }, label = { Text("Gateway Base URL") }, singleLine = true)
                    OutlinedTextField(ibkrApiKey, { ibkrApiKey = it }, label = { Text("API Key (optional)") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = when (brokerType) {
                    "Alpaca" -> alpacaApiKey.isNotBlank() && alpacaSecret.isNotBlank()
                    "IBKR" -> ibkrHost.isNotBlank() && ibkrPort.isNotBlank() && ibkrClientId.isNotBlank()
                    else -> false
                },
                onClick = {
                    val acct = (initial ?: BrokerAccount(
                        id = "${brokerType.lowercase()}-${System.currentTimeMillis()}",
                        brokerType = brokerType
                    )).copy(
                        accountName = accountName,
                        // Alpaca
                        alpacaApiKey = if (brokerType == "Alpaca") alpacaApiKey else "",
                        alpacaSecretKey = if (brokerType == "Alpaca") alpacaSecret else "",
                        alpacaBaseUrl = if (brokerType == "Alpaca") alpacaBaseUrl else "",
                        alpacaIsPaper = if (brokerType == "Alpaca") alpacaIsPaper else true,
                        // IBKR
                        ibkrHost = if (brokerType == "IBKR") ibkrHost else "",
                        ibkrPort = if (brokerType == "IBKR") ibkrPort else "",
                        ibkrClientId = if (brokerType == "IBKR") ibkrClientId else "",
                        ibkrAccountId = if (brokerType == "IBKR") ibkrAccountId else "",
                        ibkrGatewayBaseUrl = if (brokerType == "IBKR") ibkrGatewayBaseUrl else "",
                        ibkrApiKey = if (brokerType == "IBKR") ibkrApiKey else ""
                    )
                    onSave(acct)
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}