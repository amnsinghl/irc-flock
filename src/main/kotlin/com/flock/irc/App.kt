package com.flock.irc

import com.flock.EventHandlerClient
import com.flock.api.Chat
import com.flock.model.Message
import com.flock.model.SendAs
import com.flock.model.SendMessageOptionalParams
import com.flock.model.ToastMessage
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Invoke
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.client.library.event.client.ClientConnectionEstablishedEvent
import spark.Spark
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val executor = Executors.newScheduledThreadPool(10)

class App(private val store: Store, appId: String, appSecret: String, private val webHookToken: String, private val port: Int) {
    private val eventHandler: EventHandlerClient = EventHandlerClient(appId, appSecret)
    private val ircClientMap: MutableMap<String, IrcClient> = HashMap()
    private val gson = Gson()
    private val queue = ConcurrentLinkedQueue<String>()

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
        Spark.port(port)
        Spark.post("/event") { req, res ->
            println("event received")
            try {
                eventHandler.handleRequest(req.raw(), res.raw())
            } catch (e: Exception) {
                ToastMessage().text(e.message)
            }
        }
        Spark.post("/message") { req, res ->
            println("message received")
            if (req.queryParams("token") != webHookToken) {
                println("invalid webhook token")
                return@post "NOT OK"
            }
            val body = req.body()!!
            val message = gson.fromJson<Message>(body, Message::class.java)!!
            getIrcClient(message.from).onSuccess { ircClient ->
                if (!isInQueue(ircClient!!.client.nick, message.text)) {
                    val channelName = store[message.to + "channelName"]!!
                    ircClient.client.addChannel(channelName)
                    ircClient.client.sendMessage(channelName, message.text)
                }
            }
            return@post "OK"
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
        appendln("/irc setchannel <irc channel name>")
    }.toString()


    fun sendSelfVisiblefMessage(userId: String, channelId: String, text: String) {
        Chat.sendMessage(store[userId + "token"], channelId, text,
                SendMessageOptionalParams().visibleTo(listOf(userId))
                        .sendAs(SendAs().name("Irc Bot")
                                .profileImage("https://blog.openshift.com/wp-content/uploads/imported/alphie_hello_cloud.png")))
    }


    fun register(userId: String, userName: String, password: String): ListenableFuture<IrcClient> {
        val ircClient = IrcClient(userId, IrcListener(userId), store)
        ircClient.setUserNamePass(userName, password)
        ircClientMap[userId] = ircClient
        return ircClient.connect()
    }

    fun authenticate(userId: String): ListenableFuture<IrcClient> {
        val ircClient = IrcClient(userId, IrcListener(userId), store)
        ircClient.authenticateViaSasl()
        ircClientMap[userId] = ircClient
        return ircClient.connect()
    }

    fun addToQueue(senderNick: String, message: String) {
        if (queue.size > 10) {
            queue.poll()
        }
        queue.add(senderNick + message)
    }


    fun isInQueue(senderNick: String, message: String): Boolean {
        return queue.contains(senderNick + message).apply {
            if (!this) {
                addToQueue(senderNick, message)
            }
        }
    }


    inner class IrcListener(val userId: String) {

        @Handler(delivery = Invoke.Asynchronously)
        fun onConnectedEvent(v: ClientConnectionEstablishedEvent) {
            val i = 1
        }

        @Handler(delivery = Invoke.Asynchronously)
        fun onChannelMessageEvent(v: ChannelMessageEvent) {
            if (isInQueue(v.actor.nick, v.message)) {
                return
            }
            if (store.exists(userId + v.channel.name)) {
                Chat.sendMessage(store[userId + "token"], store[userId + v.channel.name], v.message,
                        SendMessageOptionalParams().sendAs(getSendas(v.actor.nick)))

            }
        }

        fun getSendas(name: String): SendAs {
            return SendAs().name(name).profileImage("https://robohash.org/$name")
        }
    }
}

fun <U> ListenableFuture<U>.onSuccess(runnable: (U?) -> Unit): ListenableFuture<U> =
        this.apply { addCallback(success = runnable) }

fun <U> ListenableFuture<U>.onFailure(runnable: (Throwable) -> Unit): ListenableFuture<U> =
        this.apply { addCallback(failure = runnable) }

fun <U> ListenableFuture<U>.addCallback(success: (U?) -> Unit = {},
                                        failure: (Throwable) -> Unit = {}) {
    val tFuture = Futures.withTimeout(this, 30, TimeUnit.SECONDS, executor)
    Futures.addCallback(tFuture, object : FutureCallback<U> {
        override fun onFailure(p0: Throwable) {
            failure(p0)
        }

        override fun onSuccess(p0: U?) {
            success(p0)
        }
    })
}