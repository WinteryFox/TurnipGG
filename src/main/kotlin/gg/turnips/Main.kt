package gg.turnips

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Turnip

fun main(args: Array<String>) {
    runApplication<Turnip>(*args)
}