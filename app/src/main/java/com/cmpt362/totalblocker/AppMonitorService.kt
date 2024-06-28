package com.cmpt362.totalblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView

class AppMonitorService : AccessibilityService() {

    companion object {
        private const val TAG = "AppMonitorService"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private val blockedApps = listOf("org.telegram.messenger") //todo: create shared preferences to store list of stuff to block
    private val handler = Handler()
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var runnable: Runnable

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        val imageView: ImageView = overlayView.findViewById(R.id.overlayImage)
        imageView.setImageResource(R.drawable.gotcha)

        // Add view to the window
        windowManager.addView(overlayView, params)

        runnable = object : Runnable {
            override fun run() {
                checkForegroundApp()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun checkForegroundApp() {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 10 // Check last 10 seconds

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val packageName = event.packageName
                Log.d(TAG, "App in foreground: $packageName")
                if (blockedApps.contains(packageName)) {
                    Log.d(TAG, "Blocking app: $packageName")
                    showOverlay()
                    return
                }
            }
        }
        hideOverlay()
    }

    private fun showOverlay() {
        if (overlayView.parent == null) {
            windowManager.addView(overlayView, overlayView.layoutParams)
        }
    }

    private fun hideOverlay() {
        if (overlayView.parent != null) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            packageNames = null
        }
        serviceInfo = info
        Log.d(TAG, "Service connected")
    }
}