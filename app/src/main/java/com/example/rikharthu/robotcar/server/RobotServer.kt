package com.example.rikharthu.robotcar.server

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import timber.log.Timber


class RobotServer(val port: Int) : Runnable {

    override fun run() {
        Timber.d("Creating discard server at $port")
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()

        try {
            val bootstrap = Bootstrap()
            bootstrap.group(bossGroup)
                    .channel(NioDatagramChannel::class.java)
                    .handler(object : ChannelInitializer<NioDatagramChannel>() {
                        @Throws(Exception::class)
                        public override fun initChannel(ch: NioDatagramChannel) {
                            ch.pipeline().addLast(DiscardServerHandler())
                        }
                    })
                    .option(ChannelOption.SO_BROADCAST, true)

            // Bind and start to accept incoming connections.
            val f = bootstrap.bind(port).sync()

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync()
        } finally {
            Timber.d("Shutting down server")
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }
}