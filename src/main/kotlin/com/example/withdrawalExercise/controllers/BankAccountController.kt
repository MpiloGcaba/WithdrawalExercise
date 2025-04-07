import com.example.withdrawalExercise.models.WithdrawalEvent
import com.example.withdrawalExercise.models.WithdrawalStatus
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    fun withdraw(@RequestParam("accountId") accountId: Long, @RequestParam("amount") amount: BigDecimal): String {
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
                "Withdrawal successful"
            } else {
                // In case the update fails for reasons other than a balance check
                "Withdrawal failed"
            }
        } else {
            // Insufficient funds
            "Insufficient funds for withdrawal"
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
