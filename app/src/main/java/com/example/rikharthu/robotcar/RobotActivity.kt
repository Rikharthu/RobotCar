package com.example.rikharthu.robotcar

import android.app.Activity
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import com.example.rikharthu.robotcar.server.RobotServer
import timber.log.Timber


private val TAG = MainActivity::class.java.simpleName

// TODO cancel advertising when someone is connected

class MainActivity : Activity(), NsdManager.RegistrationListener {

    private lateinit var nsdManager: NsdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        val port = setupUdpServer()
        Timber.d("Listening on port $port")
        registerService(port)
    }

    private lateinit var serverThread: Thread

    private fun setupUdpServer(): Int {
        serverThread = Thread(RobotServer(9001))
        serverThread.start()

        return 9001
    }

    private fun registerService(port: Int) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo()

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.serviceName = "Robot car (${Build.DEVICE})"
        serviceInfo.serviceType = RobotConstants.SERVICE_TYPE
        serviceInfo.port = port
        serviceInfo.setAttribute("hello", "world")

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, this)
    }

    override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
        Timber.d("Unregistration failed")
    }

    override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
        Timber.d("Service unregistered")
    }

    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
        Timber.d("Registration failed")
    }

    override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
        Timber.d("Service registered")
    }

}
