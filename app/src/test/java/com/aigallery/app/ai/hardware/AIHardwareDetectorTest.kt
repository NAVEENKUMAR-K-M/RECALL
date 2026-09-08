package com.aigallery.app.ai.hardware

import com.aigallery.app.ai.HardwareBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIHardwareDetectorTest {

    @Test
    fun detectQualcommSnapdragon8Gen2_identifiesCorrectly() {
        val caps = AIHardwareDetector.detect(
            context = null,
            hardwareOverride = "qcom",
            socModelOverride = "SM8550"
        )

        assertTrue("Should detect Qualcomm chipset", caps.isQualcomm)
        assertTrue("DisplayName should reflect Snapdragon 8 Gen 2", caps.socName.contains("Snapdragon 8 Gen 2"))
        assertEquals("SM8550", caps.socModel)
        assertEquals("arm64-v8a", caps.abi)
        // Since no context / native libs packaged in unit test, should fallback safely to CPU
        assertEquals(HardwareBackend.LOCAL_CPU, caps.activeBackend)
        assertEquals(NpuExecutionStatus.CPU_FALLBACK, caps.executionStatus)
        assertNotNull("Fallback reason must be provided", caps.fallbackReason)
    }

    @Test
    fun detectQualcommSnapdragon8Gen3_identifiesCorrectly() {
        val caps = AIHardwareDetector.detect(
            context = null,
            hardwareOverride = "pineapple",
            socModelOverride = "SM8650"
        )

        assertTrue("Should detect Qualcomm chipset", caps.isQualcomm)
        assertTrue("DisplayName should reflect Snapdragon 8 Gen 3", caps.socName.contains("Snapdragon 8 Gen 3"))
        assertEquals("SM8650", caps.socModel)
    }

    @Test
    fun detectQualcommSnapdragon7sGen3_SM7635_identifiesCorrectly() {
        val caps = AIHardwareDetector.detect(
            context = null,
            hardwareOverride = "qcom",
            socModelOverride = "SM7635"
        )

        assertTrue("Should detect Qualcomm chipset", caps.isQualcomm)
        assertTrue("DisplayName should reflect Snapdragon 7s Gen 3", caps.socName.contains("Snapdragon 7s Gen 3"))
        assertEquals("SM7635", caps.socModel)
    }

    @Test
    fun detectMediaTekDimensity_identifiesAsNonQualcommAndCPUFallback() {
        val caps = AIHardwareDetector.detect(
            context = null,
            hardwareOverride = "mt6886",
            socModelOverride = "Dimensity 7200"
        )

        assertFalse("MediaTek must not be detected as Qualcomm", caps.isQualcomm)
        assertTrue("DisplayName should reflect MediaTek Dimensity", caps.socName.contains("MediaTek Dimensity"))
        assertEquals(HardwareBackend.LOCAL_CPU, caps.activeBackend)
        assertEquals(NpuExecutionStatus.UNSUPPORTED_SOC, caps.executionStatus)
        assertTrue(
            "Fallback reason should mention Non-Qualcomm SoC",
            caps.fallbackReason?.contains("Non-Qualcomm SoC") == true
        )
    }

    @Test
    fun detectExynos_identifiesAsNonQualcomm() {
        val caps = AIHardwareDetector.detect(
            context = null,
            hardwareOverride = "exynos2200",
            socModelOverride = "s5e9925"
        )

        assertFalse("Exynos must not be detected as Qualcomm", caps.isQualcomm)
        assertEquals(HardwareBackend.LOCAL_CPU, caps.activeBackend)
        assertEquals(NpuExecutionStatus.UNSUPPORTED_SOC, caps.executionStatus)
    }

    @Test
    fun detectGoogleTensor_identifiesAsNonQualcomm() {
        val caps = AIHardwareDetector.detect(
            context = null,
            hardwareOverride = "tensor",
            socModelOverride = "GS201"
        )

        assertFalse("Tensor must not be detected as Qualcomm", caps.isQualcomm)
        assertEquals(HardwareBackend.LOCAL_CPU, caps.activeBackend)
        assertEquals(NpuExecutionStatus.UNSUPPORTED_SOC, caps.executionStatus)
    }
}
