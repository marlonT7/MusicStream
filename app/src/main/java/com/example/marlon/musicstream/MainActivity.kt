@file:Suppress("IMPLICIT_CAST_TO_ANY")

package com.example.marlon.musicstream

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ImageButton
import kotlinx.android.synthetic.main.activity_main.*

const val CHANNEL_ID = "default"


class MainActivity : AppCompatActivity() {

    private var isMuted = false
    private lateinit var info: ImageButton
    private lateinit var play: ImageButton
    private lateinit var stop: ImageButton
    private lateinit var mute: ImageButton
    private var radioServiceBinder: RadioServiceBinder? = null

    // This service connection object is the bridge between activity and background service.
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            // Cast and assign background service's onBind method returned iBinder object.
            radioServiceBinder = iBinder as RadioServiceBinder
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind background audio service when activity is created.
        bindRadioService()
        info = info_button
        play = play_pause_button
        stop = stop_button
        mute = mute_button
        info.setOnClickListener { openInfo() }
        play.setOnClickListener { playMusic() }
        stop.setOnClickListener { stopMusic() }
        mute.setOnClickListener { muteAudio() }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID,
                "Radio stream",
                NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Channel description"
        notificationManager.createNotificationChannel(channel)
    }


    fun sendToForeground() {
        createNotificationChannel()
        val notificationIntent = Intent(this, this@MainActivity::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_message))
                .setContentText(getText(R.string.url))
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build()

        //startForeground(1, notification)
    }

    // Plays music if not playing, pause if is playing
    private fun playMusic() {
        if (radioServiceBinder?.mediaPlayer?.isPlaying == true) {
            radioServiceBinder?.pauseAudio()
            play.setImageResource(R.drawable.ic_play_arrow_24dp)
        } else {
            radioServiceBinder?.startAudio()
            play.setImageResource(R.drawable.ic_pause_black_24dp)

        }
    }

    private fun stopMusic() {
        radioServiceBinder?.stopAudio()
        play.setImageResource(R.drawable.ic_play_arrow_24dp)
    }


    private fun muteAudio() {
        // Mute the stream audio or unmute
        if (isMuted) {
            radioServiceBinder?.mediaPlayer?.setVolume(1f, 1f)
            mute.setImageResource(R.drawable.ic_volume_off_24dp)
        } else {
            radioServiceBinder?.mediaPlayer?.setVolume(0f, 0f)
            mute.setImageResource(R.drawable.ic_volume_up_24dp)
        }
        isMuted = !isMuted
    }


    // Open the radio info in an activity
    private fun openInfo() {
        val intent = Intent(this@MainActivity, InfoActivity::class.java)
        startActivity(intent)
    }

    // Binds the radio service with the main activity
    private fun bindRadioService() {
        if (radioServiceBinder == null) {
            val intent = Intent(this@MainActivity, RadioService::class.java)

            // Below code will invoke serviceConnection's onServiceConnected method.
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    // Unbound background audio service with caller activity.
    private fun unBoundAudioService() {
        if (radioServiceBinder != null) {
            unbindService(serviceConnection)
        }
    }

    override fun onDestroy() {
        // Unbound background audio service when activity is destroyed.
        unBoundAudioService()
        super.onDestroy()
    }
}
