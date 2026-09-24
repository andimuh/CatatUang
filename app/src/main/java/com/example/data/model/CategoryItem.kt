package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

data class CategoryItem(
    val id: String,
    val name: String,
    val type: TransactionType,
    val icon: ImageVector,
    val color: Color
)

object CategoryRegistry {
    val expenseCategories = listOf(
        CategoryItem("food", "Makanan & Minuman", TransactionType.EXPENSE, Icons.Default.Restaurant, CatFood),
        CategoryItem("transport", "Transportasi", TransactionType.EXPENSE, Icons.Default.DirectionsCar, CatTransport),
        CategoryItem("shopping", "Belanja & Kebutuhan", TransactionType.EXPENSE, Icons.Default.ShoppingCart, CatShopping),
        CategoryItem("bills", "Tagihan & Listrik", TransactionType.EXPENSE, Icons.Default.Bolt, CatBills),
        CategoryItem("entertainment", "Hiburan & Rekreasi", TransactionType.EXPENSE, Icons.Default.Movie, CatEntertainment),
        CategoryItem("health", "Kesehatan & Medis", TransactionType.EXPENSE, Icons.Default.LocalHospital, CatHealth),
        CategoryItem("education", "Pendidikan", TransactionType.EXPENSE, Icons.Default.School, CatEducation),
        CategoryItem("family", "Keluarga & Rumah", TransactionType.EXPENSE, Icons.Default.FamilyRestroom, CatFamily),
        CategoryItem("other_expense", "Lain-lain", TransactionType.EXPENSE, Icons.Default.MoreHoriz, CatOther)
    )

    val incomeCategories = listOf(
        CategoryItem("salary", "Gaji Pokok", TransactionType.INCOME, Icons.Default.Payments, CatSalary),
        CategoryItem("freelance", "Freelance / Proyek", TransactionType.INCOME, Icons.Default.LaptopMac, CatFreelance),
        CategoryItem("business", "Hasil Usaha / Toko", TransactionType.INCOME, Icons.Default.Storefront, CatFamily),
        CategoryItem("investment", "Investasi & Dividen", TransactionType.INCOME, Icons.Default.TrendingUp, CatInvestment),
        CategoryItem("bonus", "Bonus & Hadiah", TransactionType.INCOME, Icons.Default.CardGiftcard, CatBonus),
        CategoryItem("other_income", "Pemasukan Lain", TransactionType.INCOME, Icons.Default.AccountBalanceWallet, CatOther)
    )

    fun getCategory(name: String, type: TransactionType): CategoryItem {
        val list = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
        return list.firstOrNull { it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true) }
            ?: CategoryItem(
                id = "custom",
                name = name,
                type = type,
                icon = if (type == TransactionType.EXPENSE) Icons.Default.ShoppingCart else Icons.Default.Payments,
                color = if (type == TransactionType.EXPENSE) CatFood else CatSalary
            )
    }
}
