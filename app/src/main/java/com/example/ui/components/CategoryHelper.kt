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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object CategoryHelper {

    val allCategories = listOf(
        CategoryInfo("Groceries", Icons.Default.ShoppingCart, Color(0xFF2E7D32)),
        CategoryInfo("Dining & Food", Icons.Default.Restaurant, Color(0xFFEF6C00)),
        CategoryInfo("Utilities & Bills", Icons.Default.ElectricalServices, Color(0xFF1565C0)),
        CategoryInfo("Transport & Fuel", Icons.Default.DirectionsCar, Color(0xFF6A1B9A)),
        CategoryInfo("Entertainment", Icons.Default.Movie, Color(0xFFC2185B)),
        CategoryInfo("Kids & Education", Icons.Default.School, Color(0xFF00838F)),
        CategoryInfo("Healthcare", Icons.Default.LocalHospital, Color(0xFFD84315)),
        CategoryInfo("Shopping", Icons.Default.Checkroom, Color(0xFF4E342E)),
        CategoryInfo("Maintenance", Icons.Default.Build, Color(0xFF455A64)),
        CategoryInfo("Other Expenses", Icons.Default.Receipt, Color(0xFF37474F))
    )

    fun getCategoryInfo(categoryName: String): CategoryInfo {
        return allCategories.find { it.name.equals(categoryName, ignoreCase = true) }
            ?: CategoryInfo(categoryName, Icons.Default.Receipt, Color(0xFF3F51B5))
    }
}
