package turnip.gg

import com.fasterxml.jackson.databind.json.JsonMapper
import io.lettuce.core.RedisClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.ConcurrentHashMap
import kotlin.RuntimeException

@Configuration
class WebSocket {
    @Autowired
    @Qualifier("TurnipWebSocketHandler")
    private lateinit var webSocketHandler: WebSocketHandler

    @Bean
    fun wsHandlerMapping() = SimpleUrlHandlerMapping(mapOf("/socket" to TurnipWebSocketHandler()), 1)

    @Bean
    fun handlerAdapter() = WebSocketHandlerAdapter()
}

@Component("TurnipWebSocketHandler")
class TurnipWebSocketHandler : WebSocketHandler {
    private val mapper = JsonMapper()
    private val redis = RedisClient.create("redis://localhost:6379").connect().reactive()
    private val islandManagers: MutableMap<String, IslandManager> = ConcurrentHashMap()

    override fun handle(session: WebSocketSession): Mono<Void> = session.receive()
            .map { it.payloadAsText }
            .flatMap { handleJson(session, it) }
            .then()

    private fun handleJson(session: WebSocketSession, rawText: String): Mono<Void> = Mono.fromSupplier {
        return@fromSupplier mapper.readTree(rawText)
    }.map { Pair(it.findValue("type").asText(), it) }
            .map {
                when (it.first) {
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
            .flatMap { payload ->
                when (payload) {
                    is CreateIsland -> {
                        require(payload.island.code.length == 5) { "Dodo code must be 5 characters" }
                        require(payload.island.islandName.isNotEmpty()) { "Island name is empty" }
                        require(payload.island.userName.isNotEmpty()) { "Host username is empty" }
                        require(payload.island.description.isNotEmpty()) { "Island description is empty" }

                        return@flatMap redis.hmset("island:${payload.island.id}", payload.island.getData())
                                .doOnNext { islandManagers[payload.island.id] = IslandManager(redis, payload.island) }
                                .flatMap { session.send(session.textMessage("Ok").toMono()) }
                    }
                    is RemoveIsland -> {
                        return@flatMap islandManagers[payload.islandId]!!.close()
                                .flatMap { session.send(session.textMessage("OK").toMono()) }
                    }
                    is ListIslands -> {
                        return@flatMap redis.keys("island:*")
                                .flatMap { redis.hgetall(it) }
                                .map { PartialIsland.fromMap(it) }
                                .collectList()
                                .map { mapper.writeValueAsString(it) }
                                .flatMap { session.send(session.textMessage(it).toMono()) }
                    }
                    is JoinQueue -> {
                        return@flatMap islandManagers[payload.islandId]!!.push(Player(payload.userId))
                                .flatMap { islandManagers[payload.islandId]!!.signalJoin() }
                                .flatMap { redis.hget("island:${payload.islandId}", "code") }
                                .flatMap { session.send(session.textMessage(it).toMono()) }
                    }
                    else -> {
                        return@flatMap Mono.error<RuntimeException>(RuntimeException())
                    }
                }
            }
            .then()
}