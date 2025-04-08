package com.example.withdrawalExercise.controllers

import com.example.withdrawalExercise.advice.SNSPublishingException
import com.example.withdrawalExercise.buildResponseEntity
import com.example.withdrawalExercise.dataResponse
import com.example.withdrawalExercise.model.WithdrawalEvent
import com.example.withdrawalExercise.dtos.WithdrawalResponseDTO
import com.example.withdrawalExercise.model.WithdrawalStatus
import com.example.withdrawalExercise.repository.AccountRepository
import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import java.math.BigDecimal

@RestController
@RequestMapping("/bank")
class BankAccountController @Autowired constructor(
    private val accountRepository: AccountRepository,
    private val snsClient: SnsClient,
    private val withdrawalTopicArn: String,
    private val gson: Gson
) {

    private val logger: Logger = LoggerFactory.getLogger(BankAccountController::class.java)

    @PostMapping("/withdraw")
    suspend fun withdraw(
        @RequestParam("accountId") accountId: Long,
        @RequestParam("amount") amount: BigDecimal
    ): dataResponse {

        if (amount <= BigDecimal.ZERO) {
            logger.warn("Invalid withdrawal amount: $amount for accountId=$accountId")
            return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                "Invalid withdrawal amount",
                "Bad Request"
            )
        }

        val currentBalance = accountRepository.findBalanceById(accountId)
        logger.info("Current balance for accountId=$accountId: $currentBalance")

        val transactionId = accountRepository.recordTransaction(accountId, amount, "WITHDRAWAL")
        logger.info("Transaction recorded with ID=$transactionId for accountId=$accountId")

        return if (currentBalance != null) {
            when {
                currentBalance >= amount -> handleWithdrawal(accountId, amount, transactionId)
                else -> {
                    publishWithdrawalEvent(accountId, amount, WithdrawalStatus.INSUFFICIENT_FUNDS, transactionId)
                    buildResponseEntity(HttpStatus.OK, "Insufficient funds for withdrawal")
                }
            }
        } else {
            buildResponseEntity(HttpStatus.OK, "Insufficient funds for withdrawal")
        }
    }

    private suspend fun handleWithdrawal(
        accountId: Long,
        amount: BigDecimal,
        transactionId: Long
    ): dataResponse {

        logger.info("Handling withdrawal for accountId=$accountId, amount=$amount, transactionId=$transactionId")
        val rowsAffected = accountRepository.updateBalance(accountId, amount)

        return if (rowsAffected > 0) {
            try {
                publishWithdrawalEvent(accountId, amount, WithdrawalStatus.SUCCESSFUL, transactionId)
                ResponseEntity.status(HttpStatus.OK)
                    .body(WithdrawalResponseDTO(body = "Withdrawal successful"))
            } catch (e: Exception) {
                logger.error("Withdrawal successful, but SNS publishing failed for accountId=$accountId", e)
                buildResponseEntity(
                    HttpStatus.OK,
                    "Withdrawal successful, but SNS publishing failed",
                    "failed to publish event"
                )
            }
        } else {
            logger.error("Failed to update account balance for accountId=$accountId during withdrawal")
            buildResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Withdrawal failed: Could not update account",
                "Failed to update balance"
            )
        }
    }

    private fun publishWithdrawalEvent(
        accountId: Long,
        amount: BigDecimal,
        status: WithdrawalStatus,
        transactionId: Long
    ) {

        val event = WithdrawalEvent(amount, accountId, status, transactionId)
        val eventJson = gson.toJson(event)

        val publishRequest = PublishRequest.builder()
            .message(eventJson)
            .topicArn(withdrawalTopicArn)
            .build()

        try {
            snsClient.publish(publishRequest)
        } catch (e: Exception) {
            throw SNSPublishingException("SNS Publishing error")
        }
    }
}
