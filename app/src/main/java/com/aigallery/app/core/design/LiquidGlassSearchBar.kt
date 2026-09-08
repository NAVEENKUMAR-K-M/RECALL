package com.aigallery.app.core.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigallery.app.core.theme.AccentCyan
import com.aigallery.app.core.theme.GlassTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens
import com.aigallery.app.core.theme.SpacingTokens

@Composable
fun LiquidGlassSearchBar(
    modifier: Modifier = Modifier,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    placeholder: String = "Search your gallery...",
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null
) {
    val glassColors = LocalGlassColors.current

    val interactionSource = remember { MutableInteractionSource() }

    val baseModifier = modifier
        .fillMaxWidth()
        .height(52.dp)

    val clickableModifier = if (readOnly && onClick != null) {
        baseModifier.glassPressEffect(onClick = onClick)
    } else {
        baseModifier
    }

    LiquidGlassContainer(
        modifier = clickableModifier,
        shape = ShapeTokens.Pill,
        backgroundColor = glassColors.surfaceHeavy,
        elevation = GlassTokens.ElevationFloating
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBackClick
                        ),
                    tint = glassColors.textPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !readOnly && focusRequester != null,
                            onClick = { focusRequester?.requestFocus() }
                        ),
                    tint = glassColors.textSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (readOnly) {
                    Text(
                        text = if (query.isEmpty()) placeholder else query,
                        color = if (query.isEmpty()) glassColors.textTertiary else (if (glassColors.isDark) Color.White else glassColors.textPrimary),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                } else {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                        textStyle = TextStyle(
                            color = if (glassColors.isDark) Color.White else glassColors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        cursorBrush = SolidColor(AccentCyan),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        color = glassColors.textTertiary,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            if (query.isNotEmpty() && onClearClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear",
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClearClick
                        ),
                    tint = glassColors.textSecondary
                )
            }
        }
    }
}
