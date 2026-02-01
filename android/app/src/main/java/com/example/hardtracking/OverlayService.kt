package com.example.hardtracking

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var keyguardManager: KeyguardManager
    private val screenReceiver = ScreenStateReceiver()

    override fun onCreate() {
        super.onCreate()
        TimeEventRepository.init(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        createOverlay()
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        updateOverlayVisibility()
        OverlaySettings.setEnabled(this, true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateOverlayVisibility()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        removeOverlayView()
        OverlaySettings.setEnabled(this, false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlay() {
        val size = dpToPx(48f)
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
            text = "▲"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xAA1B5E20.toInt())
            }
            alpha = OverlaySettings.loadAlpha(this@OverlayService)
        }

        val touchSlop = dpToPx(6f)
        button.setOnTouchListener(OverlayTouchListener(button, touchSlop))
        overlayView = button
    }

    private fun updateOverlayVisibility() {
        if (isLocked()) {
            addOverlayView()
        } else {
            removeOverlayView()
        }
    }

    private fun isLocked(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
        } else {
            keyguardManager.isKeyguardLocked
        }
    }

    private fun addOverlayView() {
        val view = overlayView ?: return
        if (view.isAttachedToWindow) return
        windowManager.addView(view, layoutParams)
    }

    private fun removeOverlayView() {
        val view = overlayView
        if (view != null && view.isAttachedToWindow) {
            windowManager.removeView(view)
        }
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
                        Toast.makeText(
                            this@OverlayService,
                            "기록이 추가되었습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return true
                }
                else -> return false
            }
        }
    }

    private inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateOverlayVisibility()
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, OverlayService::class.java)
    }
}
