package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Static base colors for Dark Mode
val DarkBgColor = Color(0xFF0D0F14)       // Deep cosmic space background
val DarkCardColor = Color(0xFF131926)     // Cosmic card background
val DarkTextWhite = Color(0xFFECEFF1)     // High-contrast white text
val DarkTextGrey = Color(0xFF90A4AE)      // Cool-tone grey subtext
val DarkBorderSlate = Color(0xFF202B40)   // Dark border line
val DarkGridLine = Color(0xFF161F30)      // Grid lines for graphs
val DarkTagBg = Color(0xFF1E2638)         // Cool-tone dark tag
val DarkSpecCard = Color(0xFF1A2235)      // Special slate blue card for highlights

// Static base colors for Light Mode
val LightBgColor = Color(0xFFF7F9FF)      // Soft light-blue app background
val LightCardColor = Color(0xFFFFFFFF)    // Pure white cards
val LightTextWhite = Color(0xFF1A1C1E)    // Dark charcoal primary text
val LightTextGrey = Color(0xFF44474E)     // Warm charcoal subtext
val LightBorderSlate = Color(0xFFE0E2EC)  // Soft light border
val LightGridLine = Color(0xFFBAC7DB)     // Soft light grid lines
val LightTagBg = Color(0xFFF1F0F4)        // Light gray badge
val LightSpecCard = Color(0xFFD1E4FF)     // Soft light blue highlight card

// Primary accents remain vibrant and accessible in both modes
val ElectricBlue = Color(0xFF2563EB)      // Premium primary blue
val NeonCyan = Color(0xFF0EA5E9)          // Electric cyan
val BullishGreen = Color(0xFF10B981)      // Vibrant green for upward trend
val BearishRed = Color(0xFFEF4444)        // Vibrant red for downward trend

// Legacy colors kept for backward compatibility
val Purple80 = Color(0xFF90CAF9)
val PurpleGrey80 = Color(0xFFB0BEC5)
val Pink80 = Color(0xFF80DEEA)
val Purple40 = Color(0xFF1565C0)
val PurpleGrey40 = Color(0xFF37474F)
val Pink40 = Color(0xFF00838F)

// Composable-dynamic custom properties that map automatically to current MaterialTheme colorScheme
val CosmicBlack: Color
    @Composable get() = MaterialTheme.colorScheme.background

val CosmicCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val TextWhite: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

val TextGrey: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val BorderSlate: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val GridLineSlate: Color
    @Composable get() = if (MaterialTheme.colorScheme.background == LightBgColor) LightGridLine else DarkGridLine

val TagBackground: Color
    @Composable get() = if (MaterialTheme.colorScheme.background == LightBgColor) LightTagBg else DarkTagBg

val SpecCardBlue: Color
    @Composable get() = if (MaterialTheme.colorScheme.background == LightBgColor) LightSpecCard else DarkSpecCard

