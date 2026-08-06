package com.example.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.PathInterpolator
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView

object NavigationAnimator {
    private val INTERPOLATOR = PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)
    private const val DURATION = 220L
    private const val SAFETY_TIMEOUT = 2500L

    enum class NavDirection {
        FORWARD, BACK
    }

    private var activeSnapshotView: ImageView? = null
    private var activeSnapshotBitmap: Bitmap? = null
    private var activeContainer: FrameLayout? = null
    private var activeWebView: WebView? = null
    private var activeDirection: NavDirection = NavDirection.FORWARD
    private var isPendingCommit = false
    private var isTransitioning = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        commitTransition("timeout")
    }

    fun takeSnapshot(view: View): Bitmap? {
        if (view.width <= 0 || view.height <= 0) return null
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Called when a link navigation, back/forward action, or URL load is triggered.
     * Captures a snapshot overlay of the outgoing page and waits for new page content commit.
     */
    fun startNavigation(webView: WebView, container: FrameLayout, direction: NavDirection) {
        if (isPendingCommit || isTransitioning) {
            commitTransition("new_navigation_started")
        }

        val snapshot = takeSnapshot(webView) ?: return

        val imageView = ImageView(container.context).apply {
            setImageBitmap(snapshot)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        container.addView(imageView)

        activeSnapshotView = imageView
        activeSnapshotBitmap = snapshot
        activeContainer = container
        activeWebView = webView
        activeDirection = direction
        isPendingCommit = true
        isTransitioning = false

        // Position incoming webView offscreen based on navigation direction
        val startOffset = if (direction == NavDirection.FORWARD) webView.width.toFloat() else -webView.width.toFloat()
        webView.translationX = startOffset

        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, SAFETY_TIMEOUT)
    }

    /**
     * Called when webpage has committed visible content, reached progress >= 30, finished, or encountered error.
     */
    fun onContentReady(webView: WebView?) {
        if (isPendingCommit && (activeWebView == null || activeWebView == webView)) {
            commitTransition("content_ready")
        }
    }

    fun cancelOrDismiss() {
        if (isPendingCommit || isTransitioning) {
            commitTransition("cancel")
        }
    }

    private fun commitTransition(reason: String) {
        mainHandler.removeCallbacks(timeoutRunnable)
        if (!isPendingCommit) return
        isPendingCommit = false

        val imageView = activeSnapshotView
        val snapshot = activeSnapshotBitmap
        val container = activeContainer
        val webView = activeWebView
        val direction = activeDirection

        if (imageView == null || container == null || webView == null) {
            cleanup()
            return
        }

        isTransitioning = true

        val webViewTargetX = 0f
        val imageTargetX = if (direction == NavDirection.FORWARD) -webView.width.toFloat() else webView.width.toFloat()

        webView.animate()
            .translationX(webViewTargetX)
            .setDuration(DURATION)
            .setInterpolator(INTERPOLATOR)
            .withLayer()
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    webView.setLayerType(View.LAYER_TYPE_NONE, null)
                    webView.translationX = 0f
                    container.removeView(imageView)
                    snapshot?.recycle()
                    cleanup()
                }
            })
            .start()

        imageView.animate()
            .translationX(imageTargetX)
            .setDuration(DURATION)
            .setInterpolator(INTERPOLATOR)
            .withLayer()
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    imageView.setLayerType(View.LAYER_TYPE_NONE, null)
                }
            })
            .start()
    }

    private fun cleanup() {
        activeSnapshotView = null
        activeSnapshotBitmap = null
        activeContainer = null
        activeWebView = null
        isPendingCommit = false
        isTransitioning = false
    }
}
