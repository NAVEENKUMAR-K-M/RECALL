package com.aigallery.app.ai.benchmark

import com.aigallery.app.ai.HardwareBackend
import com.aigallery.app.ai.hardware.NpuExecutionStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AIBenchmarkTrackerTest {

    @Before
    fun setUp() {
        AIBenchmarkTracker.reset()
    }

    @Test
    fun recordInference_tracksColdStartAndWarmLatency() {
        // First run (Cold Start)
        AIBenchmarkTracker.recordInference(
            latencyMs = 120L,
            backend = HardwareBackend.LOCAL_CPU,
            status = NpuExecutionStatus.CPU_FALLBACK
        )

        var telemetry = AIBenchmarkTracker.telemetry.value
        assertEquals(1, telemetry.totalInferenceCount)
        assertEquals(120L, telemetry.coldStartLatencyMs)
        assertEquals(0L, telemetry.warmInferenceLatencyMs)
        assertEquals(120L, telemetry.lastInferenceLatencyMs)

        // Second run (Warm)
        AIBenchmarkTracker.recordInference(
            latencyMs = 14L,
            backend = HardwareBackend.QUALCOMM_HEXAGON_NPU,
            status = NpuExecutionStatus.VERIFIED_NPU
        )

        telemetry = AIBenchmarkTracker.telemetry.value
        assertEquals(2, telemetry.totalInferenceCount)
        assertEquals(120L, telemetry.coldStartLatencyMs) // Cold start preserved
        assertEquals(14L, telemetry.warmInferenceLatencyMs)
        assertEquals(14L, telemetry.lastInferenceLatencyMs)
        assertEquals(HardwareBackend.QUALCOMM_HEXAGON_NPU, telemetry.activeBackend)
        assertEquals(NpuExecutionStatus.VERIFIED_NPU, telemetry.executionStatus)
    }

    @Test
    fun recordInference_calculatesP50MedianCorrectly() {
        // Feed 5 latencies: 10, 20, 30, 40, 50
        val samples = listOf(50L, 10L, 30L, 20L, 40L)
        for (sample in samples) {
            AIBenchmarkTracker.recordInference(
                latencyMs = sample,
                backend = HardwareBackend.LOCAL_CPU,
                status = NpuExecutionStatus.CPU_FALLBACK
            )
        }

        val telemetry = AIBenchmarkTracker.telemetry.value
        assertEquals(5, telemetry.totalInferenceCount)
        assertEquals(30L, telemetry.p50LatencyMs)
        assertEquals(30L, telemetry.averageInferenceLatencyMs)
    }

    @Test
    fun reset_resetsAllMetrics() {
        AIBenchmarkTracker.recordInference(
            latencyMs = 85L,
            backend = HardwareBackend.LOCAL_CPU,
            status = NpuExecutionStatus.CPU_FALLBACK
        )
        AIBenchmarkTracker.reset()

        val telemetry = AIBenchmarkTracker.telemetry.value
        assertEquals(0, telemetry.totalInferenceCount)
        assertEquals(0L, telemetry.coldStartLatencyMs)
        assertEquals(0L, telemetry.lastInferenceLatencyMs)
    }
}
