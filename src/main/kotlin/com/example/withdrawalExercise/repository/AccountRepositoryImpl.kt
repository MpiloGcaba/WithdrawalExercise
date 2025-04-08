package com.example.withdrawalExercise.repository

import com.example.withdrawalExercise.advice.AccountNotFoundException
import com.example.withdrawalExercise.advice.DatabaseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class AccountRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate
) : AccountRepository {

    private val logger: Logger = LoggerFactory.getLogger(AccountRepositoryImpl::class.java)

    @Transactional(readOnly = true)
    override suspend fun findBalanceById(accountId: Long): BigDecimal? {
        val sql = "SELECT balance FROM accounts WHERE id = ? AND active = TRUE"
        return try {
            withContext(Dispatchers.IO) {
                jdbcTemplate.queryForObject(sql, BigDecimal::class.java, accountId)
            }
        } catch (e: Exception) {
            logger.error("Account not found or inactive for accountId=$accountId", e)
            throw AccountNotFoundException("Account not found or inactive for accountId=$accountId")
        } catch (e: DataAccessException) {
            logger.error("Error accessing database while fetching balance for accountId=$accountId", e)
            throw DatabaseException("Error accessing database while fetching balance for accountId=$accountId", e)
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun updateBalance(accountId: Long, amount: BigDecimal): Int {
        val sql =
            "UPDATE accounts SET balance = balance - ?, last_updated = ? WHERE id = ? AND balance >= ? AND active = TRUE"
        return try {
            withContext(Dispatchers.IO) {
                jdbcTemplate.update { connection: Connection ->
                    connection.prepareStatement(sql).apply {
                        setLong(1, accountId)
                        setBigDecimal(2, amount)
                        setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
                    }
                }
            }
        } catch (e: DataAccessException) {
            logger.error("Error updating balance for accountId=$accountId", e)
            throw DatabaseException("Error updating balance for accountId=$accountId", e)
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun recordTransaction(accountId: Long, amount: BigDecimal, type: String): Long {
        val sql = "INSERT INTO transactions (account_id, amount, type, created_at) VALUES (?, ?, ?, ?)"
        val keyHolder = GeneratedKeyHolder()
        return try {
            withContext(Dispatchers.IO) {
                jdbcTemplate.update(
                    { connection: Connection ->
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).apply {
                            setLong(1, accountId)
                            setBigDecimal(2, amount)
                            setString(3, type)
                            setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()))
                        }
                    },
                    keyHolder
                )
            }
            keyHolder.key?.toLong() ?: throw DatabaseException("Failed to generate transaction ID")
        } catch (e: DataAccessException) {
            logger.error("Error recording transaction for accountId=$accountId", e)
            throw DatabaseException("Error recording transaction for accountId=$accountId", e)
        }
    }
}