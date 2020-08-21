package eu.nk2.intercom.tcp

import eu.nk2.intercom.tcp.api.AbstractTcpConnection
import eu.nk2.intercom.tcp.api.TcpConnectionListener
import eu.nk2.intercom.tcp.api.AbstractTcpServer
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.ServerSocket
import java.util.*


@Component class TcpServer(override val port: Int) : AbstractTcpServer, TcpConnectionListener {
    private val logger: Log = LogFactory.getLog(TcpServer::class.java)
    override val connections: MutableList<AbstractTcpConnection> = arrayListOf()

    private var serverSocket: ServerSocket? = null
    private val listeners: MutableList<TcpConnectionListener> = ArrayList()

    @Volatile private var isStopped = false

    init {
        try {
            serverSocket = ServerSocket(port)
            logger.info("Intercom TCP server start at port $port")
        } catch (e: IOException) {
            e.printStackTrace()
            logger.error("Intercom failed to start on port $port. Check its availability.")
        }
    }

    override fun start() {
        Thread {
            while (!isStopped) {
                try {
                    val socket = serverSocket!!.accept()
                    if (socket.isConnected) {
                        val tcpConnection = TcpConnection(socket)
                        tcpConnection.start()
                        tcpConnection.addListener(this)
                        onConnected(tcpConnection)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    override fun stop() {
        isStopped = true
    }

    override fun addListener(listener: TcpConnectionListener) {
        listeners.add(listener)
    }

    override fun onMessageReceived(connection: AbstractTcpConnection, message: ByteArray) {
        logger.trace("Received new message from " + connection.address.canonicalHostName)
        logger.trace("Class name: " + message.javaClass.canonicalName + ", toString: " + message.toString())
        for (listener in listeners) {
            listener.onMessageReceived(connection, message)
        }
    }

    override fun onConnected(connection: AbstractTcpConnection) {
        logger.trace("New connection! Ip: " + connection.address.canonicalHostName.toString() + ".")
        connections.add(connection)
        logger.trace("Current connections count: " + connections.size)
        for (listener in listeners) {
            listener.onConnected(connection)
        }
    }

    override fun onDisconnected(connection: AbstractTcpConnection) {
        logger.trace("Disconnect! Ip: " + connection.address.canonicalHostName.toString() + ".")
        connections.remove(connection)
        logger.trace("Current connections count: " + connections.size)
        for (listener in listeners) {
            listener.onDisconnected(connection)
        }
    }
}