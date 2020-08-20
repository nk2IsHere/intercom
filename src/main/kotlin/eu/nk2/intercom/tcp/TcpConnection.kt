package eu.nk2.intercom.tcp

import eu.nk2.intercom.tcp.api.AbstractTcpConnection
import eu.nk2.intercom.tcp.api.TcpConnectionListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.util.*

class TcpConnection(
    private val socket: Socket
): AbstractTcpConnection {

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val listeners: MutableList<TcpConnectionListener> = ArrayList()

    override val address: InetAddress
        get() = socket.inetAddress

    init {
        inputStream = socket.getInputStream()
        outputStream = socket.getOutputStream()
    }

    override fun send(message: Any) {
        if (message is ByteArray) {
            outputStream!!.write(message)
        }
    }

    override fun addListener(listener: TcpConnectionListener) {
        listeners.add(listener)
    }

    override fun start() =
        Thread {
            while (true) {
                val buf = ByteArray(64 * 1024)
                try {
                    val count = inputStream!!.read(buf)
                    if (count > 0) {
                        val bytes = buf.copyOf(count)
                        for (listener in listeners) {
                            listener.onMessageReceived(this, bytes)
                        }
                    } else {
                        socket.close()
                        for (listener in listeners) {
                            listener.onDisconnected(this)
                        }
                        break
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    for (listener in listeners) {
                        listener.onDisconnected(this)
                    }
                    break
                }
            }
        }.start()

    override fun close() =
        socket.close()
}
