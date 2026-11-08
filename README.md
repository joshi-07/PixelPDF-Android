# PixelPDF — Premium Native Android Image-to-PDF Converter

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline-00C853?style=for-the-badge&logo=shield&logoColor=white" />
  <img src="https://img.shields.io/badge/License-MIT-orange?style=for-the-badge" />
</p>

<p align="center">
  <a href="https://github.com/joshi-07/PixelPDF-Android/releases/latest/download/PixelPDF-v1.2.0.apk">
    <img src="https://img.shields.io/badge/📥_Download_Direct_APK-PixelPDF_v1.2.0-2EA44F?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" height="48" />
  </a>
</p>

<p align="center">
  <strong>Fast, Private & Elegant Image-to-PDF Converter for Android.</strong><br>
  Engineered with Kotlin, Jetpack Compose, Material 3, Clean Architecture, and Android's native <code>PdfDocument</code> API.
</p>

---

## 🚀 Direct APK Download

Anyone visiting this repository can download and install the standalone Android APK directly without compiling from source:

👉 **[Download PixelPDF v1.2.0 APK (Direct Download)](https://github.com/joshi-07/PixelPDF-Android/releases/latest/download/PixelPDF-v1.2.0.apk)**

*Compatible with all Android devices running Android 8.0 (Oreo / API 26) through Android 15.*

---

## 🌟 Key Highlights & Features

### 🔒 100% On-Device & Completely Private
- **Zero Internet Permission:** No internet permissions are even declared in `AndroidManifest.xml`.
- **Zero Cloud Uploads:** All decoding, resizing, arrangement, PDF rendering, and file exporting happen 100% locally on your phone.
- **Zero Trackers or Telemetry:** No ads, analytics, or background tracking.

### ⚡ Ultra-Efficient Processing Engine
- **Parallel Image Ingestion:** Selected photos and document scans are decoded concurrently using Kotlin Coroutines (`Dispatchers.IO`), importing 50+ images in fractions of a second.
- **Intelligent Memory Downsampling:** Dynamically calculates `inSampleSize` matching target PDF dimensions and uses `RGB_565` bitmap configuration, slashing RAM consumption by up to 70% and preventing `OutOfMemory` crashes on large batches.
- **Instant Garbage Collection:** Recycles intermediate bitmap allocations immediately after each page finishes drawing.

### 🎨 World-Class Jetpack Compose & Material 3 UI/UX
- **Quick Conversion Presets:**
  - 📄 **Document A4**: Standard portrait A4 with elegant margins for invoices, resumes, and study materials.
  - 🖼️ **High-Res Photo**: Preserves original image dimensions and maximum 100% quality for photo portfolios.
  - 📦 **Compact Share**: Balanced compression optimized for email, WhatsApp, and messaging attachments.
- **Dual Universal Image Picker:**
  - **Select Photos:** Modern Android Photo Picker with dynamic system limits.
  - **Browse Files:** System Storage Access Framework (SAF) to select unlimited photos from internal storage, SD cards, Downloads, or cloud sync folders.
- **Visual Image Editor:**
  - Drag-free fluid card controls (Move left, Move right, Rotate 90°, Delete).
  - **Full Image Preview Modal:** Tap any card to inspect full image dimensions, aspect ratio, and rotation angle before converting.
- **🔄 Persistent Draft Session & Auto-Recovery (New in v1.2.0):**
  - If you close the app or switch tasks while arranging a big batch of photos, PixelPDF automatically saves your in-progress selection, exact display ordering, and PDF settings.
  - Reopening the app surfaces an immediate **"Resume Draft"** card to continue right where you left off.
  - Automatically acquires persistable URI permissions on supported file providers.
- **Built-in PDF Previewer:** Native page-by-page PDF viewer powered by Android's `PdfRenderer`.
- **Live Progress Dialog:** Real-time percentage indicator, current image name, and safe coroutine cancellation.
- **Persistent Conversion History:** Re-open, re-share, or export previous conversions anytime.

---

## 🛠️ Complete PDF Customization Suite

| Setting | Options Available |
| :--- | :--- |
| **Page Sizes** | A4, A3, A5, US Letter, US Legal, Original Image Dimensions |
| **Page Orientation** | Portrait, Landscape, Automatic (Matches aspect ratio of each photo) |
| **Page Margins** | None (0 pt), Small (18 pt), Medium (36 pt), Large (54 pt) |
| **Image Scaling** | Fit to Page (Aspect-Ratio Safe), Fill Page, Maintain Original Dimensions |
| **Quality Control** | Small File Size (50%), Balanced (75%), High Quality (90%), Maximum Clarity (100%) |
| **Output Modes** | Single Combined PDF, Separate PDF for each image, Split by N images |
| **Page Decor** | Optional bottom-center numbering (`Page X of Y`), subtle borders, custom background |

---

## 🏗️ Architecture & Technology Stack

```
com.pixelpdf.app
├── data
│   ├── model          # Immutable data models (ImageItem, PdfSettings, GeneratedPdfFile)
│   └── repository     # ImageProcessor (parallel decoders), PdfEngine, HistoryManager
├── ui
│   ├── screens        # HomeScreen, EditorScreen, SettingsScreen, PreviewScreen, HistoryScreen
│   ├── theme          # Material 3 Color palette, Typography, Dynamic Edge-to-Edge Theme
│   └── viewmodel      # MainViewModel (StateFlow, Coroutine Scope, State Machine)
└── MainActivity.kt    # Single activity architecture with backstack handler
```

- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose + Material 3 (1.3.1)
- **Asynchronous Coroutines:** Kotlinx Coroutines 1.9.0
- **PDF Engine:** Native Android `android.graphics.pdf.PdfDocument`
- **PDF Renderer:** Native Android `android.graphics.pdf.PdfRenderer`
- **Image Pipeline:** Android `BitmapFactory`, `ExifInterface`, Coil Compose 2.7.0
- **Document Management:** Storage Access Framework (`ACTION_CREATE_DOCUMENT`), AndroidX `FileProvider`

---

## 📲 Installation Instructions

### Option 1: Direct Download
1. Download **[PixelPDF-v1.1.0.apk](https://github.com/joshi-07/PixelPDF-Android/releases/latest/download/PixelPDF-v1.1.0.apk)** directly on your Android phone.
2. Open the downloaded file from your browser or file manager.
3. If prompted by Android, tap **Settings** and toggle **Allow from this source**.
4. Tap **Install** and launch PixelPDF!

### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/joshi-07/PixelPDF-Android.git
cd PixelPDF-Android

# Run unit test suite
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🛡️ Privacy Guarantee

PixelPDF is completely committed to user data sovereignty:
- ❌ No internet permissions.
- ❌ No servers, cloud APIs, or analytics beacons.
- ❌ No user accounts or registrations.
- ✅ 100% of bytes processed remain strictly on your local device.

---

## 📄 License

PixelPDF is open-source software licensed under the [MIT License](LICENSE).
Copyright (c) 2026 Sughosha K Joshi.
