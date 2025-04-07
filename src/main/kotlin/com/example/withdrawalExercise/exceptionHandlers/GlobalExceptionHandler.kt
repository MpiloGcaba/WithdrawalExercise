package com.example.withdrawalExercise.exceptionHandlers

import com.example.withdrawalExercise.models.WithdrawalResponseDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

class DatabaseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InsufficientFundsException(message: String) : RuntimeException(message)
class AccountNotFoundException(message: String) : RuntimeException(message)


typealias dataResponse = ResponseEntity<WithdrawalResponseDTO>

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

    @ExceptionHandler(Exception::class)
    fun handleException(e: AccountNotFoundException): dataResponse {
        return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown exception")
    }

    private fun buildResponseEntity(
        httpStatus: HttpStatus,
        bodyMessage: String,
        errorMessage: String? = null
    ): dataResponse {
        return ResponseEntity(WithdrawalResponseDTO(body = bodyMessage, error = errorMessage), httpStatus)
    }
}