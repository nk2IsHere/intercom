package eu.nk2.intercom

import eu.nk2.intercom.tcp.TcpServer
import eu.nk2.intercom.tcp.api.AbstractTcpConnection
import eu.nk2.intercom.tcp.api.TcpConnectionListener
import org.springframework.util.SerializationUtils
import java.net.Socket
import java.io.Serializable

@ExperimentalStdlibApi
fun main(args: Array<String>) {
    val server = TcpServer(port = 1000)
    server.addListener(object: TcpConnectionListener {
        override fun onConnected(connection: AbstractTcpConnection) {
            println("connect ${connection.address.canonicalHostName}")
        }

        override fun onDisconnected(connection: AbstractTcpConnection) {
            println("disconnect ${connection.address.canonicalHostName}")
        }

        override fun onMessageReceived(connection: AbstractTcpConnection, message: ByteArray) {
            println("message ${connection.address.canonicalHostName}: ${message.decodeToString()}")
        }
    })
    server.start()

    Thread {
        val socket = Socket("localhost", 1000)
        val bundle = IntercomMethodBundle(
            0,
            0,
            hashMapOf(
                0 to 1,
                1 to 2
            ),
            0
        )

        socket.getOutputStream().write(SerializationUtils.serialize(bundle)!!)
    }.apply {
        this.start()
    }

    while(true) {

    }
}