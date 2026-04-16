package com.onlyflows.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.SavedLocation
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsSettingsScreen(
    onBack: () -> Unit = {},
) {
    val context   = LocalContext.current
    val container = (context.applicationContext as OnlyFlowsApp).container
    val locRepo   = container.savedLocationRepository
    val geofence  = container.geofenceManager
    val scope     = rememberCoroutineScope()

    val locations by locRepo.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    // Permission state
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* re-compose will re-read the permission */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Locations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (!hasFineLocation) {
                    permLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        )
                    )
                } else {
                    showAddDialog = true
                }
            }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add location")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        if (!hasFineLocation) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Location permission required",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to grant permission and add your first location.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (locations.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(
                    "No saved locations yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(locations, key = { it.id }) { loc ->
                    LocationRow(
                        location = loc,
                        onDelete = {
                            scope.launch {
                                geofence.unregister(loc.id)
                                locRepo.delete(loc)
                            }
                        },
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 56.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            }
        }

        if (showAddDialog) {
            AddLocationDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { label, lat, lng, radius ->
                    scope.launch {
                        val loc = SavedLocation(
                            id           = "loc_${UUID.randomUUID()}",
                            label        = label,
                            latitude     = lat,
                            longitude    = lng,
                            radiusMeters = radius,
                        )
                        locRepo.insert(loc)
                        geofence.register(loc.id, loc.latitude, loc.longitude, loc.radiusMeters)
                    }
                    showAddDialog = false
                },
            )
        }
    }
}

@Composable
private fun LocationRow(location: SavedLocation, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            Icons.Outlined.LocationOn,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = location.label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text  = "%.5f, %.5f  ·  ${location.radiusMeters.toInt()} m".format(
                    location.latitude, location.longitude
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AddLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, lat: Double, lng: Double, radius: Float) -> Unit,
) {
    var label  by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf("") }
    var lngStr by remember { mutableStateOf("") }
    var radStr by remember { mutableStateOf("100") }

    val isValid = label.isNotBlank() &&
            latStr.toDoubleOrNull() != null &&
            lngStr.toDoubleOrNull() != null &&
            radStr.toFloatOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Location") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = label,
                    onValueChange = { label = it },
                    label         = { Text("Label (e.g. Home)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = latStr,
                    onValueChange = { latStr = it },
                    label         = { Text("Latitude") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = lngStr,
                    onValueChange = { lngStr = it },
                    label         = { Text("Longitude") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = radStr,
                    onValueChange = { radStr = it },
                    label         = { Text("Radius (meters)") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = {
                    onConfirm(label, latStr.toDouble(), lngStr.toDouble(), radStr.toFloat())
                },
                enabled = isValid,
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
