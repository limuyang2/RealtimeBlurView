package io.github.limuyang2.realtimeblurview

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import io.github.limuyang2.realtimeblur.RealtimeBlurView
import io.github.limuyang2.realtimeblurview.databinding.ActivityMainBinding
import java.util.Random

/**
 * Created by mmin18 on 3/5/16.
 */
class MainActivity : Activity() {

    private val viewBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        (findViewById<View?>(R.id.list) as ListView).setAdapter(
            MyListAdapter(
                this,
                R.layout.list_item
            )
        )

        viewBinding.blurRadius.progress = 15
        viewBinding.blurRadius.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateRadius()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        updateRadius()

        viewBinding.drag.setOnTouchListener(touchListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("Popup").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add("List").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if ("Popup" == item.getTitle()) {
            showPopup()
        } else if ("List" == item.getTitle()) {
            startActivity(Intent(this, ListActivity::class.java))
        }
        return true
    }

    private fun showPopup() {
        val b = AlertDialog.Builder(this)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val inflater = LayoutInflater.from(this)
            val layout = inflater.inflate(R.layout.popup_layout, null)
            b.setView(layout)
        } else {
            b.setView(R.layout.popup_layout)
        }
        val dlg: Dialog = b.show()
        dlg.findViewById<View?>(R.id.btn).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                (findViewById<View?>(R.id.list) as ListView).smoothScrollToPosition(
                    Random(System.currentTimeMillis()).nextInt(
                        10
                    )
                )
            }
        })
    }

    private fun updateRadius() {
        viewBinding.blurView.setBlurRadius(viewBinding.blurRadius.progress)
        viewBinding.blurRadiusText.text = viewBinding.blurRadius.progress.toString()
    }

    private val touchListener: OnTouchListener = object : OnTouchListener {
        var dx: Float = 0f
        var dy: Float = 0f

        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            val target = findViewById<View>(R.id.blur_frame)
            if (event.action == MotionEvent.ACTION_DOWN) {
                dx = target.getX() - event.getRawX()
                dy = target.getY() - event.getRawY()
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                target.setX(event.getRawX() + dx)
                target.setY(event.getRawY() + dy)
            }
            return true
        }
    }

    private var slideUp = false

    fun doSlide(v: View?) {
        val view = findViewById<View>(R.id.blur_frame)
        view.animate().translationYBy(((if (slideUp) -1 else 1) * view.height).toFloat())
            .setDuration(1000).start()
        slideUp = !slideUp
    }
}
