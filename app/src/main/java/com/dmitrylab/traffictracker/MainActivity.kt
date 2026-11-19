package com.dmitrylab.traffictracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitrylab.traffictracker.ui.MainViewModel
import com.dmitrylab.traffictracker.ui.theme.TrafficTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrafficTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val traffic by viewModel.totalTraffic.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Traffic Tracker", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(text = "Up: ${traffic.first / (1024 * 1024)} MB", fontSize = 20.sp)
        Text(text = "Down: ${traffic.second / (1024 * 1024)} MB", fontSize = 20.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "анные обновляются каждые 5 сек.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
    }
}
