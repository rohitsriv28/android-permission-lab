package com.permissionlab.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.permissionlab.app.model.PermissionModule
import com.permissionlab.app.model.PermissionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val modules by viewModel.permissionModules.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Lab") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(modules) { module ->
                PermissionCard(
                    module = module,
                    onExploreClick = {
                        if (module.id == "camera") onNavigateToCamera()
                        else if (module.id == "gallery") onNavigateToGallery()
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    module: PermissionModule,
    onExploreClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    StatusBadge(status = module.status)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = module.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onExploreClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Explore")
            }
        }
    }
}

@Composable
fun StatusBadge(status: PermissionStatus) {
    val (text, color) = when (status) {
        PermissionStatus.GRANTED -> "Granted" to Color(0xFF4CAF50)
        PermissionStatus.NOT_GRANTED -> "Not Granted" to MaterialTheme.colorScheme.error
        PermissionStatus.PARTIAL -> "Partial" to Color(0xFFFF9800)
        PermissionStatus.NOT_CONNECTED -> "Not Connected" to Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
