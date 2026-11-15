# PixelPDF v2.0 — Premium Native Android Image-to-PDF Converter

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline-00C853?style=for-the-badge&logo=shield&logoColor=white" />
  <img src="https://img.shields.io/badge/Version-v2.0.0-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-MIT-orange?style=for-the-badge" />
</p>

<p align="center">
  <a href="https://github.com/joshi-07/PixelPDF-Android/releases/latest/download/PixelPDF-v2.0.0.apk">
    <img src="https://img.shields.io/badge/📥_Download_Direct_APK-PixelPDF_v2.0.0-2EA44F?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" height="48" />
  </a>
</p>

<p align="center">
  <strong>Fast, Private & Elegant Image-to-PDF Converter for Android.</strong><br>
  Engineered with Kotlin, Jetpack Compose, Material 3, Clean Architecture, and Android's native <code>PdfDocument</code> API.
</p>

---

## 🚀 Direct APK Download (Ultra-Fast 1.65 MB)

Anyone visiting this repository can download and install the standalone Android APK directly without compiling from source:

👉 **[Download PixelPDF v2.0.0 APK (Direct Download — 1.65 MB)](https://github.com/joshi-07/PixelPDF-Android/releases/latest/download/PixelPDF-v2.0.0.apk)**

📦 **[Download PixelPDF v2.0.0 ZIP (ZIP Archive — 1.46 MB)](https://github.com/joshi-07/PixelPDF-Android/releases/latest/download/PixelPDF-v2.0.0-apk.zip)** *(Recommended if your mobile browser blocks direct `.apk` downloads)*

> **Mobile Tip:** If downloading inside LinkedIn, Instagram, or Reddit in-app browser, tap the **three dots (`...`)** in the upper-right corner and select **"Open in Chrome / Browser"** to ensure your download completes smoothly.

*Compatible with all Android devices running Android 8.0 (Oreo / API 26) through Android 15.*

---

## 💎 What's New in Version 2.0 (50 Features + 10 UI/UX Improvements)

PixelPDF v2.0 is a monumental release bringing **50 brand-new usable features** and **10 world-class UI & UX enhancements**:

### 🎯 50 New Features

#### 📸 Image Capture & Color Processing
1. **Document Camera Scanner**: Direct in-app camera capture for scanning physical documents, receipts, or notes with auto-temporary file management via `FileProvider`.
2. **Document Clean Filter**: Adaptive enhancement filter that removes shadows and whitens scanned backgrounds.
3. **B&W / Grayscale Mode**: High-contrast black & white document conversion.
4. **High Contrast Filter**: Boosts readability for pencil sketches, faded receipts, and blueprints.
5. **Sepia Tone Filter**: Vintage archive parchment styling.
6. **Invert Filter**: Dark mode negative effect for scanning light-on-dark technical schematics.
7. **Vintage Tone Filter**: Warm faded retro film look.
8. **Cool Temperature Filter**: Bluish tone for engineering and architectural prints.
9. **Warm Temperature Filter**: Golden warm tint for portraits and photography booklets.
10. **Fine-Tuned Brightness Slider**: Granular slider from -0.5 to +0.5 for per-image exposure correction.
11. **Fine-Tuned Contrast Slider**: Granular slider from 0.5x to 2.0x for per-image contrast tuning.
12. **Horizontal Image Mirroring (Flip H)**: Instant horizontal reflection for flipped scans or selfies.
13. **Vertical Image Mirroring (Flip V)**: Quick 180° vertical reflection for upside-down scans.

#### 🛡️ Watermarking, Headers, Footers & Numbering
14. **Custom Text Watermark**: Add proprietary watermark stamps across all PDF pages.
15. **Watermark Opacity Slider**: Adjust transparency from subtle 10% to prominent 80%.
16. **Watermark Rotation Angles**: Rotate stamps at 0°, 30°, 45°, 60°, or 90°.
17. **Watermark Color Accents**: Select from Gray, Red Confidential, Blue Brand, or Dark.
18. **Running Page Headers**: Configurable header text placed at the top of every page.
19. **Header Alignment**: Left, Center, or Right alignment.
20. **Running Page Footers**: Configurable footer note or confidentiality disclaimer at the bottom of pages.
21. **4 Page Number Styles**: "1 / N", "Page 1 of N", plain "1", or decorative "- 1 -".
22. **4 Page Number Positions**: Bottom Center, Bottom Right, Bottom Left, or Top Right.

#### 📐 Geometry, Styling & Duplex Pages
23. **Rounded Corner Pages**: Stylized soft rounded image borders with customizable corner radius (0–28 pt).
24. **Duplex Blank Page Spacers**: Automatically inserts blank pages after every photo for double-sided booklet printing.
25. **Custom Render Resolution (DPI)**: Choose between 72 DPI (Web/Email), 150 DPI (Standard Office), or 300 DPI (High-Res Print).
26. **Real-time Estimated File Size**: Live calculation estimating output PDF size based on image count, quality level, and DPI.

#### 🗂️ PDF Document Metadata (Dublin Core)
27. **Document Title**: Embeds title metadata directly into the PDF binary.
28. **Document Author**: Embeds author/organization metadata.
29. **Document Subject**: Embeds subject line into PDF properties.
30. **Document Keywords**: Embeds searchable comma-separated indexing tags.

#### ⚡ Batch Operations & Organization
31. **Multi-Select Batch Mode**: Multi-select images in editor with checkboxes for bulk operations.
32. **Select All / Deselect All**: One-tap toggle for all images in the queue.
33. **Batch Delete**: Remove multiple selected images in one action.
34. **Batch Apply Filter**: Apply Document Clean, B&W, or Contrast to all selected images simultaneously.
35. **Batch Apply Brightness & Contrast**: Propagate visual exposure tuning to multiple photos.
36. **Image Duplication**: Clone any image in the list to repeat pages or create side-by-side variations.
37. **Alphabetical Sorting (A-Z & Z-A)**: Sort selected photos by file name ascending or descending.
38. **Date/Time Sorting**: Sort images chronologically by import or capture time.
39. **File Size Sorting**: Sort images from smallest to largest or vice versa.
40. **Reverse Order**: Instant 1-tap flip of entire page sequence (e.g. from last-scanned to first-scanned).
41. **Deep EXIF Inspector**: View photo metadata including resolution, file size, timestamp, and orientation.
42. **Live Image Lightbox Preview**: Full-screen modal inspector with rotation, flip, live filter testing, and tuning before converting.

#### 📚 Library, Sharing, Print & Storage
43. **Search Conversion History**: Real-time search bar across past generated PDFs by name.
44. **Favorites / Starred PDFs**: Bookmark key documents to find them instantly.
45. **Filter by Favorites**: Toggle to view only starred/bookmarked PDFs in library.
46. **Export All as ZIP**: Package all generated PDFs into a single compressed `.zip` archive for bulk backup or transfer.
47. **Native Android Direct Print**: Send generated PDF directly to Wi-Fi/Cloud printer via Android `PrintManager`.
48. **Email Attachment Shortcut**: One-tap intent to send the generated PDF directly as an email attachment.
49. **Storage & Cache Analyzer**: Displays space consumed by generated PDFs and temporary cache, with 1-tap "Clean Cache" button.
50. **Custom Color Accent Themes**: Switch between 4 rich Material 3 themes: Sapphire Blue, Emerald Green, Sunset Orange, and Royal Violet.

---

### ✨ 10 World-Class UI & UX Enhancements

1. **Material 3 Navigation Bar**: Modern bottom navigation bar with 4 distinct tabs (Convert, Arrange & Edit, Library, Settings).
2. **Floating Summary Pill**: Dynamic floating pill in the editor showing real-time statistics: `"X Pages • Est. ~Y MB • Ready"`.
3. **Contextual Top App Bar**: App bar transitions with distinct colors and action icons during multi-select mode.
4. **Haptic Touch Feedback**: Tactile haptic vibration when reordering, rotating, deleting, or applying filters.
5. **Interactive PDF Multi-Page Preview Carousel**: Horizontal page carousel in preview screen with interactive thumbnail switcher.
6. **Quick Filter Preset Chips**: Scrollable filter chips with instant preview badges right below images.
7. **Smooth Micro-Animations & Crossfades**: Seamless crossfade transitions between screens and animated card elevations.
8. **Polished Empty States**: Welcoming vector illustrations and helpful action prompts when library or queue is empty.
9. **Interactive Storage Usage Card**: Visual breakdown of app cache vs. PDF library with instant freed-space feedback.
10. **Toast & Dialog Polish**: Clean Material 3 alert dialogs with clear confirmation flows.

---

## 🔒 100% On-Device & Completely Private

- **Zero Internet Permission:** No internet permissions are declared in `AndroidManifest.xml`.
- **Zero Cloud Uploads:** All decoding, resizing, arrangement, PDF rendering, and file exporting happen 100% locally on your phone.
- **Zero Trackers or Telemetry:** No ads, analytics, or background tracking.

---

## 🛠️ Complete PDF Customization Suite

| Setting | Options Available |
| :--- | :--- |
| **Page Sizes** | A4, A3, A5, US Letter, US Legal, Original Image Dimensions |
| **Page Orientation** | Portrait, Landscape, Automatic (Matches aspect ratio of each photo) |
| **Page Margins** | None (0 pt), Small (18 pt), Medium (36 pt), Large (54 pt) |
| **Image Scaling** | Fit to Page (Aspect-Ratio Safe), Fill Page, Maintain Original Dimensions |
| **Quality Control** | Small File Size (50%), Balanced (75%), High Quality (90%), Maximum Clarity (100%) |
| **Resolution (DPI)** | 72 DPI (Web/Lightweight), 150 DPI (Office Standard), 300 DPI (High-Res Print) |
| **Watermark** | Custom text, opacity slider (10–80%), angles (0°–90°), 4 color accents |
| **Headers & Footers** | Running header, running footer, Left/Center/Right alignment |
| **Page Numbers** | 4 styles ("1 / N", "Page 1 of N", "1", "- 1 -"), 4 positions |
| **Duplex Pages** | Optional blank spacer page insertion for two-sided booklet printing |
| **Styling** | Image corner rounding (0–28 pt), subtle page borders, custom background color |
| **Metadata** | Title, Author, Subject, Keywords (Dublin Core) |
| **Output Modes** | Single Combined PDF, Separate PDF for each image, Split by N images |

---

## 🏗️ Architecture & Technology Stack

```
com.pixelpdf.app
├── data
│   ├── model          # Immutable data models (ImageItem, PdfSettings, GeneratedPdfFile, ImageFilter, AppThemeColor)
│   └── repository     # ImageProcessor (filters & decoders), PdfEngine, HistoryManager, DraftManager
├── ui
│   ├── screens        # HomeScreen, EditorScreen, SettingsScreen, PreviewScreen, HistoryScreen
│   ├── theme          # Material 3 Color palette, Typography, Dynamic Edge-to-Edge Theme
│   └── viewmodel      # MainViewModel (StateFlow, Coroutine Scope, State Machine)
├── util               # PrintHelper (Native Android PrintManager integration)
└── MainActivity.kt    # Single activity architecture with NavigationBar & BackHandler
```

- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose + Material 3 (1.3.1)
- **Asynchronous Coroutines:** Kotlinx Coroutines 1.9.0
- **PDF Engine:** Native Android `android.graphics.pdf.PdfDocument`
- **PDF Renderer:** Native Android `android.graphics.pdf.PdfRenderer`
- **Image Pipeline:** Android `BitmapFactory`, `ExifInterface`, Coil Compose 2.7.0
- **Print Integration:** Android `android.print.PrintManager`

---

## 📲 Installation Instructions

### Option 1: Direct Download
1. Download **[PixelPDF-v2.0.0.apk](https://github.com/joshi-07/PixelPDF-Android/releases/latest/download/PixelPDF-v2.0.0.apk)** directly on your Android phone.
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

## 📄 License

PixelPDF is open-source software licensed under the [MIT License](LICENSE).
Copyright (c) 2026 Sughosha K Joshi.
