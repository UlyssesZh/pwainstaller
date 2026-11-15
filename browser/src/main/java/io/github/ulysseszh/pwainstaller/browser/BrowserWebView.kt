package io.github.ulysseszh.pwainstaller.browser

import android.content.Context
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView

internal class BrowserWebView(context: Context) : WebView(context) {
    private var lastLongPressFeedback = -1L

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // A separate gesture must always be allowed its own feedback.
        if (event.actionMasked == MotionEvent.ACTION_DOWN) lastLongPressFeedback = -1L
        return super.dispatchTouchEvent(event)
    }

    override fun performHapticFeedback(feedbackConstant: Int, flags: Int): Boolean {
        if (feedbackConstant == HapticFeedbackConstants.LONG_PRESS) {
            // Android's handled-long-click feedback and Chromium's gesture ACK
            // can both request this effect for the same press. The second request
            // can restart the motor, producing a double buzz on some devices.
            val now = SystemClock.uptimeMillis()
            if (lastLongPressFeedback >= 0 && now - lastLongPressFeedback < ViewConfiguration.getLongPressTimeout()) return false
            lastLongPressFeedback = now
        }
        return super.performHapticFeedback(feedbackConstant, flags)
    }
}
