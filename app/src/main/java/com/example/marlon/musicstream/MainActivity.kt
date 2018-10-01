package com.example.marlon.musicstream

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.widget.ImageButton
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var isMuted = false
    private var bound = false
    private lateinit var info: ImageButton
    private lateinit var play: ImageButton
    private lateinit var stop: ImageButton
    private lateinit var mute: ImageButton
    private var radioService: RadioService? = null

    // This service connection object is the bridge between activity and background service.
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            // Cast and assign background service's onBind method returned iBinder object.
            val binder = iBinder as RadioService.LocalBinder
            radioService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        savedInstanceState?.run {
            radioService = getParcelable("key")
            isMuted = getBoolean("keyMute")
            changeMuteButton()
            changePlayButton()
        }

        // Adds on click method to the buttons
        info = info_button
        play = play_pause_button
        stop = stop_button
        mute = mute_button
        info.setOnClickListener { openInfo() }
        play.setOnClickListener { playMusic() }
        stop.setOnClickListener { stopMusic() }
        mute.setOnClickListener { muteAudio() }
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        bindRadioService()
    }

    // Plays music if not playing, pause if is playing
    private fun playMusic() {
        if (radioService?.mediaPlayer?.isPlaying == true) {
            radioService?.pauseAudio()
        } else {
            radioService?.startAudio()
        }
        changePlayButton()
    }

    // Stops the music and destroys the media player
    //Changes the play icon button
    private fun stopMusic() {
        radioService?.destroyAudioPlayer()
        play.setImageResource(R.drawable.ic_play_arrow_24dp)
    }


    private fun muteAudio() {
        // Mute the stream audio or unmute
        changeMuteButton()
        isMuted = !isMuted
    }


    // Open the radio info in an activity
    private fun openInfo() {
        val intent = Intent(this@MainActivity, InfoActivity::class.java)
        startActivity(intent)
    }

    // Binds the radio service with the main activity
    private fun bindRadioService() {
        if (radioService == null) {
            val intent = Intent(this@MainActivity, RadioService::class.java)

            // Below code will invoke serviceConnection's onServiceConnected method.
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

    }

    // Unbound background audio service with caller activity.
    private fun unBoundAudioService() {
        if (radioService != null) {
            unbindService(serviceConnection)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState?.run {
            radioService = getParcelable("key")
            isMuted = getBoolean("keyMute")
        }
        changeMuteButton()
        changePlayButton()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putParcelable("key", radioService)
            putBoolean("keyMute", isMuted)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        // Unbound background audio service when activity is destroyed.
        unBoundAudioService()
        super.onDestroy()
    }

    //  Changes the icon on the button depending if is muted
    private fun changeMuteButton() = if (!isMuted) {
        radioService?.mediaPlayer?.setVolume(1f, 1f)
        mute.setImageResource(R.drawable.ic_volume_off_24dp)
    } else {
        radioService?.mediaPlayer?.setVolume(0f, 0f)
        mute.setImageResource(R.drawable.ic_volume_up_24dp)
    }

    // Changes the icon on the button depending if is playing
    private fun changePlayButton() {
        if (radioService?.mediaPlayer?.isPlaying != true) {
            play.setImageResource(R.drawable.ic_play_arrow_24dp)
        } else {
            play.setImageResource(R.drawable.ic_pause_black_24dp)
        }
    }
}
