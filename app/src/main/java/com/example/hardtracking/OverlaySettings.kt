package com.example.hardtracking

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object OverlaySettings {
    private const val PREFS_NAME = "overlay_settings"
    private const val KEY_ENABLED = "overlay_enabled"
    private const val KEY_POSITION_X = "overlay_position_x"
    private const val KEY_POSITION_Y = "overlay_position_y"

    fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun loadPosition(context: Context): Pair<Int, Int> {
        val prefs = prefs(context)
        val x = prefs.getInt(KEY_POSITION_X, 0)
        val y = prefs.getInt(KEY_POSITION_Y, 240)
        return x to y
    }

    fun savePosition(context: Context, x: Int, y: Int) {
        prefs(context).edit()
            .putInt(KEY_POSITION_X, x)
            .putInt(KEY_POSITION_Y, y)
            .apply()
    }

    fun intentForPermission(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
