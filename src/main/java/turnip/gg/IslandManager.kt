package turnip.gg

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.api.reactive.RedisReactiveCommands
import reactor.core.publisher.Mono
import turnip.gg.exception.IslandFullException
import turnip.gg.exception.QueueFullException

class IslandManager(
        private val redis: RedisReactiveCommands<String, String>,
        island: Island,
        private val maxQueueSize: Int = 30, /*TODO: Make this configurable*/
        private val maxPlayersOnIsland: Int = 4 /* TODO: This too */
) : AutoCloseable {
    val islandKey = "islands:${island.id}"
    val queueKey = "queues:${island.id}"
    val playersKey = "players:${island.id}"
    private val mapper = ObjectMapper()

    fun push(player: Player): Mono<Long> = redis.llen(playersKey)
            .filter { it < maxPlayersOnIsland }
            .map { mapper.writeValueAsString(player) }
            .flatMap { redis.lpush(playersKey, it) }
            .switchIfEmpty(
                    redis.llen(queueKey)
                            .filter { it < maxQueueSize }
                            .map { mapper.writeValueAsString(player) }
                            .flatMap { redis.lpush(queueKey, it) }
                            .switchIfEmpty(Mono.error(QueueFullException()))
            )

    fun pop(): Mono<Player> = redis.llen(playersKey)
            .filter { it < maxPlayersOnIsland }
            .flatMap { redis.rpoplpush(queueKey, playersKey) }
            .map { mapper.readValue(it, Player::class.java) }
            .switchIfEmpty(Mono.error(IslandFullException()))

    fun removeFromQueue(player: Player): Mono<Long> = Mono.just(mapper)
            .map { mapper.writeValueAsString(player) }
            .flatMap { redis.lrem(queueKey, 0, it) }

    fun removeFromIsland(player: Player): Mono<Player> = Mono.just(mapper)
            .map { mapper.writeValueAsString(player) }
            .flatMap { redis.lrem(playersKey, 0, it) }
            .flatMap { pop() }

    fun signalJoin(): Mono<Player> {
        return Mono.empty() // TODO
    }

    // TODO: Probably shouldn't do this
    override fun close() {
        redis.hdel(islandKey)
                .then(redis.del(queueKey))
                .then(redis.del(playersKey))
                .block()
    }
}