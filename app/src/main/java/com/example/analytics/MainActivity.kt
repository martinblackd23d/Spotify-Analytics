package com.example.analytics

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

import android.content.Context
import android.os.Build
import java.io.FileWriter

import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.analytics.ui.theme.AnalyticsTheme

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/*class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnalyticsTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }
    }
}*/

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        setContentView(androidx.appcompat.R.layout.abc_screen_simple)
        Log.d("MainActivity", "Activity onCreate")
        val serviceIntent = Intent(this, BGService::class.java)
        startService(serviceIntent)
    }

    override fun onStart() {
        super.onStart()
        // We will start writing our code here.
        Log.d("MainActivity", "Activity onStart")

    }

    override fun onStop() {
        super.onStop()
        // Aaand we will finish off here.
        Log.d("MainActivity", "Activity onStop")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "Activity onDestroy")

    }
}

class BGService : Service() {

    private val clientId = ""
    private val redirectUri = "https://com.example.analytics/callback"
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var file: File? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var notification: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null


    override fun onCreate() {
        super.onCreate()
        Log.d("MainActivity", "Service onCreate")
        try {
            //file = File(Environment.getDataDirectory().absolutePath + "/SpotifyAnalytics/" + LocalDate.now().toString())
            file = File("/storage/emulated/0/Android/data/com.example.analytics/files/" + LocalDate.now().toString())
            //Log.d("MainActivity", "file created")
            //file?.mkdirs()
            //Log.d("MainActivity", "mkdirs")
            file?.createNewFile()
            //Log.d("MainActivity", "createNewFile")
        }
        catch (e: Exception) {
            Log.d("MainActivity", e.toString())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("SpotifyAnalytics", "SpotifyAnalytics channelName", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "SpotifyAnalytics descriptionText"
            }
            // Register the channel with the system
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
        notification = NotificationCompat.Builder(this, "SpotifyAnalytics")
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setContentTitle("Last Updated")
            .setContentText("ContentText")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setSilent(true)
        //createNotification()

        startForeground(1, notification?.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()
        /*
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BGService::lock").apply {
                    acquire()
                }
            }*/

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("MainActivity", "Connected! Yay!")
                // Now you can start interacting with App Remote
                connected()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("MainActivity", throwable.message, throwable)
                // Something went wrong when attempting to connect! Handle errors here
            }
        })
        return START_STICKY
    }

    private fun connected() {
        // Then we will write some more code here.
        // Subscribe to PlayerState
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback {
            val track: Track = it.track
            val current = LocalDateTime.now()
            val content = current.toString() + "\t" + it.toString()+ "\n"
            Log.d("MainActivity", content)
            notification?.setContentText((current.hour % 12).toString() + ":" + (current.minute).toString())
            try {
                val writer = FileWriter(file, true)
                writer.append(content)
                writer.flush()
                writer.close()
            } catch (e: Exception) {
                Log.d("MainActivity", e.toString())
                notification?.setContentText(e.toString())
            }

            notificationManager?.notify(1, notification?.build())
            /*Log.d("MainActivity", current.toString() + "\t" +
                    track.uri + "\t" +
                    track.duration + "\t" +
                    track.name + "\t" +
                    track.artist.name)
             */
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("MainActivity", "Service onBind")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "Service onDestroy")
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        notificationManager?.cancel(1)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AnalyticsTheme {
        Greeting("Android")
    }
}
