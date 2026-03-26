package me.yuriisoft.buildnotify.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import me.yuriisoft.buildnotify.mobile.service.ConnectionServiceManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val component = (application as BuildNotifyApp).component

        ConnectionServiceManager(this, component.connectionManager)
            .bind(lifecycleScope)

        setContent {
            App(screens = component.screens)
        }
    }
}
