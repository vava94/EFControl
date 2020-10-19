package com.catanddev.ef_control

import android.app.AlertDialog
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.icu.math.BigDecimal
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import java.io.IOException
import java.util.concurrent.Executors

//TODO: Дилог bluetooth

interface CommunicationCallbacks {
    fun onBTConnected(connected : Boolean)
    fun onBTData(data: ByteArray)
    fun onGPSSpeed(speed: Double)
}


class CommunicationService(val context: Context) : Service(), android.location.GpsStatus.Listener {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var gpsManager: GPSManager
    private var locationManager : LocationManager? = null
    private lateinit var locationListener : LocationListener
    private lateinit var btSocket : BluetoothSocket
    private var communicationCallbacks: CommunicationCallbacks? = null

    var isConnected = false
        private set
    var isRunning = false
        private set
    var isWriting = false
        private set

    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable

    private val mBinder: IBinder = CSBinder()

    private val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    inner class CSBinder : Binder() {
        fun getService() : CommunicationService? {
            return this@CommunicationService
        }
    }

    init {
        locationListener = LocationListener { location ->
            val mCurrentSpeed = round(location.speed.toDouble(), 3, BigDecimal.ROUND_HALF_UP)
            Handler(Looper.getMainLooper()).post {
                communicationCallbacks?.onGPSSpeed(
                        round(mCurrentSpeed * 3.6, 3, BigDecimal.ROUND_HALF_UP)
                )
            }
        }
/*
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
             val mCurrentSpeed = round(location.speed.toDouble(), 3, BigDecimal.ROUND_HALF_UP)
                Handler(Looper.getMainLooper()).post {
                    communicationCallbacks?.onGPSSpeed(
                            round(mCurrentSpeed * 3.6, 3, BigDecimal.ROUND_HALF_UP)
                    )
                }
            }
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
 */
    }

    fun btConnect(address : String, uuid: String) {

        val remoteDevice = bluetoothAdapter.getRemoteDevice(address)
        bluetoothAdapter.cancelDiscovery()
        val mWorker = Executors.newSingleThreadExecutor()
        val mRunnable = Runnable{
            try {
                btSocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(this.uuid)
                btSocket.connect()
            } catch (e0: IOException) {
                try {
                    btSocket.close()
                } catch (e1: IOException) {
                    Log.d("BT", "Unable to end the connection")
                    Log.d("BT", e1.toString())
                }
                Log.d("BT", "Socket creation failed")
                Log.d("BT", e0.toString())
                isConnected = false
            } finally {
                val mWorker1 = Executors.newSingleThreadExecutor()
                val mRunnable1 = Runnable {
                    val mInStream = btSocket.inputStream
                    val mData = ByteArray(100)
                    while (btSocket.isConnected) {
                        if (mInStream.available() >= 11) {
                            mInStream.read(mData)
                            if (mData[0].toInt() == 0xFA && mData[10].toInt() == 0xF5) {
                                communicationCallbacks?.onBTData(mData)
                            }
                        }
                    }
                }
                mWorker1.execute(mRunnable1)
                isConnected = true
            }
            Handler(Looper.getMainLooper()).post {
                communicationCallbacks?.onBTConnected(isConnected)
            }
        }
        mWorker.execute(mRunnable)
    }

    fun btDisconnect() {
        try {
            btSocket.close()
        } catch (e1 : IOException) {
            Log.d("BT", "Unable to end the connection")
            Log.d("BT", e1.toString())
        }
        isConnected = false
        communicationCallbacks?.onBTConnected(false)
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        // TODO: Проверка включенного bluetooth
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        gpsManager = GPSManager(this)
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsManager.gpsCallback = this
            gpsManager.startListening(applicationContext)
        } else {
            gpsManager.showSettingsAlert()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_STICKY
    }

    private fun round(unrounded : Double, precision : Int, roundingMode : Int) : Double {
        val bd = BigDecimal(unrounded)
        val rounded = bd.setScale(precision, roundingMode)
        return rounded.toDouble()
    }

    fun setCallbacks(communicationCallbacks: CommunicationCallbacks?) {
        if(communicationCallbacks != null) {
            this.communicationCallbacks = communicationCallbacks
        }
    }

    fun showBluetoothAlert() {
        TODO("")
    }

    fun showGPSAlert() {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("Error")
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?")
        alertDialog.setPositiveButton("Settings") {
            _, _ ->
            run {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
        }
        alertDialog.setNegativeButton("Cancel") {
            dialog, _ -> dialog.cancel()
        }
        alertDialog.show()
    }


}