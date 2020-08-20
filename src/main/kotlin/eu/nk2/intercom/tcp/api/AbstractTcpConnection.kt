package eu.nk2.intercom.tcp.api

import java.net.InetAddress

interface AbstractTcpConnection {
    val address: InetAddress

    fun start()
    fun close()
    fun send(message: Any)
    fun addListener(listener: TcpConnectionListener)
}
