package eu.nk2.intercom.tcp

import eu.nk2.intercom.tcp.api.AbstractTcpConnection
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import java.net.InetAddress
import java.net.InetSocketAddress

class NettyTcpConnection(private val ctx: ChannelHandlerContext) : AbstractTcpConnection {
    override val address: InetAddress = (ctx.channel().remoteAddress() as InetSocketAddress).address

    override fun send(message: ByteArray) {
        ctx.writeAndFlush(Unpooled.copiedBuffer(message))
            .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
    }

    override fun close() {
        ctx.close()
    }

}

fun ChannelHandlerContext.toTcpConnection() =
    NettyTcpConnection(this)