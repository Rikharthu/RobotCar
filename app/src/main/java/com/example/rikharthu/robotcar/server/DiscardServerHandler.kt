package com.example.rikharthu.robotcar.server

import com.example.rikharthu.robotcar.events.CommandEvent
import com.example.rikharthu.robotcar.events.RxEvents
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.socket.DatagramPacket
import io.netty.util.ReferenceCountUtil
import timber.log.Timber
import java.nio.charset.Charset


class DiscardServerHandler : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            val packet = msg as DatagramPacket
            val data = packet.content().toString(Charset.forName("UTF-8"))
            val x = packet.content().getByte(0)
            RxEvents.post(CommandEvent(x))
            Timber.d("Received: $data")
        } finally {
            ReferenceCountUtil.release(msg)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        Timber.e(cause, "Exception caught")
        ctx.close()
    }
}