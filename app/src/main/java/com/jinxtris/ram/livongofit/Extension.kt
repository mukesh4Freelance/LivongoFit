package com.jinxtris.ram.livongofit

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import com.google.android.gms.fitness.data.DataPoint
import java.text.DateFormat
import java.util.concurrent.TimeUnit


fun DataPoint.getStartDayString(): String = DateFormat.getDateInstance()
    .format(this.getStartTime(TimeUnit.MILLISECONDS))

var toast = { message: String -> AppContext.getInstance().toast(message) }

@JvmOverloads
fun Context.toast(message: CharSequence, length: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.isConnectedToInternet(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnected
}