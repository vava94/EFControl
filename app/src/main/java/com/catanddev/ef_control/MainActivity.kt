package com.catanddev.ef_control

import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Icon
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : AppCompatActivity(),

    CommunicationCallbacks,
    ServiceConnection,
    AdapterView.OnItemSelectedListener {

    private lateinit var adapterStrings : Array<String>
    private var communicationService : CommunicationService? = null
    private lateinit var fabREC : FloatingActionButton
    private lateinit var mMenu: Menu
    private var permissionsGranted = false
    private var sharedPrefs : SharedPreferences? = null
    private lateinit var spinner0 : Spinner
    private lateinit var spinner1 : Spinner
    private lateinit var spinner2 : Spinner
    private lateinit var spinner3 : Spinner
    private lateinit var spinner4 : Spinner
    private lateinit var spinner5 : Spinner
    private lateinit var spinner6 : Spinner
    private lateinit var spinner7 : Spinner
    private lateinit var spinner8 : Spinner
    private lateinit var spinner9 : Spinner
    private lateinit var  textView0 : TextView
    private lateinit var  textView1 : TextView
    private lateinit var  textView2 : TextView
    private lateinit var  textView3 : TextView
    private lateinit var  textView4 : TextView
    private lateinit var  textView5 : TextView
    private lateinit var  textView6 : TextView
    private lateinit var  textView7 : TextView
    private lateinit var  textView8 : TextView
    private lateinit var  textView9 : TextView
    private var viewBindings = Array( 11) { _ -> ArrayList<TextView>() }

    override fun onBTConnected(connected: Boolean) {
        Handler(mainLooper).post() {
            val mItem = mMenu.getItem(0)
            mItem.isEnabled = true
            mItem.icon.colorFilter = PorterDuffColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.MULTIPLY)
            if (connected) {
                mItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_bluetooth_disable)
                fabREC.show()
            } else {
                mItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_bluetooth_connect)
                fabREC.hide()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapterStrings = arrayOf(
            getString(R.string.disabled_txt),
            getString(R.string.gps_speed_txt),
            getString(R.string.propeller_rpm_txt),
            getString(R.string.pwm_txt),
            getString(R.string.current_txt),
            getString(R.string.voltage_txt),
            getString(R.string.t_fet_txt),
            getString(R.string.time_txt),
            getString(R.string.t_bat_txt),
            getString(R.string.capcity_txt),
            getString(R.string.status_txt)
        )
        /// Инициализация элементов выбора отображения параметров
        spinner0 = findViewById(R.id.spinner0)
        spinner1 = findViewById(R.id.spinner1)
        spinner2 = findViewById(R.id.spinner2)
        spinner3 = findViewById(R.id.spinner3)
        spinner4 = findViewById(R.id.spinner4)
        spinner5 = findViewById(R.id.spinner5)
        spinner6 = findViewById(R.id.spinner6)
        spinner7 = findViewById(R.id.spinner7)
        spinner8 = findViewById(R.id.spinner8)
        spinner9 = findViewById(R.id.spinner9)
        /// Инициализация элементов отображения значений
        textView0 = findViewById(R.id.textView0)
        textView1 = findViewById(R.id.textView1)
        textView2 = findViewById(R.id.textView2)
        textView3 = findViewById(R.id.textView3)
        textView4 = findViewById(R.id.textView4)
        textView5 = findViewById(R.id.textView5)
        textView6 = findViewById(R.id.textView6)
        textView7 = findViewById(R.id.textView7)
        textView8 = findViewById(R.id.textView8)
        textView9 = findViewById(R.id.textView9)
        // Инициализация кнопок
        fabREC = findViewById(R.id.fabREC)
        fabREC.setOnClickListener {
            val mLogPath = sharedPrefs?.getString("LOG_PATH","").toString()
            if (mLogPath.isEmpty()) {
                Toast.makeText(this, getString(R.string.log_folder_error), Toast.LENGTH_LONG).show()
            } else {
                if (communicationService!!.isWriting) {
                    communicationService?.stopWriting()
                    fabREC.setImageIcon(Icon.createWithResource(this, R.drawable.ic_baseline_fiber_manual_record_24))
                } else {
                    communicationService?.startWriting(mLogPath)
                    fabREC.setImageIcon(Icon.createWithResource(this, R.drawable.ic_baseline_stop_24))
                }
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, adapterStrings)
        spinner0.adapter = adapter
        spinner1.adapter = adapter
        spinner2.adapter = adapter
        spinner3.adapter = adapter
        spinner4.adapter = adapter
        spinner5.adapter = adapter
        spinner6.adapter = adapter
        spinner7.adapter = adapter
        spinner8.adapter = adapter
        spinner9.adapter = adapter

        spinner0.onItemSelectedListener = this
        spinner1.onItemSelectedListener = this
        spinner2.onItemSelectedListener = this
        spinner3.onItemSelectedListener = this
        spinner4.onItemSelectedListener = this
        spinner5.onItemSelectedListener = this
        spinner6.onItemSelectedListener = this
        spinner7.onItemSelectedListener = this
        spinner8.onItemSelectedListener = this
        spinner9.onItemSelectedListener = this

        sharedPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE) ?: return
        if(savedInstanceState == null) {
            sharedPrefs?.getInt("VIEW_0", 1)?.let { spinner0.setSelection(it) }
            sharedPrefs?.getInt("VIEW_1", 2)?.let { spinner1.setSelection(it) }
            sharedPrefs?.getInt("VIEW_2", 3)?.let { spinner2.setSelection(it) }
            sharedPrefs?.getInt("VIEW_3", 4)?.let { spinner3.setSelection(it) }
            sharedPrefs?.getInt("VIEW_4", 6)?.let { spinner4.setSelection(it) }
            sharedPrefs?.getInt("VIEW_5", 5)?.let { spinner5.setSelection(it) }
            sharedPrefs?.getInt("VIEW_6", 9)?.let { spinner6.setSelection(it) }
            sharedPrefs?.getInt("VIEW_7", 8)?.let { spinner7.setSelection(it) }
            sharedPrefs?.getInt("VIEW_8", 7)?.let { spinner8.setSelection(it) }
            sharedPrefs?.getInt("VIEW_9", 10)?.let { spinner9.setSelection(it) }
        }
        else {
            spinner0.setSelection(savedInstanceState.getInt("S_0"))
            spinner1.setSelection(savedInstanceState.getInt("S_1"))
            spinner2.setSelection(savedInstanceState.getInt("S_2"))
            spinner3.setSelection(savedInstanceState.getInt("S_3"))
            spinner4.setSelection(savedInstanceState.getInt("S_4"))
            spinner5.setSelection(savedInstanceState.getInt("S_5"))
            spinner6.setSelection(savedInstanceState.getInt("S_6"))
            spinner7.setSelection(savedInstanceState.getInt("S_7"))
            spinner8.setSelection(savedInstanceState.getInt("S_8"))
            spinner9.setSelection(savedInstanceState.getInt("S_9"))
            textView0.text = savedInstanceState.getString("TV_0")
            textView1.text = savedInstanceState.getString("TV_1")
            textView2.text = savedInstanceState.getString("TV_2")
            textView3.text = savedInstanceState.getString("TV_3")
            textView4.text = savedInstanceState.getString("TV_4")
            textView5.text = savedInstanceState.getString("TV_5")
            textView6.text = savedInstanceState.getString("TV_6")
            textView7.text = savedInstanceState.getString("TV_7")
            textView8.text = savedInstanceState.getString("TV_8")
            textView9.text = savedInstanceState.getString("TV_9")
        }

        try {

            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED

            ) {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.BLUETOOTH,
                                android.Manifest.permission.BLUETOOTH_ADMIN
                        ),
                        1)
            } else {
                permissionsGranted = true

            }
        } catch (e : Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }

        if (permissionsGranted) {
            val intent = Intent(this, CommunicationService::class.java)
            bindService(intent, this, BIND_AUTO_CREATE)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        mMenu = menu!!
        return super.onCreateOptionsMenu(menu)
    }

    override fun onGPSSatellites(total: Int, inFix: Int) {
        Handler(mainLooper).post {
            satellitesTextView.text = ("$inFix/$total")
        }
    }

    override fun onGPSSpeed(speed: Double) {
        Handler(mainLooper).post {
            for (mTV in viewBindings[1]) {
                mTV.text = (speed.toString() + " " + getString(R.string.kmh_txt))
            }
        }
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view : View?, p2: Int, p3: Long) {
        var mTV: TextView? = null
        var mOptions = false
        if (adapterView != null) {
            when (adapterView.id) {
                R.id.spinner0 -> {
                    mTV = textView0
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_0", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner1 -> {
                    mTV = textView1
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_1", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner2 -> {
                    mTV = textView2
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_2", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner3 -> {
                    mTV = textView3
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_3", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner4 -> {
                    mTV = textView4
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_4", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner5 -> {
                    mTV = textView5
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_5", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner6 -> {
                    mTV = textView6
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_6", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner7 -> {
                    mTV = textView7
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_7", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner8 -> {
                    mTV = textView8
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_8", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
                R.id.spinner9 -> {
                    mTV = textView9
                    with(sharedPrefs?.edit()) {
                        this?.putInt("VIEW_9", p2)
                        this?.apply()
                    }
                    mOptions = true
                }
            }
            // Если изменено отображение информации
            if(mOptions && mTV != null) {
                if(p2 == 0) {
                    mTV.text = "---"
                } else {
                    mTV.text = ""
                }
                for (mVB in viewBindings) {
                    if (mVB.contains(mTV)) {
                        mVB.remove(mTV)
                        break
                    }
                }
                viewBindings[p2].add(mTV)
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {}

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if(id == R.id.action_bt_connect) {
            if (communicationService != null) {
                if (communicationService!!.isConnected) {
                    communicationService?.btDisconnect()
                } else {
                    val mAddress = sharedPrefs?.getString("DEFAULT_REMOTE", "").toString()
                    if (mAddress.isEmpty()) {
                        Toast.makeText(this, getString(R.string.no_remote_message), Toast.LENGTH_SHORT).show()
                    } else {
                        item.isEnabled = false
                        item.icon.colorFilter = PorterDuffColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.MULTIPLY)
                        communicationService?.btConnect(mAddress, sharedPrefs?.getString("DEFAULT_UUID", "").toString())
                    }
                }
            }
        }
        else if(id == R.id.action_settings) {
            val mAddress = sharedPrefs?.getString("DEFAULT_REMOTE", "").toString()
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra("DEFAULT_REMOTE", mAddress)
                putExtra("LOG_PATH", sharedPrefs?.getString("LOG_PATH","").toString())
            }
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsGranted = true
        for(result in grantResults) {
            if(result == -1) {
                permissionsGranted = false
                Toast.makeText(this, getString(R.string.permissions_error_txt), Toast.LENGTH_LONG).show()
                break
            }
        }
        if(permissionsGranted) {
            val intent = Intent(this, CommunicationService::class.java)
            bindService(intent, this, BIND_AUTO_CREATE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString("TV_0", textView0.text.toString())
        outState.putString("TV_1", textView1.text.toString())
        outState.putString("TV_2", textView2.text.toString())
        outState.putString("TV_3", textView3.text.toString())
        outState.putString("TV_4", textView4.text.toString())
        outState.putString("TV_5", textView5.text.toString())
        outState.putString("TV_6", textView6.text.toString())
        outState.putString("TV_7", textView7.text.toString())
        outState.putString("TV_8", textView8.text.toString())
        outState.putString("TV_9", textView9.text.toString())
        outState.putInt("S_0", spinner0.selectedItemPosition)
        outState.putInt("S_1", spinner0.selectedItemPosition)
        outState.putInt("S_2", spinner0.selectedItemPosition)
        outState.putInt("S_3", spinner0.selectedItemPosition)
        outState.putInt("S_4", spinner0.selectedItemPosition)
        outState.putInt("S_5", spinner0.selectedItemPosition)
        outState.putInt("S_6", spinner0.selectedItemPosition)
        outState.putInt("S_7", spinner0.selectedItemPosition)
        outState.putInt("S_8", spinner0.selectedItemPosition)
        outState.putInt("S_9", spinner0.selectedItemPosition)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        communicationService = (service as CommunicationService.CSBinder).getService()
        communicationService!!.setCallbacks(this)
        if(!communicationService!!.isRunning) {
            val intent = Intent(this, CommunicationService::class.java)
            communicationService!!.context = this
            startService(intent)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        communicationService = null
    }

    override fun onStop() {

        if(!communicationService?.isWriting!!) {
            val intent = Intent(this, CommunicationService::class.java)
            communicationService?.context = this
            stopService(intent)
        }
        super.onStop()
    }



    override fun onBTData(data: ByteArray) {
        Handler(Looper.getMainLooper()).post {
            for (i in 2..10) {
                val mVB = viewBindings[i]
                for (mTV in mVB) {
                    when (i) {
                        2 -> mTV.text = (data[1].toInt() * 100).toString()   //RPM
                        3 -> mTV.text = (data[4].toInt().toString() + "%")   //PWM
                        4 -> mTV.text = (data[2].toInt().toString() + " А")  //I
                        5 -> mTV.text = (data[3].toInt().toString() + " V")  //U
                        6 -> mTV.text = (data[5].toInt().toString() + " °C") //T Fet
                        7 -> mTV.text = (data[7].toInt().toString() + " M")  //Time
                        8 -> mTV.text = (data[6].toInt().toString() + " °C") //T batt
                        9 -> mTV.text = (data[8].toInt().toString() + "Ah")  //Capacity
                        10 -> mTV.text = data[9].toInt().toString()          //Status
                    }
                }
            }
        }
    }
}