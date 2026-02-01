package com.example.hardtracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        TimeEventRepository.init(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlay()
        OverlaySettings.setEnabled(this, true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { view -> windowManager.removeView(view) }
        overlayView = null
        OverlaySettings.setEnabled(this, false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlay() {
        val size = dpToPx(64f)
        val (startX, startY) = OverlaySettings.loadPosition(this)
        layoutParams = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }

        val button = TextView(this).apply {
            text = "기록"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xAA1B5E20.toInt())
            }
            alpha = 0.8f
        }

        val touchSlop = dpToPx(6f)
        button.setOnTouchListener(OverlayTouchListener(button, touchSlop))
        overlayView = button
        windowManager.addView(button, layoutParams)
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private inner class OverlayTouchListener(
        private val view: View,
        private val clickThreshold: Int
    ) : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var moved = false

        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > clickThreshold || abs(dy) > clickThreshold) {
                        moved = true
                    }
                    layoutParams.x = initialX + dx
                    layoutParams.y = initialY + dy
                    windowManager.updateViewLayout(view, layoutParams)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    OverlaySettings.savePosition(this@OverlayService, layoutParams.x, layoutParams.y)
                    if (!moved) {
                        TimeEventRepository.addEvent(this@OverlayService)
                    }
                    return true
                }
                else -> return false
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, OverlayService::class.java)
    }
}
