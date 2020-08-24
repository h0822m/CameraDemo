package com.xw.cameraxdemo.view

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
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

typealias LumListener = (lum: Double) -> Unit

/**
 * Create by zcr on 2020/8/24
 */
class CameraActivity: AppCompatActivity() {

    private lateinit var cameraRecordView: PictureAndRecordView
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

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)

        // 相机单线程
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraRecordView = findViewById(R.id.camera_btn)
        previewView = findViewById(R.id.previewView)
        if (Build.VERSION.SDK_INT <= 22) {
            starCamera()
        }

        if (allPermissionGranted()) {
            starCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraRecordView.setOnPictureAndRecordListener(object : OnPictureAndRecordListener {

            override fun onTakePicture() {
                // 点击拍照
                takePhoto()
            }

            override fun onRecordVideo() {
                // 长按录制视频
                takeVideo()
            }

            override fun onFinish() {

            }

        })

        // 照片存储文件
        photoOutputDirectory = getPhotoOutputDirectory()

        // 视频存储文件
        videoOutputDirectory = getVideoOutputDirectory()
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

    private fun starCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            preView = Preview.Builder()
                .build()

            imageCapture = ImageCapture.Builder()
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer {lum ->
                        Log.d(TAG, "Average luminosity: $lum")
                    })
                }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preView, imageCapture, imageAnalyzer)

                preView?.setSurfaceProvider(previewView?.createSurfaceProvider())

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // 点击拍照
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(photoOutputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile)
            .build()

        imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {

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
    private fun takeVideo() {
        val videoCapture = videoCapture ?: return
        val videoFile = File(videoOutputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".map4")


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