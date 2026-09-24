package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DailyExpenseBarChart
import com.example.ui.components.MonthlyCategoryPieChart
import com.example.ui.theme.*
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateUtils
import com.example.ui.viewmodel.MonthlyAnalytics
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAnalyticsScreen(
    monthlyAnalytics: MonthlyAnalytics,
    selectedYear: Int,
    selectedMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = DateUtils.formatMonthYear(selectedYear, selectedMonth)
    val now = Calendar.getInstance()
    val isCurrentMonth = selectedYear == now.get(Calendar.YEAR) && selectedMonth == now.get(Calendar.MONTH)

    Scaffold(
        modifier = modifier.testTag("monthly_analytics_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Grafik & Analisis Finansial",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Laporan Pengeluaran Bulanan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (!isCurrentMonth) {
                        TextButton(onClick = onResetToCurrentMonth) {
                            Text("Bulan Ini", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Month Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousMonth,
                            modifier = Modifier.testTag("btn_prev_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Bulan Sebelumnya")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCurrentMonth) "Periode Berjalan" else "Arsip Bulanan",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrentMonth) EmeraldPrimary else MaterialTheme.colorScheme.outline
                            )
                        }

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier.testTag("btn_next_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Bulan Berikutnya")
                        }
                    }
                }
            }

            // Month Overview Summary Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Pengeluaran
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_expense"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(ExpenseRed.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Pengeluaran",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = CurrencyFormatter.formatRupiah(monthlyAnalytics.totalExpense),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rata-rata: ${CurrencyFormatter.formatRupiah(monthlyAnalytics.averageDailyExpense)}/hari",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Total Pemasukan
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_income"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(IncomeGreen.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Pemasukan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = CurrencyFormatter.formatRupiah(monthlyAnalytics.totalIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val net = monthlyAnalytics.netBalance
                            Text(
                                text = "Net: ${if (net >= 0) "+" else ""}${CurrencyFormatter.formatRupiah(net)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (net >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // 1. Daily Expense Bar Chart Component
            item {
                DailyExpenseBarChart(
                    dailyExpenses = monthlyAnalytics.dailyExpenses,
                    maxExpense = monthlyAnalytics.maxDailyExpense,
                    maxExpenseDay = monthlyAnalytics.maxExpenseDay,
                    selectedMonthName = monthName
                )
            }

            // 2. Category Pie / Donut Chart Component
            item {
                MonthlyCategoryPieChart(
                    categorySummaries = monthlyAnalytics.categoryBreakdown,
                    totalExpense = monthlyAnalytics.totalExpense
                )
            }

            // 3. Financial Insight & Health Card
            item {
                val expense = monthlyAnalytics.totalExpense
                val income = monthlyAnalytics.totalIncome
                val ratio = if (income > 0) (expense / income) * 100 else 0.0
                val isHealthy = income > 0 && ratio <= 70.0
                val isWarning = income > 0 && ratio > 90.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHealthy) MintLight.copy(alpha = 0.5f) else Color(0xFFFEF2F2)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isHealthy) EmeraldPrimary else ExpenseRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isHealthy) Icons.Default.Savings else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isHealthy) "Kondisi Finansial: Sehat" else if (isWarning) "Peringatan: Pengeluaran Tinggi" else "Evaluasi Arus Kas",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHealthy) EmeraldPrimaryVariant else ExpenseRed
                                )
                                Text(
                                    text = if (income > 0) "Rasio Belanja: ${String.format("%.1f", ratio)}% dari pemasukan" else "Belum tercatat pemasukan bulan ini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val topCategory = monthlyAnalytics.categoryBreakdown.firstOrNull()
                        val advice = when {
                            income == 0.0 && expense > 0.0 -> "Pastikan mencatat sumber pemasukan Anda agar perbandingan arus kas tetap seimbang."
                            isHealthy -> "Bagus sekali! Rasio pengeluaran Anda masih terkendali di bawah 70%. Alokasikan sisa dana Rp ${CurrencyFormatter.formatRupiah(monthlyAnalytics.netBalance, false)} untuk tabungan atau investasi."
                            isWarning -> "Pengeluaran Anda telah mendekati atau melebihi pemasukan bulan ini. Periksa kembali pos pengeluaran sekunder."
                            else -> "Kondisi keuangan cukup stabil. Pertahankan kontrol harian terhadap pos belanja non-primer."
                        }

                        Text(
                            text = advice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (topCategory != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 Pos pengeluaran terbesar adalah ${topCategory.category.name} (${CurrencyFormatter.formatRupiah(topCategory.totalAmount)}, ${String.format("%.1f", topCategory.percentage)}% dari total pengeluaran).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
