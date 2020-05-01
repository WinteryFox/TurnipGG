package turnip.gg

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller

@Controller
class Controller {
    @MessageMapping("/create")
    @SendTo("/islands/create")
    fun createIsland(island: Island): Island {
        return island
    }
}