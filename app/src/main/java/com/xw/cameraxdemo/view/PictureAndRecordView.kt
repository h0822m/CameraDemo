package com.xw.cameraxdemo.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.xw.cameraxdemo.OnPictureAndRecordListener
import com.xw.cameraxdemo.R
import com.xw.cameraxdemo.util.SizeUtils

/**
 * Create by zcr on 2020/8/24
 * 拍照录像按钮
 */
class PictureAndRecordView: View, View.OnLongClickListener, View.OnClickListener {

    private var mBgColor = 0
    private var mStrokeColor = 0
    private var mStrokeWidth = 0
    private var mDuration = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRadius = 0
    private var mProgressValue = 0
    private var isRecording = false
    private var mArcRectF: RectF? = null
    private var mBgPaint: Paint? = null
    private var mProgressPaint: Paint? = null
    private var mStartRecordTime = 0L
    private var onPictureAndRecordListener: OnPictureAndRecordListener? = null


    companion object {
        private const val PROGRESS_INTERVAL = 100L
        private const val TAG = "PictureAndRecordView"
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.PictureAndRecordView)
        mBgColor = typeArray.getColor(R.styleable.PictureAndRecordView_bg_color, Color.WHITE)
        mStrokeColor = typeArray.getColor(R.styleable.PictureAndRecordView_stroke_color, Color.GREEN)
        mStrokeWidth = typeArray.getDimensionPixelOffset(R.styleable.PictureAndRecordView_stroke_width, SizeUtils.dp2px(5f))
        mDuration = typeArray.getInteger(R.styleable.PictureAndRecordView_duration, 10)
        mRadius = typeArray.getDimensionPixelOffset(R.styleable.PictureAndRecordView_radius, SizeUtils.dp2px(40f))
        typeArray.recycle()

        mBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mBgPaint?.style = Paint.Style.FILL
        mBgPaint?.color = mBgColor

        mProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mProgressPaint?.style = Paint.Style.STROKE
        mProgressPaint?.color = mStrokeColor
        mProgressPaint?.strokeWidth = mStrokeWidth.toFloat()

        setEvent()
    }

    private fun setEvent() {
        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                mProgressValue++
                postInvalidate()
                if (mProgressValue < mDuration * 10) {
                    sendEmptyMessageDelayed(0, PROGRESS_INTERVAL)
                } else {
                    finishRecord()
                }
            }
        }

        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                mStartRecordTime = System.currentTimeMillis()
                handler.sendEmptyMessage(0)
            } else if (event.action == MotionEvent.ACTION_UP) {
                val duration = System.currentTimeMillis() - mStartRecordTime
                // 是否大于系统设定的最小长按时间
                if (duration > ViewConfiguration.getLongPressTimeout()) {
                    finishRecord()
                }
                handler.removeCallbacksAndMessages(null)
                isRecording = false
                mStartRecordTime = 0L
                mProgressValue = 0
                postInvalidate()
            }
            false
        }
        setOnClickListener(this)
        setOnLongClickListener(this)
    }

    private fun finishRecord() {
        onPictureAndRecordListener?.let {
            it.onFinish()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        mArcRectF = RectF(mStrokeWidth / 2f, mStrokeWidth / 2f, mWidth - mStrokeWidth / 2f, mHeight - mStrokeWidth / 2f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(mWidth / 2f, mHeight / 2f, mRadius.toFloat(), mBgPaint ?: Paint())

        if (isRecording) {
            canvas.drawCircle(mWidth / 2f, mHeight / 2f, mRadius / 10f, mBgPaint ?: Paint())
            val sweepAngle = 360f * mProgressValue / (mDuration * 10)
            Log.d(TAG, "sweepAngle------$sweepAngle")
            canvas.drawArc(mArcRectF ?: RectF(), -90f, sweepAngle, false, mProgressPaint ?: Paint())
        }
    }

    override fun onClick(v: View?) {
        onPictureAndRecordListener?.let {
            it.onTakePicture()
        }
    }

    override fun onLongClick(v: View?): Boolean {
        isRecording = true
        onPictureAndRecordListener?.let {
            it.onRecordVideo()
        }
        return true
    }

    fun setOnPictureAndRecordListener(onPictureAndRecordListener: OnPictureAndRecordListener) {
        this.onPictureAndRecordListener = onPictureAndRecordListener
    }
}