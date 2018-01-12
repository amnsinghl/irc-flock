package com.flock.irc

import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.feature.auth.SaslPlain
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress

//b9c5a695-0855-4c30-a4f4-c8da4c89cd53

fun main(args: Array<String>) {
    println("Hello, World")
    Main()
//    System.setProperty("socksProxyHost", "localhost")
//    System.setProperty("socksProxyPort", "9050")
//    System.out.println(InetAddress.getByName("127.0.0.2").hostName);
//    val client = Client.builder().nick("nicki").serverHost("irc.freenode.net").build()
////    val client = Client.builder().nick("nicki").serverHost("freenodeok2gncmy.onion").build()
//    client.authManager.addProtocol(SaslPlain(client, "amansinghal", "password"))
//    client.connect()
//    client.setInputListener { it -> println(it) }
//    client.setOutputListener { it -> println(it) }
}
