package com.xw.cameraxdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.xw.cameraxdemo.view.CameraActivity

class MainActivity : AppCompatActivity() {

    private lateinit var jumpBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        jumpBtn = findViewById(R.id.jump_btn)

        jumpBtn.setOnClickListener {
            jumpToCameraActivity()
        }
    }

    private fun jumpToCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }
}


