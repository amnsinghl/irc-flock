package com.flock.irc

import com.flock.EventHandlerClient
import com.flock.model.ToastMessage
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.feature.auth.SaslEcdsaNist256PChallenge
import org.kitteh.irc.client.library.feature.auth.SaslPlain
import redis.clients.jedis.Jedis
import spark.Spark
import java.lang.Exception
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Main {
    private val store: Jedis = Jedis("127.0.0.1")
    private val eventHandler: EventHandlerClient = EventHandlerClient("05bf2a80-19e8-46bc-84de-62c24e667836", "b9c5a695-0855-4c30-a4f4-c8da4c89cd53")
    private val ircClientMap: Map<String, Client> = HashMap()

    init {
        println("ping " + store.ping())
        startListeningToEvents()
    }

    fun startListeningToEvents() {
        eventHandler.setClientSlashCommandListener { it ->
            println(it.command + " " + it.text)
            val commandSplit = it.text.split(delimiters = " ", limit = 2)
            val command = commandSplit[0]
            val text = commandSplit[1]
            when (command) {
                "login" -> {
                    val split = text.split(" ")
                    register(it.userId, split[0], split[1])
                    ToastMessage().text("Login successful")
                }
                "sendmessage" -> {

                    ToastMessage()
                }
//                "setChannel" -> {
//
//                }
                else -> {
                    ToastMessage().text("Unknown command")
                }
            }
        }
        eventHandler.setAppInstallListener { it ->
            store[it.userId + "token"] = it.token
        }
        Spark.port(4567)
        Spark.post("/event") { req, res ->
            Thread.sleep(2000)
            println("event received")
            try {
                eventHandler.handleRequest(req.raw(), res.raw())
            } catch (e: Exception) {
                ToastMessage().text(e.message)
            }
        }
    }

    fun setChannel(ircChannelName: String, flockChannelId: String) {

    }

//    fun getClient() {
//        if(ir)
//    }

    fun register(userId: String, userName: String, password: String) {
        val ircClient = IrcClient(userId, store)
//        ircClient.setUserNamePass(userName, password)
        ircClient.connect().onSuccess {
            ircClient.setPublicKey()
        }
    }
}


fun <U> ListenableFuture<U>.onSuccess(runnable: (U) -> Unit): ListenableFuture<U> =
        this.apply { addCallback(success = runnable) }

fun <U> ListenableFuture<U>.onFailure(runnable: (Throwable) -> Unit): ListenableFuture<U> =
        this.apply { addCallback(failure = runnable) }

fun <U> ListenableFuture<U>.addCallback(success: (U) -> Unit = {},
                                        failure: (Throwable) -> Unit = {}) {
    Futures.addCallback(this, object : FutureCallback<U> {
        override fun onFailure(p0: Throwable) {
            failure(p0)
        }

        override fun onSuccess(p0: U?) {
            if (p0 != null) {
                success(p0)
            } else {
                failure(Throwable("null success"))
            }
        }
    })
}