package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MemberAvatar(
    name: String,
    colorHex: String,
    iconName: String = "person",
    size: Dp = 40.dp,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val parsedColor = parseColorHex(colorHex)
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    val borderModifier = if (isActive) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(borderModifier)
            .clip(CircleShape)
            .background(parsedColor),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp
            )
        } else {
            val iconVector = when (iconName) {
                "face" -> Icons.Default.Face
                "star" -> Icons.Default.Star
                "favorite" -> Icons.Default.Favorite
                else -> Icons.Default.Person
            }
            Icon(
                imageVector = iconVector,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

fun parseColorHex(colorHex: String): Color {
    return try {
        val clean = colorHex.replace("#", "")
        if (clean.length == 6) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else {
            Color(0xFF3F51B5)
        }
    } catch (e: Exception) {
        Color(0xFF3F51B5)
    }
}
