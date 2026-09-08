package com.aigallery.app.presentation.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aigallery.app.core.design.LiquidGlassContainer
import com.aigallery.app.core.design.glassPressEffect
import com.aigallery.app.core.theme.GlassTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens
import com.aigallery.app.domain.model.Album
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glassColors = LocalGlassColors.current
    val formattedCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(album.itemCount)

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .glassPressEffect(onClick = onClick)
            .clip(ShapeTokens.Card)
            .background(Color(0xFF14141A))
            .border(GlassTokens.BorderSubtle, glassColors.borderSubtle, ShapeTokens.Card)
    ) {
        // Large Cover Image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(album.coverUri)
                .size(480, 560)
                .crossfade(200)
                .build(),
            contentDescription = album.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Cinematic Bottom Vignette Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x20000000),
                            Color(0xCC000000)
                        ),
                        startY = 100f
                    )
                )
        )

        // Floating Glass Info Pill
        LiquidGlassContainer(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(10.dp),
            shape = ShapeTokens.Card,
            backgroundColor = Color(0x60121218),
            elevation = GlassTokens.ElevationSubtle
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = album.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$formattedCount items",
                    color = Color(0xFFB0B0B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
