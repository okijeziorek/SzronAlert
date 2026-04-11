package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.FrostPhoto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDocumentationScreen(
    viewModel: PhotoDocumentationViewModel = hiltViewModel()
) {
    val photos by viewModel.photos.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.photo_doc_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Camera intent — placeholder */ }
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.photo_take_picture))
            }
        }
    ) { padding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.photo_no_photos),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoItem(
                        photo = photo,
                        onDelete = { viewModel.deletePhoto(photo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoItem(
    photo: FrostPhoto,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(photo.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (photo.note.isNotBlank()) {
                    Text(
                        text = photo.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.location_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
