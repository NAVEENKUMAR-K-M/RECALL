package com.aigallery.app.presentation.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigallery.app.core.design.LiquidGlassButton
import com.aigallery.app.core.design.LiquidGlassToolbar
import com.aigallery.app.core.theme.ColorTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.SpacingTokens

@Composable
fun SelectionTopBar(
    count: Int,
    onCloseClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current

    LiquidGlassToolbar(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = SpacingTokens.Section, vertical = SpacingTokens.Small)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cancel Selection",
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCloseClick
                    ),
                tint = glassColors.textPrimary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "$count selected",
                color = glassColors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.SelectAll,
                contentDescription = "Select All",
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSelectAllClick
                    ),
                tint = glassColors.textPrimary
            )
        }
    }
}

@Composable
fun SelectionBottomBar(
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassToolbar(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = SpacingTokens.Section, vertical = SpacingTokens.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Share", fontSize = 14.sp)
            }

            LiquidGlassButton(
                onClick = onDeleteClick,
                contentColor = ColorTokens.AccentHeart
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = ColorTokens.AccentHeart
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Delete", fontSize = 14.sp, color = ColorTokens.AccentHeart)
            }
        }
    }
}
