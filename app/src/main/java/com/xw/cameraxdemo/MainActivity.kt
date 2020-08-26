package com.xw.cameraxdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.xw.cameraxdemo.view.CameraActivity
import com.xw.cameraxdemo.view.CameraVideoActivity

class MainActivity : AppCompatActivity() {

    // 跳转拍照按钮
    private lateinit var jumpPhotoBtn: Button
    // 跳转拍照和录视频按钮
    private lateinit var jumpPhotoAndVideoBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        jumpPhotoBtn = findViewById(R.id.jump_photo_btn)
        jumpPhotoAndVideoBtn = findViewById(R.id.jump_photo_video_btn)

        jumpPhotoBtn.setOnClickListener {
            jumpToCameraActivity()
        }

        jumpPhotoAndVideoBtn.setOnClickListener {
            jumpToCameraVideoActivity()
        }
    }

    private fun jumpToCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun jumpToCameraVideoActivity() {
        val intent = Intent(this, CameraVideoActivity::class.java)
        startActivity(intent)
    }
}


