import com.example.withdrawalExercise.models.WithdrawalEvent
import com.example.withdrawalExercise.models.WithdrawalResponseDTO
import com.example.withdrawalExercise.models.WithdrawalStatus
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import java.math.BigDecimal

@RestController
@RequestMapping("/bank")
class BankAccountController @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
    private val snsClient: SnsClient,
    @Value("\${aws.sns.withdrawal-topic-arn}") private val withdrawalTopicArn: String,
    private val gson: Gson
) {

    @PostMapping("/withdraw")
    fun withdraw(
        @RequestParam("accountId") accountId: Long,
        @RequestParam("amount") amount: BigDecimal
    ): ResponseEntity<WithdrawalResponseDTO> {
        // Check current balance
        val sql = "SELECT balance FROM accounts WHERE id = ?"
        val currentBalance = jdbcTemplate.queryForObject(sql, arrayOf(accountId), BigDecimal::class.java)

        return if (currentBalance != null && currentBalance >= amount) {
            // Update balance
            val updateSql = "UPDATE accounts SET balance = balance - ? WHERE id = ?"
            val rowsAffected = jdbcTemplate.update(updateSql, amount, accountId)

            if (rowsAffected > 0) {
                // After a successful withdrawal, publish a withdrawal event to SNS
                publishWithdrawalEvent(accountId, amount, WithdrawalStatus.SUCCESSFUL)
                ResponseEntity.status(HttpStatus.OK)
                    .body(WithdrawalResponseDTO(body = "Withdrawal successful"))
            } else {
                // In case the update fails for reasons other than a balance check
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WithdrawalResponseDTO(body = "Withdrawal failed"))
            }
        } else {
            // Insufficient funds
            ResponseEntity.status(HttpStatus.OK)
                .body(WithdrawalResponseDTO(body = "Insufficient funds for withdrawal"))
        }
    }

    private fun publishWithdrawalEvent(
        accountId: Long,
        amount: BigDecimal,
        status: WithdrawalStatus
    ) {

        val event = WithdrawalEvent(amount, accountId, status)
        val eventJson = gson.toJson(event)

        val publishRequest = PublishRequest.builder()
            .message(eventJson)
            .topicArn(withdrawalTopicArn)
            .build()


        snsClient.publish(publishRequest)

    }
}
