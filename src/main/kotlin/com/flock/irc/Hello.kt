package com.flock.irc

import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.feature.auth.SaslPlain

//b9c5a695-0855-4c30-a4f4-c8da4c89cd53

fun main(args: Array<String>) {
    println("Hello, World")
    Main()
//    val client = Client.builder().nick("nick").serverHost("irc.freenode.net").build()
//    client.authManager.addProtocol(SaslPlain(client, "amansinghal", "password"))
//    client.connect()
//    client.setInputListener { it -> println(it) }
//    client.setOutputListener { it -> println(it) }
}
