package com.bitvale.checkboxswitcherdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        switcher_two.setOnCheckedChangeListener {
            switcher_one.setChecked(!switcher_one.isChecked)
            switcher_three.setChecked(!switcher_three.isChecked)
        }
    }
}
