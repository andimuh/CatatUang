package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CatFood
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.util.CurrencyFormatter

@Composable
fun DailyExpenseBarChart(
    dailyExpenses: Map<Int, Double>,
    maxExpense: Double,
    maxExpenseDay: Int?,
    selectedMonthName: String,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    val daysCount = dailyExpenses.keys.maxOrNull() ?: 30

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_expense_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tren Pengeluaran Harian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$selectedMonthName • Sentuh batang untuk melihat detail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (maxExpenseDay != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Puncak: Tgl $maxExpenseDay",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Day Info Banner
            if (selectedDay != null) {
                val amount = dailyExpenses[selectedDay] ?: 0.0
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tanggal $selectedDay $selectedMonthName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(amount),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (amount > 0) ExpenseRed else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (maxExpense <= 0) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada catatan pengeluaran di bulan ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Bar chart container with scroll
                val scrollState = rememberScrollState()

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Maximum indicator
                    Text(
                        text = "Maks: ${CurrencyFormatter.formatRupiah(maxExpense)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        // Guide line at 50%
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .align(Alignment.Center)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        // Scrollable bars
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            for (day in 1..daysCount) {
                                val amount = dailyExpenses[day] ?: 0.0
                                val ratio = if (maxExpense > 0) (amount / maxExpense).toFloat() else 0f
                                val isSelected = selectedDay == day
                                val isMaxDay = maxExpenseDay == day && amount > 0

                                val animatedHeight by animateFloatAsState(
                                    targetValue = ratio.coerceIn(0.04f, 1f),
                                    animationSpec = tween(durationMillis = 400),
                                    label = "barHeight_$day"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedDay = if (selectedDay == day) null else day
                                        },
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    // Bar pill
                                    val brush = when {
                                        isMaxDay -> Brush.verticalGradient(
                                            listOf(ExpenseRed, Color(0xFFFB7185))
                                        )
                                        isSelected -> Brush.verticalGradient(
                                            listOf(EmeraldPrimary, Color(0xFF34D399))
                                        )
                                        amount > 0 -> Brush.verticalGradient(
                                            listOf(Color(0xFF0D9488), Color(0xFF5EEAD4))
                                        )
                                        else -> Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(if (isSelected) 18.dp else 14.dp)
                                            .fillMaxHeight(fraction = animatedHeight)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                            .background(brush)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Day Number Label
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = if (isSelected || isMaxDay) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> EmeraldPrimary
                                            isMaxDay -> ExpenseRed
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
