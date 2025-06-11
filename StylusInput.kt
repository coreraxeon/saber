// File: app/src/main/java/com/example/stylusdraw/ui/stylusinput/StylusInput.kt
package com.example.stylusdraw.ui.stylusinput

import android.app.Activity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.samsung.android.sdk.penremote.ButtonEvent
import com.samsung.android.sdk.penremote.SpenEvent
import com.samsung.android.sdk.penremote.SpenEventListener
import com.samsung.android.sdk.penremote.SpenRemote
import com.samsung.android.sdk.penremote.SpenRemote.ConnectionResultCallback
import com.samsung.android.sdk.penremote.SpenUnit
import com.samsung.android.sdk.penremote.SpenUnitManager

/**
 * StylusInput
 *  · Listens for S-Pen side-button (off-screen & fallback)
 *  · Converts hover/fallback events to callbacks on a provided Listener
 *
 * Note: This class no longer attaches its own OnTouchListener.
 * Instead, it provides handleTouchEvent(...) which your View can call
 * from a combined OnTouchListener. Hover (ACTION_HOVER_MOVE) is still
 * handled internally via OnGenericMotionListener on `targetView`.
 */
class StylusInput(
    private val activity: Activity,
    private val targetView: View,
    private val listener: Listener
) {
    interface Listener {
        fun onSideButtonDown()
        fun onSideButtonUp()
        fun onPenHover(x: Float, y: Float, isEraser: Boolean)
        fun onStylusDown(x: Float, y: Float, isEraser: Boolean)
        fun onStylusMove(x: Float, y: Float, isEraser: Boolean)
        fun onStylusUp(x: Float, y: Float, isEraser: Boolean)
    }

    private val TAG = "StylusInput"
    private var unitMgr: SpenUnitManager? = null
    private var buttonUnit: SpenUnit?      = null
    private var sideDown: Boolean          = false

    init {
        bindRemote()
        bindHoverListener()
    }

    /** Connect to the Samsung S-Pen Remote and watch for button events. */
    private fun bindRemote() {
        SpenRemote.getInstance().connect(activity, object : ConnectionResultCallback {
            override fun onSuccess(m: SpenUnitManager) {
                unitMgr    = m
                buttonUnit = m.getUnit(SpenUnit.TYPE_BUTTON)
                m.registerSpenEventListener(object : SpenEventListener {
                    override fun onEvent(e: SpenEvent) {
                        val be = ButtonEvent(e)
                        if (be.action == ButtonEvent.ACTION_DOWN) {
                            sideDown = true
                            listener.onSideButtonDown()
                        } else {
                            sideDown = false
                            listener.onSideButtonUp()
                        }
                        Log.d(TAG, "Side btn = $sideDown")
                    }
                }, buttonUnit)
            }

            override fun onFailure(err: Int) {
                Log.e(TAG, "S-Pen connect failed: $err")
            }
        })
    }

    /** Install a GenericMotionListener on targetView to get HOVER events. */
    private fun bindHoverListener() {
        targetView.setOnGenericMotionListener { _, ev ->
            // Check if stylus side-button state flipped mid-hover:
            val localDown = (ev.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
            if (localDown != sideDown) {
                sideDown = localDown
                if (localDown) listener.onSideButtonDown() else listener.onSideButtonUp()
                targetView.invalidate()
            }

            // If this is a stylus hover‐move, report hover coords:
            if (ev.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS &&
                ev.actionMasked == MotionEvent.ACTION_HOVER_MOVE
            ) {
                listener.onPenHover(ev.x, ev.y, sideDown)
                return@setOnGenericMotionListener true
            }
            false
        }
    }

    /**
     * Must be called from your View’s OnTouchListener so that:
     *  - Stylus DOWN/MOVE/UP on screen still update the eraser/draw state.
     *
     * Returns true if the event was consumed by StylusInput (always true for stylus).
     */
    fun handleTouchEvent(ev: MotionEvent): Boolean {
        // Only process stylus/eraser touches
        val toolType = ev.getToolType(0)
        if (toolType != MotionEvent.TOOL_TYPE_STYLUS && toolType != MotionEvent.TOOL_TYPE_ERASER) {
            return false
        }

        val x = ev.x
        val y = ev.y
        val isEraser = sideDown || toolType == MotionEvent.TOOL_TYPE_ERASER

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                listener.onStylusDown(x, y, isEraser)
                // We also call onPenHover immediately, so the circle updates right away:
                listener.onPenHover(x, y, isEraser)
            }
            MotionEvent.ACTION_MOVE -> {
                listener.onStylusMove(x, y, isEraser)
                listener.onPenHover(x, y, isEraser)
            }
            MotionEvent.ACTION_UP -> {
                listener.onStylusUp(x, y, isEraser)
                // On lift, we no longer report an eraser hover (isEraser=false)
                listener.onPenHover(x, y, false)
                targetView.performClick() // accessibility event
            }
        }
        return true
    }

    /** Disconnect from S-Pen when no longer needed. */
    fun cleanup() {
        unitMgr?.let { m ->
            buttonUnit?.let { m.unregisterSpenEventListener(it) }
        }
        SpenRemote.getInstance().disconnect(activity)
    }
}
