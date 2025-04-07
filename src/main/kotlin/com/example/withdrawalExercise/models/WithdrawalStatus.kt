package com.example.withdrawalExercise.models

enum class WithdrawalStatus(private val status: String) {
    SUCCESSFUL("successful"),
    INSUFFICIENT_FUNDS("insufficient funds"),;

    override fun toString(): String {
        return status
    }
}