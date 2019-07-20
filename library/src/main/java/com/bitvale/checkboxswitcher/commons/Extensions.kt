package com.bitvale.checkboxswitcher.commons

import android.content.Context
import android.util.TypedValue

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 20-Jul-19
 */
fun Context.toPx(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

fun isLollipopAndAbove(): Boolean {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
}