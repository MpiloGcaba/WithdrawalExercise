import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import java.math.BigDecimal

@RestController
@RequestMapping("/bank")
class BankAccountController @Autowired constructor(private val jdbcTemplate: JdbcTemplate) {

    private val snsClient: SnsClient = SnsClient.builder()
        .region(Region.YOUR_REGION) // Specify your region
        .build()

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
                val event = WithdrawalEvent(amount, accountId, "SUCCESSFUL")
                val eventJson = event.toJson() // Convert event to JSON
                val snsTopicArn = "arn:aws:sns:YOUR_REGION:YOUR_ACCOUNT_ID:YOUR_TOPIC_NAME"
                val publishRequest = PublishRequest.builder()
                    .message(eventJson)
                    .topicArn(snsTopicArn)
                    .build()

                snsClient.publish(publishRequest)
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
}

data class WithdrawalEvent(
    val amount: BigDecimal,
    val accountId: Long,
    val status: String
) {
    // Convert to JSON String
    fun toJson(): String {
        return "{\"amount\":\"$amount\",\"accountId\":$accountId,\"status\":\"$status\"}"
    }
}
