package com.flock.irc

import com.flock.EventHandlerClient
import com.flock.api.Chat
import com.flock.model.SendAs
import com.flock.model.SendMessageOptionalParams
import com.flock.model.ToastMessage
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Invoke
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.client.library.event.client.ClientConnectionEstablishedEvent
import redis.clients.jedis.Jedis
import spark.Spark
import java.lang.Exception

class Main {
    private val store: Jedis = Jedis("127.0.0.1")
    private val eventHandler: EventHandlerClient = EventHandlerClient("05bf2a80-19e8-46bc-84de-62c24e667836", "b9c5a695-0855-4c30-a4f4-c8da4c89cd53")
    private val ircClientMap: MutableMap<String, IrcClient> = HashMap()

    init {
        println("ping " + store.ping())
        startListeningToEvents()
    }

    fun startListeningToEvents() {
        eventHandler.setClientSlashCommandListener { slash ->
            println(slash.command + " " + slash.text)
            val commandSplit = slash.text.split(delimiters = " ", limit = 2)
            val command = commandSplit[0].toLowerCase()
            when (command) {
                "login" -> {
                    val text = commandSplit[1]
                    val split = text.split(" ")
                    register(slash.userId, split[0], split[1]).onSuccess { client ->
                        sendSelfVisiblefMessage(slash.userId, slash.chat, "Login successful")
                        client!!.setPublicKey()
                    }
                }
                "authenticate" -> {
                    authenticate(slash.userId).onSuccess {
                        sendSelfVisiblefMessage(slash.userId, slash.chat, "Authentication successful")
                    }
                }
                "sendmessage" -> {
                    val text = commandSplit[1]
                    getIrcClient(slash.userId).onSuccess { ircClient ->
                        val channelName = store[slash.chat + "channelName"]
                        ircClient!!.client.addChannel(channelName)
                        ircClient.client.sendMessage(channelName, text)
                    }

                }
                "setchannel" -> {
                    if (store.exists(slash.chat + "channelName")) {
                        return@setClientSlashCommandListener ToastMessage().text("This channel is already linked with ${store[slash.chat + "channelName"]}")
                    }
                    val channelName = commandSplit[1]
                    getIrcClient(slash.userId).onSuccess { ircClient ->
                        ircClient!!.client.addChannel(channelName)
                        store[slash.userId + channelName] = slash.chat
                        store[slash.chat + "channelName"] = channelName
                    }
                }
                "help" -> {
                    sendSelfVisiblefMessage(slash.userId, slash.chat, helpText)
                }
            }
            ToastMessage()
        }
        eventHandler.setAppInstallListener { it ->
            store[it.userId + "token"] = it.token
        }
        Spark.port(4567)
        Spark.post("/event") { req, res ->
            println("event received")
            try {
                eventHandler.handleRequest(req.raw(), res.raw())
            } catch (e: Exception) {
                ToastMessage().text(e.message)
            }
        }
    }

    fun getIrcClient(userId: String): ListenableFuture<IrcClient> {
        return if (ircClientMap.containsKey(userId)) {
            Futures.immediateFuture(ircClientMap[userId])
        } else {
            authenticate(userId)
        }
    }

    private val helpText = StringBuilder().apply {
        appendln("/irc login <username> <password>")
        appendln("/irc authenticate")
        appendln("/irc sendmessage <message>")
        appendln("/irc setchannel <irc channel name>")
    }.toString()


    fun sendSelfVisiblefMessage(userId: String, channelId: String, text: String) {
        Chat.sendMessage(store[userId + "token"], channelId, text,
                SendMessageOptionalParams().visibleTo(listOf(userId))
                        .sendAs(SendAs().name("Irc Bot").profileImage("https://blog.openshift.com/wp-content/uploads/imported/alphie_hello_cloud.png")))
    }


    fun register(userId: String, userName: String, password: String): ListenableFuture<IrcClient> {
        val ircClient = IrcClient(userId, IrcListener(userId, store), store)
        ircClient.setUserNamePass(userName, password)
        ircClientMap[userId] = ircClient
        return ircClient.connect()
    }

    fun authenticate(userId: String): ListenableFuture<IrcClient> {
        val ircClient = IrcClient(userId, IrcListener(userId, store), store)
        ircClient.authenticateViaSasl()
        ircClientMap[userId] = ircClient
        return ircClient.connect()
    }

    class IrcListener(val userId: String, val store: Jedis) {


        @Handler(delivery = Invoke.Asynchronously)
        fun onConnectedEvent(v: ClientConnectionEstablishedEvent) {
            val i = 1
        }

        @Handler(delivery = Invoke.Asynchronously)
        fun onChannelMessageEvent(v: ChannelMessageEvent) {
            if (store.exists(userId + v.channel.name)) {
                Chat.sendMessage(store[userId + "token"], store[userId + v.channel.name], v.message, SendMessageOptionalParams().sendAs(getSendas(v.actor.nick)))
            }
        }

        fun getSendas(name: String): SendAs {
            return SendAs().name(name).profileImage("https://api.adorable.io/avatars/100/$name.png")
        }
    }

}


fun <U> ListenableFuture<U>.onSuccess(runnable: (U?) -> Unit): ListenableFuture<U> =
        this.apply { addCallback(success = runnable) }

fun <U> ListenableFuture<U>.onFailure(runnable: (Throwable) -> Unit): ListenableFuture<U> =
        this.apply { addCallback(failure = runnable) }

fun <U> ListenableFuture<U>.addCallback(success: (U?) -> Unit = {},
                                        failure: (Throwable) -> Unit = {}) {
    Futures.addCallback(this, object : FutureCallback<U> {
        override fun onFailure(p0: Throwable) {
            failure(p0)
        }

        override fun onSuccess(p0: U?) {
            success(p0)
        }
    })
}