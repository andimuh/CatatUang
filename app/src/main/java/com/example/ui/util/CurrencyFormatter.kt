package com.example.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    private val indonesianSymbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val formatter = DecimalFormat("#,###", indonesianSymbols)

    fun formatRupiah(amount: Double, withPrefix: Boolean = true): String {
        val formatted = formatter.format(amount)
        return if (withPrefix) "Rp $formatted" else formatted
    }

    fun parseAmount(text: String): Double? {
        val clean = text.replace("[^0-9]".toRegex(), "")
        return clean.toDoubleOrNull()
    }
}
