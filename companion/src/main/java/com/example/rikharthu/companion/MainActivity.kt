package com.example.rikharthu.companion

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel


class MainActivity : AppCompatActivity(), NsdManager.DiscoveryListener {

    private lateinit var nsdManager: NsdManager
    private val services = mutableListOf<NsdServiceInfo>()
    private lateinit var serviceAdapter: ArrayAdapter<String>
    private var selectedService: NsdServiceInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        serviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        serviceList.adapter = serviceAdapter
        serviceList.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val service = services.get(position)
            onServiceSelected(service)
        }

    }

    override fun onResume() {
        super.onResume()
        discoverServices()
    }

    override fun onPause() {
        super.onPause()
        nsdManager.stopServiceDiscovery(this)
    }

    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
        Timber.d("onServiceFound: $serviceInfo")
        services.add(serviceInfo)
        runOnUiThread {
            onServicesUpdated()
        }
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
        Timber.d("onServiceLost")
        val service = services.find { it.serviceName == serviceInfo.serviceName }
        if (service != null) {
            services.remove(service)
        }
        runOnUiThread {
            onServicesUpdated()
        }
    }

    override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
        Timber.d("onStopDiscoveryFailed")

    }

    override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
        Timber.d("onStartDiscoveryFailed")
    }

    override fun onDiscoveryStarted(serviceType: String?) {
        Timber.d("onDiscoveryStarted")
    }

    override fun onDiscoveryStopped(serviceType: String?) {
        Timber.d("onDiscoveryStopped")
    }

    private fun onServiceSelected(service: NsdServiceInfo) {
        ConnectDialog.newInstance(service).apply {
            connectListener = {
                openController(it)
            }
        }.show(supportFragmentManager, "connect_dialog")
    }

    private fun openController(service: NsdServiceInfo) {
        Timber.d("Connecting to $service")
        startActivity(Intent(this, ControllerActivity::class.java).apply {
            putExtra(ControllerActivity.KEY_SERVICE_INFO, service)
        })
    }

    private fun discoverServices() {
        nsdManager.discoverServices(RobotConstants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this)
    }

    private fun onServicesUpdated() {
        serviceAdapter.clear()
        serviceAdapter.addAll(services.map { it.serviceName })
        serviceAdapter.notifyDataSetChanged()
    }

    private val serviceResolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Timber.e("Could not resolve service [$serviceInfo], error code: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            if (selectedService == null) {
                return
            }
            if (selectedService!!.serviceName == serviceInfo.serviceName) {
                selectedService = serviceInfo
                Toast.makeText(this@MainActivity, "Connecting to ${selectedService!!.host}", Toast.LENGTH_SHORT).show()
                connectToService(selectedService!!)
            }
        }
    }

    private var workerHandler: Handler? = null

    private fun connectToService(service: NsdServiceInfo) {
        val handlerThread = HandlerThread("worker")
        handlerThread.start()
        workerHandler = Handler(handlerThread.looper)
        val channel = DatagramChannel.open()

        val newData = "Current time is: " + System.currentTimeMillis()

        val buf = ByteBuffer.allocate(48)
        buf.clear()
        buf.put(newData.toByteArray())
        buf.flip()

        workerHandler!!.post {
            val bytesSent = channel.send(buf, InetSocketAddress(service.host.hostAddress, service.port))
            Timber.d("Send $bytesSent bytes")
            val buf = ByteBuffer.allocate(48)
            buf.clear()
            val sender = channel.receive(buf)
            Timber.d("Received: $buf from $sender")
        }
    }
}
