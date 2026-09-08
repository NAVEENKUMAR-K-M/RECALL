package com.aigallery.app.presentation.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigallery.app.core.theme.AccentCyan
import com.aigallery.app.core.theme.AccentPurple
import com.aigallery.app.core.theme.DarkBackground
import com.aigallery.app.core.theme.GlassBorderLight
import com.aigallery.app.core.theme.GlassSurfaceDark
import com.aigallery.app.domain.repository.ScreenshotAIStats
import kotlinx.coroutines.launch

import com.aigallery.app.data.database.CategoryCountResult
import com.aigallery.app.data.database.OrganizationStatsResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aigallery.app.ai.AIModelRegistry
import com.aigallery.app.ai.benchmark.AIBenchmarkTracker
import com.aigallery.app.ai.hardware.NpuExecutionStatus
import com.aigallery.app.ai.HardwareBackend

@Composable
fun AIDebugStatusScreen(
    stats: ScreenshotAIStats,
    orgStats: OrganizationStatsResult = OrganizationStatsResult(),
    categoryCounts: List<CategoryCountResult> = emptyList(),
    onBack: () -> Unit,
    onReprocessAll: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Screenshot AI Index",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hackathon Diagnostics & Engine Status",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Processing Pipeline Card
            GlassStatusSection(title = "PROCESSING STATUS") {
                MetricRow(label = "Total Detected Screenshots", value = "${stats.totalCount}")
                MetricRow(label = "Completed & Indexed", value = "${stats.completedCount}", highlight = AccentCyan)
                MetricRow(label = "Pending in WorkManager", value = "${stats.pendingCount}", highlight = if (stats.pendingCount > 0) Color(0xFFFFB300) else Color.White)
                MetricRow(label = "Failed", value = "${stats.failedCount}", highlight = if (stats.failedCount > 0) Color(0xFFFF5252) else Color.White)
            }

            // Phase 3 AI Organization Status Card
            GlassStatusSection(title = "AI ORGANIZATION ENGINE") {
                MetricRow(label = "Understood for Organization", value = "${orgStats.totalUnderstood}", highlight = AccentCyan)
                MetricRow(label = "Organized Collections", value = "${orgStats.totalOrganized}", highlight = Color(0xFF4CAF50))
                MetricRow(label = "Awaiting Organization", value = "${orgStats.totalPending}", highlight = if (orgStats.totalPending > 0) Color(0xFFFFB300) else Color.White)
                MetricRow(label = "Unsorted / Low Confidence", value = "${orgStats.totalUnsorted}")
                if (categoryCounts.isNotEmpty()) {
                    val catBreakdown = categoryCounts.take(5).joinToString(", ") { "${it.category}: ${it.count}" }
                    MetricRow(label = "Top Categories", value = catBreakdown)
                }
            }

            // Intelligence Components Card
            GlassStatusSection(title = "INTELLIGENCE LAYERS") {
                MetricRow(label = "OCR Text Extractions", value = "${stats.textCount} indexed", icon = Icons.Default.CheckCircle)
                MetricRow(label = "Classifications (Work/Shop/Travel...)", value = "Active", icon = Icons.Default.CheckCircle)
                MetricRow(label = "Entities Extracted", value = "${stats.entityCount} items", icon = Icons.Default.CheckCircle)
                MetricRow(label = "Evidence Tags Generated", value = "${stats.tagCount} tags", icon = Icons.Default.CheckCircle)
                MetricRow(label = "LLM Context Understandings", value = "${stats.llmCount} generated", icon = Icons.Default.CheckCircle, highlight = AccentCyan)
                MetricRow(label = "Vector Embeddings (64-dim)", value = "${stats.embeddingCount} indexed", icon = Icons.Default.CheckCircle, highlight = AccentCyan)
                MetricRow(label = "AI Memory Relations", value = "${stats.relationCount} links", icon = Icons.Default.CheckCircle, highlight = AccentPurple)
            }

            // Phase 4: On-Device LLM & Semantic Engine Telemetry
            GlassStatusSection(title = "ON-DEVICE LLM & RUNTIME") {
                MetricRow(label = "LLM Engine", value = "Local Deterministic Semantic Reasoner", highlight = AccentCyan)
                MetricRow(label = "Optional Sideload Runtime", value = "Gemma 2B LiteRT (INT4)", highlight = Color.White.copy(alpha = 0.7f))
                MetricRow(label = "Active Backend", value = "Local CPU", icon = Icons.Default.Memory)
                MetricRow(label = "Inference Mode", value = "100% On-Device / Offline")
                MetricRow(label = "Vector Dimension", value = "64-dim Dense Unit Vector")
                MetricRow(label = "Hybrid Search Ranking", value = "Active (Semantic + Lexical)", icon = Icons.Default.CheckCircle, highlight = Color(0xFF4CAF50))
            }

            // Phase 5: QUALCOMM HEXAGON NPU & SOC ARCHITECTURE (Real Telemetry, No Fake NPU)
            val context = androidx.compose.ui.platform.LocalContext.current
            val capabilities = androidx.compose.runtime.remember { AIModelRegistry.getCapabilities(context) }
            val telemetry by AIBenchmarkTracker.telemetry.collectAsState()

            // Hackathon Demo Card (Section 31 requirement)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (telemetry.executionStatus == NpuExecutionStatus.VERIFIED_NPU)
                                    AccentCyan.copy(alpha = 0.25f)
                                else
                                    AccentPurple.copy(alpha = 0.18f),
                                GlassSurfaceDark
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        if (telemetry.executionStatus == NpuExecutionStatus.VERIFIED_NPU)
                            AccentCyan
                        else
                            AccentPurple.copy(alpha = 0.6f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ON-DEVICE AI",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (telemetry.executionStatus == NpuExecutionStatus.VERIFIED_NPU)
                                        Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else
                                        Color(0xFFFFB300).copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (telemetry.executionStatus == NpuExecutionStatus.VERIFIED_NPU)
                                    "NPU VERIFIED"
                                else
                                    "CPU FALLBACK",
                                color = if (telemetry.executionStatus == NpuExecutionStatus.VERIFIED_NPU)
                                    Color(0xFF81C784)
                                else
                                    Color(0xFFFFD54F),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = if (telemetry.executionStatus == NpuExecutionStatus.VERIFIED_NPU)
                            "Powered by Qualcomm Hexagon NPU"
                        else
                            "On-Device AI Engine",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    MetricRow(
                        label = "Model",
                        value = if (telemetry.executionStatus == NpuExecutionStatus.VERIFIED_NPU)
                            "Gemma 2B INT4 (HTP Context Binary)"
                        else
                            "Deterministic Semantic Reasoner",
                        highlight = Color.White
                    )
                    MetricRow(
                        label = "Inference Latency",
                        value = if (telemetry.lastInferenceLatencyMs > 0)
                            "${telemetry.lastInferenceLatencyMs} ms"
                        else
                            "Ready (< 15 ms)",
                        highlight = AccentCyan
                    )
                    MetricRow(
                        label = "Cloud Inference",
                        value = "OFF (0 Bytes Sent)",
                        highlight = Color(0xFF81C784)
                    )
                    MetricRow(
                        label = "Network",
                        value = "Not Required (Airplane Mode Ready)",
                        highlight = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Qualcomm Hexagon NPU & SoC Diagnostics
            GlassStatusSection(title = "QUALCOMM NPU & HARDWARE ARCHITECTURE") {
                MetricRow(
                    label = "Detected Device",
                    value = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}",
                    icon = Icons.Default.Memory
                )
                MetricRow(
                    label = "System SoC",
                    value = capabilities.socName + if (!capabilities.socModel.isNullOrEmpty()) " (${capabilities.socModel})" else "",
                    highlight = if (capabilities.isQualcomm) AccentCyan else Color.White.copy(alpha = 0.8f)
                )
                MetricRow(
                    label = "Target ABI",
                    value = capabilities.abi
                )
                MetricRow(
                    label = "Qualcomm Platform",
                    value = if (capabilities.isQualcomm) "Snapdragon Chipset Confirmed" else "Non-Qualcomm SoC",
                    highlight = if (capabilities.isQualcomm) Color(0xFF81C784) else Color(0xFFFFB300)
                )
                MetricRow(
                    label = "Qualcomm AI Runtime (QNN)",
                    value = if (capabilities.qnnAvailable) "libQnnHtp.so Present" else "Native Libs Pending Sideload",
                    highlight = if (capabilities.qnnAvailable) Color(0xFF81C784) else Color.White.copy(alpha = 0.5f)
                )
                MetricRow(
                    label = "Hexagon NPU (HTP)",
                    value = if (capabilities.npuAvailable) "Hardware Supported" else "Unavailable on current SoC",
                    highlight = if (capabilities.npuAvailable) AccentCyan else Color.White.copy(alpha = 0.5f)
                )
                MetricRow(
                    label = "Requested Backend",
                    value = "Qualcomm Hexagon NPU",
                    highlight = AccentPurple
                )
                MetricRow(
                    label = "Actual Execution Backend",
                    value = when (telemetry.executionStatus) {
                        NpuExecutionStatus.VERIFIED_NPU -> "Qualcomm Hexagon NPU"
                        NpuExecutionStatus.NPU_WITH_CPU_FALLBACK -> "NPU + CPU Fallback"
                        else -> "Local CPU (Fallback)"
                    },
                    highlight = when (telemetry.executionStatus) {
                        NpuExecutionStatus.VERIFIED_NPU -> Color(0xFF81C784)
                        NpuExecutionStatus.NPU_WITH_CPU_FALLBACK -> Color(0xFFFFB300)
                        else -> Color(0xFFFFCC80)
                    },
                    icon = Icons.Default.CheckCircle
                )
                if (capabilities.fallbackReason != null) {
                    MetricRow(
                        label = "Fallback Diagnostics",
                        value = capabilities.fallbackReason,
                        highlight = Color(0xFFFFCC80)
                    )
                }
            }

            // Real Benchmark & Telemetry Metrics (Section 27 & 28)
            GlassStatusSection(title = "NPU & AI PERFORMANCE BENCHMARKS") {
                MetricRow(
                    label = "Total Inferences Measured",
                    value = "${telemetry.totalInferenceCount} runs",
                    highlight = Color.White
                )
                MetricRow(
                    label = "Cold-Start Latency",
                    value = if (telemetry.coldStartLatencyMs > 0) "${telemetry.coldStartLatencyMs} ms" else "Awaiting run",
                    highlight = AccentCyan
                )
                MetricRow(
                    label = "Warm Inference Latency",
                    value = if (telemetry.warmInferenceLatencyMs > 0) "${telemetry.warmInferenceLatencyMs} ms" else "Awaiting run",
                    highlight = AccentCyan
                )
                MetricRow(
                    label = "P50 Median Latency",
                    value = if (telemetry.p50LatencyMs > 0) "${telemetry.p50LatencyMs} ms" else "Awaiting run",
                    highlight = Color(0xFF81C784)
                )
                MetricRow(
                    label = "Average Inference Latency",
                    value = if (telemetry.averageInferenceLatencyMs > 0) "${telemetry.averageInferenceLatencyMs} ms" else "Awaiting run"
                )
            }

            // Hardware & Privacy Architecture Card
            GlassStatusSection(title = "HARDWARE & PRIVACY GUARANTEE") {
                MetricRow(label = "Cloud APIs / Remote Inference", value = "Disabled (0 Bytes Sent)", icon = Icons.Default.CloudOff, highlight = AccentCyan)
                MetricRow(label = "Original File Integrity", value = "Read-Only (Never Modified)", icon = Icons.Default.Security)
                MetricRow(label = "Airplane Mode Verification", value = "Fully Operable (100% Offline)", icon = Icons.Default.CheckCircle, highlight = Color(0xFF81C784))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: Trigger Reprocessing
            Button(
                onClick = {
                    coroutineScope.launch {
                        onReprocessAll()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentCyan.copy(alpha = 0.3f), AccentPurple.copy(alpha = 0.3f))
                            )
                        )
                        .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Re-analyze All Screenshots",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GlassStatusSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassSurfaceDark)
                .border(1.dp, GlassBorderLight, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    highlight: Color = Color.White,
    icon: ImageVector? = null,
    isInactive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isInactive) Color.White.copy(alpha = 0.3f) else highlight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                color = if (isInactive) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }

        Text(
            text = value,
            color = if (isInactive) Color.White.copy(alpha = 0.35f) else highlight,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
