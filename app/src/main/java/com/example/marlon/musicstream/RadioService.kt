package com.example.marlon.musicstream

import android.app.Service
import android.content.Intent
import android.os.IBinder


class RadioService(url: String = "http://us5.internet-radio.com:8110/listen.pls&t=.m3u") : Service() {
    private val radioServiceBinder = RadioServiceBinder(url)
    override fun onBind(intent: Intent?): IBinder {
        return radioServiceBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)

    }
}
