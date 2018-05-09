package com.example.rikharthu.robotcar

import android.app.Activity
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import com.example.rikharthu.robotcar.events.CommandEvent
import com.example.rikharthu.robotcar.events.RxEvents
import com.example.rikharthu.robotcar.server.RobotServer
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import timber.log.Timber
import java.io.IOException


private const val LED_PIN = "BCM17"

// TODO cancel advertising when someone is connected

class MainActivity : Activity(), NsdManager.RegistrationListener {


    private lateinit var nsdManager: NsdManager
    private var ledPin: Gpio? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        val port = setupUdpServer()
        Timber.d("Listening on port $port")
        registerService(port)

        val service = PeripheralManager.getInstance()
        try {
            // Create GPIO connection for LED.
            ledPin = service.openGpio(LED_PIN)
            // Configure as an output.
            ledPin!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            // High voltage is considered active
            ledPin!!.setActiveType(Gpio.ACTIVE_HIGH)
        } catch (e: IOException) {
            Timber.e(e, "Error on PeripheralIO API")
        }
        ledPin!!.value = true

        RxEvents.of(CommandEvent::class.java).subscribe {
            Timber.d("Received command: $it at ${System.currentTimeMillis()}")
        }
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
