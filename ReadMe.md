# Bank Account Controller Enhancement

## Comparison with Original Code

The original code issues:

1. **Unreachable Code**: The SNS notification code was placed after a return statement, making it unreachable.
2. **No Transaction Management**: Lacked proper transaction boundaries.
3. **Basic SQL Protection**: Had parameterized queries but no additional security measures.
4. **No Separation of Concerns**: Mixed database access with business logic.
5. **Hardcoded Configuration**: AWS configurations were hardcoded.
6. **No Error Handling**: Limited error handling for database operations.
7. **No Audit Trail**: No transaction recording for audit purposes.

> The business capability remains unchanged, but the implementation is now much more secure, maintainable, and robust.

---

## Approach Outline

I've transformed the original bank account withdrawal functionality with a focus on:

1. Converting Java to Kotlin
2. Enhancing security through proper SQL protection and transaction management
3. Improving architecture using the repository pattern
4. Adding dependency injection for better testability and reusability
5. Ensuring proper error handling and audit capabilities
6. Maintaining the core business capability of secure account withdrawals

> The fundamental business capability remains exactly the same:  
> Checking account balance, performing withdrawals only with sufficient funds, and notifying external systems upon
> successful transactions.

---

## Implementation Choices & Security Enhancements

### 1. Repository Pattern

**Choice**: Separated data access logic into a dedicated repository interface and implementation.

**Security Benefits**:

- Centralizes data access policies
- Limits SQL exposure throughout application
- Enables consistent application of security rules

```kotlin
interface AccountRepository {
    fun findBalanceById(accountId: Long): BigDecimal
    fun updateBalance(accountId: Long, amount: BigDecimal): Int
    fun recordTransaction(accountId: Long, amount: BigDecimal, type: String): Long
}
```

### 2. Transaction Management

**Choice**: Added comprehensive transaction annotations.

**Security Benefits**:

- Ensures data consistency with Isolation.REPEATABLE_READ
- Prevents concurrent modifications that could lead to inconsistent account states
- Guarantees atomic operations with proper rollback behavior

```kotlin
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.REPEATABLE_REA
)
```

### 3. Prepared Statements

**Choice**: Used explicit PreparedStatement creation instead of simple template usage.

**Security Benefits**:

- Prevents SQL injection by properly parameterizing queries
- Enables additional parameter validation
- Provides explicit control over statement preparation

```kotlin
jdbcTemplate.update { connection: Connection ->
    connection.prepareStatement(sql).apply {
        setLong(1, accountId)
        setBigDecimal(2, amount)
        setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
    }
}
```

### 4. Audit Trail

**Choice**: Added transaction logging and timestamp tracking.

**Security Benefits**:
- Creates verifiable history of all financial transactions
- Enables detection of suspicious activities
- Provides data for forensic analysis if needed

```kotlin
 override fun recordTransaction(accountId: Long, amount: BigDecimal, type: String): Long {
    val sql = "INSERT INTO transactions (account_id, amount, type, created_at) VALUES (?, ?, ?, ?)"
 }
```

### 5. Account Status Verification

**Choice**: Added active account verification in SQL queries.

**Security Benefits**:
- Prevents operations on inactive/frozen/suspended accounts
- Creates additional security gate beyond just balance checking
- Reduces attack surface for account manipulation
```kotlin
 val sql = "SELECT balance FROM accounts WHERE id = ? AND active = TRUE"
```

### 6. Configuration Externalization

**Choice**: Moved AWS credentials and endpoints to external configuration.

**Security Benefits**:
- Prevents hardcoded secrets in source code
- Enables environment-specific configurations
- Allows for centralized secret management

```kotlin
@Value("\${aws.region}")
private lateinit var awsRegion: String
```
### 7. Proper JSON Serialization

**Choice**: Used Gson for JSON handling instead of manual string formatting.

**Security Benefits**:
- Prevents JSON injection attacks through proper escaping
- Handles special characters and formatting edge cases securely
- Reduces risk of malformed messages

```kotlin
    val eventJson = gson.toJson(event)
```
### 8. Concurrency

**Choice**: Use kotlin's coroutine to make

**Security Benefit**:

### Library Usage Documentation
#### 1. AWS SDK
- SnsClient: Client for Amazon Simple Notification Service
- PublishRequest: Request object for publishing messages to SNS topics
- Region: AWS Region specification

#### 2. Gson
- Gson: Google's JSON serialization/deserialization library
- toJson(): Converts Java objects to JSON strings

#### 3. Spring Transaction Management
- @Transactional(readOnly = true): Optimizes read-only transactions
- @Transactional(propagation = Propagation.REQUIRED): Ensures operations run in a transaction
- @Transactional(isolation = Isolation.REPEATABLE_READ): Sets isolation level to prevent dirty reads
- rollbackFor = [Exception::class]: Defines which exceptions trigger rollback
#### 4. JDBC
- GeneratedKeyHolder: Holds auto-generated keys from database operations
- PreparedStatementCreator: Creates PreparedStatements with specific parameters
- Connection.prepareStatement(): Creates prepared statements for SQL execution

#### 5. Logging
- LoggerFactory.getLogger(): Creates SLF4J logger instances
- logger.info/error/warn: Different logging levels for appropriate events

#### 6. Coroutines
- withContext(Dispatchers.IO) is used to switch to the IO dispatcher for blocking operations (like database queries), ensuring that we don't block the main thread.




