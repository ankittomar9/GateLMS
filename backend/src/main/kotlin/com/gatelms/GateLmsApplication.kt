package com.gatelms

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GateLmsApplication

fun main(args: Array<String>) {
    runApplication<GateLmsApplication>(*args)
}
