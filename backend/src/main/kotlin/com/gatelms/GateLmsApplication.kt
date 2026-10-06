package com.gatelms

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GateLmsApplication

fun main(args: Array<String>) {
    runApplication<GateLmsApplication>(*args)
}
