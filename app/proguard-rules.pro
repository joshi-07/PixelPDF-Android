# PixelPDF ProGuard Rules for Production Release Optimization

# Keep Data Models for JSON serialization in DraftManager & HistoryManager
-keepclassmembers class com.pixelpdf.app.data.model.** { *; }
-keep class com.pixelpdf.app.data.model.** { *; }

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# Jetpack Compose & Material 3
-dontwarn androidx.compose.**
-dontwarn androidx.material3.**

# Coroutines
-dontwarn kotlinx.coroutines.**
