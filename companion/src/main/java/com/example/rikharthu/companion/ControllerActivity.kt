package com.example.rikharthu.companion

import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_controller.*
import timber.log.Timber
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class ControllerActivity : AppCompatActivity() {

    private lateinit var serviceInfo: NsdServiceInfo
    private lateinit var networkHandler: Handler
    private lateinit var channel: DatagramChannel
    private lateinit var address: InetSocketAddress

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

        upBtn.setOnClickListener {
            send("FORWARD${System.currentTimeMillis()}".toByteArray())
        }
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
