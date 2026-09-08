package com.aigallery.app.ai.hardware

import android.content.Context
import android.os.Build
import com.aigallery.app.ai.HardwareBackend
import java.io.File
import java.util.Locale

object AIHardwareDetector {

    fun detect(
        context: Context? = null,
        hardwareOverride: String? = null,
        socModelOverride: String? = null,
        abiOverride: String? = null,
        systemPropProvider: ((String) -> String)? = null
    ): AIHardwareCapabilities {
        val hardware = (hardwareOverride ?: runCatching { Build.HARDWARE }.getOrNull()).orEmpty()
        val socModel = (socModelOverride ?: getSocModel(systemPropProvider)).orEmpty()
        val abi = abiOverride ?: runCatching { Build.SUPPORTED_ABIS?.firstOrNull() }.getOrNull() ?: "arm64-v8a"

        val combined = "$hardware $socModel".lowercase(Locale.ROOT)

        val isQualcomm = isQualcommChipset(combined)
        val socName = determineSocDisplayName(hardware, socModel, isQualcomm)

        val qnnAvailable = isQnnLibraryPresent(context)
        val npuAvailable = isQualcomm && (isSnapdragonNpuCompatible(combined) || qnnAvailable)

        val activeBackend: HardwareBackend
        val executionStatus: NpuExecutionStatus
        val fallbackReason: String?

        when {
            !isQualcomm -> {
                activeBackend = HardwareBackend.LOCAL_CPU
                executionStatus = NpuExecutionStatus.UNSUPPORTED_SOC
                fallbackReason = "Non-Qualcomm SoC ($socName) detected. Running on Local CPU."
            }
            !qnnAvailable -> {
                activeBackend = HardwareBackend.LOCAL_CPU
                executionStatus = NpuExecutionStatus.CPU_FALLBACK
                fallbackReason = "Snapdragon device ($socName) detected, but QNN HTP runtime libraries (libQnnHtp.so) are not yet packaged."
            }
            !npuAvailable -> {
                activeBackend = HardwareBackend.LOCAL_CPU
                executionStatus = NpuExecutionStatus.CPU_FALLBACK
                fallbackReason = "Snapdragon chipset ($socName) does not support required Hexagon HTP architecture."
            }
            else -> {
                activeBackend = HardwareBackend.QUALCOMM_HEXAGON_NPU
                executionStatus = NpuExecutionStatus.VERIFIED_NPU
                fallbackReason = null
            }
        }

        return AIHardwareCapabilities(
            isQualcomm = isQualcomm,
            socName = socName,
            socModel = socModel.ifBlank { null },
            hardwareName = hardware,
            abi = abi,
            qnnAvailable = qnnAvailable,
            npuAvailable = npuAvailable,
            supportedLLM = true,
            activeBackend = activeBackend,
            executionStatus = executionStatus,
            fallbackReason = fallbackReason
        )
    }

    private fun isQualcommChipset(combined: String): Boolean {
        return combined.contains("qcom") ||
                combined.contains("qualcomm") ||
                combined.contains("snapdragon") ||
                combined.contains("sm8") ||
                combined.contains("sm7") ||
                combined.contains("sm6") ||
                combined.contains("lahaina") || // SM8350
                combined.contains("taro") ||    // SM8450
                combined.contains("kalama") ||  // SM8550 (8 Gen 2)
                combined.contains("pineapple") || // SM8650 (8 Gen 3)
                combined.contains("volcano")    // SM7635 (7s Gen 3)
    }

    private fun isSnapdragonNpuCompatible(combined: String): Boolean {
        return combined.contains("sm8") ||
                combined.contains("sm7") ||
                combined.contains("kalama") ||
                combined.contains("pineapple") ||
                combined.contains("volcano") ||
                combined.contains("taro") ||
                combined.contains("lahaina") ||
                combined.contains("8 gen") ||
                combined.contains("7 gen")
    }

    private fun determineSocDisplayName(hardware: String, socModel: String, isQualcomm: Boolean): String {
        val hw = hardware.lowercase(Locale.ROOT)
        val soc = socModel.lowercase(Locale.ROOT)

        return when {
            soc.contains("sm8650") || hw.contains("pineapple") -> "Snapdragon 8 Gen 3 (SM8650)"
            soc.contains("sm8550") || hw.contains("kalama") -> "Snapdragon 8 Gen 2 (SM8550)"
            soc.contains("sm8475") -> "Snapdragon 8+ Gen 1 (SM8475)"
            soc.contains("sm8450") || hw.contains("taro") -> "Snapdragon 8 Gen 1 (SM8450)"
            soc.contains("sm7635") || hw.contains("volcano") -> "Snapdragon 7s Gen 3 (SM7635)"
            soc.contains("sm7550") || soc.contains("7 gen 3") -> "Snapdragon 7 Gen 3 (SM7550)"
            soc.contains("sm7475") || soc.contains("7+ gen 2") -> "Snapdragon 7+ Gen 2 (SM7475)"
            isQualcomm && socModel.isNotBlank() -> "Qualcomm Snapdragon ($socModel)"
            isQualcomm -> "Qualcomm Snapdragon ($hardware)"
            hw.contains("mt") || hw.contains("k68") || soc.contains("dimensity") || soc.contains("mt") -> {
                if (socModel.isNotBlank()) "MediaTek Dimensity ($socModel)" else "MediaTek Dimensity ($hardware)"
            }
            hw.contains("exynos") || soc.contains("exynos") -> "Samsung Exynos"
            hw.contains("tensor") || soc.contains("tensor") -> "Google Tensor"
            else -> if (socModel.isNotBlank()) socModel else hardware.ifBlank { "Generic ARM64" }
        }
    }

    private fun getSocModel(customProvider: ((String) -> String)?): String {
        if (customProvider != null) {
            val fromCustom = customProvider("ro.soc.model")
            if (fromCustom.isNotBlank()) return fromCustom
        }

        // Try Build.SOC_MODEL on Android 12+ (API 31+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val model = Build.SOC_MODEL
                if (!model.isNullOrBlank()) return model
            }
        } catch (_: Throwable) {}

        // Try system property reflection
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)

            for (prop in listOf("ro.soc.model", "ro.board.platform", "ro.hardware.chipname")) {
                val value = getMethod.invoke(null, prop) as? String
                if (!value.isNullOrBlank()) return value
            }
        } catch (_: Throwable) {}

        return ""
    }

    private fun isQnnLibraryPresent(context: Context?): Boolean {
        if (context == null) return false

        try {
            val nativeDir = File(context.applicationInfo.nativeLibraryDir)
            if (nativeDir.exists() && nativeDir.isDirectory) {
                val qnnHtp = File(nativeDir, "libQnnHtp.so")
                val qnnCpu = File(nativeDir, "libQnnCpu.so")
                if (qnnHtp.exists() || qnnCpu.exists()) return true
            }
        } catch (_: Throwable) {}

        // Check if loadable in system vendor paths
        return try {
            System.loadLibrary("QnnHtp")
            true
        } catch (_: Throwable) {
            false
        }
    }
}
