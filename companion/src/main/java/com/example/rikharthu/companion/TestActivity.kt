package com.example.rikharthu.companion

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import com.example.rikharthu.companion.views.DpadView
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        dpadView.listener = {
            textView.text = when (it) {
                DpadView.CENTER -> "Center"
                DpadView.RIGHT -> "Right"
                DpadView.UP -> "Top"
                DpadView.LEFT -> "Left"
                DpadView.DOWN -> "Bottom"
                else -> {
                    "Unknown"
                }
            }
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                vibrator.vibrate(35)
            }
        }
    }
}
