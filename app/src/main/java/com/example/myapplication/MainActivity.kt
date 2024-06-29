package com.example.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.Runnable

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var myMessenger: Messenger? = null
    private var exampleService: Messenger? = null
    private var isServiceBound = false
    private var isCounterRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private val serviceConnection = object  : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            exampleService = Messenger(service)
            isServiceBound = true
            sendMessageToService(MSG_IS_RUNNING)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            exampleService = null
            isServiceBound = false
        }
    }

    inner class ResponseHandler : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            when (msg.what){
                MSG_START -> {
                    isCounterRunning = true
                    startPolling()
                    binding.button.text = "Restart"
                    updateScreenCounter(msg.arg1)
                }
                MSG_RESTART -> {
                    updateScreenCounter(msg.arg1)
                }
                MSG_GET_COUNTER ->
                    updateScreenCounter(msg.arg1)
                MSG_IS_RUNNING ->
                    if (msg.arg1 == 1){
                        isCounterRunning = true
                        startPolling()
                    }else{
                        isCounterRunning = false
                    }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.isNotEmpty() && permissions.values.all { it }) {

        } else {
            Toast.makeText(this, "Need permission.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        myMessenger = Messenger(ResponseHandler())
        startService()

        binding.button.setOnClickListener {
            if (isCounterRunning) {
               sendMessageToService(MSG_RESTART)
            } else {
                sendMessageToService(MSG_START)
            }
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, ExampleService::class.java)
        this.startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startPolling() {
        handler.post(object : Runnable {
            override fun run() {
                sendMessageToService(MSG_GET_COUNTER)
                if (isCounterRunning) {
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun updateScreenCounter(counter: Int){
        if (counter > 0){
            binding.textView.text = counter.toString()
        }else{
            binding.textView.text = ""
            isCounterRunning = false
        }
    }

    private fun sendMessageToService(messageId: Int) {
        val msg: Message = Message.obtain(null, messageId, 0, 0)
        msg.replyTo = myMessenger
        try {
            exampleService?.send(msg)
        }catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService()
    }


private fun unbindService(){
    if (isServiceBound){
        unbindService(serviceConnection)
        isServiceBound = false
    }
}

    private fun checkPermissions() {
        val isAllGranted = REQUEST_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (isAllGranted) {
            Toast.makeText(this, "permission is Granted", Toast.LENGTH_SHORT).show()
        } else {
            launcher.launch(REQUEST_PERMISSIONS)
        }

    }

    companion object {
        private val REQUEST_PERMISSIONS: Array<String> = buildList {
            add(android.Manifest.permission.FOREGROUND_SERVICE)
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
}