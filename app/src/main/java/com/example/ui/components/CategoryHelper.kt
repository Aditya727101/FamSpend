package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object CategoryHelper {
    val allCategories = listOf(
        CategoryInfo("Groceries", Icons.Default.ShoppingCart, Color(0xFF4CAF50)),
        CategoryInfo("Utilities & Bills", Icons.Default.ElectricalServices, Color(0xFFF44336)),
        CategoryInfo("Entertainment", Icons.Default.Movie, Color(0xFF9C27B0)),
        CategoryInfo("Transport & Fuel", Icons.Default.DirectionsCar, Color(0xFFFF9800)),
        CategoryInfo("Healthcare", Icons.Default.LocalHospital, Color(0xFF2196F3)),
        CategoryInfo("Dining & Food", Icons.Default.Restaurant, Color(0xFFFF7043)),
        CategoryInfo("Rent & Mortgage", Icons.Default.Home, Color(0xFFD32F2F)),
        CategoryInfo("Insurance", Icons.Default.Security, Color(0xFF00897B)),
        CategoryInfo("Kids & Education", Icons.Default.School, Color(0xFF00ACC1)),
        CategoryInfo("Shopping", Icons.Default.Checkroom, Color(0xFF8D6E63)),
        CategoryInfo("Maintenance", Icons.Default.Build, Color(0xFF546E7A)),
        CategoryInfo("Travel & Vacation", Icons.Default.Flight, Color(0xFF3949AB)),
        CategoryInfo("Personal Care", Icons.Default.Spa, Color(0xFFD81B60)),
        CategoryInfo("Gifts & Donations", Icons.Default.CardGiftcard, Color(0xFFFFB300)),
        CategoryInfo("Other Expenses", Icons.Default.Receipt, Color(0xFF607D8B))
    )

    fun getCategoryInfo(categoryName: String): CategoryInfo {
        val lower = categoryName.lowercase(java.util.Locale.getDefault())
        if (lower.contains("grocer") || lower.contains("supermarket") || lower.contains("food")) {
            return allCategories.find { it.name == "Groceries" }!!
        }
        if (lower.contains("bill") || lower.contains("utilit") || lower.contains("electric") || lower.contains("water") || lower.contains("wifi") || lower.contains("rent")) {
            return allCategories.find { it.name == "Utilities & Bills" }!!
        }
        if (lower.contains("entertain") || lower.contains("movie") || lower.contains("netflix") || lower.contains("game")) {
            return allCategories.find { it.name == "Entertainment" }!!
        }
        if (lower.contains("transport") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("cab") || lower.contains("uber")) {
            return allCategories.find { it.name == "Transport & Fuel" }!!
        }
        if (lower.contains("health") || lower.contains("doctor") || lower.contains("medicin") || lower.contains("pharmacy")) {
            return allCategories.find { it.name == "Healthcare" }!!
        }
        return allCategories.find { it.name.equals(categoryName, ignoreCase = true) }
            ?: CategoryInfo(categoryName, Icons.Default.Receipt, Color(0xFF607D8B))
    }
}
