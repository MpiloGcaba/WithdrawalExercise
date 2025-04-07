package com.example.withdrawalExercise.models

import java.math.BigDecimal

data class WithdrawalEvent(
    val amount: BigDecimal,
    val accountId: Long,
    val status: WithdrawalStatus,
)
