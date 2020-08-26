package com.xw.cameraxdemo.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.Surface
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.extensions.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xw.cameraxdemo.OnPictureAndRecordListener
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import com.xw.cameraxdemo.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias LumListener = (lum: Double) -> Unit

/**
 * Create by zcr on 2020/8/24
 */
class CameraActivity: AppCompatActivity() {

    // 拍照与录视频自定义按钮
    private lateinit var cameraRecordView: PictureAndRecordView
    // 拍照按钮
    private lateinit var cameraBtn: ImageButton
    // 相机转换按钮
    private lateinit var cameraSwitchBtn: ImageButton
    // 闪光转换按钮
    private lateinit var flashSwitchBtn: ImageButton
    // 默认自动闪光模式
    private var flashMode = 0
    private val flashIconArray = arrayOf(R.drawable.ic_flash_auto, R.drawable.ic_flash_on, R.drawable.ic_flash_off)
    private var preView: Preview? = null
    private var camera: Camera? = null
    private var previewView: PreviewView? = null
    // 拍照
    private var imageCapture: ImageCapture? = null
    // 视频
    private var videoCapture: VideoCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var photoOutputDirectory: File
    private lateinit var videoOutputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    // 是否拍照
    private var isTakePicture = false
    // CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        // 宽高比
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)

        // 导航栏返回键
        val actionBar = supportActionBar
        actionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        // 相机单线程
        cameraExecutor = Executors.newSingleThreadExecutor()

//        cameraRecordView = findViewById(R.id.camera_btn)
        cameraBtn = findViewById(R.id.camera_btn)
        previewView = findViewById(R.id.previewView)
        cameraSwitchBtn = findViewById(R.id.camera_switch_btn)
        flashSwitchBtn = findViewById(R.id.camera_flash_btn)

        if (Build.VERSION.SDK_INT <= 22) {
            starCamera()
        }

        if (allPermissionGranted()) {
            starCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        // 由于官方api bind lifecycle 不支持多个UseCase 弃用这种方案
//        cameraRecordView.setOnPictureAndRecordListener(object : OnPictureAndRecordListener {
//
//            override fun onTakePicture() {
//                isTakePicture = true
//                // 点击拍照
//                takePhoto()
//            }
//
//            override fun onRecordVideo() {
//                isTakePicture = false
//                // 长按录制视频
//                takeVideo()
//            }
//
//            @SuppressLint("RestrictedApi")
//            override fun onFinish() {
//                // 视频录制完成
//                videoCapture?.stopRecording()
//            }
//
//        })

        // 相机拍摄方向
        cameraSwitchBtn.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            starCamera()
        }

        // 闪光
        flashSwitchBtn.setOnClickListener {
            flashMode = (flashMode + 1) % 3
            flashSwitchBtn.setImageResource(flashIconArray[flashMode])
            imageCapture?.flashMode = getMode(flashMode)
        }

        cameraBtn.setOnClickListener {
            // 拍照
            takePhoto()
        }

        // 照片存储文件
        photoOutputDirectory = getPhotoOutputDirectory()

        // 视频存储文件
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

    private fun getMode(position: Int): Int {
        return when(position) {
            0 -> ImageCapture.FLASH_MODE_AUTO
            1 -> ImageCapture.FLASH_MODE_ON
            2 -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                starCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun starCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        try {
            cameraProviderFuture.addListener(Runnable {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // 获取屏幕的分辨率
                val displayMetrics = DisplayMetrics()
                previewView?.display?.getRealMetrics(displayMetrics)

                // 获取宽高比
                val screenAspectRatio = aspectRatio(displayMetrics.widthPixels, displayMetrics.heightPixels)

                // 旋转方向
                val rotation = previewView?.display?.rotation ?: Surface.ROTATION_0

                if (cameraProvider == null) {
                    Toast.makeText(this, "相机初始化失败", Toast.LENGTH_SHORT).show()
                    return@Runnable
                }

                // 相机选择
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                // 预览builder
                val preViewBuilder = Preview.Builder()
                // 设置预览外部扩展
                setPreviewExtender(preViewBuilder, cameraSelector)
                preView = preViewBuilder
                    // 设置宽高比
                    .setTargetAspectRatio(screenAspectRatio)
                    // 设置当前屏幕的旋转
                    .setTargetRotation(rotation)
                    .build()

                // 拍照builder
                val imgCaptureBuilder = ImageCapture.Builder()
                // 设置拍照外部扩展
                setImageCaptureExtender(imgCaptureBuilder, cameraSelector)
                imageCapture = imgCaptureBuilder
                    // 优化捕获速度，但是可能会降低照片质量
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    // 设置宽高比
                    .setTargetAspectRatio(screenAspectRatio)
                    // 设置初始旋转角度
                    .setTargetRotation(rotation)
                    .build()

                // 视频builder
                videoCapture = VideoCapture.Builder()
                    // 设置宽高比
                    .setTargetAspectRatio(screenAspectRatio)
                    // 设置当前旋转
                    .setTargetRotation(rotation)
                    // 分辨率
                    .setVideoFrameRate(25)
                    // bit率
                    .setAudioBitRate(3 * 1024 * 1024)
                    .build()

                imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, LuminosityAnalyzer {lum ->
                            Log.d(TAG, "Average luminosity: $lum")
                        })
                    }

                cameraProvider.unbindAll()
                // 目前官方api不支持这样的bind，弃用该种方案来拍照和录视频
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preView, imageCapture, /*videoCapture,*/ imageAnalyzer)

                preView?.setSurfaceProvider(previewView?.createSurfaceProvider())

            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            e.printStackTrace()
        }
    }

    // 点击拍照
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(photoOutputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        val photoOutputOption = ImageCapture.OutputFileOptions.Builder(photoFile)
            .build()

        imageCapture.takePicture(photoOutputOption, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val saveUrl = Uri.fromFile(photoFile)
                val msg = "Photo capture succeeded: $saveUrl"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exception.message}")
            }

        })
    }

    // 长按录视频
    @SuppressLint("RestrictedApi")
    private fun takeVideo() {
        val videoCapture = videoCapture ?: return
        val videoFile = File(videoOutputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".mp4")
        val videoOutputOption = VideoCapture.OutputFileOptions.Builder(videoFile)
            .build()

        videoCapture.startRecording(videoOutputOption, ContextCompat.getMainExecutor(this), object : VideoCapture.OnVideoSavedCallback {

            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                val saveUrl = Uri.fromFile(videoFile)
                val msg = "Video capture succeeded: $saveUrl"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
            }

            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                Log.e(TAG, "Video capture failed: $message")
            }
        })
    }

    // 获取宽高比
    private fun aspectRatio(widthPixels: Int, heightPixels: Int): Int {
        val preViewRatio = max(widthPixels, heightPixels).toDouble() / min(widthPixels, heightPixels).toDouble()
        if (abs(preViewRatio - RATIO_4_3_VALUE) <= abs(preViewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    // 给预览设置外部扩展
    private fun setPreviewExtender(builder: Preview.Builder, cameraSelector: CameraSelector) {

        // 自动模式
        val autoPreviewExtender = AutoPreviewExtender.create(builder)
        if (autoPreviewExtender.isExtensionAvailable(cameraSelector)) {
            autoPreviewExtender.enableExtension(cameraSelector)
        }

        // 散景模式
        val bokehPreviewExtender = BokehPreviewExtender.create(builder)
        if (bokehPreviewExtender.isExtensionAvailable(cameraSelector)) {
            bokehPreviewExtender.enableExtension(cameraSelector)
        }

        // HDR模式
        val hdrPreviewExtender = HdrPreviewExtender.create(builder)
        if (hdrPreviewExtender.isExtensionAvailable(cameraSelector)) {
            hdrPreviewExtender.enableExtension(cameraSelector)
        }

        // 美颜模式
        val beautyPreviewExtender = BeautyPreviewExtender.create(builder)
        if (beautyPreviewExtender.isExtensionAvailable(cameraSelector)) {
            beautyPreviewExtender.enableExtension(cameraSelector)
        }

        // 夜晚模式
        val nightPreviewExtender = NightPreviewExtender.create(builder)
        if (nightPreviewExtender.isExtensionAvailable(cameraSelector)) {
            nightPreviewExtender.enableExtension(cameraSelector)
        }
    }

    // 给拍照设置外部扩展
    private fun setImageCaptureExtender(builder: ImageCapture.Builder, cameraSelector: CameraSelector) {

        // 自动模式
        val autoImageCaptureExtender = AutoImageCaptureExtender.create(builder)
        if (autoImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            autoImageCaptureExtender.enableExtension(cameraSelector)
        }

        // 散景模式
        val bokehImageCaptureExtender = BokehImageCaptureExtender.create(builder)
        if (bokehImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            bokehImageCaptureExtender.enableExtension(cameraSelector)
        }

        // HDR模式
        val hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder)
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            hdrImageCaptureExtender.enableExtension(cameraSelector)
        }

        // 美颜模式
        val beautyImageCaptureExtender = BeautyImageCaptureExtender.create(builder)
        if (beautyImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            beautyImageCaptureExtender.enableExtension(cameraSelector)
        }

        // 夜晚模式
        val nightImageCaptureExtender = NightImageCaptureExtender.create(builder)
        if (nightImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            nightImageCaptureExtender.enableExtension(cameraSelector)
        }
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // 照片存储位置
    private fun getPhotoOutputDirectory(): File {
        val photoDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdir() }
        }
        return if (null != photoDir && photoDir.exists()) photoDir else filesDir
    }

    // 视频存储位置
    private fun getVideoOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_video_dir)).apply { mkdir() }
        }
        return if (null != mediaDir && mediaDir.exists()) mediaDir else filesDir
    }

    private class LuminosityAnalyzer(private val listener: LumListener): ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }
        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map {
                it.toInt() and 0xFF
            }
            val lum = pixels.average()
            listener(lum)
            image.close()
        }

    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}