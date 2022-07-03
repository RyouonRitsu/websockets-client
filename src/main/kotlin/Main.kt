import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

val ban: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet())

fun main() {
    val cmdList = listOf("chat", "whisper", "exit")
    val client = HttpClient {
        install(WebSockets)
    }
    var userId = -1
    println("- Welcome to use this application! -")
    runBlocking {
        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/") {
            for (message in incoming) {
                when (message) {
                    is Frame.Text -> {
                        userId = message.readText().toInt()
                        break
                    }
                    else -> continue
                }
            }
        }
        while (true) {
            println("- Now is in command mode! -")
            println("- To start communicating, please input your command like: ${cmdList.joinToString(", ")}! -")
            val cmd = readLine() ?: ""
            if (cmd.equals("exit", true)) break
            else {
                when (cmd) {
                    "chat" -> {
                        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {
                            send("$userId")
                            val messageOutputRoutine = launch { outputMessages() }
                            val userInputRoutine = launch { inputMessages() }

                            userInputRoutine.join() // Wait for completion; either "exit" or error
                            messageOutputRoutine.cancelAndJoin()
                        }
                    }
                    "whisper" -> {
                        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/whisper") {
                            send("$userId")
                            val messageOutputRoutine = launch { outputMessages() }
                            val userInputRoutine = launch { inputMessages() }

                            userInputRoutine.join() // Wait for completion; either "exit" or error
                            messageOutputRoutine.cancelAndJoin()
                        }
                    }
                    else -> println("sys: command not found: $cmd")
                }
            }
        }
    }
    client.close()
    println("- Connection closed. Goodbye! -")
}

suspend fun DefaultClientWebSocketSession.outputMessages() {
    try {
        for (message in incoming) {
            when (message) {
                is Frame.Text -> {
                    var find = false
                    ban.filter { it != ".world" }.forEach {
                        if (message.readText().contains(it)) {
                            find = true
                            return@forEach
                        }
                    }
                    if (find) continue
                    if (".world" in ban && message.readText().startsWith("[") && !message.readText()
                            .startsWith("[system]")
                    )
                        continue
                    println(message.readText())
                }
                else -> continue
            }
        }
    } catch (e: CancellationException) {
        println("sys: receiving cancelled, back to command mode")
    } catch (e: Exception) {
        println("* Error while receiving: ${e.localizedMessage} *")
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages() {
    while (true) {
        val message = readLine() ?: ""
        if (message.equals("exit", true)) return
        if (message.startsWith(".ban")) {
            val blockWord = message.substringAfter(".ban").trim()
            if (blockWord !in ban) {
                ban += blockWord
                println("sys: $blockWord is banned")
            } else println("sys: $blockWord is already banned")
            continue
        }
        if (message.startsWith(".unban")) {
            val blockWord = message.substringAfter(".unban").trim()
            if (blockWord in ban) {
                ban -= blockWord
                println("sys: $blockWord is unbanned")
            } else println("sys: $blockWord is not banned")
            continue
        }
        try {
            send(message)
        } catch (e: Exception) {
            println("* Error while sending: ${e.localizedMessage} *")
            return
        }
    }
}