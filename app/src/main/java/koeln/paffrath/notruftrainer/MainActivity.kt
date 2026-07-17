package koeln.paffrath.notruftrainer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import koeln.paffrath.notruftrainer.ui.theme.NotruftrainerTheme

class MainActivity : ComponentActivity() {
    private var resumeCounter by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, PowerButtonDetectorService::class.java))
        enableEdgeToEdge()
        setContent {
            NotruftrainerTheme {
                NotruftrainerApp(
                    resumeSignal = resumeCounter,
                    onExitKiosk = { KioskManager.exitKiosk(this@MainActivity) },
                )
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
private fun NotruftrainerApp(resumeSignal: Int, onExitKiosk: () -> Unit) {
    var showDialer by remember { mutableStateOf(false) }
    LaunchedEffect(resumeSignal) {
        showDialer = false
    }
    if (showDialer) {
        DialerScreen()
    } else {
        key(resumeSignal) {
            PinScreen(onEmergency = { showDialer = true }, onExit = onExitKiosk)
        }
    }
}
