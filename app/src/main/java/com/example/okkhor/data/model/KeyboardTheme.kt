package com.example.okkhor.data.model

data class KeyboardTheme(
    val id: String,
    val name: String,
    val isDark: Boolean = true,
    val backgroundColor: Long = 0xFF14171A,
    val keyBackgroundColor: Long = 0xFF24272D,
    val keyBorderColor: Long = 0xFF3C414A,
    val keyBottomRimColor: Long = 0xFF181A1F,
    val keyTextColor: Long = 0xFFF1F3F5,
    val keySecondaryTextColor: Long = 0xFF9CA3AF,
    val specialKeyBackgroundColor: Long = 0xFF1C1F24,
    val specialKeyBorderColor: Long = 0xFF333842,
    val specialKeyTextColor: Long = 0xFF00C49F,
    val pressedKeyBackgroundColor: Long = 0xFF313640,
    val accentColor: Long = 0xFF00A884,
    val accentTextColor: Long = 0xFFFFFFFF,
    val candidateBarBackgroundColor: Long = 0xFF101215,
    val candidateBarTextColor: Long = 0xFFE2E8F0,
    val candidateHighlightColor: Long = 0xFF00A884,
    val candidateHighlightTextColor: Long = 0xFFFFFFFF,
    val popupBackgroundColor: Long = 0xFF24272D,
    val popupTextColor: Long = 0xFFF1F3F5,
    val keyCornerRadiusDp: Int = 9,
    val keyElevationDp: Int = 2,
    val borderWidthDp: Float = 1.2f,
    val keySpacingDp: Float = 4.0f
) {
    fun toJson(): String {
        return """{
  "version": 2,
  "id": "$id",
  "name": "$name",
  "isDark": $isDark,
  "backgroundColor": $backgroundColor,
  "keyBackgroundColor": $keyBackgroundColor,
  "keyBorderColor": $keyBorderColor,
  "keyBottomRimColor": $keyBottomRimColor,
  "keyTextColor": $keyTextColor,
  "keySecondaryTextColor": $keySecondaryTextColor,
  "specialKeyBackgroundColor": $specialKeyBackgroundColor,
  "specialKeyBorderColor": $specialKeyBorderColor,
  "specialKeyTextColor": $specialKeyTextColor,
  "pressedKeyBackgroundColor": $pressedKeyBackgroundColor,
  "accentColor": $accentColor,
  "accentTextColor": $accentTextColor,
  "candidateBarBackgroundColor": $candidateBarBackgroundColor,
  "candidateBarTextColor": $candidateBarTextColor,
  "candidateHighlightColor": $candidateHighlightColor,
  "candidateHighlightTextColor": $candidateHighlightTextColor,
  "popupBackgroundColor": $popupBackgroundColor,
  "popupTextColor": $popupTextColor,
  "keyCornerRadiusDp": $keyCornerRadiusDp,
  "keyElevationDp": $keyElevationDp,
  "borderWidthDp": $borderWidthDp,
  "keySpacingDp": $keySpacingDp
}"""
    }

    companion object {
        fun fromJson(jsonStr: String): KeyboardTheme? {
            return try {
                fun findLong(key: String, default: Long): Long {
                    val regex = "\"$key\"\\s*:\\s*([0-9A-Fa-fx]+)".toRegex()
                    val match = regex.find(jsonStr)?.groupValues?.get(1) ?: return default
                    return match.removePrefix("0x").toLongOrNull() ?: default
                }
                fun findInt(key: String, default: Int): Int {
                    val regex = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
                    return regex.find(jsonStr)?.groupValues?.get(1)?.toIntOrNull() ?: default
                }
                fun findFloat(key: String, default: Float): Float {
                    val regex = "\"$key\"\\s*:\\s*([0-9.]+)".toRegex()
                    return regex.find(jsonStr)?.groupValues?.get(1)?.toFloatOrNull() ?: default
                }
                fun findString(key: String, default: String): String {
                    val regex = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                    return regex.find(jsonStr)?.groupValues?.get(1) ?: default
                }
                fun findBool(key: String, default: Boolean): Boolean {
                    val regex = "\"$key\"\\s*:\\s*(true|false)".toRegex()
                    return regex.find(jsonStr)?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: default
                }

                val keyBg = findLong("keyBackgroundColor", 0xFF24272DL)
                val specialKeyBg = findLong("specialKeyBackgroundColor", 0xFF1C1F24L)

                KeyboardTheme(
                    id = findString("id", "custom_${System.currentTimeMillis()}"),
                    name = findString("name", "Custom Theme"),
                    isDark = findBool("isDark", true),
                    backgroundColor = findLong("backgroundColor", 0xFF14171AL),
                    keyBackgroundColor = keyBg,
                    keyBorderColor = findLong("keyBorderColor", 0xFF3C414AL),
                    keyBottomRimColor = findLong("keyBottomRimColor", 0xFF181A1FL),
                    keyTextColor = findLong("keyTextColor", 0xFFF1F3F5L),
                    keySecondaryTextColor = findLong("keySecondaryTextColor", 0xFF9CA3AFL),
                    specialKeyBackgroundColor = specialKeyBg,
                    specialKeyBorderColor = findLong("specialKeyBorderColor", 0xFF333842L),
                    specialKeyTextColor = findLong("specialKeyTextColor", 0xFF00C49FL),
                    pressedKeyBackgroundColor = findLong("pressedKeyBackgroundColor", 0xFF313640L),
                    accentColor = findLong("accentColor", 0xFF00A884L),
                    accentTextColor = findLong("accentTextColor", 0xFFFFFFFFL),
                    candidateBarBackgroundColor = findLong("candidateBarBackgroundColor", 0xFF101215L),
                    candidateBarTextColor = findLong("candidateBarTextColor", 0xFFE2E8F0L),
                    candidateHighlightColor = findLong("candidateHighlightColor", 0xFF00A884L),
                    candidateHighlightTextColor = findLong("candidateHighlightTextColor", 0xFFFFFFFFL),
                    popupBackgroundColor = findLong("popupBackgroundColor", keyBg),
                    popupTextColor = findLong("popupTextColor", 0xFFF1F3F5L),
                    keyCornerRadiusDp = findInt("keyCornerRadiusDp", 9).coerceIn(0, 24),
                    keyElevationDp = findInt("keyElevationDp", 2).coerceIn(0, 8),
                    borderWidthDp = findFloat("borderWidthDp", 1.2f).coerceIn(0.5f, 3.0f),
                    keySpacingDp = findFloat("keySpacingDp", 4.0f).coerceIn(1.0f, 8.0f)
                )
            } catch (e: Exception) {
                null
            }
        }

        // 1. Flagship Signature: Okkhor Comfort Slate (Eye-friendly, distinct borders, Ridmik-standard)
        val OkkhorEmerald = KeyboardTheme(
            id = "okkhor_emerald",
            name = "Okkhor Emerald Slate",
            isDark = true,
            backgroundColor = 0xFF14171A,
            keyBackgroundColor = 0xFF24272D,
            keyBorderColor = 0xFF3D434E,
            keyBottomRimColor = 0xFF181A1F,
            keyTextColor = 0xFFF1F3F5,
            keySecondaryTextColor = 0xFF9CA3AF,
            specialKeyBackgroundColor = 0xFF1C1F24,
            specialKeyBorderColor = 0xFF333842,
            specialKeyTextColor = 0xFF00C49F,
            pressedKeyBackgroundColor = 0xFF313640,
            accentColor = 0xFF00A884,
            accentTextColor = 0xFFFFFFFF,
            candidateBarBackgroundColor = 0xFF101215,
            candidateBarTextColor = 0xFFE2E8F0,
            candidateHighlightColor = 0xFF00A884,
            candidateHighlightTextColor = 0xFFFFFFFF,
            popupBackgroundColor = 0xFF24272D,
            popupTextColor = 0xFFF1F3F5,
            keyCornerRadiusDp = 9,
            keyElevationDp = 2,
            borderWidthDp = 1.2f,
            keySpacingDp = 4.0f
        )

        // 2. Classic Teal & Gold
        val ClassicTealGold = KeyboardTheme(
            id = "classic_teal_gold",
            name = "Classic Teal & Gold",
            isDark = true,
            backgroundColor = 0xFF101824,
            keyBackgroundColor = 0xFF1D293B,
            keyBorderColor = 0xFF334661,
            keyBottomRimColor = 0xFF131D2C,
            keyTextColor = 0xFFF8FAFC,
            keySecondaryTextColor = 0xFF94A3B8,
            specialKeyBackgroundColor = 0xFF243248,
            specialKeyBorderColor = 0xFF3B4F6D,
            specialKeyTextColor = 0xFFF5B041,
            pressedKeyBackgroundColor = 0xFF2D3E59,
            accentColor = 0xFFF5B041,
            accentTextColor = 0xFF0F172A,
            candidateBarBackgroundColor = 0xFF0C131D,
            candidateBarTextColor = 0xFFF8FAFC,
            candidateHighlightColor = 0xFFF5B041,
            candidateHighlightTextColor = 0xFF0F172A,
            popupBackgroundColor = 0xFF1D293B,
            popupTextColor = 0xFFF8FAFC,
            keyCornerRadiusDp = 9,
            keyElevationDp = 2,
            borderWidthDp = 1.2f,
            keySpacingDp = 4.0f
        )

        // 3. Midnight AMOLED (True pitch dark with crisp borders)
        val MidnightAmoled = KeyboardTheme(
            id = "midnight_amoled",
            name = "Midnight AMOLED",
            isDark = true,
            backgroundColor = 0xFF000000,
            keyBackgroundColor = 0xFF17171A,
            keyBorderColor = 0xFF303036,
            keyBottomRimColor = 0xFF0A0A0C,
            keyTextColor = 0xFFFAFAFA,
            keySecondaryTextColor = 0xFF8E8E93,
            specialKeyBackgroundColor = 0xFF222227,
            specialKeyBorderColor = 0xFF3C3C44,
            specialKeyTextColor = 0xFF38BDF8,
            pressedKeyBackgroundColor = 0xFF2B2B32,
            accentColor = 0xFF0EA5E9,
            accentTextColor = 0xFF000000,
            candidateBarBackgroundColor = 0xFF0B0B0D,
            candidateBarTextColor = 0xFFF4F4F5,
            candidateHighlightColor = 0xFF0EA5E9,
            candidateHighlightTextColor = 0xFF000000,
            popupBackgroundColor = 0xFF1E1E23,
            popupTextColor = 0xFFFAFAFA,
            keyCornerRadiusDp = 8,
            keyElevationDp = 0,
            borderWidthDp = 1.2f,
            keySpacingDp = 4.0f
        )

        // 4. Paper White (Clean, soft cream-grey background with prominent key borders)
        val PaperWhite = KeyboardTheme(
            id = "paper_white",
            name = "Paper White",
            isDark = false,
            backgroundColor = 0xFFECEEF2,
            keyBackgroundColor = 0xFFFFFFFF,
            keyBorderColor = 0xFFCCD2DC,
            keyBottomRimColor = 0xFFB4BCC8,
            keyTextColor = 0xFF111827,
            keySecondaryTextColor = 0xFF6B7280,
            specialKeyBackgroundColor = 0xFFDFE4EC,
            specialKeyBorderColor = 0xFFC5CDD9,
            specialKeyTextColor = 0xFF1F2937,
            pressedKeyBackgroundColor = 0xFFE5E9F0,
            accentColor = 0xFF2563EB,
            accentTextColor = 0xFFFFFFFF,
            candidateBarBackgroundColor = 0xFFE2E6EE,
            candidateBarTextColor = 0xFF111827,
            candidateHighlightColor = 0xFF2563EB,
            candidateHighlightTextColor = 0xFFFFFFFF,
            popupBackgroundColor = 0xFFFFFFFF,
            popupTextColor = 0xFF111827,
            keyCornerRadiusDp = 9,
            keyElevationDp = 2,
            borderWidthDp = 1.2f,
            keySpacingDp = 4.0f
        )

        // 5. Sunset Amber
        val SunsetAmber = KeyboardTheme(
            id = "sunset_amber",
            name = "Sunset Amber",
            isDark = true,
            backgroundColor = 0xFF1C1412,
            keyBackgroundColor = 0xFF2C201C,
            keyBorderColor = 0xFF4A3731,
            keyBottomRimColor = 0xFF17100D,
            keyTextColor = 0xFFFFF8F6,
            keySecondaryTextColor = 0xFFA8948E,
            specialKeyBackgroundColor = 0xFF382924,
            specialKeyBorderColor = 0xFF58423B,
            specialKeyTextColor = 0xFFFB923C,
            pressedKeyBackgroundColor = 0xFF43312B,
            accentColor = 0xFFEA580C,
            accentTextColor = 0xFFFFFFFF,
            candidateBarBackgroundColor = 0xFF140E0C,
            candidateBarTextColor = 0xFFFFF8F6,
            candidateHighlightColor = 0xFFEA580C,
            candidateHighlightTextColor = 0xFFFFFFFF,
            popupBackgroundColor = 0xFF2C201C,
            popupTextColor = 0xFFFFF8F6,
            keyCornerRadiusDp = 9,
            keyElevationDp = 2,
            borderWidthDp = 1.2f,
            keySpacingDp = 4.0f
        )

        // 6. High Contrast OLED
        val HighContrast = KeyboardTheme(
            id = "high_contrast",
            name = "High Contrast OLED",
            isDark = true,
            backgroundColor = 0xFF000000,
            keyBackgroundColor = 0xFF1C1C1E,
            keyBorderColor = 0xFF48484A,
            keyBottomRimColor = 0xFF0E0E10,
            keyTextColor = 0xFFFFFFFF,
            keySecondaryTextColor = 0xFFE5E5EA,
            specialKeyBackgroundColor = 0xFF2C2C2E,
            specialKeyBorderColor = 0xFF545458,
            specialKeyTextColor = 0xFFFFD600,
            pressedKeyBackgroundColor = 0xFF3A3A3C,
            accentColor = 0xFFFFD600,
            accentTextColor = 0xFF000000,
            candidateBarBackgroundColor = 0xFF000000,
            candidateBarTextColor = 0xFFFFFFFF,
            candidateHighlightColor = 0xFFFFD600,
            candidateHighlightTextColor = 0xFF000000,
            popupBackgroundColor = 0xFF2C2C2E,
            popupTextColor = 0xFFFFFFFF,
            keyCornerRadiusDp = 7,
            keyElevationDp = 0,
            borderWidthDp = 1.3f,
            keySpacingDp = 4.0f
        )

        // 7. Obsidian Amber Gold (Luxury obsidian dark with warm artisan amber accents - Image 3 benchmark)
        val ObsidianAmber = KeyboardTheme(
            id = "obsidian_amber",
            name = "Obsidian Amber Gold",
            isDark = true,
            backgroundColor = 0xFF121418,
            keyBackgroundColor = 0xFF20232A,
            keyBorderColor = 0xFF363B47,
            keyBottomRimColor = 0xFF14161B,
            keyTextColor = 0xFFF9FAFB,
            keySecondaryTextColor = 0xFFA1A8B8,
            specialKeyBackgroundColor = 0xFF1A1D23,
            specialKeyBorderColor = 0xFF303642,
            specialKeyTextColor = 0xFFE5A96A,
            pressedKeyBackgroundColor = 0xFF2C313C,
            accentColor = 0xFFE5A96A,
            accentTextColor = 0xFF121418,
            candidateBarBackgroundColor = 0xFF0E1013,
            candidateBarTextColor = 0xFFF9FAFB,
            candidateHighlightColor = 0xFFE5A96A,
            candidateHighlightTextColor = 0xFF121418,
            popupBackgroundColor = 0xFF20232A,
            popupTextColor = 0xFFF9FAFB,
            keyCornerRadiusDp = 10,
            keyElevationDp = 2,
            borderWidthDp = 1.2f,
            keySpacingDp = 4.0f
        )

        val ALL_PRESETS = listOf(
            OkkhorEmerald,
            ObsidianAmber,
            ClassicTealGold,
            MidnightAmoled,
            PaperWhite,
            SunsetAmber,
            HighContrast
        )

        fun getById(id: String): KeyboardTheme {
            return ALL_PRESETS.find { it.id == id } ?: OkkhorEmerald
        }
    }
}
