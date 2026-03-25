package me.yuriisoft.buildnotify.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import me.yuriisoft.buildnotify.mobile.data.discovery.AndroidNsdDiscovery

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val component = AppComponent::class.create(AndroidNsdDiscovery())

        setContent {
            App(screens = component.screens)
        }
    }
}
