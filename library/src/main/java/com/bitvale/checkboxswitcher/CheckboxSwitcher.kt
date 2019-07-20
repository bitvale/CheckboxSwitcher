package com.bitvale.checkboxswitcher

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
import android.view.View
import android.view.ViewOutlineProvider
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

    init {
        attrs?.let { retrieveAttributes(attrs, defStyleAttr) }
    }

    private fun retrieveAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.CheckboxSwitcher,
            defStyleAttr, R.style.BaseCheckboxSwitcher
        )

        switchElevation = typedArray.getDimension(R.styleable.CheckboxSwitcher_elevation, 0f)

        defHeight = typedArray.getDimensionPixelOffset(R.styleable.CheckboxSwitcher_switcher_height, 0)
        defWidth = typedArray.getDimensionPixelOffset(R.styleable.CheckboxSwitcher_switcher_width, 0)

        val bgColor = typedArray.getColor(R.styleable.CheckboxSwitcher_switcher_bg_color, 0)

        switcherPaint.color = bgColor

        typedArray.recycle()

        if (!isLollipopAndAbove() && switchElevation > 0f) {
            shadowPaint.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            shadowPaint.alpha = 51 // 20%
            setShadowBlurRadius(switchElevation)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            width = defWidth
            height = defHeight
        }

        if (!isLollipopAndAbove()) {
            width += switchElevation.toInt() * 2
            height += switchElevation.toInt() * 2
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (isLollipopAndAbove()) {
            outlineProvider = SwitchOutline(w, h)
            elevation = switchElevation
        } else {
            shadowOffset = switchElevation
        }

        switcherRect.left = shadowOffset
        switcherRect.top = shadowOffset / 2
        switcherRect.right = width.toFloat() - shadowOffset
        switcherRect.bottom = height.toFloat() - shadowOffset - shadowOffset / 2

        switcherCornerRadius = height / 4f

        if (!isLollipopAndAbove()) generateShadow()
    }

    override fun onDraw(canvas: Canvas?) {
        // shadow
        if (!isLollipopAndAbove() && switchElevation > 0f && !isInEditMode) {
            canvas?.drawBitmap(shadow as Bitmap, 0f, shadowOffset, null)
        }

        // switcher
        canvas?.drawRoundRect(switcherRect, switcherCornerRadius, switcherCornerRadius, switcherPaint)
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class SwitchOutline internal constructor(internal var width: Int, internal var height: Int) :
        ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, width, height, switcherCornerRadius)
        }
    }
}