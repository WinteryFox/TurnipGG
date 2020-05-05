package turnip.gg

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.stereotype.Component

@Component
class Redis(
    private val factory: ReactiveRedisConnectionFactory,
    private val operations: ReactiveRedisOperations<String, String>
)