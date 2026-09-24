package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// FamSpend Unified Luxury Fintech Design Tokens
val FamPrimary = Color(0xFF4F46E5)        // Deep Electric Indigo
val FamPrimaryDark = Color(0xFF3730A3)    // Dark Indigo
val FamPrimaryLight = Color(0xFFEEF2FF)   // Soft Indigo Tint
val FamPrimaryGradientStart = Color(0xFF6366F1) // Radiant Indigo
val FamPrimaryGradientEnd = Color(0xFF4338CA)   // Deep Indigo

val FamSuccess = Color(0xFF10B981)        // Emerald Green
val FamSuccessDark = Color(0xFF059669)    // Deep Forest Emerald
val FamSuccessLight = Color(0xFFECFDF5)   // Light Mint / Emerald tint

val FamWarning = Color(0xFFF59E0B)        // Warm Amber
val FamWarningLight = Color(0xFFFEF3C7)   // Light Amber Tint

val FamDanger = Color(0xFFEF4444)         // Vibrant Coral Red
val FamDangerLight = Color(0xFFFEE2E2)    // Light Coral Tint

val FamTextPrimary = Color(0xFF0F172A)    // Dark Slate 900
val FamTextSecondary = Color(0xFF64748B)  // Slate Grey 500
val FamTextTertiary = Color(0xFF94A3B8)   // Slate Grey 400

val FamBackground = Color(0xFFF8FAFC)     // Clean Slate 50 background
val FamCardBackground = Color(0xFFFFFFFF) // Pure white card
val FamBorder = Color(0xFFE2E8F0)         // Subtle Slate 200 border

// Dark Mode Tokens
val FamDarkBackground = Color(0xFF0B0F19)
val FamDarkSurface = Color(0xFF131B2E)
val FamDarkSurfaceVariant = Color(0xFF1E293B)
val FamDarkBorder = Color(0xFF1E293B)
val FamDarkTextPrimary = Color(0xFFF8FAFC)
val FamDarkTextSecondary = Color(0xFF94A3B8)

// Gradients
val FamHeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
)
val FamHeroGradientDark = Brush.linearGradient(
    colors = listOf(Color(0xFF1E1B4B), Color(0xFF2E1065))
)
val FamIncomeGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
)

// Compatibility aliases
val EmeraldAccent = FamSuccess
val MintLight = FamSuccessLight
val CoralWarning = FamDanger
val GoldAccent = FamWarning

