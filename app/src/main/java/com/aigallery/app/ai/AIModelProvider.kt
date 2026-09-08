package com.aigallery.app.ai

import android.content.Context
import com.aigallery.app.ai.hardware.AIHardwareCapabilities
import com.aigallery.app.ai.hardware.AIHardwareDetector
import com.aigallery.app.ai.llm.LocalOnDeviceLLM
import com.aigallery.app.ai.llm.OnDeviceLLM
import com.aigallery.app.ai.qualcomm.QualcommLLMProvider

enum class HardwareBackend(val displayName: String) {
    LOCAL_CPU("Local CPU"),
    LOCAL_GPU("Local GPU"),
    QUALCOMM_HEXAGON_NPU("Qualcomm Hexagon NPU")
}

interface AIModelProvider {
    fun backendName(): String
    fun getHardwareBackend(): HardwareBackend
    fun isAvailable(): Boolean
}

/**
 * Standard on-device CPU execution provider.
 * Fully offline, private, and deterministic.
 */
class LocalCPUModelProvider : AIModelProvider {
    override fun backendName(): String = "Local On-Device CPU"
    override fun getHardwareBackend(): HardwareBackend = HardwareBackend.LOCAL_CPU
    override fun isAvailable(): Boolean = true
}

/**
 * Qualcomm Hexagon NPU provider abstraction for Snapdragon AI Engine / QNN.
 */
class QualcommNPUModelProvider(
    private val isNpuActive: Boolean = false
) : AIModelProvider {
    override fun backendName(): String = if (isNpuActive) "Qualcomm Hexagon NPU (HTP Active)" else "Qualcomm Hexagon NPU (CPU Fallback)"
    override fun getHardwareBackend(): HardwareBackend = if (isNpuActive) HardwareBackend.QUALCOMM_HEXAGON_NPU else HardwareBackend.LOCAL_CPU
    override fun isAvailable(): Boolean = isNpuActive
}

/**
 * Unified Model Registry for dynamically resolving the best available AI provider.
 */
object AIModelRegistry {

    @Volatile
    private var cachedCapabilities: AIHardwareCapabilities? = null

    fun getCapabilities(context: Context): AIHardwareCapabilities {
        return cachedCapabilities ?: synchronized(this) {
            cachedCapabilities ?: AIHardwareDetector.detect(context).also {
                cachedCapabilities = it
            }
        }
    }

    fun getLLMProvider(context: Context): OnDeviceLLM {
        val caps = getCapabilities(context)
        return if (caps.isQualcomm) {
            QualcommLLMProvider(context)
        } else {
            LocalOnDeviceLLM(context)
        }
    }

    fun getActiveBackend(context: Context): HardwareBackend {
        return getCapabilities(context).activeBackend
    }
}


