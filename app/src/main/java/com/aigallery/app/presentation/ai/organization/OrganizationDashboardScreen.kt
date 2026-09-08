package com.aigallery.app.presentation.ai.organization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigallery.app.core.theme.AccentCyan
import com.aigallery.app.core.theme.AccentPurple
import com.aigallery.app.core.theme.DarkBackground
import com.aigallery.app.core.theme.GlassBorderLight
import com.aigallery.app.core.theme.GlassSurfaceDark
import com.aigallery.app.domain.organization.model.ScreenshotCategory
import com.aigallery.app.domain.organization.repository.OrganizationSettings

@Composable
fun OrganizationDashboardScreen(
    viewModel: OrganizationDashboardViewModel,
    onCategoryClick: (ScreenshotCategory) -> Unit,
    onOpenDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettingsSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.FolderSpecial,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Organization",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smart Categories & Folders",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDebug) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug Diagnostics",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stat Cards Overview
                item {
                    StatisticsGlassCard(
                        totalUnderstood = uiState.stats.totalUnderstood,
                        totalOrganized = uiState.stats.totalOrganized,
                        totalPending = uiState.stats.totalPending,
                        totalUnsorted = uiState.stats.totalUnsorted
                    )
                }

                // Batch Organize Action Banner
                item {
                    val pendingCount = uiState.stats.totalPending
                    val unorganizedCount = (uiState.stats.totalUnderstood - uiState.stats.totalOrganized).coerceAtLeast(0)
                    val readyToOrganize = pendingCount > 0 || unorganizedCount > 0
                    val isOrganizing = uiState.isOrganizing

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        AccentCyan.copy(alpha = 0.15f),
                                        AccentPurple.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isOrganizing) "Organizing screenshots..."
                                        else if (readyToOrganize) "${if (pendingCount > 0) pendingCount else unorganizedCount} screenshots ready"
                                        else "Library fully organized",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isOrganizing) "Applying AI categories and smart collections..."
                                        else if (readyToOrganize) "Sort detected screenshots into intelligent collections."
                                        else "All screenshots categorized into smart folders.",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Button(
                                    onClick = { viewModel.organizeNow() },
                                    enabled = !isOrganizing && (readyToOrganize || uiState.stats.totalUnderstood > 0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentCyan,
                                        disabledContainerColor = Color.White.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    if (isOrganizing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = if (readyToOrganize) "Organize Now" else if (uiState.stats.totalOrganized > 0) "Reorganize" else "Up to date",
                                            color = if (readyToOrganize || uiState.stats.totalOrganized > 0) Color.Black else Color.White.copy(alpha = 0.4f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (isOrganizing) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = AccentCyan,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                // Section Title: Intelligent Collections
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "INTELLIGENT COLLECTIONS",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "${uiState.categories.count { it.count > 0 }} active",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Category Rows
                if (uiState.categories.all { it.count == 0 } && uiState.stats.totalUnderstood == 0) {
                    item {
                        EmptyOrganizationCard()
                    }
                } else {
                    items(
                        items = uiState.categories,
                        key = { it.category.id }
                    ) { item ->
                        CategoryRowItem(
                            item = item,
                            onClick = { onCategoryClick(item.category) }
                        )
                    }
                }
            }
        }

        // Settings Sheet
        if (showSettingsSheet) {
            OrganizationSettingsSheet(
                settings = uiState.settings,
                onSettingsChange = { newSettings -> viewModel.updateSettings(newSettings) },
                onReorganizeAll = { viewModel.reorganizeAll() },
                onCleanOrganizedCopies = { viewModel.cleanAllOrganizedCopies() },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

@Composable
private fun StatisticsGlassCard(
    totalUnderstood: Int,
    totalOrganized: Int,
    totalPending: Int,
    totalUnsorted: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderLight, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Screenshot Library",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "On-Device",
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMetricBox(
                    label = "Indexed",
                    count = totalUnderstood,
                    icon = Icons.Rounded.TaskAlt,
                    color = AccentCyan
                )
                StatMetricBox(
                    label = "Organized",
                    count = totalOrganized,
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50)
                )
                StatMetricBox(
                    label = "Awaiting",
                    count = totalPending,
                    icon = Icons.Default.HourglassTop,
                    color = if (totalPending > 0) Color(0xFFFFB300) else Color.White.copy(alpha = 0.5f)
                )
                StatMetricBox(
                    label = "Unsorted",
                    count = totalUnsorted,
                    icon = Icons.Default.QuestionMark,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun StatMetricBox(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$count",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CategoryRowItem(
    item: CategoryDisplayItem,
    onClick: () -> Unit
) {
    val hasItems = item.count > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurfaceDark)
            .border(
                1.dp,
                if (hasItems) GlassBorderLight else GlassBorderLight.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon with subtle glow if active
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hasItems) AccentCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            1.dp,
                            if (hasItems) AccentCyan.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.category.icon,
                        contentDescription = item.category.displayName,
                        tint = if (hasItems) AccentCyan else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = item.category.displayName,
                        color = if (hasItems) Color.White else Color.White.copy(alpha = 0.45f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (item.subcategories.isNotEmpty()) {
                            item.subcategories.take(3).joinToString(" • ")
                        } else {
                            item.category.description
                        },
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasItems) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${item.count}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyOrganizationCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderLight, RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Nothing organized yet",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Run AI Organization to automatically sort your screenshots into intelligent collections.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
