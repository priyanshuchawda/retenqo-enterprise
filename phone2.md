# Phone 2 — Redmi Note 11 Pro+ 5G

**Author:** [Priyanshu Chawda](https://github.com/priyanshuchawda) (`priyanshuchawda`)  
**Captured:** 2026-08-22 (live ADB dump)  
**Role:** Secondary test device for Retenqo (after storage cleanup)  
**ADB serial:** `3e68912fdff2`  
**ADB command prefix:** `adb -s 3e68912fdff2`  
**Storage note:** Cleared / available for Retenqo testing.

**Phone 1 (primary dev):** [DEV_DEVICE_REDMi13.md](./DEV_DEVICE_REDMi13.md) — Redmi 13 5G (`1431df87`)

---

## Connected now

| Check | Value |
|-------|--------|
| Connection | USB, `device` state |
| Transport | `usb:1-3` |
| Codename | `peux` / `peux_in` |
| Market name | **Redmi Note 11 Pro+ 5G** |
| Model | `2201116SI` |
| Region | India (`IN`) |

---

## Identity & software

| Property | Value |
|----------|--------|
| Brand / OEM | Redmi / Xiaomi |
| Android | **13** (API **33**) |
| Security patch | **2024-12-01** |
| MIUI / HyperOS | **OS 1.0.9.0.TKCINXM** (HyperOS 1 / MIUI V816) |
| Build ID | `TKQ1.221114.001` |
| Build incremental | `V816.0.9.0.TKCINXM` |
| First API level | 30 (shipped on Android 11) |
| Kernel | Linux **5.4.274** (aarch64, QGKI) |
| Fingerprint | `Redmi/peux_in/peux:13/TKQ1.221114.001/V816.0.9.0.TKCINXM:user/release-keys` |
| Locale | `en-IN` |
| Carrier (SIM 1) | Idea (`LOADED`) |
| Dual SIM | DSDS (SIM 2 absent) |
| USB debugging | **Enabled** |
| Developer options | **Enabled** |
| Termux | **Installed** (`com.termux`) |
| User apps installed | **109** |

---

## SoC & compute

| Component | Spec |
|-----------|------|
| Chipset | **Qualcomm SM6375** — Snapdragon **695 5G** (6 nm) |
| Platform | `holi` / `qcom` |
| CPU layout | **8 cores** — 2× Cortex-A78 @ **2.21 GHz** + 6× Cortex-A55 @ **1.80 GHz** |
| CPU parts (live) | 6× `0xd05` (A55) + 2× `0xd41` (A78) |
| GPU | **Adreno 619** — OpenGL ES **3.2** (`Adreno619v2`) |
| NPU / Hexagon | **None** (budget/mid-tier — no dedicated AI accelerator) |
| NNAPI feature level | **7** |
| Dalvik heap max (per app) | **512 MB** (typical) |
| Benchmark class (public) | ~400K AnTuTu (SD 695 class) |

**vs Phone 1 (Redmi 13 5G):** Older chip (2021 vs 2024) but **Adreno 619 beats Adreno 613** for GPU tasks. **8 GB RAM** vs 6 GB. **Android 13** vs 16 — Phone 1 is better for OS parity with iQOO loaner.

---

## Memory & storage

| Resource | Value |
|----------|--------|
| RAM (total) | **~7.4 GB** (`MemTotal` 7,610,512 kB) → **8 GB** SKU |
| RAM available (scan time) | ~3.4 GB |
| Swap | ~6.1 GB configured |
| Internal storage | **~128 GB** class (`/data` ~102 GB partition) |
| microSD | **~30 GB** card mounted (`D6F0-0915`) |
| Storage type | **UFS 2.2** (device class) |
| File-based encryption | **Yes** |

**Assumption for this doc:** Storage will be freed before serious dev (OpenCamera, WhatsApp media, SD duplicates). Treat as **~128 GB internal + optional SD** available for APK, models, and test data.

---

## Display

| Spec | Value |
|------|--------|
| Panel | **6.67"** AMOLED |
| Resolution | **1080 × 2400** |
| Density | **440 dpi** |
| Refresh rates | **120 Hz** / **60 Hz** |
| HDR | **Supported** (HDR10, HLG) |
| Notch / cutout | Center punch-hole, **90 px** top inset |
| Max brightness (nits) | Up to **450 nits** (config) |
| Protection (retail) | Corning Gorilla Glass 5 |

Better panel than Phone 1 (AMOLED + HDR vs IPS LCD).

---

## Cameras (HAL dump)

| ID | Facing | Active array | Role |
|----|--------|--------------|------|
| 0 | Back | 4000 × 3000 | Main (**108 MP** sensor) |
| 1 | Front | 2304 × 1728 | Selfie (**~4 MP** default stream / 16 MP sensor) |
| 2 | Back | 1600 × 1200 | Depth / auxiliary |
| 3 | Back | 3280 × 2464 | **8 MP ultrawide** |
| 4–5 | Back | 4000 × 3000 | Main (logical duplicates) |

Retail setup: **108 MP** main + **8 MP** ultrawide + **2 MP** macro/depth, **16 MP** front.  
Flash: **Yes** (rear).

**For Retenqo:** Same as Phone 1 — Pulse uses **screenshot**, not camera.

---

## Audio & sensors

| Category | Hardware |
|----------|----------|
| Audio | Qualcomm holi platform codec |
| Low-latency audio | Supported |
| Accelerometer | TDK-Invensense **ICM4x6xx** |
| Gyroscope | **ICM4x6xx** (real hardware — not virtual) |
| Magnetometer | AKM **ak0991x** |
| Light | Rohm **BU27030** |
| Proximity | **Elliptic Labs** (software fusion) |
| Fingerprint | Side-mounted capacitive |
| Step counter / detector | Qualcomm |
| Xiaomi extras | AOD sensor, pickup, large-area touch detect |

**Better sensor stack than Phone 1** (real gyro, richer fusion).

---

## Connectivity

| Interface | Status |
|-----------|--------|
| Wi‑Fi | **Enabled** (connected at scan time) |
| Mobile data | **LTE/5G** capable (SD 695 5G) |
| Bluetooth | **Off** (device name: `Redmi Note 11 Pro+`) |
| NFC | Not in feature list |
| USB | **MTP + ADB** |
| GPS | Supported |
| 5G bands (retail) | SA/NSA (India variant) |

---

## Power

| Spec | Value |
|------|--------|
| Battery tech | Li‑poly |
| Design capacity | **4500 mAh** (manufacturer spec) |
| Level (scan time) | **46%** |
| Fast charge (retail) | **67 W** wired |
| Charging (live USB) | High-current capable (`Max charging current: 12200000` µA class) |

---

## Security & permissions (Retenqo)

Same Android capability surface as Phone 1:

| Capability | Supported |
|------------|-----------|
| NotificationListenerService | ✅ |
| Clipboard / Share intent | ✅ |
| MediaProjection (Pulse) | ✅ |
| AlarmManager / Calendar | ✅ |
| On-device ML (small models) | ⚠️ CPU/GPU only |
| Background services | ⚠️ MIUI/HyperOS battery rules — pin app during dev |

---

## What we can do on this phone (assuming storage OK)

### Good for

1. **Second-device testing** — different screen size, Android 13 behavior, MIUI quirks  
2. **Same Retenqo app** — install debug APK, test Pulse, notifications, timeline  
3. **GPU-heavy UI** — Adreno 619 handles Compose animations well  
4. **8 GB RAM** — slightly more headroom for **1B LLM** experiments than Phone 1  
5. **Real gyro / sensors** — if you add motion or orientation features later  
6. **Termux** — same quick scripting as Phone 1  

### Same AI limits as Phone 1

| Layer | Realistic approach |
|-------|-------------------|
| OCR | ML Kit on-device |
| LLM | **≤1B INT4** via llama.cpp — CPU/GPU, slow |
| Embeddings | Small ONNX model or TF-IDF |
| NPU demo | ❌ Defer to **iQOO 15** |

---

## Phone 1 vs Phone 2 — which for what?

| Factor | Phone 1 — Redmi 13 5G | Phone 2 — Note 11 Pro+ 5G |
|--------|----------------------|---------------------------|
| **Primary dev** | ✅ **Yes** | Secondary tester |
| Android | **16** (closer to iQOO) | 13 |
| RAM | 6 GB | **8 GB** |
| GPU | Adreno 613 | **Adreno 619** |
| CPU | SD 4 Gen 2 (newer, 4 nm) | SD 695 (older, 6 nm) |
| Display | IPS 120 Hz | **AMOLED 120 Hz + HDR** |
| Sensors | Virtual gyro | **Real gyro** |
| Battery | 5030 mAh | 4500 mAh |
| Storage (assumed) | ~35 GB free now | Cleared before use |
| Retenqo AI stack | Prototype shell | Same — not NPU target |

**Recommendation:** Build on **Phone 1**, flash builds to **Phone 2** for compatibility testing on Android 13 + 8 GB.

---

## Suggested dev workflow

```bash
export ANDROID_SERIAL=3e68912fdff2

adb devices -l
adb shell getprop ro.product.marketname   # → Redmi Note 11 Pro+ 5G

# Install same debug APK as Phone 1
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Compare behavior
adb logcat -s Retenqo:*
```

When both phones are plugged in:

```bash
adb devices -l
# 1431df87  → Redmi 13 5G   (phone 1)
# 3e68912fdff2 → Note 11 Pro+ 5G (phone 2)
```

---

## Open decisions (device-specific)

1. Use as **Android 13 regression device** only, or also for LLM experiments (8 GB helps slightly).  
2. Store large models on **microSD** (`/storage/D6F0-0915/`) if internal space is tight after cleanup.  
3. MIUI battery saver — same pinning rules as Phone 1 for NotificationListener + foreground service.  

---

## Raw reference commands

```bash
adb -s 3e68912fdff2 shell getprop ro.product.model
adb -s 3e68912fdff2 shell cat /proc/cpuinfo
adb -s 3e68912fdff2 shell cat /proc/meminfo
adb -s 3e68912fdff2 shell df -h /data
adb -s 3e68912fdff2 shell dumpsys display | head -30
adb -s 3e68912fdff2 shell dumpsys battery
```

---

*Live ADB inspection of serial `3e68912fdff2`. Retail specs cross-checked with Redmi Note 11 Pro+ 5G (2201116SI) datasheets. Storage treated as resolved per project assumption.*
