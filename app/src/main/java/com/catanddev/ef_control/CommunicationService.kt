package com.catanddev.ef_control

import android.app.Activity
import android.app.AlertDialog
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.math.BigDecimal
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

//TODO: Дилог bluetooth

interface CommunicationCallbacks {
    fun onBTConnected(connected : Boolean)
    fun onBTData(data: ByteArray)
    fun onGPSSpeed(speed: Double)
    fun onGPSSatellites(total : Int, inFix : Int)
}


class CommunicationService : Service(), android.location.GpsStatus.Listener {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var btSocket : BluetoothSocket
    private var communicationCallbacks: CommunicationCallbacks? = null
    lateinit var context : Context
    var isConnected = false
        private set
    var isRunning = false
        private set
    var isWriting = false
        private set
    private var kmhSpeed = 0.0
    private var locationManager : LocationManager? = null
    private var locationListener : LocationListener
    private val mBinder: IBinder = CSBinder()
    private var outputStream : OutputStream ? = null
    private val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    inner class CSBinder : Binder() {
        fun getService() : CommunicationService? {
            return this@CommunicationService
        }
    }

    init {
        locationListener = LocationListener { location ->

            val mCurrentSpeed = round(location.speed.toDouble(), 3, BigDecimal.ROUND_HALF_UP)
            kmhSpeed = round(mCurrentSpeed * 3.6, 3, BigDecimal.ROUND_HALF_UP)
            Handler(Looper.getMainLooper()).post {
                communicationCallbacks?.onGPSSpeed(kmhSpeed)
            }

        }
    }

    fun btConnect(address : String, uuid: String) {

        if (bluetoothAdapter == null) {
            communicationCallbacks?.onBTConnected(false)
            return
        }

        if (!(bluetoothAdapter!!.isEnabled)) {
            showBluetoothAlert()
            if (!(bluetoothAdapter!!.isEnabled)) {
                communicationCallbacks?.onBTConnected(false)
                return
            }
        }

        val remoteDevice = bluetoothAdapter!!.getRemoteDevice(address)
        bluetoothAdapter?.cancelDiscovery()
        val mWorker = Executors.newSingleThreadExecutor()
        val mRunnable = Runnable{
            try {
                isConnected = true
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
            }
            if (isConnected) {
                val mWorker1 = Executors.newSingleThreadExecutor()
                val mRunnable1 = Runnable {
                    val mInStream = btSocket.inputStream
                    val mData = ByteArray(100)
                    val sdf = SimpleDateFormat("HH:mm:ss")
                    var currentDate : String
                    while (btSocket.isConnected) {
                        if (mInStream.available() >= 11) {
                            mInStream.read(mData)
                            if (mData[0].toInt() == 0xFA && mData[10].toInt() == 0xF5) {

                                communicationCallbacks?.onBTData(mData)
                            }
                            if (isWriting) {
                                currentDate = sdf.format(Date())
                                outputStream!!.write(currentDate.toByteArray())
                                outputStream!!.write(' '.toInt())
                                outputStream!!.write(mData)
                                outputStream!!.flush()
                            }
                        }
                    }
                }
                mWorker1.execute(mRunnable1)
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



    override fun onGpsStatusChanged( p0 : Int) {
        var mSatellites = 0
        var mSatellitesInFix = 0

        if ((ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(
                    (context as Activity),
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    2)
        }

        for (mSatellite in locationManager?.getGpsStatus(null)?.satellites!!) {
            if (mSatellite.usedInFix()) {
                mSatellitesInFix ++
            }
            mSatellites ++
        }
        Handler(Looper.getMainLooper()).post {
            communicationCallbacks?.onGPSSatellites(mSatellites, mSatellitesInFix)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !(bluetoothAdapter!!.isEnabled)) {
            showBluetoothAlert()
        }
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if(locationManager != null && !(locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
            showGPSAlert()
        }

        if (locationManager == null) {
           locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        val mCriteria = Criteria()
        mCriteria.accuracy = Criteria.ACCURACY_FINE
        mCriteria.isSpeedRequired = true
        mCriteria.isSpeedRequired = true
        mCriteria.isAltitudeRequired = false
        mCriteria.isBearingRequired = false
        mCriteria.isCostAllowed = true
        mCriteria.powerRequirement = Criteria.POWER_MEDIUM

        val mBestProvider = locationManager?.getBestProvider(mCriteria, true)

        if ((ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(
                    (context as Activity),
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    1)
        }
        if (mBestProvider != null && mBestProvider.isNotEmpty()) {
            locationManager?.requestLocationUpdates(mBestProvider, 100, 0.0f, locationListener)
        } else {
            val mProviders = locationManager?.getProviders(true)
            if (mProviders != null) {
                for(mProvider in mProviders) {
                    locationManager?.requestLocationUpdates(mProvider, 100, 0.0f, locationListener)
                }
            }
        }
        locationManager?.addGpsStatusListener(this)
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

    private fun showBluetoothAlert() {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(getString(R.string.error_txt))
        alertDialog.setMessage(getString(R.string.bt_enable_message))
        alertDialog.setPositiveButton(getString(R.string.action_settings)) {
            _, _ ->
            run {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            }
        }
        alertDialog.setNegativeButton(getString(R.string.cancel_txt)) {
            dialog, _ -> dialog.cancel()
        }
        alertDialog.show()
    }

    private fun showGPSAlert() {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(getString(R.string.error_txt))
        alertDialog.setMessage(getString(R.string.gps_enable_message))
        alertDialog.setPositiveButton(getString(R.string.action_settings)) {
            _, _ ->
            run {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
        }
        alertDialog.setNegativeButton(getString(R.string.cancel_txt)) {
            dialog, _ -> dialog.cancel()
        }
        alertDialog.show()
    }

    fun startWriting(logPath : String) {
        val sdf = SimpleDateFormat("dd.M.yyyy_HH-mm-ss")
        val currentDate = sdf.format(Date())
        val mDir = DocumentFile.fromTreeUri(context, Uri.parse(logPath))

        context.grantUriPermission(packageName, mDir!!.uri, Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        context.contentResolver.takePersistableUriPermission(mDir.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val mLogfile = mDir.createFile("//MIME txt",  "$currentDate.txt" )
        outputStream = contentResolver.openOutputStream(mLogfile!!.uri)
        isWriting = true
    }

    fun stopWriting() {
        isWriting = false
        outputStream?.flush()
        outputStream?.close()
    }

}