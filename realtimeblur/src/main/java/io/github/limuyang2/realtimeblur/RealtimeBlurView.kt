package io.github.limuyang2.realtimeblur

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.createBitmap
import kotlin.math.max

/**
 * A realtime blurring overlay (like iOS UIVisualEffectView). Just put it above
 * the view you want to blur and it doesn't have to be in the same ViewGroup
 *
 *  * realtimeBlurRadius (10dp)
 *  * realtimeDownsampleFactor (4)
 *  * realtimeOverlayColor (#aaffffff)
 *
 */
open class RealtimeBlurView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var mDownsampleFactor: Float = 0f // default 4
    private var mOverlayColor: Int = 0 // default #aaffffff
    private var mBlurRadius: Int = 0// default 10dp (0 < r <= 25)

    private val mBlurImpl: BlurImpl
    private var mDirty = false
    private var mBitmapToBlur: Bitmap? = null
    private var mBlurredBitmap: Bitmap? = null
    private var mBlurringCanvas: Canvas? = null
    private var mIsRendering = false
    private val mPaint: Paint = Paint()
    private val mRectSrc = Rect()
    private val mRectDst = Rect()

    // mDecorView should be the root view of the activity (even if you are on a different window like a dialog)
    private var mDecorView: View? = null

    // If the view is on different root view (usually means we are on a PopupWindow),
    // we need to manually call invalidate() in onPreDraw(), otherwise we will not be able to see the changes
    private var mDifferentRoot = false

    protected open val blurImpl: BlurImpl get() = AndroidStockBlurImpl()

    init {
        mBlurImpl = this.blurImpl // provide your own by override getBlurImpl()

        context.withStyledAttributes(attrs, R.styleable.RealtimeBlurView) {
            mBlurRadius = getInt(
                R.styleable.RealtimeBlurView_realtimeBlurRadius,
                10
            )
            mDownsampleFactor = getFloat(R.styleable.RealtimeBlurView_realtimeDownsampleFactor, 4f)
            mOverlayColor = getColor(R.styleable.RealtimeBlurView_realtimeOverlayColor, -0x55000001)
        }
    }

    fun setBlurRadius(radius: Int) {
        if (mBlurRadius != radius) {
            mBlurRadius = radius
            mDirty = true
            invalidate()
        }
    }

    fun setDownsampleFactor(factor: Float) {
        require(!(factor <= 0)) { "Downsample factor must be greater than 0." }

        if (mDownsampleFactor != factor) {
            mDownsampleFactor = factor
            mDirty = true // may also change blur radius
            releaseBitmap()
            invalidate()
        }
    }

    fun setOverlayColor(color: Int) {
        if (mOverlayColor != color) {
            mOverlayColor = color
            invalidate()
        }
    }

    private fun releaseBitmap() {
        mBitmapToBlur?.recycle()
        mBitmapToBlur = null

        mBlurredBitmap?.recycle()
        mBlurredBitmap = null
    }

    protected fun release() {
        releaseBitmap()
        mBlurImpl.release()
    }

    protected fun prepare(): Boolean {
        if (mBlurRadius == 0) {
            release()
            return false
        }

        var downsampleFactor = mDownsampleFactor
        var radius = mBlurRadius / downsampleFactor
        if (radius > 25) {
            radius = 25f
            downsampleFactor = downsampleFactor * radius / 25
        }

        val width = getWidth()
        val height = getHeight()

        val scaledWidth = max(1, (width / downsampleFactor).toInt())
        val scaledHeight = max(1, (height / downsampleFactor).toInt())

        var dirty = mDirty

        if (mBlurringCanvas == null || mBitmapToBlur == null) {
            dirty = true
            releaseBitmap()

            try {
                mBitmapToBlur = createBitmap(scaledWidth, scaledHeight)
                if (mBitmapToBlur == null) {
                    return false
                }
                mBlurringCanvas = Canvas(mBitmapToBlur!!)

            } catch (_: OutOfMemoryError) {
                // Bitmap.createBitmap() may cause OOM error
                // Simply ignore and fallback
                release()
                return false
            }
        }

        if (dirty) {
            if (mBlurImpl.prepare(context, radius.toInt())) {
                mDirty = false
            } else {
                return false
            }
        }

        return true
    }


    private val preDrawListener: ViewTreeObserver.OnPreDrawListener =
        ViewTreeObserver.OnPreDrawListener {

            val decor = mDecorView
            if (decor != null && isShown && prepare()) {
                val locations = IntArray(2)

                decor.getLocationOnScreen(locations)
                var x = -locations[0]
                var y = -locations[1]

                getLocationOnScreen(locations)
                x += locations[0]
                y += locations[1]

                // just erase transparent
                mBitmapToBlur!!.eraseColor(mOverlayColor and 0xffffff)

                val rc = mBlurringCanvas!!.save()
                mIsRendering = true
                RENDERING_COUNT++
                try {
                    mBlurringCanvas!!.scale(
                        1f * mBitmapToBlur!!.width / width,
                        1f * mBitmapToBlur!!.height / height
                    )
                    mBlurringCanvas!!.translate(-x.toFloat(), -y.toFloat())

                    decor.background?.draw(mBlurringCanvas!!)
                    decor.draw(mBlurringCanvas!!)
                } catch (_: StopException) {
                } finally {
                    mIsRendering = false
                    RENDERING_COUNT--
                    mBlurringCanvas!!.restoreToCount(rc)
                }


                mBlurredBitmap = mBlurImpl.blur(mBitmapToBlur!!)


                invalidate()
            }

            true
        }

    protected val activityDecorView: View?
        get() {
            var ctx = context
            var i = 0
            while (i < 4 && ctx != null && (ctx !is Activity) && (ctx is ContextWrapper)) {
                ctx = ctx.baseContext
                i++
            }
            if (ctx is Activity) {
                return ctx.window.decorView
            } else {
                return null
            }
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mDecorView = this.activityDecorView.also {
            if (it != null) {
                it.viewTreeObserver.addOnPreDrawListener(preDrawListener)
                mDifferentRoot = it.rootView !== rootView
                if (mDifferentRoot) {
                    it.postInvalidate()
                }
            } else {
                mDifferentRoot = false
            }
        }
    }

    override fun onDetachedFromWindow() {
        mDecorView?.viewTreeObserver?.removeOnPreDrawListener(preDrawListener)
        release()
        super.onDetachedFromWindow()
    }

    override fun draw(canvas: Canvas) {
        if (mIsRendering) {
            // Quit here, don't draw views above me
            throw STOP_EXCEPTION
        } else if (RENDERING_COUNT > 0) {
            // Doesn't support blurview overlap on another blurview
        } else {
            super.draw(canvas)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBlurredBitmap(canvas, mBlurredBitmap, mOverlayColor)
    }

    /**
     * Custom draw the blurred bitmap and color to define your own shape
     *
     * @param canvas
     * @param blurredBitmap
     * @param overlayColor
     */
    protected open fun drawBlurredBitmap(
        canvas: Canvas,
        blurredBitmap: Bitmap?,
        overlayColor: Int,
    ) {
        if (blurredBitmap != null) {
            mRectSrc.right = blurredBitmap.width
            mRectSrc.bottom = blurredBitmap.height
            mRectDst.right = width
            mRectDst.bottom = height
            canvas.drawBitmap(blurredBitmap, mRectSrc, mRectDst, null)
        }
        mPaint.color = overlayColor
        canvas.drawRect(mRectDst, mPaint)
    }

    private class StopException : RuntimeException()


    companion object {
        private var RENDERING_COUNT = 0

        private val STOP_EXCEPTION = StopException()
    }
}
