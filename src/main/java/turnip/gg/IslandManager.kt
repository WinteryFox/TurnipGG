package turnip.gg

import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

class IslandManager(val island: Island,
                    val capacity: Int = 10 /*TODO: Make this configurable*/) {

    val currentPlayers = AtomicInteger(0)

    fun signalJoin(): Mono<Void> {  // When the mono completes, the user is allowed to join

    }
}