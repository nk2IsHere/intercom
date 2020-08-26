package eu.nk2.intercom.tcp

import eu.nk2.intercom.tcp.api.AbstractTcpConnection
import eu.nk2.intercom.tcp.api.TcpConnectionListener
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList


class TcpConnection(private val socket: Socket): AbstractTcpConnection {

    private var inputStream: InputStream = socket.getInputStream()
    private var outputStream: OutputStream = socket.getOutputStream()
    private val listeners: MutableList<TcpConnectionListener> = CopyOnWriteArrayList()

    override val address: InetAddress
        get() = socket.inetAddress

    override fun send(message: ByteArray) {
        outputStream.write(message)
    }

    override fun addListener(listener: TcpConnectionListener) {
        listeners.add(listener)
    }

    override fun start() =
        Thread {
            while (true) {
                if(socket.isClosed) {
                    for (listener in listeners) {
                        listener.onDisconnected(this)
                    }
                    break
                }
                if(inputStream.available() > 0) {
                    val buf = ByteArray(64 * 1024)
                    val count = inputStream.read(buf)
                    if (count > 0) {
                        val bytes: ByteArray = buf.copyOf(count)
                        for (listener in listeners) {
                            listener.onMessageReceived(this, bytes)
                        }
                    }
                }
                /* else {
                    socket.close()
                    for (listener in listeners) {
                        listener.onDisconnected(this)
                    }
                    break
                }*/
            }
        }.start()

    override fun close() =
        socket.close()
}
