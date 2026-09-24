package com.example.data.model

enum class WalletType(val displayName: String, val code: String) {
    CASH("Tunai / Cash", "CASH"),
    BANK("Rekening Bank", "BANK"),
    E_WALLET("Dompet Digital (E-Wallet)", "E_WALLET");

    companion object {
        fun fromCode(code: String): WalletType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.displayName.equals(code, ignoreCase = true) }
                ?: CASH
        }
    }
}
