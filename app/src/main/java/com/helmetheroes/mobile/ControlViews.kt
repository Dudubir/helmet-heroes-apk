package com.helmetheroes.mobile

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/** Sends key down/up events to the game view and remembers which keys are held. */
class KeySender(
    private val target: () -> View?,
    private val onPress: () -> Unit = {}
) {
    private val held = HashSet<Int>()

    fun press(code: Int) {
        if (held.add(code)) {
            onPress()
            dispatch(KeyEvent.ACTION_DOWN, code)
        }
    }

    fun release(code: Int) {
        if (held.remove(code)) dispatch(KeyEvent.ACTION_UP, code)
    }

    fun releaseAll() {
        held.toList().forEach { release(it) }
    }

    private fun dispatch(action: Int, code: Int) {
        val v = target() ?: return
        v.requestFocus()
        val t = SystemClock.uptimeMillis()
        v.dispatchKeyEvent(KeyEvent(t, t, action, code, 0))
    }
}

/** Round on-screen button that holds a key while it is touched. */
@SuppressLint("ViewConstructor")
class KeyButton(
    context: Context,
    private val label: String,
    private val keyCode: Int,
    private val keys: KeySender
) : View(context) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(170, 255, 255, 255)
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private var down = false

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 4f
        fill.color = if (down) Color.argb(150, 255, 255, 255) else Color.argb(70, 255, 255, 255)
        canvas.drawCircle(cx, cy, r, fill)
        canvas.drawCircle(cx, cy, r, ring)
        text.textSize = r * if (label.length > 2) 0.5f else 0.8f
        canvas.drawText(label, cx, cy - (text.descent() + text.ascent()) / 2f, text)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = true
                keys.press(keyCode)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                down = false
                keys.release(keyCode)
                invalidate()
            }
        }
        return true
    }
}

/** Virtual joystick that holds the four movement keys (8 directions). */
@SuppressLint("ViewConstructor")
class JoystickView(context: Context, private val keys: KeySender) : View(context) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(150, 255, 255, 255)
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 255, 255, 255) }
    private var knobX = 0f
    private var knobY = 0f
    private var pointerId = -1

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 4f
        canvas.drawCircle(cx, cy, r, basePaint)
        canvas.drawCircle(cx, cy, r, ringPaint)
        canvas.drawCircle(cx + knobX, cy + knobY, r * 0.38f, knobPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerId = e.getPointerId(0)
                update(e.x, e.y)
            }
            MotionEvent.ACTION_MOVE -> {
                val i = e.findPointerIndex(pointerId)
                if (i >= 0) update(e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reset()
        }
        return true
    }

    private fun update(x: Float, y: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 4f
        var dx = x - cx
        var dy = y - cy
        val dist = hypot(dx, dy)
        if (dist > r) {
            dx = dx / dist * r
            dy = dy / dist * r
        }
        knobX = dx
        knobY = dy
        val dead = r * 0.35f
        set(AppConfig.KEY_MOVE_LEFT, dx < -dead)
        set(AppConfig.KEY_MOVE_RIGHT, dx > dead)
        set(AppConfig.KEY_MOVE_UP, dy < -dead)
        set(AppConfig.KEY_MOVE_DOWN, dy > dead)
        invalidate()
    }

    private fun set(code: Int, on: Boolean) {
        if (on) keys.press(code) else keys.release(code)
    }

    private fun reset() {
        knobX = 0f
        knobY = 0f
        pointerId = -1
        keys.release(AppConfig.KEY_MOVE_LEFT)
        keys.release(AppConfig.KEY_MOVE_RIGHT)
        keys.release(AppConfig.KEY_MOVE_UP)
        keys.release(AppConfig.KEY_MOVE_DOWN)
        invalidate()
    }
}
