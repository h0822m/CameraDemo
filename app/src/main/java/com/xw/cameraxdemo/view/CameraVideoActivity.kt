package com.xw.cameraxdemo.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.VideoCapture
import androidx.camera.view.CameraView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xw.cameraxdemo.OnPictureAndRecordListener
import com.xw.cameraxdemo.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Create by zcr on 2020/8/26
 * 拍照与录视频
 */
class CameraVideoActivity: AppCompatActivity() {

    // 照片路径
    private lateinit var photoOutputDirectory: File
    // 视频路径
    private lateinit var videoOutputDirectory: File
    // 拍照与录视频自定义按钮
    private lateinit var cameraRecordView: PictureAndRecordView
    // preview
    private lateinit var cameraView: CameraView
    // 相机转换按钮
    private lateinit var cameraSwitchBtn: ImageButton
    // 闪光转换按钮
    private lateinit var flashSwitchBtn: ImageButton
    // executor
    private lateinit var executorService: ExecutorService
    // 是否拍照,默认不是
    private var isTakingPicture = false
    // CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    // 默认自动闪光模式
    private var flashMode = 0
    private val flashIconArray = arrayOf(R.drawable.ic_flash_auto, R.drawable.ic_flash_on, R.drawable.ic_flash_off)

    companion object {
        private const val TAG = "CameraVideoActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_video_x)

        // 导航栏返回键
        val actionBar = supportActionBar
        actionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        cameraView = findViewById(R.id.previewView)
        cameraSwitchBtn = findViewById(R.id.camera_switch_btn)
        flashSwitchBtn = findViewById(R.id.camera_flash_btn)
        cameraRecordView = findViewById(R.id.camera_btn)

        executorService = Executors.newSingleThreadExecutor()
        cameraView.bindToLifecycle(this)

        // 相机拍摄方向
        cameraSwitchBtn.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            cameraView.cameraLensFacing = lensFacing
        }

        // 闪光
        flashSwitchBtn.setOnClickListener {
            flashMode = (flashMode + 1) % 3
            flashSwitchBtn.setImageResource(flashIconArray[flashMode])
            cameraView.flash = getMode(flashMode)
        }

        if (Build.VERSION.SDK_INT <= 22) {
            // 拍照与视频
            takePictureAndVideo()
        }

        if (allPermissionGranted()) {
            // 拍照与视频
            takePictureAndVideo()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        photoOutputDirectory = getPhotoOutputDirectory()
        videoOutputDirectory = getVideoOutputDirectory()

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

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun takePictureAndVideo() {
        cameraRecordView.setOnPictureAndRecordListener(object : OnPictureAndRecordListener {
            override fun onTakePicture() {
                isTakingPicture = true
                // 拍照
                takePicture()
            }

            override fun onRecordVideo() {
                isTakingPicture = false
                // 视频
                takeVideo()
            }

            override fun onFinish() {
                // 停止录制视频
                cameraView.stopRecording()
            }
        })
    }

    // 拍照
    private fun takePicture() {
        val photoFile = File(photoOutputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")
        cameraView.captureMode = CameraView.CaptureMode.IMAGE
        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        val photoOutputFileOptions = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .setMetadata(metadata)
            .build()

        cameraView.takePicture(photoOutputFileOptions, executorService, object : ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                var saveUrl = outputFileResults.savedUri
                if (saveUrl == null) saveUrl = Uri.fromFile(photoFile)

                val photoOutputPath = photoFile.absolutePath
                // 保存
                onFileSaved(saveUrl, photoOutputPath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
            }

        })

    }

    // 视频
    @SuppressLint("RestrictedApi")
    private fun takeVideo() {
        val videoFile = File(photoOutputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".mp4")
        cameraView.captureMode = CameraView.CaptureMode.VIDEO
        val videoOutputFileOptions = VideoCapture
            .OutputFileOptions
            .Builder(videoFile)
            .build()

        cameraView.startRecording(videoOutputFileOptions, executorService, object : VideoCapture.OnVideoSavedCallback {

            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                var saveUrl = outputFileResults.savedUri
                if (saveUrl == null) saveUrl = Uri.fromFile(videoFile)

                val photoOutputPath = videoFile.absolutePath
                // 保存
                onFileSaved(saveUrl, photoOutputPath)
            }

            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                Log.e(TAG, "Video capture failed: $message", cause)
            }
        })
    }

    private fun onFileSaved(saveUrl: Uri?, filePath: String) {
        if (saveUrl == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            sendBroadcast(Intent(android.hardware.Camera.ACTION_NEW_PICTURE, saveUrl))
        }

        val mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(saveUrl.path))
        MediaScannerConnection.scanFile(applicationContext, arrayOf(File(saveUrl.path).absolutePath), arrayOf(mimeTypeFromExtension)) { path, uri ->
            Log.d(TAG, "Image capture scanned into media store: $uri path: $path")
        }

        PreViewActivity.start(this, filePath, !isTakingPicture)
    }

    private fun getMode(position: Int): Int {
        return when(position) {
            0 -> ImageCapture.FLASH_MODE_AUTO
            1 -> ImageCapture.FLASH_MODE_ON
            2 -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    // 照片存储位置
    private fun getPhotoOutputDirectory(): File {
        val photoDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_camera_photo_dir)).apply { mkdir() }
        }
        return if (null != photoDir && photoDir.exists()) photoDir else filesDir
    }

    // 视频存储位置
    private fun getVideoOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_camera_video_dir)).apply { mkdir() }
        }
        return if (null != mediaDir && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        executorService.shutdown()
        super.onDestroy()
    }
}