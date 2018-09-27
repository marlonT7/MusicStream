package com.example.marlon.musicstream

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder


class RadioServiceBinder(private val audioUrl: String) : Binder() {
    // Media player that play audio.
    var mediaPlayer: MediaPlayer? = null

    // Start play audio.
    fun startAudio() {
        initMediaPlayer()
        mediaPlayer?.start()
    }

    // Pause playing audio.
    fun pauseAudio() {
        mediaPlayer?.pause()
    }
    // Stop playing audio.
    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.prepare()
    }

    // Initialise audio player.
    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                AudioManager.STREAM_MUSIC
                setDataSource(audioUrl)
                prepare()
            }
        }
    }

    // Destroy audio player.
    fun destroyAudioPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.stop()
            }
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

}