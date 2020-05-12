package turnip.gg

interface Payload {
    val type: String
}

data class CreateIsland(override val type: String,
                        val island: Island) : Payload

data class RemoveIsland(override val type: String,
                        val islandId: String) : Payload

data class ListIslands(override val type: String) : Payload

data class JoinQueue(override val type: String,
                     val userId: String,
                     val islandId: String) : Payload