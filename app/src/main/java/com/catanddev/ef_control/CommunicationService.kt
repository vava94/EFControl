package com.catanddev.ef_control

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.icu.math.BigDecimal
import android.location.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.round

//TODO: Дилог bluetooth

interface CommunicationCallbacks {
    fun onBTConnected(connected: Boolean)
    fun onBTData(data: ByteArray)
    fun onGPSSpeed(speed: Double)
    fun onGPSSatellites(total: Int, inFix: Int)
}


class CommunicationService : Service(), LocationListener, GpsStatus.Listener{

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var broadcastReceiver: BroadcastReceiver
    private lateinit var btSocket : BluetoothSocket
    private var communicationCallbacks: CommunicationCallbacks? = null
    private lateinit var gnssStatusCallback: GnssStatus.Callback
    lateinit var context : Context
    var isConnected = false
        private set
    var isRunning = false
        private set
    var isWriting = false
        private set
    private var kmhSpeed = 0.0
    private lateinit var locationManager : LocationManager
    private val mBinder: IBinder = CSBinder()
    private var outputStream : OutputStream ? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private var testArray = byteArrayOf(
        0xfc.toByte(),
        10.toByte(),
        11.toByte(),
        12.toByte(),
        13.toByte(),
        14.toByte(),
        15.toByte(),
        16.toByte(),
        18.toByte(),
        0xfe.toByte()
    )

    inner class CSBinder : Binder() {
        fun getService() : CommunicationService? {
            return this@CommunicationService
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            gnssStatusCallback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    super.onSatelliteStatusChanged(status)
                    var mSatellites = 0
                    var mSatellitesInFix = 0

                    if ((ActivityCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_DENIED)) {
                        ActivityCompat.requestPermissions(
                            (context as Activity),
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ),
                            2
                        )
                    }

                    for (mSatellite in locationManager?.getGpsStatus(null)?.satellites!!) {
                        if (mSatellite.usedInFix()) {
                            mSatellitesInFix ++
                        }
                        mSatellites ++
                    }
                    communicationCallbacks?.onGPSSatellites(mSatellites, mSatellitesInFix)
                }
            }
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                val btDevice : BluetoothDevice =
                    intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!;
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    //Device has disconnected
                    btDisconnect()
                }
            }

        }
    }

    fun btConnect(address: String, uuid: String) {

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
                    val mData = ByteArray(20)
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    var mDataString : String
                    var mV : String

                    while (btSocket.isConnected) {
                        if (mInStream.available() >= 11) {
                            mInStream.read(mData)
                            if (mData[0] == 0xFC.toByte() && mData[10] == 0xFE.toByte()) {
                                communicationCallbacks?.onBTData(mData)
                                if (isWriting) {
                                    mV = ((mData[3].toInt() + 400) * 0.1).toString()
                                    if(mV.length > 4) mV = mV.substring(0..3)
                                    mDataString = (sdf.format(Date()) + ";" + kmhSpeed + "км/ч;"
                                            + (mData[1].toInt() * 100).toString() + "RPM;"
                                            + mData[2].toString() + "А;"
                                            + mData[4].toString() + "%;"
                                            + mV + "V;"
                                            + mData[7].toString() + "мин;"
                                            + mData[5].toString() + "TFet;"
                                            +  mData[6].toString() + "TBat"
                                            //+ ';' + mData[8].toString()
                                            //+ ';' + mData[9].toString()
                                            +"\r\n")
                                    outputStream!!.write(mDataString.toByteArray())
                                    outputStream!!.flush()
                                }
                            }
                        }
                    }
                    btDisconnect()
                }
                mWorker1.execute(mRunnable1)
            }
            communicationCallbacks?.onBTConnected(isConnected)
        }
        mWorker.execute(mRunnable)
    }

    fun btDisconnect() {
        try {
            btSocket.close()
        } catch (e1: IOException) {
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
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        this.registerReceiver(broadcastReceiver, filter)
    }

    override fun onLocationChanged(location: Location) {
        val mCurrentSpeed = round(location.speed.toDouble())
        kmhSpeed = mRound(mCurrentSpeed * 3.6)
        communicationCallbacks?.onGPSSpeed(kmhSpeed)
        // Emulator display data test
        /*communicationCallbacks?.onBTData(testArray)
        testArray[1] ++
        testArray[2] ++
        testArray[3] ++
        testArray[4] ++
        testArray[5] ++
        testArray[6] ++
        testArray[7] ++
        testArray[8] ++
        testArray[9] ++*/
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !(bluetoothAdapter!!.isEnabled)) {
            showBluetoothAlert()
        }
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if(!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
            showGPSAlert()
        }

       locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val mCriteria = Criteria()
        mCriteria.accuracy = Criteria.ACCURACY_FINE
        mCriteria.isSpeedRequired = true
        mCriteria.isSpeedRequired = true
        mCriteria.isAltitudeRequired = false
        mCriteria.isBearingRequired = false
        mCriteria.isCostAllowed = true
        mCriteria.powerRequirement = Criteria.POWER_MEDIUM

        val mBestProvider = locationManager?.getBestProvider(mCriteria, true)

        if ((ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED) ||
                (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(
                (context as Activity),
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        }
        if (mBestProvider != null && mBestProvider.isNotEmpty()) {
            locationManager.requestLocationUpdates(mBestProvider, 100, 0.0f, this)
        } else {
            val mProviders = locationManager.getProviders(true)
            for(mProvider in mProviders) {
                locationManager.requestLocationUpdates(mProvider, 100, 0.0f, this)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, Handler(mainLooper))
        } else {
            locationManager.addGpsStatusListener(this)
        }
        isRunning = true
        return START_STICKY
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        }

    }

    private fun mRound(unrounded: Double) : Double {
        return round(unrounded * 100) / 100
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
        alertDialog.setPositiveButton(getString(R.string.action_settings)) { _, _ ->
            run {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            }
        }
        alertDialog.setNegativeButton(getString(R.string.cancel_txt)) { dialog, _ -> dialog.cancel()
        }
        alertDialog.show()
    }

    private fun showGPSAlert() {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(getString(R.string.error_txt))
        alertDialog.setMessage(getString(R.string.gps_enable_message))
        alertDialog.setPositiveButton(getString(R.string.action_settings)) { _, _ ->
            run {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
        }
        alertDialog.setNegativeButton(getString(R.string.cancel_txt)) { dialog, _ -> dialog.cancel()
        }
        alertDialog.show()
    }

    fun startWriting(logPath: String) {
        val sdf = SimpleDateFormat("dd.M.yyyy_HH-mm-ss", Locale.getDefault())
        val currentDate = sdf.format(Date())
        val mDir = DocumentFile.fromTreeUri(context, Uri.parse(logPath))

        context.grantUriPermission(
            packageName,
            mDir!!.uri,
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.contentResolver.takePersistableUriPermission(
            mDir.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val mLogfile = mDir.createFile("//MIME txt", "$currentDate.txt")
        outputStream = contentResolver.openOutputStream(mLogfile!!.uri)
        isWriting = true
    }

    fun stopWriting() {
        isWriting = false
        outputStream?.flush()
        outputStream?.close()
    }

    override fun onGpsStatusChanged(event: Int) {
        var mSatellites = 0
        var mSatellitesInFix = 0

        if ((ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(
                (context as Activity),
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ),
                2
            )
        }
        for (mSatellite in locationManager?.getGpsStatus(null)?.satellites!!) {
            if (mSatellite.usedInFix()) {
                mSatellitesInFix ++
            }
            mSatellites ++
        }
        communicationCallbacks?.onGPSSatellites(mSatellites, mSatellitesInFix)
    }

}