package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.core.app.NotificationCompat

const val MSG_START = 1
const val MSG_RESTART = 2
const val MSG_GET_COUNTER = 3
const val MSG_IS_RUNNING = 4

class ExampleService : Service() {

    private lateinit var  mMessenger: Messenger
    private val handler = Handler(Looper.getMainLooper())
    private val channelId = "CHANNEL_ID"
    private val notificationId = 1

    private lateinit var notificationIntent: Intent
    private lateinit var pendingIntent: PendingIntent
    private lateinit var notificationManager: NotificationManager

    var isRunning = false
    private var counter = 21

    inner class IncommingHandler:
        Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            var clientMessenger = msg.replyTo
            var replyArg = 0
            when(msg.what){
                MSG_START -> replyArg = startCountdown()
                MSG_RESTART -> replyArg = restartCountdown()
                MSG_GET_COUNTER -> replyArg = getProgress()
                MSG_IS_RUNNING -> replyArg = if (isRunning) 1 else 0
                else -> super.handleMessage(msg)
            }
            val msg: Message = Message.obtain(null, msg.what, replyArg, 0)
            try {
                clientMessenger?.send(msg)
            }catch (e: RemoteException){
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        val channel = NotificationChannel(
            channelId,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        notificationIntent = Intent(this, MainActivity::class.java)
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        mMessenger = Messenger(IncommingHandler())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        notificationManager.deleteNotificationChannel(channelId)
    }

    override fun onBind(intent: Intent?): IBinder {
        return mMessenger.binder
    }

    fun getProgress(): Int{
        return counter
    }

    fun restartCountdown(): Int {
        return 20
    }

     fun startCountdown(): Int {
        counter = 20
        isRunning = true

         startForegroundService()
        handler.post(object : Runnable{
            override fun run() {
                if (counter>0){
                    handler.postDelayed(this,1000)
                    counter--
                    createNotification(counter)
                }
                else{
                    isRunning = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }

        })
        return counter
    }

    private fun startForegroundService(){
        startForegroundService(notificationIntent)
    }

    private fun createNotification(counter: Int) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Service Samples")
            .setContentText("Progress: $counter")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }
}