package com.example.rikharthu.companion

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_connect_acknowledge.*
import timber.log.Timber

private const val KEY_SERVICE_INFO = "service_info"

class ConnectDialog : DialogFragment(), NsdManager.ResolveListener {

    var connectListener: ((NsdServiceInfo) -> Unit)? = null

    private lateinit var serviceInfo: NsdServiceInfo
    private lateinit var nsdManager: NsdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO null-check
        serviceInfo = arguments!!.getParcelable(KEY_SERVICE_INFO)
        nsdManager = context!!.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_connect_acknowledge, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        serviceName.text = serviceInfo.serviceName
        connectBtn.setOnClickListener {
            connectListener?.invoke(serviceInfo)
        }
    }

    override fun onResume() {
        super.onResume()

        val params = dialog.window!!.attributes
        dialog.window!!.attributes = params as android.view.WindowManager.LayoutParams


        // TODO check if is restoring and info is already populated
        nsdManager.resolveService(serviceInfo, this)
    }

    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
        Timber.e("Could not resolve service $serviceInfo, error code: $errorCode")
    }

    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
        Timber.d("Service has been resolved: $serviceInfo")
        this.serviceInfo = serviceInfo
        connectBtn.isEnabled = true
        ipAddress.text = "${serviceInfo.host}:${serviceInfo.port}"
    }

    companion object {
        @JvmStatic
        fun newInstance(serviceInfo: NsdServiceInfo): ConnectDialog {
            return ConnectDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_SERVICE_INFO, serviceInfo)
                }
            }
        }
    }
}