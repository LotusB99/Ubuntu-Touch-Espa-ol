package com.example.launcher

import android.app.KeyguardManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Lomiri-inspired visual lock screen, implemented with native Android/Kotlin views.
 * It deliberately does not collect or validate a PIN: Android Keyguard remains
 * the only component that authenticates the device owner.
 */
class LomiriLockScreenActivity : AppCompatActivity() {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var hint: TextView
    private var downY = 0f

    private val localeUy = Locale("es", "UY")
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
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        val root = FrameLayout(this).apply {
            // Lomiri's lockscreen uses the selected wallpaper with a configurable
            // black scrim. This gradient is the fallback when no wallpaper is supplied.
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(37, 31, 49), Color.rgb(25, 37, 54), Color.rgb(8, 17, 29))
            )
            setPadding(dp(28), dp(44), dp(28), dp(30))
        }

        // Dark scrim, matching Lockscreen.qml's darkenBackground layer.
        root.addView(View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0.20f
        }, FrameLayout.LayoutParams(-1, -1))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val brand = TextView(this).apply {
            text = "lomiri"
            setTextColor(Color.WHITE)
            textSize = 15f
            letterSpacing = 0.18f
            gravity = Gravity.CENTER
            alpha = 0.88f
        }
        clock = TextView(this).apply {
            text = "--:--"
            setTextColor(Color.WHITE)
            textSize = 78f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = -0.035f
        }
        date = TextView(this).apply {
            setTextColor(Color.rgb(243, 243, 231))
            textSize = 17f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            alpha = 0.94f
        }
        val separator = View(this).apply {
            setBackgroundColor(Color.WHITE)
            alpha = 0.32f
        }
        val instruction = TextView(this).apply {
            text = "Deslizá hacia arriba para desbloquear"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            alpha = 0.82f
        }
        hint = instruction

        content.addView(brand, LinearLayout.LayoutParams(-1, dp(30)))
        content.addView(clock, LinearLayout.LayoutParams(-1, dp(100)))
        content.addView(date, LinearLayout.LayoutParams(-1, dp(34)))
        content.addView(separator, LinearLayout.LayoutParams(dp(42), dp(1)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(22)
            bottomMargin = dp(22)
        })
        content.addView(instruction, LinearLayout.LayoutParams(-1, dp(32)))

        root.addView(content, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))

        val footer = TextView(this).apply {
            text = "Desbloqueo protegido por Android"
            setTextColor(Color.WHITE)
            textSize = 11f
            alpha = 0.58f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(8))
            setOnClickListener { requestSystemUnlock() }
        }
        root.addView(footer, FrameLayout.LayoutParams(-1, dp(48), Gravity.BOTTOM))

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val delta = downY - event.rawY
                    if (delta > dp(55)) requestSystemUnlock()
                    true
                }
                else -> true
            }
        }
        setContentView(root)
        tick.run()
    }

    private fun requestSystemUnlock() {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (keyguard.isKeyguardLocked && Build.VERSION.SDK_INT >= 26) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { finish() }
                override fun onDismissCancelled() {
                    hint.text = "El teléfono sigue protegido por su bloqueo"
                }
            })
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
