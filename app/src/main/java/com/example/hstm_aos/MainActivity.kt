package com.example.hstm_aos

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.hstm_aos.Fragment.GuideAndHelpFragment
import com.example.hstm_aos.Fragment.HomeFragment
import com.example.hstm_aos.ble.BleManager
import com.example.hstm_aos.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tabContainer: LinearLayout
    private val tabs = mutableListOf<View>()
    lateinit var bleManager: BleManager
        private set

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        requestBlePermission()
        bleManager = BleManager(this)
        bleManager.startScan(clear = true)

        tabContainer = findViewById(R.id.tabContainer)

        addTab("Home", R.drawable.inno_home_icon, HomeFragment())
        addTab("Guide & Help", R.drawable.inno_guide_and_help_icon, GuideAndHelpFragment())

        selectTab(0)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
    }

    private fun requestBlePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                1001
            )
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1002
            )
        }
    }


    private fun addTab(title: String, iconRes: Int, fragment: Fragment) {
        val tab = layoutInflater.inflate(R.layout.view_custom_tab, tabContainer, false)

        val icon = tab.findViewById<ImageView>(R.id.tabIcon)
        val text = tab.findViewById<TextView>(R.id.tabText)

        icon.setImageResource(iconRes)
        text.text = title

        val index = tabs.size
        tabs.add(tab)

        tab.setOnClickListener {
            selectTab(index)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = resources.displayMetrics.density.times(10).toInt()

        tabContainer.addView(tab, params)
    }

    private fun selectTab(selectedIndex: Int) {
        tabs.forEachIndexed { index, view ->
            val icon = view.findViewById<ImageView>(R.id.tabIcon)
            val text = view.findViewById<TextView>(R.id.tabText)

            if (index == selectedIndex) {
                view.setBackgroundResource(R.drawable.bg_tab_selected)
                text.visibility = View.VISIBLE
                icon.setColorFilter(Color.WHITE)

                view.setPadding(
                    dp(20),
                    dp(10),
                    dp(20),
                    dp(10)
                )

            } else {
                view.setBackgroundResource(R.drawable.bg_tab_unselected)
                text.visibility = View.GONE
                icon.setColorFilter(Color.GRAY)

                view.setPadding(
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10)
                )
            }
        }
    }

    //공통함수로 나중에 뺴야함
    private fun dp(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

}
