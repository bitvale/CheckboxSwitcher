package com.bitvale.checkboxswitcher

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import com.bitvale.checkboxswitcher.commons.isLollipopAndAbove
import com.bitvale.checkboxswitcher.commons.toPx
import kotlin.math.min

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 19-Jul-19
 */
class CheckboxSwitcher @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val switcherPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var defHeight = 0
    private var defWidth = 0

    private var switcherCornerRadius = 0f

    private var shadow: Bitmap? = null
    private var switchElevation = 0f
    private var shadowOffset = 0f

    private val switcherRect = RectF(0f, 0f, 0f, 0f)
    private val thumbRect = RectF(0f, 0f, 0f, 0f)

    @ColorInt
    private var bgColor = 0
    @ColorInt
    private var onColor = 0
    @ColorInt
    private var offColor = 0
    @ColorInt
    private var currentColor = 0

    @Dimension(unit = Dimension.PX)
    private var thumbPadding = 0

    private var touchRect: Rect? = null
    private var touchOutside = false

    private var elevationAnimator: ValueAnimator? = null

    private var currentElevation = 0f
        set(value) {
            if (field != value) {
                field = value
                if (!isLaidOut) return

                if (!isLollipopAndAbove()) {
                    shadowOffset = value
                    generateShadow()
                    postInvalidateOnAnimation()
                } else {
                    elevation = value
                }
            }
        }

    init {
        attrs?.let { retrieveAttributes(attrs, defStyleAttr) }
        if (!isLollipopAndAbove() && switchElevation > 0f) {
            shadowPaint.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            shadowPaint.alpha = 51 // 20%
            setShadowBlurRadius(switchElevation)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
        currentElevation = switchElevation
    }

    private fun retrieveAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.CheckboxSwitcher,
            defStyleAttr, R.style.BaseCheckboxSwitcher
        )

        switchElevation = typedArray.getDimension(R.styleable.CheckboxSwitcher_elevation, 0f)

        defHeight = typedArray.getDimensionPixelOffset(R.styleable.CheckboxSwitcher_switcher_height, 0)

        bgColor = typedArray.getColor(R.styleable.CheckboxSwitcher_switcher_bg_color, 0)
        onColor = typedArray.getColor(R.styleable.CheckboxSwitcher_thumb_on_color, 0)
        offColor = typedArray.getColor(R.styleable.CheckboxSwitcher_thumb_off_color, 0)

        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)

        if (heightMode != MeasureSpec.EXACTLY) {
            height = defHeight
        }

        thumbPadding = (height * 12) / 100 // 12%

        var width = (height * 2 - thumbPadding * 1.5f).toInt()

        if (!isLollipopAndAbove()) {
            width += switchElevation.toInt() * 2
            height += switchElevation.toInt() * 2
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (!isLollipopAndAbove()) {
            shadowOffset = switchElevation
        } else {
            elevation = switchElevation
        }

        switcherRect.left = shadowOffset
        switcherRect.top = shadowOffset / 2
        switcherRect.right = width.toFloat() - shadowOffset
        switcherRect.bottom = height.toFloat() - shadowOffset - shadowOffset / 2

        thumbRect.left = switcherRect.left + thumbPadding
        thumbRect.top = shadowOffset / 2 + thumbPadding
        thumbRect.bottom = height - shadowOffset - shadowOffset / 2 - thumbPadding
        thumbRect.right = thumbRect.left + thumbRect.height()

        switcherCornerRadius = height / 4f

        if (!isLollipopAndAbove()) {
            generateShadow()
        } else {
            outlineProvider = SwitchOutline(width, height)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        // shadow
        if (!isLollipopAndAbove() && switchElevation > 0f && !isInEditMode) {
            canvas?.drawBitmap(shadow as Bitmap, 0f, shadowOffset, null)
        }

        // switcher
        switcherPaint.color = bgColor
        canvas?.drawRoundRect(switcherRect, switcherCornerRadius, switcherCornerRadius, switcherPaint)

        // thumb
        switcherPaint.color = offColor
        canvas?.drawRoundRect(thumbRect, switcherCornerRadius, switcherCornerRadius, switcherPaint)
    }

    private fun generateShadow() {
        if (switchElevation == 0f) return
        if (!isInEditMode) {
            if (shadow == null) {
                shadow = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
            } else {
                shadow?.eraseColor(Color.TRANSPARENT)
            }
            val c = Canvas(shadow as Bitmap)

            c.drawRoundRect(switcherRect, switcherCornerRadius, switcherCornerRadius, shadowPaint)

            val rs = RenderScript.create(context)
            val blur = ScriptIntrinsicBlur.create(rs, Element.U8(rs))
            val input = Allocation.createFromBitmap(rs, shadow)
            val output = Allocation.createTyped(rs, input.type)
            blur.setRadius(switchElevation)
            blur.setInput(input)
            blur.forEach(output)
            output.copyTo(shadow)
            input.destroy()
            output.destroy()
            blur.destroy()
        }
    }

    private fun setShadowBlurRadius(elevation: Float) {
        val maxElevation = context.toPx(24f)
        switchElevation = min(25f * (elevation / maxElevation), 25f)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            touchRect = Rect(left, top, right, bottom)
            updateElevation(true)
        }
        if (event?.action == MotionEvent.ACTION_MOVE) {
            if (touchOutside) return false
            if (touchRect?.contains(left + event.x.toInt(), top + event.y.toInt()) == false) {
                updateElevation(false)
                touchOutside = true
                return false
            }
        }
        if (event?.action == MotionEvent.ACTION_UP) {
            if (touchOutside) {
                touchOutside = false
                return false
            }
            animateSwitch()
            updateElevation(false)
        }
        return true
    }

    private fun animateSwitch() {

    }

    private fun updateElevation(pressed: Boolean) {
        val to = if (pressed) switchElevation / 2
        else switchElevation

        elevationAnimator?.cancel()

        elevationAnimator = ValueAnimator.ofFloat(currentElevation, to).apply {
            addUpdateListener {
                currentElevation = it.animatedValue as Float
            }
            duration = 250
            start()
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class SwitchOutline internal constructor(internal var width: Int, internal var height: Int) :
        ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, width, height, switcherCornerRadius)
        }
    }
}