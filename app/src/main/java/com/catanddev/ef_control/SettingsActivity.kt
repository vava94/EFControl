package com.catanddev.ef_control

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

private lateinit var remoteSpinner: Spinner
private lateinit var allDevices: MutableSet<BluetoothDevice>

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val mAddress = intent.getStringExtra("DEFAULT_REMOTE")
        remoteSpinner = findViewById(R.id.remote_spinner)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            // device doesn't support bluetooth
        } else {

            // bluetooth is off, ask user to on it.
            if (!bluetoothAdapter.isEnabled) {
                val enableAdapter = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableAdapter, 0)
            }

            allDevices = bluetoothAdapter.bondedDevices
            val mAdapterList = ArrayList<String>()
            mAdapterList.add("---")
            if (allDevices.size > 0) {
                var mIndex = 0
                var mSet = false
                for(mCurrentDevice in allDevices) {
                    mAdapterList.add(mCurrentDevice.name)
                    if(!mSet) mIndex++
                    if(mCurrentDevice.address == mAddress) mSet = true
                }
                remoteSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mAdapterList)
                if (mSet) remoteSpinner.setSelection(mIndex)
            }

        }
        title = getString(R.string.action_settings)
    }

    override fun onResume() {
        remoteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val sharedPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    if (p2 == 0) putString("DEFAULT_REMOTE", "")
                    else {
                        putString("DEFAULT_REMOTE", allDevices.elementAt(p2 - 1).address)
                        putString("DEFAULT_UUID", allDevices.elementAt(p2 - 1).uuids[0].toString())
                    }
                    apply()
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }
        super.onResume()
    }

}