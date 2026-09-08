package com.aigallery.app.ai.qualcomm

enum class QNNBackendType(val libraryName: String, val displayName: String) {
    HTP("libQnnHtp.so", "Qualcomm Hexagon Tensor Processor (NPU)"),
    GPU("libQnnGpu.so", "Qualcomm Adreno GPU"),
    CPU("libQnnCpu.so", "Qualcomm Kryo CPU")
}

enum class QNNPerformanceProfile {
    BURST,
    SUSTAINED_HIGH_PERFORMANCE,
    BALANCED,
    POWER_SAVER
}

enum class QNNQuantization(val displayName: String, val bitsPerWeight: Int) {
    INT4("W4A16 / INT4 Quantized", 4),
    INT8("W8A16 / INT8 Quantized", 8),
    FP16("FP16 Half-Precision", 16)
}

data class QNNModelProfile(
    val modelName: String = "Gemma-2B-QNN-HTP",
    val modelVersion: String = "qnn_v2.22.0",
    val parameterCount: String = "2.0 Billion",
    val quantization: QNNQuantization = QNNQuantization.INT4,
    val targetSoc: String = "Snapdragon 8 Gen 2 (SM8550)",
    val qnnSdkVersion: String = "2.22.0.240428",
    val contextBinaryFilename: String = "gemma_2b_sm8550_htp.bin",
    val performanceProfile: QNNPerformanceProfile = QNNPerformanceProfile.SUSTAINED_HIGH_PERFORMANCE
)
