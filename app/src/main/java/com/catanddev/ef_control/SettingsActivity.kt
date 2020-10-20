package com.catanddev.ef_control

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile

class SettingsActivity : AppCompatActivity() {

    private lateinit var allDevices: MutableSet<BluetoothDevice>
    private lateinit var folderButton: ImageButton
    private lateinit var remoteSpinner: Spinner
    private lateinit var pathTextView : TextView

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            0 -> {
                if (resultCode == RESULT_OK) {
                    val treeUri: Uri? = data?.data
                    val pickedDir: DocumentFile? = treeUri?.let { DocumentFile.fromTreeUri(this, it) }
                    grantUriPermission(packageName, treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(treeUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val _a = pickedDir?.listFiles()
                    val sharedPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
                    val aaaPath = data.dataString
                    with(sharedPrefs.edit()) {
                        putString("LOG_PATH", data.dataString)
                        apply()
                    }
                    var mPath = pickedDir!!.uri.lastPathSegment.toString()
                    mPath = if(mPath[mPath.length - 1] == ':') mPath.removeRange(mPath.length - 1, mPath.length) else mPath.replace(":", "/")
                    pathTextView.text = ("/storage/$mPath")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val mAddress = intent.getStringExtra("DEFAULT_REMOTE")
        pathTextView = findViewById(R.id.pathTextView)
        var mPath = Uri.parse(intent.getStringExtra("LOG_PATH")).lastPathSegment.toString()
        mPath = if(mPath[mPath.length - 1] == ':') mPath.removeRange(mPath.length - 1, mPath.length) else mPath.replace(":", "/")
        pathTextView.text = ("/storage/$mPath")
        folderButton = findViewById(R.id.folderButton)
        folderButton.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                        (this as Activity),
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        2)

            }
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 0)
        }

        remoteSpinner = findViewById(R.id.remote_spinner)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            // device doesn't support bluetooth
        } else {

            // bluetooth is off, ask user to on it.
            if (!bluetoothAdapter.isEnabled) {
                val enableAdapter = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableAdapter)
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