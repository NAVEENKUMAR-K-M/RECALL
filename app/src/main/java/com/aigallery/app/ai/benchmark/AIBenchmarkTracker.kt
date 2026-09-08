package com.aigallery.app.ai.benchmark

import com.aigallery.app.ai.HardwareBackend
import com.aigallery.app.ai.hardware.NpuExecutionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BenchmarkTelemetry(
    val coldStartLatencyMs: Long = 0,
    val warmInferenceLatencyMs: Long = 0,
    val lastInferenceLatencyMs: Long = 0,
    val averageInferenceLatencyMs: Long = 0,
    val p50LatencyMs: Long = 0,
    val totalInferenceCount: Int = 0,
    val activeBackend: HardwareBackend = HardwareBackend.LOCAL_CPU,
    val executionStatus: NpuExecutionStatus = NpuExecutionStatus.CPU_FALLBACK
)

object AIBenchmarkTracker {

    private val _telemetry = MutableStateFlow(BenchmarkTelemetry())
    val telemetry: StateFlow<BenchmarkTelemetry> = _telemetry.asStateFlow()

    private val latencyHistory = mutableListOf<Long>()

    @Synchronized
    fun recordInference(
        latencyMs: Long,
        backend: HardwareBackend,
        status: NpuExecutionStatus
    ) {
        val current = _telemetry.value
        val isFirst = current.totalInferenceCount == 0

        latencyHistory.add(latencyMs)
        if (latencyHistory.size > 100) {
            latencyHistory.removeAt(0)
        }

        val coldStart = if (isFirst) latencyMs else current.coldStartLatencyMs
        val warmLatency = if (!isFirst) latencyMs else current.warmInferenceLatencyMs

        val totalSum = latencyHistory.sum()
        val avg = if (latencyHistory.isNotEmpty()) totalSum / latencyHistory.size else latencyMs

        val sorted = latencyHistory.sorted()
        val p50 = if (sorted.isNotEmpty()) sorted[sorted.size / 2] else latencyMs

        _telemetry.value = BenchmarkTelemetry(
            coldStartLatencyMs = coldStart,
            warmInferenceLatencyMs = warmLatency,
            lastInferenceLatencyMs = latencyMs,
            averageInferenceLatencyMs = avg,
            p50LatencyMs = p50,
            totalInferenceCount = current.totalInferenceCount + 1,
            activeBackend = backend,
            executionStatus = status
        )
    }

    @Synchronized
    fun reset() {
        latencyHistory.clear()
        _telemetry.value = BenchmarkTelemetry()
    }
}
