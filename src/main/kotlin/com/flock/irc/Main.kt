package com.flock.irc

fun main(args: Array<String>) {
    println("Hello, World")
    val redisHostName = System.getenv("REDIS_HOSTNAME")
    val store = if (redisHostName == null) {
        Store()
    } else {
        JedisStore(redisHostName)
    }
    App(store, System.getenv("APP_ID"), System.getenv("APP_SECRET"),
            System.getenv("WEBHOOK_TOKEN"), System.getenv("PORT").toInt())
}
