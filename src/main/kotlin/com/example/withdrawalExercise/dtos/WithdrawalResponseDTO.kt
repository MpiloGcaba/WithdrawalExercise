package com.example.withdrawalExercise.dtos

data class WithdrawalResponseDTO(
    val body: String,
    val error: String? = null
)