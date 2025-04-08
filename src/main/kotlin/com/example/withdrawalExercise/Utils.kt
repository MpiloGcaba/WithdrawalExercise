package com.example.withdrawalExercise

import com.example.withdrawalExercise.dtos.WithdrawalResponseDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

typealias dataResponse = ResponseEntity<WithdrawalResponseDTO>

fun buildResponseEntity(
    httpStatus: HttpStatus,
    responseBody: String,
    errorMessage: String? = null
): dataResponse {
    return ResponseEntity(WithdrawalResponseDTO(body = responseBody, error = errorMessage), httpStatus)
}