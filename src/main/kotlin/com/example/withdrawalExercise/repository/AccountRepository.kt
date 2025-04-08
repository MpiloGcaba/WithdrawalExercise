package com.example.withdrawalExercise.repository

import java.math.BigDecimal

interface AccountRepository {
    suspend fun findBalanceById(accountId: Long): BigDecimal?
    suspend fun updateBalance(accountId: Long, amount: BigDecimal): Int
    suspend fun recordTransaction(accountId: Long, amount: BigDecimal, type: String): Long
}