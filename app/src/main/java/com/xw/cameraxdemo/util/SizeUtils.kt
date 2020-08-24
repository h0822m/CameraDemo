package com.xw.cameraxdemo.util

import android.content.res.Resources

/**
 * Create by zcr on 2020/8/24
 * 尺寸工具类
 */
class SizeUtils {

    companion object {
        /**
         * dp to px
         */
        fun dp2px(dpValue: Float): Int {
            val scale = Resources.getSystem().displayMetrics.density
            return (dpValue * scale + 0.5f).toInt()
        }

        /**
         * px to dp
         */
        fun px2dp(pxValue: Float): Int {
            val scale = Resources.getSystem().displayMetrics.density
            return (pxValue / scale + 0.5f).toInt()
        }

        /**
         * sp to px
         */
        fun sp2px(spValue: Float): Int {
            val fontScale = Resources.getSystem().displayMetrics.scaledDensity
            return (spValue * fontScale + 0.5f).toInt()
        }

        /**
         * px to sp
         */
        fun px2sp(pxValue: Float): Int {
            val fontScale = Resources.getSystem().displayMetrics.scaledDensity
            return (pxValue / fontScale + 0.5f).toInt()
        }
    }
}