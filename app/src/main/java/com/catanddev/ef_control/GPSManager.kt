package com.catanddev.ef_control

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import java.lang.Exception

interface GPSCallback {
    fun onGPSUpdate(location : Location)
}

/// TODO: Заменть на GNSS Measurement
class GPSManager(var context: Context) : android.location.GpsStatus.Listener {

    private val gpsMinTime : Long = 500
    private val gpsMinDistance = 0.0f
    private var locationManager : LocationManager? = null
    private var locationListener: LocationListener? = null
    var gpsCallback: GPSCallback? = null


    init {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                gpsCallback?.onGPSUpdate(location)
            }
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
    }

    fun showSettingsAlert() {
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

    override fun onGpsStatusChanged(p0: Int) {
        var mSattelites = 0
        var mSattelitesInFix = 0

        if ((ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
            (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(
                (context as Activity),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                228)
        }
        val mTimeToFix = locationManager?.getGpsStatus(null)?.timeToFirstFix
        Log.i("GPS", "Time to first fix = " + mTimeToFix.toString())
        for (mSattelite in locationManager?.getGpsStatus(null)?.satellites!!) {
            if (mSattelite.usedInFix()) {
                mSattelitesInFix ++
            }
            mSattelites ++
        }
        Log.i("GPS", "$mSattelites Used In Last Fix ( $mSattelites)")
    }

    fun startListening(context: Context) {
        if(locationManager == null) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        val mCriteria = Criteria()
        mCriteria.accuracy = Criteria.ACCURACY_FINE
        mCriteria.isSpeedRequired = true
        mCriteria.isAltitudeRequired = false
        mCriteria.isBearingRequired = false
        mCriteria.isCostAllowed = true
        mCriteria.powerRequirement = Criteria.POWER_LOW
        val mBestProvider = locationManager?.getBestProvider(mCriteria, true)

        if ((ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
            (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(
                (context as Activity),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                228)
        }
        if (mBestProvider != null && mBestProvider.isNotEmpty()) {
            locationManager?.requestLocationUpdates(mBestProvider, gpsMinTime, gpsMinDistance, locationListener!!)
        } else {
            val mProviders = locationManager?.getProviders(true)
            if (mProviders != null) {
                for(mProvider in mProviders) {
                    locationManager?.requestLocationUpdates(mProvider, gpsMinTime, gpsMinDistance, locationListener!!)
                }
            }
        }
    }

    fun stopListening() {
        try {
            if(locationManager != null && locationListener != null) {
                locationManager?.removeUpdates(locationListener!!)
            }
            locationManager = null
        } catch (mException : Exception) {
            mException.printStackTrace()
        }
    }
}