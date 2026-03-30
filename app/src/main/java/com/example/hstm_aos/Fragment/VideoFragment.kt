package com.example.hstm_aos.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.hstm_aos.FullScreenVideoActivity
import com.example.hstm_aos.R
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity

class VideoFragment : Fragment(R.layout.fragment_video) {

    private lateinit var video: VideoView
    private lateinit var btnPlay: ImageView
    private lateinit var playPause: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var control: View
    private lateinit var btnFull: ImageView

    private var isPlaying = false
    private var hasPlayedOnce = false
    private val handler = Handler(Looper.getMainLooper())
    private var videoUrl: String? = null

    private val REQUEST_FULLSCREEN = 1001
    private var lastPosition: Int = 0 // ← 현재 위치 저장용

    companion object {
        fun newInstance(url: String): VideoFragment {
            val fragment = VideoFragment()
            fragment.arguments = Bundle().apply { putString("video_url", url) }
            return fragment
        }
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        video = v.findViewById(R.id.videoView)
        btnPlay = v.findViewById(R.id.btnPlay)
        playPause = v.findViewById(R.id.play_pause)
        seekBar = v.findViewById(R.id.seekBar)
        control = v.findViewById(R.id.controlLayout)
        btnFull = v.findViewById(R.id.btnFull)

        seekBar.progressDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.seekbar_progress)
        seekBar.thumb.setColorFilter(Color.parseColor("#0061F2"), android.graphics.PorterDuff.Mode.SRC_IN)

        videoUrl = arguments?.getString("video_url")
        val uri = videoUrl?.let { Uri.parse(it) }

        if (uri != null) video.setVideoURI(uri)
        else video.setVideoPath("android.resource://${requireContext().packageName}/${R.raw.sample}")

        video.setOnPreparedListener {
            video.seekTo(lastPosition)
            video.pause()
            seekBar.max = video.duration
            btnPlay.visibility = View.VISIBLE
            playPause.setImageResource(R.drawable.inno_play_white_icon)
        }

        btnPlay.setOnClickListener { toggle() }
        playPause.setOnClickListener { toggle() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) video.seekTo(p)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnFull.setOnClickListener {
            val intent = Intent(requireContext(), FullScreenVideoActivity::class.java)
            intent.putExtra("video_url", videoUrl)
            intent.putExtra("position", video.currentPosition)
            intent.putExtra("is_playing", isPlaying)
            startActivityForResult(intent, REQUEST_FULLSCREEN)
            pause()
        }
    }

    private fun toggle() { if (isPlaying) pause() else play() }

    private fun play() {
        video.start()
        if (!hasPlayedOnce) btnPlay.visibility = View.GONE
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FULLSCREEN && resultCode == AppCompatActivity.RESULT_OK) {
            lastPosition = data?.getIntExtra("video_pos", 0) ?: 0
            val wasPlaying = data?.getBooleanExtra("is_playing", false) ?: false

            video.setOnPreparedListener { mp ->
                video.seekTo(lastPosition)

                if (wasPlaying) {
                    video.start()
                    isPlaying = true
                    playPause.setImageResource(R.drawable.inno_pause_white_icon)
                    startProgress()
                } else {
                    video.pause()
                    playPause.setImageResource(R.drawable.inno_play_white_icon)
                }
            }

            // 이미 준비된 상태라면 바로 적용
            if (video.duration > 0) {
                video.seekTo(lastPosition)
                if (wasPlaying) play() else pause()
            }
        }
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            // 프래그먼트가 hide 될 때
            lastPosition = video.currentPosition
            pause()
        }
    }

    override fun onPause() {
        super.onPause()
        lastPosition = video.currentPosition // ← Fragment가 사라질 때 위치 저장
        pause()
    }
}
