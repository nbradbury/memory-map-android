package com.nbradbury.memorymap

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nbradbury.memorymap.ui.MainViewModel
import com.nbradbury.memorymap.ui.MapScreen
import com.nbradbury.memorymap.ui.theme.MemoryMapTheme
import java.time.Clock

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel((application as MemoryMapApp).repository, Clock.systemDefaultZone()) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Pick up new photos (and permission changes made in Settings) whenever the app comes back.
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = vm.refresh()
        })
        setContent {
            MemoryMapTheme {
                val state by vm.uiState.collectAsStateWithLifecycle()
                MapScreen(state, vm)
            }
        }
    }
}
