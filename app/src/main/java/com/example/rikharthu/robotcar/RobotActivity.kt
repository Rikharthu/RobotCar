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
import com.google.android.things.pio.Pwm
import timber.log.Timber
import java.io.IOException


private const val LED_PIN = "BCM17"

// TODO cancel advertising when someone is connected

class MainActivity : Activity(), NsdManager.RegistrationListener {


    private lateinit var nsdManager: NsdManager
    private var ledPin: Gpio? = null
    private lateinit var pwmLeft: Pwm
    private lateinit var pwmRight: Pwm

    private var in1: Gpio? = null
    private var in2: Gpio? = null
    private var in3: Gpio? = null
    private var in4: Gpio? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        val port = setupUdpServer()
        Timber.d("Listening on port $port")
        registerService(port)

        val peripheralManager = PeripheralManager.getInstance()
        val pwmPorts = peripheralManager.pwmList
        Timber.d("PWM ports: $pwmPorts")
        try {
            // Create GPIO connection for LED.
            ledPin = peripheralManager.openGpio(LED_PIN)
            // Configure as an output.
            ledPin!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            // High voltage is considered active
            ledPin!!.setActiveType(Gpio.ACTIVE_HIGH)

            pwmLeft = peripheralManager.openPwm("PWM0")
            pwmLeft.setPwmFrequencyHz(980.0)
            pwmLeft.setPwmDutyCycle(90.0)
            pwmLeft.setEnabled(true)

            pwmRight = peripheralManager.openPwm("PWM1")
            pwmRight.setPwmFrequencyHz(980.0)
            pwmRight.setPwmDutyCycle(90.0)
            pwmRight.setEnabled(true)

            // green, in1
            in1 = peripheralManager.openGpio("BCM23")
            in1?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            in1?.value = false

            // yellow, in2
            in2 = peripheralManager.openGpio("BCM24")
            in2?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            in2?.value = false

            // orange, in3
            in3 = peripheralManager.openGpio("BCM27")
            in3?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            in3?.value = false

            // red, in4
            in4 = peripheralManager.openGpio("BCM22")
            in4?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            in4?.value = false

        } catch (e: IOException) {
            Timber.e(e, "Error on PeripheralIO API")
        }
        ledPin!!.value = true

        RxEvents.of(CommandEvent::class.java).subscribe {
            when(it.code){
                1.toByte()->{
                    in1?.value=!true
                    in2?.value=!false
                    in3?.value=true
                    in4?.value=false
                }
                2.toByte()->{
                    in1?.value=!false
                    in2?.value=!true
                    in3?.value=true
                    in4?.value=false
                }
                3.toByte()->{
                    in1?.value=!true
                    in2?.value=!false
                    in3?.value=false
                    in4?.value=true
                }
                4.toByte()->{
                    in1?.value=!false
                    in2?.value=!true
                    in3?.value=false
                    in4?.value=true
                }
                0.toByte()->{
                    in1?.value=!false
                    in2?.value=!false
                    in3?.value=false
                    in4?.value=false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            ledPin?.close()
            pwmLeft.setEnabled(false)
            pwmLeft.close()
            pwmRight.setEnabled(false)
            pwmRight.close()
        } catch (e: IOException) {
            Timber.e(e, "Could not close pins")
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
