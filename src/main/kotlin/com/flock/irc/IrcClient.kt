package com.flock.irc

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.feature.auth.SaslEcdsaNist256PChallenge
import org.kitteh.irc.client.library.feature.auth.SaslPlain
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

class IrcClient(private val userId: String, listener: Main.IrcListener, private val store: Store) {
    val client = Client.builder().serverHost("irc.freenode.net").build().apply {
        eventManager.registerEventListener(listener)
    }

    fun connect(): ListenableFuture<IrcClient> {
        client.nick = store[userId + "ircUserName"]!!
        client.connect()
        val future = SettableFuture.create<IrcClient>()
        client.setInputListener { it ->
            println(it)
            if (it.contains("MOTD") || it.contains("authentication successful")) {
                future.set(this)
            }
        }
        client.setOutputListener { it -> println(it) }
        return future
    }

    fun authenticateViaSasl() {
        val (ecPublicKey, ecPrivateKey) = getKey()
        val ircUserName = store[userId + "ircUserName"]!!
        client.authManager.addProtocol(SaslEcdsaNist256PChallenge(client, ircUserName, ecPrivateKey))
    }

    fun setUserNamePass(userName: String, password: String) {
        store[userId + "ircUserName"] = userName
        client.authManager.addProtocol(SaslPlain(client, userName, password))
    }

    fun hasSetKey(): Boolean {
        return store.exists(userId)
    }

    fun setPublicKey() {
        val (ecPublicKey, ecPrivateKey) = getKey()
        client.sendRawLine("asfdfwefasdfasd")
        client.sendMessage("NickServ", "SET PUBKEY " + SaslEcdsaNist256PChallenge.getCompressedBase64PublicKey(ecPublicKey))
    }

    fun getKey(): Pair<ECPublicKey, ECPrivateKey> {
        return if (hasSetKey()) {
            SaslEcdsaNist256PChallenge.getPublicKey(store[userId]!!) to SaslEcdsaNist256PChallenge.getPrivateKey(store[userId + "private"]!!)
        } else {
            val ecKeyPair = SaslEcdsaNist256PChallenge.getNewKey()
            store[userId] = SaslEcdsaNist256PChallenge.base64Encode(ecKeyPair.public)
            store[userId + "private"] = SaslEcdsaNist256PChallenge.base64Encode(ecKeyPair.private)
            ecKeyPair.public to ecKeyPair.private
        }
    }

}