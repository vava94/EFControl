package com.catanddev.ef_control

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.TextView

lateinit var gpsSpeedTextView : TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gpsSpeedTextView = findViewById(R.id.gpsSpeedTextView)

    }
}