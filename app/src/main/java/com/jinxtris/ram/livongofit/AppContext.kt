package com.jinxtris.ram.livongofit

import android.app.Application
import android.util.Log
import com.jinxtris.ram.livongofit.broadcast.ConnectivityReceiver
import java.io.PrintWriter
import java.io.StringWriter

class AppContext : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: AppContext? = null

        @Synchronized
        fun getInstance(): AppContext {
            return instance!!
        }
    }

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            handleUncaughtException(e)
        }
    }

    private fun handleUncaughtException(e: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        Log.e(TAG, sw.toString())
    }

    fun setConnectivityListener(listener: ConnectivityReceiver.ConnectivityReceiverListener) {
        ConnectivityReceiver.connectivityReceiverListener = listener
    }
}