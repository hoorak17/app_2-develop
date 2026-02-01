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
    private const val KEY_SIZE_DP = "overlay_size_dp"
    private const val KEY_ALPHA = "overlay_alpha"
    private const val DEFAULT_SIZE_DP = 48f
    private const val DEFAULT_ALPHA = 0.8f

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

    fun loadSizeDp(context: Context): Float {
        return prefs(context).getFloat(KEY_SIZE_DP, DEFAULT_SIZE_DP)
    }

    fun saveSizeDp(context: Context, sizeDp: Float) {
        prefs(context).edit().putFloat(KEY_SIZE_DP, sizeDp).apply()
    }

    fun loadAlpha(context: Context): Float {
        return prefs(context).getFloat(KEY_ALPHA, DEFAULT_ALPHA)
    }

    fun saveAlpha(context: Context, alpha: Float) {
        prefs(context).edit().putFloat(KEY_ALPHA, alpha).apply()
    }

    fun resetPosition(context: Context) {
        prefs(context).edit()
            .remove(KEY_POSITION_X)
            .remove(KEY_POSITION_Y)
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
