package com.bitvale.checkboxswitcher.commons

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 20-Jul-19
 */
fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

fun Context.toPx(value: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

fun isLollipopAndAbove(): Boolean {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
}

fun Context.getThumbDrawable(@DrawableRes resId: Int): VectorDrawableCompat? {
    return try {
        return VectorDrawableCompat.create(resources, resId, null)
    } catch (e: Resources.NotFoundException) {
        null
    }
}

fun Drawable?.getBitmapFromDrawable(): Bitmap? {
    return if (this == null) null
    else when (this::class) {
        VectorDrawableCompat::class -> (this as VectorDrawableCompat).getBitmapFromVector()
        BitmapDrawable::class -> (this as BitmapDrawable).bitmap
        else -> throw ClassCastException("$this is not supported!")
    }
}

fun VectorDrawableCompat.getBitmapFromVector(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}