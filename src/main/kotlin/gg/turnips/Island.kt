package gg.turnips

data class PartialIsland(
        val id: String,
        val hemisphere: Int,
        val islandName: String,
        val userName: String,
        val price: Int,
        val description: String
) {
    fun getData(): Map<String, String> {
        return mapOf(
                Pair("id", id),
                Pair("hemisphere", hemisphere.toString()),
                Pair("islandName", islandName),
                Pair("userName", userName),
                Pair("price", price.toString()),
                Pair("description", description)
        )
    }

    companion object {
        fun fromMap(map: Map<String, String>): PartialIsland {
            return PartialIsland(
                    map["id"] ?: error(""),
                    (map["hemisphere"] ?: error("")).toInt(),
                    map["islandName"] ?: error(""),
                    map["userName"] ?: error(""),
                    (map["price"] ?: error("")).toInt(),
                    map["description"] ?: error("")
            )
        }
    }
}


data class Island(
        val id: String,
        val code: String,
        val hemisphere: Int,
        val islandName: String,
        val userName: String,
        val price: Int,
        val description: String
) {
    fun getData(): Map<String, String> {
        return mapOf(
                Pair("id", id),
                Pair("code", code),
                Pair("hemisphere", hemisphere.toString()),
                Pair("islandName", islandName),
                Pair("userName", userName),
                Pair("price", price.toString()),
                Pair("description", description)
        )
    }

    companion object {
        fun fromMap(map: Map<String, String>): Island {
            return Island(
                    map["id"] ?: error(""),
                    map["code"] ?: error(""),
                    (map["hemisphere"] ?: error("")).toInt(),
                    map["islandName"] ?: error(""),
                    map["userName"] ?: error(""),
                    (map["price"] ?: error("")).toInt(),
                    map["description"] ?: error("")
            )
        }
    }
}