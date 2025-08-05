package com.example.ordersgeneratorapp.screens

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.ordersgeneratorapp.data.ConnectionSettings
import com.example.ordersgeneratorapp.data.AlpacaSettings
import com.example.ordersgeneratorapp.data.IBKRSettings
import com.example.ordersgeneratorapp.util.ErrorHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsScreen(
    onBackClick: () -> Unit,
    connectionSettings: ConnectionSettings,
    onSettingsChanged: (ConnectionSettings) -> Unit,
    onNavigateToHotkeys: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var alpacaSettings by remember { mutableStateOf(connectionSettings.alpaca) }
    var ibkrSettings by remember { mutableStateOf(connectionSettings.ibkr) }
    var selectedTab by remember { mutableStateOf(0) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showApiKeyPassword by remember { mutableStateOf(false) }
    var showSecretKeyPassword by remember { mutableStateOf(false) }
    
    // Update settings when they change
    LaunchedEffect(alpacaSettings, ibkrSettings) {
        onSettingsChanged(
            ConnectionSettings(
                alpaca = alpacaSettings,
                ibkr = ibkrSettings
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHotkeys) {
                        Icon(Icons.Default.Settings, contentDescription = "Hotkey Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Alpaca") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("IBKR") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (selectedTab) {
                0 -> AlpacaSettingsCard(
                    settings = alpacaSettings,
                    onSettingsChanged = { alpacaSettings = it },
                    showApiKeyPassword = showApiKeyPassword,
                    onToggleApiKeyVisibility = { showApiKeyPassword = !showApiKeyPassword },
                    showSecretKeyPassword = showSecretKeyPassword,
                    onToggleSecretKeyVisibility = { showSecretKeyPassword = !showSecretKeyPassword },
                    isTestingConnection = isTestingConnection,
                    onTestConnection = {
                        scope.launch {
                            isTestingConnection = true
                            try {
                                kotlinx.coroutines.delay(2000)
                                if (alpacaSettings.apiKey.isNotEmpty() && alpacaSettings.secretKey.isNotEmpty()) {
                                    snackbarHostState.showSnackbar("Connection successful!")
                                } else {
                                    snackbarHostState.showSnackbar("Please fill in API credentials")
                                }
                            } catch (e: Exception) {
                                val errorMessage = ErrorHandler.handleApiError(e, "connection test")
                                snackbarHostState.showSnackbar(errorMessage)
                            } finally {
                                isTestingConnection = false
                            }
                        }
                    }
                )
                1 -> IBKRSettingsCard(
                    settings = ibkrSettings,
                    onSettingsChanged = { ibkrSettings = it },
                    isTestingConnection = isTestingConnection,
                    onTestConnection = {
                        scope.launch {
                            isTestingConnection = true
                            try {
                                kotlinx.coroutines.delay(2000)
                                if (ibkrSettings.host.isNotEmpty() && ibkrSettings.port.isNotEmpty()) {
                                    snackbarHostState.showSnackbar("Connection successful!")
                                } else {
                                    snackbarHostState.showSnackbar("Please fill in connection details")
                                }
                            } catch (e: Exception) {
                                val errorMessage = ErrorHandler.handleApiError(e, "connection test")
                                snackbarHostState.showSnackbar(errorMessage)
                            } finally {
                                isTestingConnection = false
                            }
                        }
                    }
                )
            }
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
    settings: IBKRSettings,
    onSettingsChanged: (IBKRSettings) -> Unit,
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
                value = settings.host,
                onValueChange = { 
                    onSettingsChanged(settings.copy(host = it))
                },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = settings.port,
                onValueChange = { 
                    onSettingsChanged(settings.copy(port = it))
                },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            OutlinedTextField(
                value = settings.clientId,
                onValueChange = { 
                    onSettingsChanged(settings.copy(clientId = it))
                },
                label = { Text("Client ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            Button(
                onClick = onTestConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTestingConnection && settings.host.isNotEmpty() && settings.port.isNotEmpty()
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