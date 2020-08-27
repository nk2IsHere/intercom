package eu.nk2.intercom.tcp

import eu.nk2.intercom.tcp.api.AbstractTcpServer
import eu.nk2.intercom.tcp.api.TcpConnectionListener
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.timeout.ReadTimeoutHandler
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList


@Service class TcpServer(override val port: Int) : AbstractTcpServer {
    private val logger: Log = LogFactory.getLog(TcpServer::class.java)

    private lateinit var group: EventLoopGroup
    private lateinit var serverBootstrap: ServerBootstrap
    private lateinit var channelFuture: ChannelFuture

    private val listeners: MutableList<TcpConnectionListener> = CopyOnWriteArrayList()

    init {
        try {
            group = NioEventLoopGroup()
            serverBootstrap = ServerBootstrap()
            serverBootstrap.group(group)
            serverBootstrap.channel(NioServerSocketChannel::class.java)
            serverBootstrap.localAddress(InetSocketAddress("0.0.0.0", port))
            serverBootstrap.childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().addLast("readTimeoutHandler", ReadTimeoutHandler(5))
                    socketChannel.pipeline().addLast(TcpServerConnectionHandler(
                        listeners = this@TcpServer.listeners
                    ))
                }
            })

            logger.info("Intercom TCP server start at port $port")
        } catch (e: IOException) {
            e.printStackTrace()
            group.shutdownGracefully().sync()
            logger.error("Intercom failed to start on port $port. Check its availability.")
        }
    }

    override fun start() {
        channelFuture = serverBootstrap.bind().sync()
    }

    override fun stop() {
        channelFuture.channel().closeFuture().sync()
        group.shutdownGracefully().sync()
    }

    override fun addListener(listener: TcpConnectionListener) {
        listeners.add(listener)
    }

    class TcpServerConnectionHandler(private val listeners: List<TcpConnectionListener>): ChannelInboundHandlerAdapter() {

        override fun handlerAdded(ctx: ChannelHandlerContext) {
            synchronized(this) {
                listeners.forEach { it.onConnected(ctx.toTcpConnection()) }
            }
        }

        override fun handlerRemoved(ctx: ChannelHandlerContext) {
            synchronized(this) {
                listeners.forEach { it.onDisconnected(ctx.toTcpConnection()) }
            }
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            synchronized(this) {
                val inBuffer = msg as ByteBuf
                val bytes = ByteArray(inBuffer.readableBytes())
                inBuffer.duplicate().readBytes(bytes)

                listeners.forEach { it.onMessageReceived(ctx.toTcpConnection(), bytes) }
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }
}