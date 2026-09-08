package com.aigallery.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aigallery.app.core.theme.GlassTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = ShapeTokens.Sheet,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = GlassTokens.AlphaScrim),
        dragHandle = null
    ) {
        LiquidGlassContainer(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = ShapeTokens.Sheet,
            backgroundColor = glassColors.surfaceHeavy,
            elevation = GlassTokens.ElevationSheet
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glass Drag Handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(glassColors.textTertiary.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(20.dp))

                content()
            }
        }
    }
}
