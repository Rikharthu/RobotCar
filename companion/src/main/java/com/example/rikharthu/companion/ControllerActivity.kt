package com.example.rikharthu.companion

import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import com.example.rikharthu.companion.views.DpadView
import kotlinx.android.synthetic.main.activity_controller.*
import timber.log.Timber
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.*

class ControllerActivity : AppCompatActivity() {

    private lateinit var serviceInfo: NsdServiceInfo
    private lateinit var networkHandler: Handler
    private lateinit var channel: DatagramChannel
    private lateinit var address: InetSocketAddress
    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)

        // TODO null checks
        serviceInfo = intent.getParcelableExtra(KEY_SERVICE_INFO)

        val networkThread = HandlerThread("network-thread")
        networkThread.start()
        networkHandler = Handler(networkThread.looper)
        channel = DatagramChannel.open()
        address = InetSocketAddress(serviceInfo.host.hostAddress, serviceInfo.port)
    }

    override fun onResume() {
        super.onResume()
        val timerTask = object : TimerTask() {
            override fun run() {
                // TODO refactor, since these kotlin syncx use findViewById all the time
                when (dpadView.currentSector) {
                    DpadView.UP -> {
                        Timber.d("forward")
                        send(RobotConstants.COMMAND_FORWARD)
                    }
                    DpadView.LEFT -> {
                        Timber.d("left")
                        send(RobotConstants.COMMAND_LEFT)
                    }
                    DpadView.RIGHT -> {
                        Timber.d("right")
                        send(RobotConstants.COMMAND_RIGHT)
                    }
                    DpadView.DOWN -> {
                        Timber.d("back")
                        send(RobotConstants.COMMAND_BACK)
                    }
                    else -> {
                        Timber.d("stop")
                        send(RobotConstants.COMMAND_STOP)
                    }
                }
            }
        }
        timer = Timer("timer")
        timer.scheduleAtFixedRate(timerTask, 100, 10)
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    fun send(data: ByteArray) {
        networkHandler.post {
            val buf = ByteBuffer.wrap(data)
            val bytesSent = channel.send(buf, address)
            Timber.d("Sent $bytesSent bytes")
        }
    }

    companion object {
        const val KEY_SERVICE_INFO = "service_info"
    }
}
