package net.blockhost.trestle.game

import android.content.Context
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import org.lwjgl.glfw.CallbackBridge
import kotlin.math.hypot

internal object GlfwKey {
    const val SPACE = 32
    const val A = 65
    const val D = 68
    const val E = 69
    const val Q = 81
    const val S = 83
    const val T = 84
    const val W = 87
    const val ESCAPE = 256
    const val LEFT_SHIFT = 340
    const val LEFT_CONTROL = 341
}

@SuppressLint("ViewConstructor")
internal class MinecraftControlOverlayView(
    context: Context,
    private val onChatRequested: () -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CHALK
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    private val pointerTargets = mutableMapOf<Int, PointerTarget>()
    private val heldKeys = mutableSetOf<Int>()
    private val activeButtons = mutableSetOf<ControlAction>()
    private var joystickPointer: Int? = null
    private var joystickX = 0f
    private var joystickY = 0f
    private var inputGrabbed = false

    init {
        isFocusable = true
        contentDescription = "Minecraft touch controls"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = layout()
        drawJoystick(canvas, layout.joystick)
        layout.buttons.forEach { button -> drawButton(canvas, button) }
        if (!inputGrabbed) {
            textPaint.textSize = 11f * density
            textPaint.color = MUTED
            canvas.drawText("MENU CURSOR", width * 0.5f, height - 12f * density, textPaint)
            textPaint.color = CHALK
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            -> {
                val index = event.actionIndex
                beginPointer(event.getPointerId(index), event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_MOVE -> {
                repeat(event.pointerCount) { index ->
                    movePointer(event.getPointerId(index), event.getX(index), event.getY(index))
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            -> {
                endPointer(event.getPointerId(event.actionIndex))
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
            }
            MotionEvent.ACTION_CANCEL -> releaseAllInputs()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setGameInputGrabbed(grabbed: Boolean) {
        inputGrabbed = grabbed
        invalidate()
    }

    fun tapKey(key: Int) {
        CallbackBridge.sendKey(key, true)
        CallbackBridge.sendKey(key, false)
    }

    fun releaseAllInputs() {
        pointerTargets.clear()
        activeButtons.toList().forEach(::releaseAction)
        activeButtons.clear()
        heldKeys.toList().forEach { key -> CallbackBridge.sendKey(key, false) }
        heldKeys.clear()
        joystickPointer = null
        joystickX = 0f
        joystickY = 0f
        invalidate()
    }

    private fun beginPointer(pointerId: Int, x: Float, y: Float) {
        val layout = layout()
        layout.buttons.firstOrNull { it.bounds.contains(x, y) }?.let { button ->
            pointerTargets[pointerId] = PointerTarget.Button(button.action)
            pressAction(button.action)
            invalidate()
            return
        }
        if (joystickPointer == null && layout.joystick.touchBounds.contains(x, y)) {
            joystickPointer = pointerId
            pointerTargets[pointerId] = PointerTarget.Joystick
            updateJoystick(x, y, layout.joystick)
            return
        }
        pointerTargets[pointerId] = PointerTarget.Look(x, y)
        if (!inputGrabbed) CallbackBridge.setCursor(x, y)
    }

    private fun movePointer(pointerId: Int, x: Float, y: Float) {
        when (val target = pointerTargets[pointerId]) {
            PointerTarget.Joystick -> updateJoystick(x, y, layout().joystick)
            is PointerTarget.Look -> {
                if (inputGrabbed) {
                    CallbackBridge.moveCursor(
                        (x - target.lastX) * LOOK_SENSITIVITY,
                        (y - target.lastY) * LOOK_SENSITIVITY,
                    )
                } else {
                    CallbackBridge.setCursor(x, y)
                }
                target.lastX = x
                target.lastY = y
            }
            is PointerTarget.Button,
            null,
            -> Unit
        }
    }

    private fun endPointer(pointerId: Int) {
        when (val target = pointerTargets.remove(pointerId)) {
            PointerTarget.Joystick -> {
                releaseMovementKeys()
                joystickPointer = null
                joystickX = 0f
                joystickY = 0f
            }
            is PointerTarget.Button -> releaseAction(target.action)
            is PointerTarget.Look,
            null,
            -> Unit
        }
        invalidate()
    }

    private fun updateJoystick(x: Float, y: Float, joystick: JoystickLayout) {
        val deltaX = x - joystick.centerX
        val deltaY = y - joystick.centerY
        val distance = hypot(deltaX, deltaY)
        val scale = if (distance > joystick.radius) joystick.radius / distance else 1f
        joystickX = deltaX * scale
        joystickY = deltaY * scale
        setKeyHeld(GlfwKey.A, joystickX < -joystick.deadZone)
        setKeyHeld(GlfwKey.D, joystickX > joystick.deadZone)
        setKeyHeld(GlfwKey.W, joystickY < -joystick.deadZone)
        setKeyHeld(GlfwKey.S, joystickY > joystick.deadZone)
        invalidate()
    }

    private fun releaseMovementKeys() {
        listOf(GlfwKey.W, GlfwKey.A, GlfwKey.S, GlfwKey.D).forEach { key -> setKeyHeld(key, false) }
    }

    private fun setKeyHeld(key: Int, held: Boolean) {
        if (held && heldKeys.add(key)) CallbackBridge.sendKey(key, true)
        if (!held && heldKeys.remove(key)) CallbackBridge.sendKey(key, false)
    }

    private fun pressAction(action: ControlAction) {
        if (!activeButtons.add(action)) return
        when (action) {
            is ControlAction.Key -> setKeyHeld(action.key, true)
            is ControlAction.Mouse -> CallbackBridge.sendMouseButton(action.button, true)
            is ControlAction.Scroll -> CallbackBridge.sendScroll(0.0, action.amount)
            ControlAction.Chat -> CallbackBridge.sendKey(GlfwKey.T, true)
        }
    }

    private fun releaseAction(action: ControlAction) {
        activeButtons.remove(action)
        when (action) {
            is ControlAction.Key -> setKeyHeld(action.key, false)
            is ControlAction.Mouse -> CallbackBridge.sendMouseButton(action.button, false)
            is ControlAction.Scroll -> Unit
            ControlAction.Chat -> {
                CallbackBridge.sendKey(GlfwKey.T, false)
                postDelayed(onChatRequested, CHAT_KEYBOARD_DELAY_MILLIS)
            }
        }
    }

    private fun drawJoystick(canvas: Canvas, joystick: JoystickLayout) {
        fillPaint.color = CONTROL_FILL
        strokePaint.color = CONTROL_STROKE
        canvas.drawCircle(joystick.centerX, joystick.centerY, joystick.radius, fillPaint)
        canvas.drawCircle(joystick.centerX, joystick.centerY, joystick.radius, strokePaint)
        fillPaint.color = OCHRE_ACTIVE
        canvas.drawCircle(
            joystick.centerX + joystickX,
            joystick.centerY + joystickY,
            joystick.radius * 0.38f,
            fillPaint,
        )
        textPaint.textSize = 10f * density
        textPaint.color = CHALK
        canvas.drawText("MOVE", joystick.centerX, joystick.centerY - joystick.radius - 8f * density, textPaint)
    }

    private fun drawButton(canvas: Canvas, button: ControlButton) {
        val pressed = button.action in activeButtons
        fillPaint.color = if (pressed) OCHRE_ACTIVE else CONTROL_FILL
        strokePaint.color = if (pressed) OCHRE else CONTROL_STROKE
        canvas.drawRoundRect(button.bounds, 9f * density, 9f * density, fillPaint)
        canvas.drawRoundRect(button.bounds, 9f * density, 9f * density, strokePaint)
        textPaint.textSize = if (button.label.length <= 2) 16f * density else 10f * density
        textPaint.color = CHALK
        val baseline = button.bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(button.label, button.bounds.centerX(), baseline, textPaint)
    }

    private fun layout(): ControlLayout {
        val unit = 48f * density
        val gap = 8f * density
        val edge = 14f * density
        val top = 14f * density
        val joystickRadius = minOf(72f * density, height * 0.19f)
        val joystick = JoystickLayout(
            centerX = edge + joystickRadius,
            centerY = height - edge - joystickRadius,
            radius = joystickRadius,
            deadZone = joystickRadius * 0.28f,
        )
        val buttons = buildList {
            fun addButton(label: String, action: ControlAction, left: Float, buttonTop: Float, size: Float = unit) {
                add(ControlButton(label, action, RectF(left, buttonTop, left + size, buttonTop + size)))
            }

            addButton("II", ControlAction.Key(GlfwKey.ESCAPE), edge, top)
            addButton("CHAT", ControlAction.Chat, edge + unit + gap, top, unit * 1.2f)
            addButton("INV", ControlAction.Key(GlfwKey.E), width - edge - unit * 2 - gap, top)
            addButton("DROP", ControlAction.Key(GlfwKey.Q), width - edge - unit, top)

            val right = width - edge - unit
            val bottom = height - edge - unit
            addButton("JUMP", ControlAction.Key(GlfwKey.SPACE), right, bottom - unit - gap, unit)
            addButton("ATK", ControlAction.Mouse(0), right - unit - gap, bottom - unit - gap, unit)
            addButton("USE", ControlAction.Mouse(1), right, bottom, unit)
            addButton("SNEAK", ControlAction.Key(GlfwKey.LEFT_SHIFT), right - unit - gap, bottom, unit)
            addButton("RUN", ControlAction.Key(GlfwKey.LEFT_CONTROL), right - unit * 2 - gap * 2, bottom, unit)
            addButton("<", ControlAction.Scroll(1.0), width * 0.5f - unit - gap * 0.5f, bottom, unit)
            addButton(">", ControlAction.Scroll(-1.0), width * 0.5f + gap * 0.5f, bottom, unit)
        }
        return ControlLayout(joystick, buttons)
    }

    private sealed interface PointerTarget {
        data object Joystick : PointerTarget
        data class Look(var lastX: Float, var lastY: Float) : PointerTarget
        data class Button(val action: ControlAction) : PointerTarget
    }

    private sealed interface ControlAction {
        data object Chat : ControlAction
        data class Key(val key: Int) : ControlAction
        data class Mouse(val button: Int) : ControlAction
        data class Scroll(val amount: Double) : ControlAction
    }

    private data class ControlButton(
        val label: String,
        val action: ControlAction,
        val bounds: RectF,
    )

    private data class JoystickLayout(
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val deadZone: Float,
    ) {
        val touchBounds = RectF(
            centerX - radius * 1.25f,
            centerY - radius * 1.25f,
            centerX + radius * 1.25f,
            centerY + radius * 1.25f,
        )
    }

    private data class ControlLayout(
        val joystick: JoystickLayout,
        val buttons: List<ControlButton>,
    )

    private companion object {
        const val CHAT_KEYBOARD_DELAY_MILLIS = 120L
        const val LOOK_SENSITIVITY = 1.15f
        val CHALK = Color.rgb(231, 227, 217)
        val MUTED = Color.rgb(169, 164, 154)
        val OCHRE = Color.rgb(190, 143, 69)
        val OCHRE_ACTIVE = Color.argb(210, 190, 143, 69)
        val CONTROL_FILL = Color.argb(150, 23, 23, 21)
        val CONTROL_STROKE = Color.argb(210, 169, 164, 154)
    }
}
