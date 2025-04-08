package com.example.withdrawalExercise.model

import java.math.BigDecimal

data class WithdrawalEvent(
    val amount: BigDecimal,
    val accountId: Long,
    val status: WithdrawalStatus,
    val transactionId: Long
)
