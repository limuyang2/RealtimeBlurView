package io.github.limuyang2.realtimeblur

import android.content.Context
import android.graphics.Bitmap

class EmptyBlurImpl : BlurImpl {
    override fun prepare(context: Context, radius: Int): Boolean {
        return false
    }

    override fun release() {
    }

    override fun blur(input: Bitmap): Bitmap? = null
}
