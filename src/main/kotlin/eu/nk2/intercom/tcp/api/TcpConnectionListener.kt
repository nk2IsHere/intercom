package eu.nk2.intercom.tcp.api

interface TcpConnectionListener {
    fun onConnected(connection: AbstractTcpConnection)
    fun onDisconnected(connection: AbstractTcpConnection)
    fun onMessageReceived(connection: AbstractTcpConnection, message: ByteArray)
}
