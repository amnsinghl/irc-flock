package com.flock.irc

import java.util.function.Consumer

class Command(private val command: String, private val _consumer: Consumer<String>?, private val _onEnd: () -> Unit) {

    internal fun consume(s: String) {
        Thread { _consumer?.accept(s) }.start()
    }

    internal fun end() {
        Thread {
            _onEnd()
        }.start()
    }

    fun isEndString(s: String): Boolean {
        return s.contains("End of /" + command)
    }
}