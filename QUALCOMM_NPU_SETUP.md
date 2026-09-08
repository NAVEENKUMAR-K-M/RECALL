# QUALCOMM HEXAGON NPU SETUP & DEPLOYMENT GUIDE
## AI Gallery — Phase 5: Hardware Acceleration Architecture

This document defines the complete, official workflow for deploying, compiling, and running neural network inference on the **Qualcomm Hexagon NPU** via **Qualcomm AI Engine Direct (QNN)** and the **Hexagon Tensor Processor (HTP)** backend for the **AI Gallery** Android application.

---

## 1. Hardware Architecture Overview

```
                      AI Gallery (Android 14+)
                                 │
                                 ↓
                         AIModelRegistry
                                 │
            ┌────────────────────┴────────────────────┐
            ↓                                         ↓
   Non-Qualcomm SoC / Dev                       Qualcomm Snapdragon
   (e.g., Nothing Phone 3a)                   (e.g., iQOO 11/12 - SM8550/SM8650)
            │                                         │
            ↓                                         ↓
   LocalOnDeviceLLM / CPU Fallback           QualcommLLMProvider
   • Deterministic Semantic Reasoner                  │
   • Real Telemetry: CPU Fallback                     ↓
                                             Qualcomm AI Engine Direct (QNN)
                                             libQnnHtp.so / libQnnSystem.so
                                                      │
                                                      ↓
                                              Hexagon HTP Backend
                                             (v73/v75 NPU Cores)
                                                      │
                                                      ↓
                                             100% Offline / Real NPU
```

### Strict "No Fake NPU" Policy
The application enforces strict runtime integrity:
1. **Never mock NPU execution**: If the device runs a non-Qualcomm chipset (e.g., MediaTek Dimensity 7200 on Nothing Phone 3a), the app detects this via `AIHardwareDetector` and routes inference cleanly to `LOCAL_CPU`.
2. **Honest telemetry**: The UI reports `Requested: NPU`, `Actual: Local CPU Fallback`, and states the exact fallback reason (`"Non-Qualcomm SoC"`, `"libQnnHtp.so not packaged"`, or `"Context binary missing"`).
3. **Verified badge**: The UI card `"Powered by Qualcomm Hexagon NPU"` only displays when `isNpuVerified == true` and execution confirms HTP execution.

---

## 2. Target Device & Qualcomm SoC Specifications

| Parameter | Target Production Device | Development / Fallback Device |
| :--- | :--- | :--- |
| **Device Model** | **iQOO 11 / iQOO 12 / Neo 9 Pro** | **Nothing Phone 3a** |
| **Processor SoC** | **Qualcomm Snapdragon 8 Gen 2 / 8 Gen 3** | MediaTek Dimensity 7200 Pro |
| **SoC Model ID** | **SM8550 / SM8650** | MT6886 |
| **NPU Architecture** | **Qualcomm Hexagon HTP (v73 / v75)** | MediaTek APU 650 |
| **Target ABI** | `arm64-v8a` | `arm64-v8a` |
| **Android OS** | Android 14 / Android 15 (API 34/35) | Android 14 (API 34) |
| **AI Runtime** | **Qualcomm AI Engine Direct (QNN v2.20+)** | Local On-Device CPU Fallback |

---

## 3. Toolchain & SDK Requirements

To compile and package Qualcomm QNN models, the developer workstation requires:

1. **Qualcomm AI Engine Direct SDK (QNN)**: Version `2.20.0` or later
   - Download from the official [Qualcomm Developer Network (QDN)](https://developer.qualcomm.com/software/qualcomm-ai-engine-direct-sdk).
2. **Hexagon SDK**: Hexagon SDK 5.x (for HTP architecture targets `v73` and `v75`).
3. **Android NDK**: Version `r26b` or `r27`.
4. **Python**: Python 3.10+ with `onnx`, `onnxruntime`, `torch`, and `qnn` tools.

### Environment Variables
Configure your workstation environment (`~/.bashrc` or Windows System Variables):

```bash
# Qualcomm SDK Roots
export QNN_SDK_ROOT="/opt/qcom/qnn-2.20.0"
export HEXAGON_SDK_ROOT="/opt/qcom/Hexagon_SDK/5.4.0"
export ANDROID_NDK_ROOT="/path/to/android-sdk/ndk/26.1.10909125"

# Path additions
export PATH="$QNN_SDK_ROOT/bin/x86_64-linux-clang:$PATH"
export LD_LIBRARY_PATH="$QNN_SDK_ROOT/lib/x86_64-linux-clang:$LD_LIBRARY_PATH"
export PYTHONPATH="$QNN_SDK_ROOT/lib/python:$PYTHONPATH"
```

---

## 4. Model Selection & Quantization Strategy

### Model Profile: Gemma 2B INT4
- **Base Architecture**: Google Gemma 2B Instruct
- **Parameters**: 2.5 Billion
- **Quantization**: **INT4 (w4a16)**
  - *Weights*: 4-bit integer
  - *Activations*: 16-bit floating point (HTP tensor engine)
- **Model Size Comparison**:
  - FP16 Uncompressed: ~5.0 GB (exceeds typical mobile VRAM budget)
  - INT8: ~2.5 GB (moderate latency, high memory pressure)
  - **INT4 (w4a16)**: **~1.2 GB** (optimal mobile footprint, fits within LPDDR5 bandwidth, high token/sec on Hexagon HTP)

---

## 5. Model Conversion & QNN Context Binary Generation

Qualcomm Hexagon HTP achieves maximum throughput and zero cold-start graph compilation latency by compiling the model into an **SoC-Specific QNN Context Binary**.

### Step 1: Export Source Model to ONNX
Export the Gemma 2B model to optimized ONNX format:
```bash
python -m optimum.exporters.onnx \
  --model google/gemma-2b-it \
  --task text-generation-with-past \
  --device cpu \
  gemma_2b_onnx/
```

### Step 2: Quantize & Convert to QNN Intermediate Model (.dlc)
```bash
python $QNN_SDK_ROOT/bin/x86_64-linux-clang/qnn-onnx-converter \
  --input_network gemma_2b_onnx/model.onnx \
  --output_path gemma_2b_htp.dlc \
  --quantization_overrides gemma_quant_overrides.json \
  --act_bitwidth 16 \
  --weights_bitwidth 4
```

### Step 3: Generate SoC-Specific QNN Context Binary
Generate the context binary specifically targeting the **SM8550 (Snapdragon 8 Gen 2, HTP v73)**:
```bash
$QNN_SDK_ROOT/bin/x86_64-linux-clang/qnn-context-binary-generator \
  --model gemma_2b_htp.dlc \
  --backend $QNN_SDK_ROOT/lib/aarch64-android/libQnnHtp.so \
  --soc_model SM8550 \
  --target_arch v73 \
  --output_dir compiled_context/ \
  --binary_file gemma_2b_sm8550_htp.bin
```

---

## 6. Android Packaging & Native Libraries

### Native Libraries (`jniLibs`)
Copy the official Qualcomm QNN runtime libraries from the SDK to the Android project:

```
app/src/main/jniLibs/arm64-v8a/
├── libQnnHtp.so               # Hexagon Tensor Processor backend dispatcher
├── libQnnHtpV73Skel.so        # Hexagon DSP execution skeleton (v73 / SM8550)
├── libQnnHtpV73Stub.so        # ARM-to-DSP RPC stub
├── libQnnHtpPrepare.so        # Graph compiler / memory planner
├── libQnnSystem.so            # Context binary reader
└── libc++_shared.so           # Standard C++ runtime
```

### Context Binary Placement
Place the compiled binary in internal assets or sideload via storage:
```
app/src/main/assets/ai/qualcomm/
└── gemma_2b_sm8550_htp.bin
```
Or at runtime via device storage:
`/data/data/com.aigallery.app/files/models/qualcomm/gemma_2b_sm8550_htp.bin`

---

## 7. Runtime Detection & Fallback Workflow

When `AIModelRegistry.getLLMProvider(context)` is called:
1. `AIHardwareDetector` inspects `Build.HARDWARE`, `Build.SOC_MODEL`, `/sys/devices/soc0/soc_id`, and `ro.board.platform`.
2. Checks for `libQnnHtp.so` in native library path.
3. Checks for `gemma_2b_sm8550_htp.bin` in model directory.
4. **Execution Matrix**:

| Device Hardware | QNN Libs Present | Context Binary Present | Resulting Execution | Telemetry Status |
| :--- | :--- | :--- | :--- | :--- |
| **Snapdragon SM8550** | **Yes** | **Yes** | **Qualcomm Hexagon NPU (HTP)** | `VERIFIED_NPU` |
| Snapdragon SM8550 | Yes | No | Local CPU Fallback | `MODEL_MISSING` |
| Snapdragon SM8550 | No | Any | Local CPU Fallback | `CPU_FALLBACK` |
| Nothing Phone 3a | Any | Any | Local CPU Fallback | `UNSUPPORTED_SOC` |

---

## 8. Verification & Performance Profiling

### Step 1: Device SoC Verification
Verify connected target device SoC via ADB:
```bash
adb shell getprop ro.soc.model
# Output for Snapdragon 8 Gen 2: SM8550

adb shell getprop ro.board.platform
# Output: kalama
```

### Step 2: NPU Execution Verification via Logcat
Filter logcat for real Qualcomm telemetry tags:
```bash
adb logcat -s AI_BACKEND AI_HARDWARE QNN_INITIALIZATION NPU_EXECUTION
```
Expected output on iQOO Snapdragon 8 Gen 2:
```
AI_HARDWARE: Detected SoC: SM8550 (Qualcomm Snapdragon), ABI: arm64-v8a
QNN_INITIALIZATION: QNN HTP v73 backend loaded successfully.
NPU_EXECUTION: Running inference on Hexagon NPU. Latency: 14 ms
```

Expected output on Nothing Phone 3a (Clean Fallback):
```
AI_HARDWARE: Detected SoC: MT6886 (MediaTek Dimensity), ABI: arm64-v8a
AI_BACKEND: Non-Qualcomm device detected. Routing to Local CPU Fallback. No fake NPU.
```

### Step 3: Offline Guarantee Verification
1. Enable **Airplane Mode** (`adb shell cmd connectivity airplane-mode enable`).
2. Disconnect Wi-Fi and Mobile Data.
3. Capture or browse screenshots in AI Gallery.
4. Open **Screenshot AI Index (Debug Screen)**:
   - Verify all OCR, Entity extraction, Auto-organization, and LLM inferences succeed locally.
   - Verify `Cloud APIs: Disabled (0 Bytes Sent)` and `Airplane Mode: Fully Operable`.
