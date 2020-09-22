package com.catanddev.ef_control

import android.content.pm.PackageManager
import android.icu.math.BigDecimal
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.location.Location;
import android.location.LocationManager;
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.Exception
import kotlin.math.round

lateinit var gpsSpeedTextView : TextView

class MainActivity : AppCompatActivity(), GPSCallback {

    private lateinit var gpsManager: GPSManager
    private var speed : Double = 0.0
    var isGPSEnabled : Boolean = false
    lateinit var locationManager : LocationManager
    var currentSpeed : Double = 0.0
    var kmphSpeed : Double = 0.0
    lateinit var  gpsSpeedTextView : TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gpsSpeedTextView = findViewById(R.id.gpsSpeedTextView)
        try {
            if (ContextCompat.checkSelfPermission(applicationContext,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    228)
            }
        } catch (e : Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }
        getCurrentSpeed()
    }

    fun getCurrentSpeed() {
        gpsSpeedTextView.text = "Wait for GPS ..."
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        gpsManager = GPSManager(this)
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGPSEnabled) {
            gpsManager.startListening(applicationContext)
            gpsManager.setGPSCallbck(this)
        } else {
            gpsManager.showSettingsAlert()
        }
    }

    override fun onGPSUpdate(location: Location) {
        speed = location.speed.toDouble()
        currentSpeed = round(speed, 3, BigDecimal.ROUND_HALF_UP)
        kmphSpeed = round(currentSpeed * 3.6, 3, BigDecimal.ROUND_HALF_UP)
        gpsSpeedTextView.text = kmphSpeed.toString() + " km/h"
    }

    override fun onDestroy() {
        gpsManager.stopListening()
        gpsManager.setGPSCallbck(null)
        super.onDestroy()
    }

    fun round(unrounded : Double, precision : Int, roundingMode : Int) : Double {
        val bd = BigDecimal(unrounded)
        val rounded = bd.setScale(precision, roundingMode)
        return rounded.toDouble()
    }
}