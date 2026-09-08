package com.aigallery.app.presentation.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigallery.app.core.theme.ColorTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens
import com.aigallery.app.core.utils.DateTimeUtils
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.model.MediaMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    item: MediaItem,
    metadata: MediaMetadata?,
    onDismissRequest: () -> Unit
) {
    val glassColors = LocalGlassColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = ShapeTokens.Sheet,
        containerColor = Color(0xFF14141C),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Filename
            Text(
                text = item.filename,
                color = glassColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Formatted Date
            Text(
                text = DateTimeUtils.formatDetailDate(item.dateTaken),
                color = glassColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Metadata Grid Rows
            // 1. Dimensions & File Size
            val dimensions = if (item.width > 0 && item.height > 0) {
                "${item.width} × ${item.height} • ${item.formattedSize}"
            } else {
                item.formattedSize
            }
            InfoItemRow(
                icon = Icons.Rounded.Image,
                title = "Format & Size",
                subtitle = dimensions
            )

            // 2. Album & Storage Path
            val albumLocation = if (item.albumName.isNotBlank()) {
                "${item.albumName} (${item.relativePath.ifBlank { "Internal Storage" }})"
            } else {
                item.relativePath.ifBlank { "Internal Storage" }
            }
            InfoItemRow(
                icon = Icons.Rounded.FolderOpen,
                title = "Album",
                subtitle = albumLocation
            )

            // 3. Camera EXIF (if photo and available)
            if (metadata?.cameraModel != null) {
                val specs = buildList {
                    metadata.focalLength?.let { add(it) }
                    metadata.aperture?.let { add(it) }
                    metadata.shutterSpeed?.let { add(it) }
                    metadata.iso?.let { add(it) }
                }.joinToString(" • ")

                InfoItemRow(
                    icon = Icons.Rounded.CameraAlt,
                    title = metadata.cameraModel,
                    subtitle = specs.ifBlank { "Captured with device camera" }
                )
            }

            // 4. GPS Location (if available)
            if (metadata?.locationName != null) {
                InfoItemRow(
                    icon = Icons.Rounded.LocationOn,
                    title = "Location",
                    subtitle = metadata.locationName
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val glassColors = LocalGlassColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = ColorTokens.AccentElectric
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = glassColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = glassColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
