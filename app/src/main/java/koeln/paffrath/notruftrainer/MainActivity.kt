package koeln.paffrath.notruftrainer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import koeln.paffrath.notruftrainer.ui.theme.NotruftrainerTheme

class MainActivity : ComponentActivity() {
    private var resumeCounter by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, PowerButtonDetectorService::class.java))
        enableEdgeToEdge()
        setContent {
            NotruftrainerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NotruftrainerApp(resumeSignal = resumeCounter)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        KioskManager.applyKioskPolicies(this)
        resumeCounter++
    }
}

@Composable
private fun NotruftrainerApp(resumeSignal: Int) {
    var showDialer by remember { mutableStateOf(false) }
    LaunchedEffect(resumeSignal) {
        showDialer = false
    }
    if (showDialer) {
        DialerScreen()
    } else {
        PinScreen(onEmergency = { showDialer = true })
    }
}
