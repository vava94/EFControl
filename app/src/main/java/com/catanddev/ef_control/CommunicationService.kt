package com.catanddev.ef_control

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder


interface CommunicationCallbacks {
    fun onBTData(data: Array<Char>)
    fun onGPSSpeed(speed: Double)
}


class CommunicationService : Service(), GPSCallback {


    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var gpsManager: GPSManager
    private lateinit var locationManager : LocationManager

    private var communicationCallbacks: CommunicationCallbacks? = null

    var isRunning = false
        private set
    var isWriting = false
        private set

    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable

    private val mBinder: IBinder = CSBinder()

    inner class CSBinder : Binder() {
        fun getService() : CommunicationService? {
            return this@CommunicationService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        //TODO("Return the communication channel to the service.")
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

        mHandler = Handler()
        mRunnable = Runnable { test() }
        mHandler.postDelayed(mRunnable, 5000)
        isRunning = true
        return START_STICKY
    }

    fun setCallbacks(communicationCallbacks: CommunicationCallbacks?) {
        if(communicationCallbacks != null) {
            this.communicationCallbacks = communicationCallbacks
        }
    }

    fun test() {
        val _t = arrayOf('a')
        communicationCallbacks?.onBTData(_t)
    }

    override fun onGPSUpdate(location: Location) {
        /*
        speed = location.speed.toDouble()
        currentSpeed = round(speed, 3, BigDecimal.ROUND_HALF_UP)
        kmhSpeed = round(currentSpeed * 3.6, 3, BigDecimal.ROUND_HALF_UP)
        val mText = "$kmhSpeed" + " " + getString(R.string.kmh_txt)
        for (mTV in viewBindings[1]) {
            mTV.text = mText
        }*/
    }


}