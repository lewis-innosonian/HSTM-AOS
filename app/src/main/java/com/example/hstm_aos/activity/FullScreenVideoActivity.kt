package com.example.hstm_aos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.hstm_aos.R
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.core.content.ContextCompat

class FullScreenVideoActivity : AppCompatActivity() {

    private lateinit var video: VideoView
    private lateinit var playPause: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var btnExit: ImageView

    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_video)

        video = findViewById(R.id.videoView)
        playPause = findViewById(R.id.play_pause)
        seekBar = findViewById(R.id.seekBar)
        btnExit = findViewById(R.id.btnExit)

        // SeekBar 디자인
        seekBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.seekbar_progress)
        seekBar.thumb.setColorFilter(Color.parseColor("#0061F2"), android.graphics.PorterDuff.Mode.SRC_IN)

        // 인텐트로 전달된 비디오 URL, 위치, 재생 상태
        val videoUrl = intent.getStringExtra("video_url")

        Log.d("kimtest555","${videoUrl}")
        val startPos = intent.getIntExtra("position", 0)
        val shouldPlay = intent.getBooleanExtra("is_playing", false) // ← 재생 여부

        val uri = videoUrl?.let { Uri.parse(it) }
        if (uri != null) video.setVideoURI(uri)

        video.setOnPreparedListener {
            seekBar.max = video.duration
            video.seekTo(startPos)

            if (shouldPlay) {
                video.start()
                isPlaying = true
                playPause.setImageResource(R.drawable.inno_pause_white_icon)
                startProgress()
            } else {

                isPlaying = false
                playPause.setImageResource(R.drawable.inno_play_white_icon)
            }
        }

        playPause.setOnClickListener { toggle() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) video.seekTo(p)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnExit.setOnClickListener {
            exitFullscreen()
        }
    }


    private fun toggle() {
        if (isPlaying) pause() else play()
    }

    private fun play() {
        video.start()
        playPause.setImageResource(R.drawable.inno_pause_white_icon)
        isPlaying = true
        startProgress()
    }

    private fun pause() {
        video.pause()
        playPause.setImageResource(R.drawable.inno_play_white_icon)
        isPlaying = false
    }

    private fun startProgress() {
        handler.post(object : Runnable {
            override fun run() {
                if (isPlaying) {
                    seekBar.progress = video.currentPosition
                    handler.postDelayed(this, 500)
                }
            }
        })
    }

    private fun exitFullscreen() {
        // 현재 위치 전달
        val intent = Intent()
        intent.putExtra("video_pos", video.currentPosition)
        intent.putExtra("is_playing", isPlaying)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        exitFullscreen()
    }
}
