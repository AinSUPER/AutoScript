package com.autoscript.utils

import android.content.Context
import android.widget.Toast
import com.autoscript.AutoScriptApp

object ToastUtils {

    private var toast: Toast? = null

    fun show(message: String) {
        show(AutoScriptApp.getAppContext(), message)
    }

    fun show(context: Context, message: String) {
        toast?.cancel()
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
            show()
        }
    }

    fun show(context: Context, resId: Int) {
        show(context, context.getString(resId))
    }

    fun showLong(message: String) {
        showLong(AutoScriptApp.getAppContext(), message)
    }

    fun showLong(context: Context, message: String) {
        toast?.cancel()
        toast = Toast.makeText(context, message, Toast.LENGTH_LONG).apply {
            show()
        }
    }

    fun cancel() {
        toast?.cancel()
        toast = null
    }
}
