package eu.nk2.intercom.tcp

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.InetSocketAddress


class TcpClient(
    host: String,
    port: Int,
    private val onConnected: (TcpClient) -> Unit = {  },
    private val onDisconnected: (TcpClient) -> Unit = {  },
    private val onMessage: (TcpClient, ByteArray) -> Unit = { _, _ ->  }
) {
    private val group: EventLoopGroup = NioEventLoopGroup()
    private val clientBootstrap: Bootstrap = Bootstrap()
    private val clientConnectionHandler: TcpClientConnectionHandler = TcpClientConnectionHandler(this)

    private lateinit var channelFuture: ChannelFuture

    init {
        clientBootstrap.group(group)
        clientBootstrap.channel(NioSocketChannel::class.java)
        clientBootstrap.remoteAddress(InetSocketAddress(host, port))
        clientBootstrap.handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(socketChannel: SocketChannel) {
                socketChannel.pipeline().addLast(clientConnectionHandler)
            }
        })
    }

    fun connect() {
        synchronized(this) {
            channelFuture = clientBootstrap.connect().sync()
        }
    }

    fun close() {
        synchronized(this) {
            channelFuture.channel().closeFuture().sync()
            group.shutdownGracefully().sync()
            onDisconnected(this)
        }
    }

    fun send(message: ByteArray) {
        synchronized(this) {
            clientConnectionHandler.context.writeAndFlush(Unpooled.copiedBuffer(message))
        }
    }

    class TcpClientConnectionHandler(private val tcpClient: TcpClient): SimpleChannelInboundHandler<ByteBuf>() {
        lateinit var context: ChannelHandlerContext

        override fun channelActive(channelHandlerContext: ChannelHandlerContext) {
            synchronized(this) {
                context = channelHandlerContext
                tcpClient.onConnected(tcpClient)
            }
        }

        override fun channelRead0(channelHandlerContext: ChannelHandlerContext, message: ByteBuf) {
            synchronized(this) {
                val bytes = ByteArray(message.readableBytes())
                message.duplicate().readBytes(bytes)

                tcpClient.onMessage(tcpClient, bytes)
            }
        }

        override fun exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable) {
            synchronized(this) {
                cause.printStackTrace()
                channelHandlerContext.close()
                tcpClient.close()
            }
        }
    }
}