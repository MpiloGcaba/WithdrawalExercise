package com.example.withdrawalExercise.advice

import com.example.withdrawalExercise.buildResponseEntity
import com.example.withdrawalExercise.dataResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
class DatabaseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InsufficientFundsException(message: String) : RuntimeException(message)
class AccountNotFoundException(message: String) : RuntimeException(message)
class SNSPublishingException(message: String) : RuntimeException(message)

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DatabaseException::class)
    fun handleDatabaseException(e: DatabaseException): dataResponse {
        return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR,"Internal Server Error")
    }

    @ExceptionHandler(InsufficientFundsException::class)
    fun handleInsufficientFundsException(e: InsufficientFundsException): dataResponse {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, "Insufficient funds")
    }

    @ExceptionHandler(AccountNotFoundException::class)
    fun handleAccountNotFoundException(e: AccountNotFoundException): dataResponse {
        return buildResponseEntity(HttpStatus.NOT_FOUND, "Account not found")
    }

    @ExceptionHandler(SNSPublishingException::class)
    fun handleSNSPublishingException(e: SNSPublishingException): dataResponse {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, "SNS Publishing error")
    }
}