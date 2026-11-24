package com.example.launcher

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lomiri-inspired lock screen. Android Keyguard remains the only authenticator. */
class LomiriLockScreenActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var notificationText: TextView
    private var downY = 0f
    private var downX = 0f
    private val localeUy = Locale("es", "UY")
    private var receiverRegistered = false

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNotificationCount(intent?.getIntExtra(NotificationListener.EXTRA_NOTIFICATION_COUNT, -1) ?: -1)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            val now = Date()
            clock.text = SimpleDateFormat("HH:mm", localeUy).format(now)
            date.text = SimpleDateFormat("EEEE d 'de' MMMM", localeUy).format(now)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeUy) else it.toString() }
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        if (Build.VERSION.SDK_INT >= 26) setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        val root = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.lomiri_fondo)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

        // Header: lightweight digital clock and localized date, both rendered by Kotlin.
        val header = FrameLayout(this)
        clock = TextView(this).apply {
            text = "--:--"
            setTextColor(Color.WHITE)
            textSize =  seventySp()
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = -0.045f
        }
        date = TextView(this).apply {
            setTextColor(0xFFEADDE5.toInt())
            textSize = 21f
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        header.addView(clock, FrameLayout.LayoutParams(-1, (screenH * 0.105f).toInt(), Gravity.TOP))
        header.addView(date, FrameLayout.LayoutParams(-1, (screenH * 0.045f).toInt(), Gravity.TOP).apply {
            topMargin = (screenH * 0.112f).toInt()
        })
        root.addView(header, FrameLayout.LayoutParams(-1, (screenH * 0.20f).toInt(), Gravity.TOP))

        // Separate transparent PNG overlay. Its center is intentionally clear for live text.
        val ringSize = (screenW * 0.98f).toInt().coerceAtMost((screenH * 0.43f).toInt())
        val ring = ImageView(this).apply {
            setImageResource(R.drawable.lomiri_anillo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Indicador circular de notificaciones"
        }
        val ringTop = (screenH * 0.285f).toInt()
        root.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = ringTop
        })

        notificationText = TextView(this).apply {
            text = "Sin notificaciones"
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            isSingleLine = true
            setShadowLayer(3f, 0f, 1f, 0x55000000)
        }
        // Align the live counter with the clear center of the PNG.
        root.addView(notificationText, FrameLayout.LayoutParams((ringSize * 0.66f).toInt(), dp(64),
            Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = ringTop + (ringSize * 0.455f).toInt()
        })

        val swipeHint = TextView(this).apply {
            text = "Deslizá hacia arriba para desbloquear"
            setTextColor(0xFFEDE7ED.toInt())
            textSize = 14f
            gravity = Gravity.CENTER
            alpha = 0.88f
        }
        root.addView(swipeHint, FrameLayout.LayoutParams(-1, dp(48), Gravity.BOTTOM).apply {
            bottomMargin = dp(24)
        })

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downY = event.rawY; downX = event.rawX; true }
                MotionEvent.ACTION_UP -> {
                    val dy = downY - event.rawY
                    val dx = kotlin.math.abs(downX - event.rawX)
                    if (dy > dp(55) && dy > dx) requestSystemUnlock()
                    true
                }
                else -> true
            }
        }
        setContentView(root)
        tick.run()
        updateNotificationCount()
    }

    private fun seventySp(): Float =  seventySpValue()
    private fun seventySpValue(): Float = (resources.displayMetrics.widthPixels / resources.displayMetrics.scaledDensity * 0.205f).coerceIn(64f, 88f)

    private fun updateNotificationCount(receivedCount: Int = -1) {
        val count = if (receivedCount >= 0) receivedCount else NotificationListener.getTotalNotificationCount()
        notificationText.text = when (count) {
            0 -> "Sin notificaciones"
            1 -> "1 notificación"
            else -> "$count notificaciones"
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(NotificationListener.ACTION_NOTIFICATION_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(notificationReceiver, filter)
        }
        receiverRegistered = true
        updateNotificationCount()
    }

    private fun requestSystemUnlock() {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (keyguard.isKeyguardLocked && Build.VERSION.SDK_INT >= 26) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { finish() }
                override fun onDismissCancelled() { /* Android keeps its secure lock screen visible. */ }
            })
        } else finish()
    }

    override fun onStop() {
        if (receiverRegistered) {
            try { unregisterReceiver(notificationReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
