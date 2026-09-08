package com.aigallery.app.ai.qualcomm

import com.aigallery.app.ai.HardwareBackend
import com.aigallery.app.ai.benchmark.AIBenchmarkTracker
import com.aigallery.app.ai.hardware.AIHardwareCapabilities
import com.aigallery.app.ai.hardware.NpuExecutionStatus
import com.aigallery.app.ai.llm.LLMContext
import com.aigallery.app.ai.llm.LLMResult
import java.io.File
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QualcommLLMProviderTest {

    @Before
    fun setUp() {
        AIBenchmarkTracker.reset()
    }

    @Test
    fun onNonQualcommDevice_strictlyUsesCpuFallbackAndNeverClaimsNpu() = runTest {
        val nonQualcommCaps = AIHardwareCapabilities(
            isQualcomm = false,
            socName = "MediaTek Dimensity 7200",
            socModel = "MT6886",
            hardwareName = "mt6886",
            abi = "arm64-v8a",
            qnnAvailable = false,
            npuAvailable = false,
            supportedLLM = true,
            activeBackend = HardwareBackend.LOCAL_CPU,
            executionStatus = NpuExecutionStatus.UNSUPPORTED_SOC,
            fallbackReason = "Non-Qualcomm SoC (MediaTek Dimensity 7200). Routing to Local CPU."
        )

        val provider = QualcommLLMProvider(
            context = null,
            capabilitiesOverride = nonQualcommCaps
        )

        val loadResult = provider.loadLLM()
        assertTrue("Loading should succeed in fallback mode", loadResult.isSuccess)
        assertEquals(HardwareBackend.LOCAL_CPU, provider.actualBackend)
        assertEquals(NpuExecutionStatus.UNSUPPORTED_SOC, provider.executionStatus)
        assertTrue(provider.getBackendName().contains("Local CPU"))
        assertFalse(provider.getBackendName().contains("HTP Active"))

        // Run inference
        val context = LLMContext(
            screenshotId = 101L,
            ocrText = "Flight AI 202 to Mumbai confirmed on 15 Oct. Terminal 2 Gate 4.",
            entities = listOf(
                com.aigallery.app.data.database.ScreenshotEntityItem(screenshotId = 101L, type = "FLIGHT", value = "AI 202"),
                com.aigallery.app.data.database.ScreenshotEntityItem(screenshotId = 101L, type = "LOCATION", value = "Mumbai")
            ),
            category = "TRAVEL"
        )

        val result = provider.generate("Analyze this screenshot", context)
        assertTrue("Inference must succeed via semantic fallback", result is LLMResult.Success)
        val success = result as LLMResult.Success
        assertNotNull(success.understanding.title)
        assertTrue(success.backend.contains("Local CPU"))

        // Telemetry must record CPU fallback honestly
        val telemetry = AIBenchmarkTracker.telemetry.value
        assertEquals(HardwareBackend.LOCAL_CPU, telemetry.activeBackend)
        assertEquals(NpuExecutionStatus.UNSUPPORTED_SOC, telemetry.executionStatus)
        assertEquals(1, telemetry.totalInferenceCount)
    }

    @Test
    fun onQualcommDeviceWithoutQnnLibs_reportsCpuFallbackAccurately() = runTest {
        val qualcommWithoutLibs = AIHardwareCapabilities(
            isQualcomm = true,
            socName = "Snapdragon 8 Gen 2 (SM8550)",
            socModel = "SM8550",
            hardwareName = "kalama",
            abi = "arm64-v8a",
            qnnAvailable = false,
            npuAvailable = true,
            supportedLLM = true,
            activeBackend = HardwareBackend.LOCAL_CPU,
            executionStatus = NpuExecutionStatus.CPU_FALLBACK,
            fallbackReason = "QNN native libraries not yet packaged."
        )

        val provider = QualcommLLMProvider(
            context = null,
            capabilitiesOverride = qualcommWithoutLibs
        )

        val loadResult = provider.loadLLM()
        assertTrue(loadResult.isSuccess)
        assertEquals(HardwareBackend.LOCAL_CPU, provider.actualBackend)
        assertEquals(NpuExecutionStatus.CPU_FALLBACK, provider.executionStatus)
        assertFalse("Cannot claim NPU when QNN libs are missing", provider.getBackendName().contains("HTP Active"))
    }

    @Test
    fun onVerifiedQualcommDeviceWithContextBinary_activatesNPU() = runTest {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "qnn_test_" + UUID.randomUUID())
        val modelsDir = File(tempDir, "models/qualcomm")
        modelsDir.mkdirs()
        val binaryFile = File(modelsDir, "gemma_2b_sm8550_htp.bin")
        binaryFile.writeBytes(ByteArray(1024) { 0x42 })

        try {
            val verifiedCaps = AIHardwareCapabilities(
                isQualcomm = true,
                socName = "Snapdragon 8 Gen 2 (SM8550)",
                socModel = "SM8550",
                hardwareName = "kalama",
                abi = "arm64-v8a",
                qnnAvailable = true,
                npuAvailable = true,
                supportedLLM = true,
                activeBackend = HardwareBackend.QUALCOMM_HEXAGON_NPU,
                executionStatus = NpuExecutionStatus.VERIFIED_NPU,
                fallbackReason = null
            )

            val provider = QualcommLLMProvider(
                context = null,
                capabilitiesOverride = verifiedCaps,
                baseFilesDir = tempDir
            )

            val loadResult = provider.loadLLM()
            assertTrue(loadResult.isSuccess)
            assertEquals(HardwareBackend.QUALCOMM_HEXAGON_NPU, provider.actualBackend)
            assertEquals(NpuExecutionStatus.VERIFIED_NPU, provider.executionStatus)
            assertTrue(provider.getBackendName().contains("Qualcomm Hexagon NPU"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
