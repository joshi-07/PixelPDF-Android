package com.pixelpdf.app.data.model

import androidx.compose.ui.graphics.Color

enum class AppThemeColor(val displayName: String, val primaryColor: Color, val secondaryColor: Color) {
    SAPPHIRE_BLUE("Sapphire Blue", Color(0xFF2563EB), Color(0xFF60A5FA)),
    EMERALD_GREEN("Emerald Green", Color(0xFF059669), Color(0xFF34D399)),
    SUNSET_ORANGE("Sunset Orange", Color(0xFFEA580C), Color(0xFFFB923C)),
    ROYAL_VIOLET("Royal Violet", Color(0xFF7C3AED), Color(0xFFA78BFA))
}
