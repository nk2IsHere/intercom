package eu.nk2.intercom.tcp.api

import java.net.InetAddress

interface AbstractTcpConnection {
    val address: InetAddress

    fun send(message: ByteArray)
    fun close()
}