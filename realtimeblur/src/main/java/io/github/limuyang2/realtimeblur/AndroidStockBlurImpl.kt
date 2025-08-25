package io.github.limuyang2.realtimeblur

import android.content.Context
import android.graphics.Bitmap
import com.google.android.renderscript.ImageToolkit

class AndroidStockBlurImpl : BlurImpl {


    private var mRadius: Int = 4

    override fun prepare(context: Context, radius: Int): Boolean {
        if (radius < 0) {
            this.mRadius = 0
        } else if (radius > 25) {
            this.mRadius = 25
        } else {
            this.mRadius = radius
        }

        return true
    }

    override fun release() {
    }

    override fun blur(input: Bitmap): Bitmap? {
        if (mRadius <= 0 || mRadius > 25) return null

        return ImageToolkit.blur(input, mRadius)
    }


}
