package com.aigallery.app.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.SpacingTokens
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeHeader(
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current
    val formattedCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(totalItems)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.Section, vertical = SpacingTokens.Small)
    ) {
        Text(
            text = "iQOO Gallery",
            color = glassColors.textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (totalItems > 0) "$formattedCount photos" else "No photos yet",
            color = glassColors.textTertiary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
    }
}
