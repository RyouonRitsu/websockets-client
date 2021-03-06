import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val cmdList = listOf("chat", "whisper", "exit")
    val client = HttpClient {
        install(WebSockets)
    }
    println("Welcome to use this application!")
    runBlocking {
        while (true) {
            println("To start communicating, please input your command like: ${cmdList.joinToString(", ")}!")
            val cmd = readLine() ?: ""
            if (cmd.equals("exit", true)) break
            else {
                when (cmd) {
                    "chat" -> {
                        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {
                            val messageOutputRoutine = launch { outputMessages() }
                            val userInputRoutine = launch { inputMessages() }

                            userInputRoutine.join() // Wait for completion; either "exit" or error
                            messageOutputRoutine.cancelAndJoin()
                        }
                    }
                    "whisper" -> {
                        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/whisper") {
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
    println("Connection closed. Goodbye!")
}

suspend fun DefaultClientWebSocketSession.outputMessages() {
    try {
        for (message in incoming) {
            when (message) {
                is Frame.Text -> println(message.readText())
                else -> continue
            }
        }
    } catch (e: CancellationException) {
        println("sys: receiving cancelled, back to command mode")
    } catch (e: Exception) {
        println("Error while receiving: ${e.localizedMessage}")
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages() {
    while (true) {
        val message = readLine() ?: ""
        if (message.equals("exit", true)) return
        try {
            send(message)
        } catch (e: Exception) {
            println("Error while sending: ${e.localizedMessage}")
            return
        }
    }
}