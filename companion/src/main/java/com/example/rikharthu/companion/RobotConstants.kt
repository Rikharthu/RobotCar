package com.example.rikharthu.companion

object RobotConstants {
    const val SERVICE_TYPE = "_robotcar._udp"

    @JvmField
    val COMMAND_FORWARD = byteArrayOf(1)
    @JvmField
    val COMMAND_LEFT = byteArrayOf(2)
    @JvmField
    val COMMAND_RIGHT = byteArrayOf(3)
    @JvmField
    val COMMAND_BACK = byteArrayOf(4)
    @JvmField
    val COMMAND_STOP = byteArrayOf(0)
}