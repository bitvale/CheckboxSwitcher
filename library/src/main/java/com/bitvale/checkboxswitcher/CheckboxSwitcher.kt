package com.bitvale.checkboxswitcher

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.core.animation.doOnStart
import androidx.core.graphics.withTranslation
import com.bitvale.checkboxswitcher.commons.*
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
    private val iconPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    private var defHeight = 0
    var isChecked = false

    private var switcherCornerRadius = 0f

    private var shadow: Bitmap? = null
    private var switchElevation = 0f
    private var shadowOffset = 0f

    private val switcherRect = RectF(0f, 0f, 0f, 0f)
    private val thumbRect = RectF(0f, 0f, 0f, 0f)
    private var iconRect = RectF(0f, 0f, 0f, 0f)

    @ColorInt
    private var bgColor = 0
    @ColorInt
    private var onColor = 0
    @ColorInt
    private var offColor = 0
    @ColorInt
    private var thumbColor = 0

    private var thumbIcon: Bitmap? = null

    @Dimension(unit = Dimension.PX)
    private var thumbPadding = 0

    private var touchRect: Rect? = null
    private var touchOutside = false

    private var animatorSet: AnimatorSet? = null

    private var elevationAnimator: ValueAnimator? = null
    private var thumbTranslateX = 0f

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
        if (isChecked) {
            thumbColor = onColor
            iconPaint.alpha = OPAQUE.toInt()
        } else {
            thumbColor = offColor
            iconPaint.alpha = TRANSPARENT.toInt()
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

        isChecked = typedArray.getBoolean(R.styleable.CheckboxSwitcher_android_checked, false)

        bgColor = typedArray.getColor(R.styleable.CheckboxSwitcher_switcher_bg_color, 0)
        onColor = typedArray.getColor(R.styleable.CheckboxSwitcher_thumb_on_color, 0)
        offColor = typedArray.getColor(R.styleable.CheckboxSwitcher_thumb_off_color, 0)

        val drawableResId = typedArray.getResourceId(R.styleable.CheckboxSwitcher_thumb_icon, 0)
        if (drawableResId > 0) {
            val drawable = context.getThumbDrawable(drawableResId)
            thumbIcon = drawable.getBitmapFromDrawable()
        }

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

        if (isChecked) thumbTranslateX = width - shadowOffset * 2 - thumbPadding * 2 - thumbRect.width()
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
        canvas?.withTranslation(
            x = thumbTranslateX
        ) {
            switcherPaint.color = thumbColor
            drawRoundRect(thumbRect, switcherCornerRadius, switcherCornerRadius, switcherPaint)
            drawIcon(canvas)
        }
    }

    private fun drawIcon(canvas: Canvas?) {
        thumbIcon?.let {
            val offset = thumbRect.width() / 2 - thumbPadding
            iconRect.left = thumbRect.centerX() - offset
            iconRect.top = thumbRect.centerY() - offset
            iconRect.right = thumbRect.centerX() + offset
            iconRect.bottom = thumbRect.centerY() + offset
            canvas?.drawBitmap(it, null, iconRect, iconPaint)
        }
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
        animatorSet?.cancel()
        animatorSet = AnimatorSet()

        var translateA = 0f
        var translateB = (width - shadowOffset * 2 - thumbPadding * 2 - thumbRect.width())
        var alphaA = TRANSPARENT
        var alphaB = OPAQUE

        if (isChecked) {
            translateA = translateB
            translateB = 0f
            alphaA = OPAQUE
            alphaB = TRANSPARENT
        }

        val toColor = if (!isChecked) onColor else offColor

        val translateAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                thumbTranslateX = lerp(translateA, translateB, value)
                invalidate()
            }

            interpolator = BounceInterpolator(BOUNCE_ANIM_AMPLITUDE, BOUNCE_ANIM_FREQUENCY)
            duration = TRANSLATE_ANIMATION_DURATION
        }

        val alphaAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                iconPaint.alpha = lerp(alphaA, alphaB, value).toInt()
            }
            duration = ALPHA_ANIMATION_DURATION
        }

        val colorAnimator = ValueAnimator().apply {
            addUpdateListener {
                thumbColor = it.animatedValue as Int
            }
            setIntValues(thumbColor, toColor)
            setEvaluator(ArgbEvaluator())
            duration = COLOR_ANIMATION_DURATION
        }

        animatorSet?.apply {
            doOnStart {
                isChecked = !isChecked
                listener?.invoke(isChecked)
            }
            playTogether(translateAnimator, colorAnimator, alphaAnimator)
            start()
        }
    }

    private fun updateElevation(pressed: Boolean) {
        val to = if (pressed) switchElevation / 2
        else switchElevation

        elevationAnimator?.cancel()

        elevationAnimator = ValueAnimator.ofFloat(currentElevation, to).apply {
            addUpdateListener {
                currentElevation = it.animatedValue as Float
            }
            duration = 200
            start()
        }
    }

    private var listener: ((isChecked: Boolean) -> Unit)? = null

    /**
     * Register a callback to be invoked when the isChecked state of this switch
     * changes.
     *
     * @param listener the callback to call on isChecked state change
     */
    fun setOnCheckedChangeListener(listener: (isChecked: Boolean) -> Unit) {
        this.listener = listener
    }

    /**
     * <p>Changes the isChecked state of this switch.</p>
     *
     * @param checked true to check the switch, false to uncheck it
     * @param withAnimation use animation
     */
    fun setChecked(checked: Boolean, withAnimation: Boolean = true) {
        if (this.isChecked != checked) {
            if (withAnimation) {
                animateSwitch()
            } else {
                this.isChecked = checked
                if (!checked) {
                    thumbColor = offColor
                    thumbTranslateX = 1f
                    iconPaint.alpha = TRANSPARENT.toInt()
                } else {
                    thumbColor = onColor
                    thumbTranslateX = (width - shadowOffset * 2 - thumbPadding * 2 - thumbRect.width())
                    iconPaint.alpha = OPAQUE.toInt()
                }
                invalidate()
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        super.onSaveInstanceState()
        return Bundle().apply {
            putBoolean(KEY_CHECKED, isChecked)
            putParcelable(STATE, super.onSaveInstanceState())
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable(STATE))
            isChecked = state.getBoolean(KEY_CHECKED)
            if (!isChecked) forceUncheck()
        }
    }

    private fun forceUncheck() {
        thumbColor = offColor
        thumbTranslateX = 1f
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class SwitchOutline internal constructor(internal var width: Int, internal var height: Int) :
        ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, width, height, switcherCornerRadius)
        }
    }
}