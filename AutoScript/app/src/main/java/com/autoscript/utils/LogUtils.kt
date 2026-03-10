package com.autoscript.utils

import android.util.Log
import com.autoscript.BuildConfig

object LogUtils {

    private const val DEFAULT_TAG = "AutoScript"

    private var isDebug = BuildConfig.DEBUG

    fun setDebug(debug: Boolean) {
        isDebug = debug
    }

    fun d(message: String) {
        d(DEFAULT_TAG, message)
    }

    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }

    fun i(message: String) {
        i(DEFAULT_TAG, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun w(message: String) {
        w(DEFAULT_TAG, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun e(message: String) {
        e(DEFAULT_TAG, message)
    }

    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    fun e(message: String, throwable: Throwable) {
        e(DEFAULT_TAG, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    fun v(message: String) {
        v(DEFAULT_TAG, message)
    }

    fun v(tag: String, message: String) {
        if (isDebug) {
            Log.v(tag, message)
        }
    }

    fun json(json: String) {
        if (isDebug) {
            val tag = "$DEFAULT_TAG-JSON"
            Log.d(tag, json)
        }
    }
}
