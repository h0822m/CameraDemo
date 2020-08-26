package com.xw.cameraxdemo.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.xw.cameraxdemo.R

/**
 * Create by zcr on 2020/8/26
 * 预览
 */
class PreViewActivity: AppCompatActivity() {

    // 照片
    private lateinit var photoView: ImageView
    // 视频
    private lateinit var videoView: VideoView

    companion object {
        private const val KEY_PREVIEW_URL = "key_preview_url"
        private const val KEY_IS_VIDEO = "key_is_video"
        fun start(activity: Activity, url: String, isVideo: Boolean) {
            val intent = Intent(activity, PreViewActivity::class.java)
            intent.putExtra(KEY_PREVIEW_URL, url)
            intent.putExtra(KEY_IS_VIDEO, isVideo)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_x)

        photoView = findViewById(R.id.image_preview)
        videoView = findViewById(R.id.video_preview)

        // 导航栏返回键
        val actionBar = supportActionBar
        actionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        val path = intent.getStringExtra(KEY_PREVIEW_URL)
        val isVideo = intent.getBooleanExtra(KEY_IS_VIDEO, false)

        if (!isVideo) {
            Glide.with(this).load(path).into(photoView)
            videoView.visibility = View.VISIBLE
        } else {
            photoView.visibility = View.GONE
            showVideo(path)
        }
    }

    private fun showVideo(path: String) {
        val mediaController = MediaController(this)
        videoView.setVideoPath(path)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)
        videoView.start()
    }

    // 返回键事件
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
    }
}