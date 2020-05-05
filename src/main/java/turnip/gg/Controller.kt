package turnip.gg

import com.fasterxml.jackson.databind.json.JsonMapper
import io.lettuce.core.RedisClient
import io.lettuce.core.api.reactive.RedisReactiveCommands
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@Controller
@RestController
class Controller {
    val redis: RedisReactiveCommands<String, String> = RedisClient.create("redis://localhost:6379").connect().reactive()

    @RequestMapping("/verify")
    fun verifyDodoCode(code: String): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }

    @MessageMapping("/create")
    @SendTo("/islands/create")
    fun createIsland(island: Island): Mono<Island> {
        require(
                island.code.length == 5 &&
                        island.islandName.isNotEmpty() &&
                        island.userName.isNotEmpty() &&
                        island.description.isNotEmpty()
        )

        return Mono.fromCallable { JsonMapper().writeValueAsString(island) }
                .flatMap { redis.hmset("island:${island.id}", island.getData()) }
                .thenReturn(Island(
                        island.id,
                        "",
                        island.hemisphere,
                        island.islandName,
                        island.userName,
                        island.price,
                        island.description
                ))
    }
}