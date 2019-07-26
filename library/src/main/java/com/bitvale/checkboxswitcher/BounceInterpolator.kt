package com.bitvale.checkboxswitcher

import android.view.animation.Interpolator
import kotlin.math.cos
import kotlin.math.pow

/**
 * Created by Evgenii Neumerzhitckii
 * (read more on https://evgenii.com/blog/spring-button-animation-on-android/)
 */
class BounceInterpolator(private val amplitude: Double, private val frequency: Double) : Interpolator {
    override fun getInterpolation(time: Float): Float {
        return (-1 * Math.E.pow(-time / amplitude) * cos(frequency * time) + 1).toFloat()
    }
}