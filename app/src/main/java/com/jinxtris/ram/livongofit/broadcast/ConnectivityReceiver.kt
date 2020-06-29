package com.jinxtris.ram.livongofit.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jinxtris.ram.livongofit.isConnectedToInternet

class ConnectivityReceiver : BroadcastReceiver() {

    private var passOnlyOnce: Boolean = false

    companion object {
        var connectivityReceiverListener: ConnectivityReceiverListener? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val isConnected = context?.isConnectedToInternet() == true
        if (passOnlyOnce != isConnected) {
            passOnlyOnce = isConnected
            connectivityReceiverListener?.onNetworkConnectionChanged(isConnected)
        }
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }
}