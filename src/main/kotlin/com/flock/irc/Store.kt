package com.flock.irc

import redis.clients.jedis.Jedis

open class Store {
    private val map: MutableMap<String, String> = HashMap()
    open operator fun get(key: String) = map[key]

    open operator fun set(key: String, value: String) {
        map[key] = value
    }

    open fun exists(key: String) = map.contains(key)

    open fun ping() = "pong"
}

class JedisStore(private val address: String) : Store() {
    private val jedis = Jedis(address)

    override operator fun get(key: String): String = jedis[key]

    override fun ping(): String {
        return jedis.ping()
    }

    override fun exists(key: String): Boolean {
        return jedis.exists(key)
    }

    override operator fun set(key: String, value: String) {
        jedis[key] = value
    }
}