package com.bigdevil.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.Toast
import com.bigdevil.sphereview.sample.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_sphere_view.view.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        for (i in 0..8) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_sphere_view, null)
            val text = "测试数据$i"
            view.textView.text = text
            view.setOnClickListener {
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            }
            sphereView.addView(view)
        }

        start.setOnClickListener { sphereView.startLoop() }
        stop.setOnClickListener { sphereView.stopLoop() }
        add.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.item_sphere_view, null)
            val text = "测试数据${sphereView.childCount}"
            view.textView.text = text
            view.setOnClickListener {
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            }
            sphereView.addView(view)
        }
        remove.setOnClickListener { sphereView.removeViewAt(sphereView.childCount - 1) }
        loopSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                sphereView.loopSpeed = progress.toFloat()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        minScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                sphereView.minScale = 2f * progress / 100
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        maxScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                sphereView.maxScale = 2f * progress / 100
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        minAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                sphereView.minAlpha = 1f * progress / 100
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        maxElevation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                sphereView.maxElevation = progress
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }
}
