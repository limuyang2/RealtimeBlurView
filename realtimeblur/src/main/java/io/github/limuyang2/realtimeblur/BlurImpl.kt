package io.github.limuyang2.realtimeblur

import android.content.Context
import android.graphics.Bitmap

interface BlurImpl {
    fun prepare(context: Context, radius: Int): Boolean

    fun release()

    fun blur(input: Bitmap): Bitmap?
}
