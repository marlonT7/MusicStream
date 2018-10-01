package com.example.marlon.musicstream

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

// Params for the notification channel
const val CHANNEL_ID = "2"
const val CHANNEL_NAME = "LOW CHANNEL"

class RadioService() : MediaBrowserServiceCompat(), Parcelable {
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private val iBinder = LocalBinder()

    private var manager: NotificationManager? = null

    private lateinit var builder: NotificationCompat.Builder

    override fun onBind(intent: Intent?): IBinder {
        builder = createNotification()

        return iBinder
    }

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var pendingIntent: PendingIntent


    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): RadioService = this@RadioService
    }


    // Media player that play audio.
    var mediaPlayer: MediaPlayer? = null
    var url = "http://us5.internet-radio.com:8110/listen.pls&t=.m3u"

    constructor(parcel: Parcel) : this() {
        pendingIntent = parcel.readParcelable(PendingIntent::class.java.classLoader)
        url = parcel.readString()
    }


    // Start play audio.
    fun startAudio() {
        initMediaPlayer()
        mediaPlayer?.start()
    }

    // Pause playing audio.
    fun pauseAudio() {
        mediaPlayer?.pause()

    }

    // Initialise audio player.
    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                AudioManager.STREAM_MUSIC
                setDataSource(url)
                prepare()
            }

            startForeground(1, builder.build())

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
            stopForeground(true)
        }
    }

    private fun getNotificationManager() {
        if (manager == null) {
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
    }

    // Creates a channel for the notifications in android Oreo or higher
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val lowChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            getNotificationManager()
            manager?.createNotificationChannel(lowChannel)
        }
    }

    private fun createNotification(): NotificationCompat.Builder {
        createChannel()
        val intent = Intent(this, MainActivity::class.java)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

//        val remoteView = RemoteViews(packageName, R.layout.notification)
//        remoteView.setImageViewResource(R.id.image, R.mipmap.ic_launcher)
//        remoteView.setTextViewText(R.id.title, "Title")
//        remoteView.setTextViewText(R.id.text, "Url")
        mediaSession = MediaSessionCompat(this, "radio")
        val controller = mediaSession.controller
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        val noisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                pauseAudio()
            }
        }

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        val callbacks = object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                mediaSession.isActive = true
                mediaPlayer?.start()
                registerReceiver(noisyReceiver, intentFilter)
            }

            override fun onStop() {
                unregisterReceiver(noisyReceiver)
                mediaSession.isActive = false
                destroyAudioPlayer()
            }

            override fun onPause() {
                pauseAudio()
             }
        }
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
        mediaSession.setPlaybackState(stateBuilder.build())


        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(callbacks)

        // Set the session's token so that client activities can communicate with it.
        sessionToken = mediaSession.sessionToken


        // creates a notification builder depending of the android version
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(applicationContext)
        } else {
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        }
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(getString(R.string.notification_message))
                .setContentText(getString(R.string.url))

                // Enable launching the player by clicking the notification
                .setContentIntent(controller.sessionActivity)

                // Add a pause button
                .addAction(NotificationCompat.Action(
                        R.drawable.ic_pause_black_24dp, getString(R.string.label_pause),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                // Add stop but1ton
                .addAction(NotificationCompat.Action(
                        R.drawable.ic_stop_24dp, getString(R.string.label_pause),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_STOP)))
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP))

                // Take advantage of MediaStyle features
                .setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0))

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(pendingIntent, flags)
        parcel.writeString(url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RadioService> {
        override fun createFromParcel(parcel: Parcel): RadioService {
            return RadioService(parcel)
        }

        override fun newArray(size: Int): Array<RadioService?> {
            return arrayOfNulls(size)
        }
    }

}
