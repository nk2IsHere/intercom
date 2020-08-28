package eu.nk2.intercom.tcp.api

interface AbstractTcpServer {
    val port: Int

    fun start()
    fun stop()
    fun addListener(listener: TcpConnectionListener)
}
