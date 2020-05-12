package turnip.gg

import com.fasterxml.jackson.databind.json.JsonMapper
import io.lettuce.core.RedisClient
import io.lettuce.core.api.reactive.RedisReactiveCommands
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.ConcurrentHashMap

@Configuration
class WebSocket {

    @Bean
    fun wsHandlerMapping(): HandlerMapping {
        val mapping = mapOf("/socket" to TurnipWebSocketHandler())
        val order = -1 // before annotated controllers

        return SimpleUrlHandlerMapping(mapping, order)
    }

    @Bean
    fun handlerAdapter() = WebSocketHandlerAdapter()
}

class TurnipWebSocketHandler : WebSocketHandler {

    val mapper = JsonMapper()
    val redis: RedisReactiveCommands<String, String> = RedisClient.create("redis://localhost:6379").connect().reactive()
    val islandManagers: MutableMap<String, IslandManager> = ConcurrentHashMap()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val input = session.receive()
                .map { it.payloadAsText }
                .flatMap { handleJson(session, it) }
                .then()

        return input
    }

    fun handleJson(session: WebSocketSession, rawText: String): Mono<Void> = Mono.fromSupplier {
        return@fromSupplier mapper.readTree(rawText)
    }.map { Pair(it.findValue("type").asText(), it) }
            .map {
                when(it.first) {
                    "CreateIsland" -> { // Add island to list
                        return@map mapper.treeToValue(it.second, CreateIsland::class.java)
                    }
                    "RemoveIsland" -> { // Remove island from list
                        return@map mapper.treeToValue(it.second, RemoveIsland::class.java)
                    }
                    "ListIslands" -> { // List all islands
                        return@map mapper.treeToValue(it.second, ListIslands::class.java)
                    }
                    "JoinQueue" -> { // User is queued to join an island
                        return@map mapper.treeToValue(it.second, JoinQueue::class.java)
                    }
                    else -> {
                        throw Exception()
                    }
                }
            }
            .flatMap {
                if (it is CreateIsland) {
                    require(
                            it.island.code.length == 5 &&
                                    it.island.islandName.isNotEmpty() &&
                                    it.island.userName.isNotEmpty() &&
                                    it.island.description.isNotEmpty()
                    )

                    return@flatMap redis.hmset("island:${it.island.id}", it.island.getData())
                            .doOnNext { _ -> islandManagers[it.island.id] = IslandManager(it.island) }
                            .flatMap { session.send(session.textMessage("Ok").toMono()) }
                } else if (it is RemoveIsland) {
                    return@flatMap redis.hdel("island:${it.islandId}")
                            .doOnNext { _ -> islandManagers.remove(it.islandId) }
                            .flatMap { session.send(session.textMessage("Ok").toMono()) }
                } else if (it is ListIslands) {
                    return@flatMap redis.keys("island:*")
                            .flatMap { redis.hgetall(it) }
                            .map { PartialIsland.fromMap(it) }
                            .collectList()
                            .map { mapper.writeValueAsString(it) }
                            .flatMap { session.send(session.textMessage(it).toMono()) }
                } else if (it is JoinQueue) {
                    // Join queue, tell the manager that the queue has had someone join, when there is an opening get
                    // the code and send it to the user
                    return@flatMap redis.rpush("islandQueue:${it.islandId}", it.userId)
                            .flatMap { _ -> islandManagers[it.islandId]!!.signalJoin() }
                            .flatMap { _ -> redis.hget("island:${it.islandId}", "code") }
                            .flatMap { session.send(session.textMessage(it).toMono()) }
                } else {
                    return@flatMap Mono.error(Exception())
                }
            }
            .then()
}