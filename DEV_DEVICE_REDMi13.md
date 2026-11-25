# Development Device — Redmi 13 5G

**Author:** [Priyanshu Chawda](https://github.com/priyanshuchawda) (`priyanshuchawda`)  
**Captured:** 2026-08-22 (live ADB dump)  
**Role:** Primary daily phone for Retenqo development and personal use  
**ADB serial:** `1431df87`  
**ADB command prefix:** `adb -s 1431df87`

---

## Connected now

| Check | Value |
|-------|--------|
| Connection | USB, `device` state |
| Transport | `usb:1-3` |
| Codename | `breeze` / `breeze_in` |
| Market name | **Redmi 13 5G** |
| Model | `2406ERN9CI` |
| Region | India (`IN`) |

---

## Identity & software

| Property | Value |
|----------|--------|
| Brand / OEM | Redmi / Xiaomi |
| Android | **16** (API **36**) |
| Security patch | **2026-06-01** |
| MIUI / HyperOS | **OS 3.0.302.0.WNUINXM** (HyperOS 3) |
| Build ID | `BP2A.250605.031.A3` |
| Build incremental | `OS3.0.302.0.WNUINXM` |
| First API level | 34 (shipped on Android 14) |
| Kernel | Linux **5.10.236** (aarch64) |
| Fingerprint | `Redmi/breeze_in/breeze:16/BP2A.250605.031.A3/OS3.0.302.0.WNUINXM:user/release-keys` |
| Locale | `en-IN` |
| Carrier (SIM 1) | Jio 4G (`405864`) |
| Dual SIM | DSDS (SIM 2 absent) |
| USB debugging | **Enabled** |
| Developer options | **Enabled** |
| Termux | **Installed** (`com.termux`) |

---

## SoC & compute

| Component | Spec |
|-----------|------|
| Chipset | **Qualcomm SM4450** — Snapdragon **4 Gen 2 AE** (4 nm) |
| Platform | `parrot` / `qcom` |
| CPU layout | **8 cores** — 2× Cortex-A78 @ **2.30 GHz** + 6× Cortex-A55 @ **1.96 GHz** |
| CPU parts (live) | 6× `0xd05` (A55) + 2× `0xd41` (A78) |
| GPU | **Adreno 613** — OpenGL ES **3.2** |
| GPU max clock | ~Adreno613v1 (budget tier) |
| NPU / Hexagon | **None** (no dedicated AI accelerator like iQOO Hexagon) |
| NNAPI feature level | **7** (`persist.device_config.nnapi_native.current_feature_level`) |
| NNAPI extensions | **Denied on product** (`ro.nnapi.extensions.deny_on_product=true`) |
| DSP mount | `/vendor/dsp` present (Qualcomm CDSP — limited vs flagship HTP) |
| Dalvik heap max (per app) | **512 MB** |
| Benchmark class (public) | ~425K AnTuTu (SD 4 Gen 2 AE class) |

**Takeaway:** Fine for app UI, SQLite, notifications, OCR, and **small** on-device models (~1B INT4). Not a stand-in for iQOO’s multi-model NPU pipeline.

---

## Memory & storage (live)

| Resource | Value |
|----------|--------|
| RAM (total) | **~5.4 GB** (`MemTotal` 5,531,208 kB) → **6 GB** SKU |
| RAM available (now) | ~1.9 GB |
| Swap | ~6.1 GB configured |
| Internal `/data` | **104 GB** total — **69 GB used**, **~35 GB free** (67%) |
| microSD (`6538-6239`) | **~30 GB** — **5.6 GB used**, **~24 GB free** |
| App install size | ~19.7 GB |
| App data | ~31.1 GB |
| App cache | ~9.7 GB |
| Photos / videos | ~1.5 GB / ~10.1 GB |
| Storage type | **UFS 2.2** (device class) |
| File-based encryption | **Yes** |

---

## Display

| Spec | Value |
|------|--------|
| Panel | **6.79"** IPS LCD |
| Resolution | **1080 × 2460** |
| Density | **440 dpi** |
| Refresh rates | **120 / 90 / 60 / 50 / 48 / 45 / 40 / 30 Hz** |
| Notch / cutout | Center punch-hole, **121 px** top inset |
| Touch | Internal |
| HDR | Not supported on panel |
| Protection (retail) | Gorilla Glass 3 (manufacturer spec) |

Good for Pulse UI and demo — large enough for floating action button + timeline.

---

## Cameras (HAL dump)

| ID | Facing | Active array | Role |
|----|--------|--------------|------|
| 0 | Back | 4000 × 3000 | Main (**108 MP** sensor, binned modes) |
| 1 | Front | 4208 × 3120 | Selfie (**~13 MP**) |
| 2 | Back | 1600 × 1200 | Depth / auxiliary (**2 MP** class) |
| 3 | Back | 4000 × 3000 | Main (logical duplicate) |
| 4 | Front | 4208 × 3120 | Front (logical duplicate) |

Main sensor (community HW DB): Samsung **S5KHM6**.  
Flash: **Yes** (rear).

**For Retenqo:** Camera not needed for MVP except optional “scan document” later; Pulse uses **screenshot**, not camera stream.

---

## Audio & sensors

| Category | Hardware |
|----------|----------|
| Audio codec | Qualcomm `ravelin` family |
| Low-latency audio | Supported |
| Mic / speaker | Standard smartphone I/O |
| Accelerometer | Bosch **BMA2x2** |
| Magnetometer | Memsic **MMC56x3x** |
| Light | Sensortek **STK6A2x** |
| Proximity | **Virtual** (software) |
| Gyroscope | **Virtual** (fusion — no dedicated gyro chip) |
| Fingerprint | **Goodix** (side or rear — capacitive) |
| Step counter / detector | Qualcomm software |
| Xiaomi extras | Pickup sensor, large-area touch detect |

---

## Connectivity (live)

| Interface | Status |
|-----------|--------|
| Wi‑Fi | **Connected** — WPA2, 2.4 GHz, ~72 Mbps link, RSSI ~−62 to −68 dBm |
| Mobile data | **LTE** (Jio 4G) |
| 5G SA | Enabled in modem config (`persist.radio.sa.user.enabled=1`) |
| Bluetooth | **Off** (name when on: `JIO_Redmi 13 5G`) |
| NFC | Not in feature list |
| USB | **MTP + ADB** (debugging on) |
| GPS | Supported |

---

## Power (live)

| Spec | Value |
|------|--------|
| Battery tech | Li‑poly |
| Level (now) | **76%** |
| Charging | USB, **~500 mA** reported (likely slow/negotiating on this port) |
| Design capacity | **5030 mAh** (manufacturer spec) |
| Fast charge (retail) | **33 W** wired |

---

## Security & permissions surface (for Retenqo)

| Capability | On this device |
|------------|----------------|
| NotificationListenerService | ✅ Required — user grant in Settings |
| Clipboard read (Android 10+) | ✅ Foreground + focus rules apply |
| Share intent (`ACTION_SEND`) | ✅ Standard |
| MediaProjection (screenshot / Pulse) | ✅ One-time user consent dialog |
| AccessibilityService (optional) | ✅ Heavier permission; use only if needed |
| AlarmManager / Calendar | ✅ With permissions |
| On-device ML | ⚠️ CPU/GPU only — small models |
| Background work | ⚠️ MIUI/HyperOS aggressive battery — need foreground service + exemption |
| `QUERY_ALL_PACKAGES` | Avoid; use `<queries>` |

---

## What we **can** do on this phone (Retenqo v1)

### Build now (recommended)

1. **Full Android app shell** — Kotlin, Jetpack Compose, Material 3  
2. **Local database** — Room / SQLite for notes, sources, embeddings metadata  
3. **TYPE A capture — Manual Pulse** — MediaProjection → bitmap → OCR/LLM extract → delete image  
4. **TYPE B capture** — NotificationListener (text), clipboard listener, share target  
5. **Timeline UI** — list of saved memory cards with source app + timestamp  
6. **Voice input** — `SpeechRecognizer` (online for dev) or record + Whisper **tiny** offline  
7. **Simple RAG** — keyword + optional small embedding model (e.g. MiniLM-class via ONNX Mobile)  
8. **Tools** — AlarmManager, CalendarContract intents  
9. **ADB deploy loop** — `./gradlew installDebug` from laptop  
10. **Termux experiments** — quick Python/shell tests without rebuilding APK  

### AI backend strategy on Redmi 13

| Layer | Realistic approach |
|-------|-------------------|
| OCR | **ML Kit Text Recognition** (on-device) or Google ML Kit |
| Screen understanding | OCR + **rules** first; optional **Gemma 1B** / **Qwen2 0.5B** via llama.cpp JNI |
| Embeddings | Small model (~23M–100M) or TF-IDF fallback for device demo |
| Q&A LLM | **≤1B INT4** on CPU; expect **~5–15 tok/s** — good for demo, not production speed |
| Cloud fallback (dev only) | Gemini API from laptop during build week — **not** for final device demo |

### Demo that still works here

- Airplane mode after seeding notes  
- “Pulse this assignment screen → becomes a note → ask deadline by voice”  
- Show notification capture from WhatsApp/Slack-style mock  
- Citation card: “From Pulse on Chrome, Aug 22”  

---

## What we **cannot** do well here (defer to iQOO 15)

| Goal | Why not on Redmi 13 |
|------|---------------------|
| Hexagon NPU multi-model story | No Hexagon HTP; judges’ NPU narrative needs loaner |
| 3B+ LLM fluent offline | RAM and CPU bottleneck |
| ~220 tok/s inference | Flagship NPU only |
| Heavy VLM (image → rich JSON) | Use OCR + small LLM instead |
| 24/7 Focus polling | Battery + thermal on budget SoC |

---

## Suggested dev workflow

```bash
# Always target this serial when multiple phones plugged in
export ANDROID_SERIAL=1431df87

adb devices -l
adb shell getprop ro.product.marketname   # → Redmi 13 5G
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Logs while testing Pulse / notifications
adb logcat -s Retenqo:* NotificationListener:*

# Grant notification access (manual on phone)
# Settings → Apps → Special access → Notification access → Retenqo
```

### Project phases on this device

| Phase | Scope | Device fit |
|-------|--------|------------|
| **P0** | Pulse + SQLite + timeline | ✅ Perfect |
| **P1** | Notifications + clipboard + share | ✅ Perfect |
| **P2** | Voice + alarm tool | ✅ Good |
| **P3** | OCR extract on Pulse | ✅ ML Kit |
| **P4** | Small LLM Q&A + RAG | ⚠️ 1B max, tune prompts |
| **P5** | NPU pipeline + 3B + embed stack | ❌ → iQOO loaner |

---

## Comparison: this phone vs iQOO (upcoming)

| | Redmi 13 5G (this) | iQOO 15 (loaner) |
|--|-------------------|------------------|
| SoC | SD 4 Gen 2 AE | SD 8 Elite Gen 5 |
| RAM | 6 GB | 12–16 GB |
| NPU | None meaningful | Hexagon ~220 tok/s |
| Android | 16 / HyperOS 3 | OriginOS 6 |
| Retenqo role | **Build UI + capture + logic** | **Final AI + demo + judging** |
| Office Kit | No | Yes |

---

## Open decisions (device-specific)

1. **Pulse implementation** — MediaProjection vs `AccessibilityService` screenshot (MediaProjection is cleaner for demo).  
2. **LLM runtime** — llama.cpp in NDK vs MLC LLM vs Gemini Nano (Nano may not be on this device).  
3. **MIUI background kills** — pin app, disable battery saver for Retenqo during dev.  
4. **SD card** — store models on `/storage/6538-6239/` if APK size is an issue (~24 GB free).  

---

## Raw reference commands

```bash
# Re-dump this doc's source data
adb -s 1431df87 shell getprop ro.product.model
adb -s 1431df87 shell cat /proc/cpuinfo
adb -s 1431df87 shell cat /proc/meminfo
adb -s 1431df87 shell df -h /data
adb -s 1431df87 shell dumpsys display | head -40
adb -s 1431df87 shell dumpsys battery
```

---

*Document generated from live `adb` inspection of serial `1431df87`. Retail specs cross-checked with public Redmi 13 5G (2406ERN9CI) datasheets.*
