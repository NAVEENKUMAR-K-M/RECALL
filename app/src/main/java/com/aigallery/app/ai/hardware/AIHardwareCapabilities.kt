package com.aigallery.app.ai.hardware

import com.aigallery.app.ai.HardwareBackend

enum class NpuExecutionStatus(val displayName: String) {
    VERIFIED_NPU("Hexagon NPU (HTP Active)"),
    NPU_WITH_CPU_FALLBACK("NPU + CPU Fallback"),
    CPU_FALLBACK("Local CPU Fallback"),
    UNSUPPORTED_SOC("Non-Qualcomm SoC"),
    MODEL_MISSING("QNN Context Binary Missing")
}

data class AIHardwareCapabilities(
    val isQualcomm: Boolean,
    val socName: String,
    val socModel: String?,
    val hardwareName: String,
    val abi: String,
    val qnnAvailable: Boolean,
    val npuAvailable: Boolean,
    val supportedLLM: Boolean,
    val activeBackend: HardwareBackend,
    val executionStatus: NpuExecutionStatus,
    val fallbackReason: String? = null
)
