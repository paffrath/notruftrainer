package koeln.paffrath.notruftrainer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

private val ScreenBackground = Color(0xFFF4F2F0)
private val TextPrimary = Color(0xFF1B1B1B)
private val TextSecondary = Color(0xFF7A7A7A)
private val ChipBackground = Color(0xFFE5E2E0)
private val CallGreen = Color(0xFF00A651)
private val MedInfoRed = Color(0xFFD9532F)

private val KEY_LETTERS = mapOf(
    "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "0" to "+",
)

@Composable
fun DialerScreen() {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current
    val mapping = findMapping(input)

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            findMapping(input)?.let {
                placeCall(context, it.actualTarget)
                input = ""
            }
        }
    }

    fun attemptCall() {
        val target = mapping?.actualTarget ?: return
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            placeCall(context, target)
            input = ""
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Notruf",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(28.dp))
        if (input.isEmpty()) {
            AddContactsHint()
        }
        Spacer(Modifier.weight(1f))
        if (input.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = ChipBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Notrufnummer",
                    fontSize = 15.sp,
                    color = Color(0xFF4A4A4A),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        } else {
            Text(
                text = input,
                fontSize = 46.sp,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Spacer(Modifier.weight(1f))
        DialPad(
            onDigit = { digit -> input += digit },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(14.dp))
        BottomActionRow(
            onCall = ::attemptCall,
            onDelete = { if (input.isNotEmpty()) input = input.dropLast(1) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AddContactsHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = ChipBackground,
            modifier = Modifier.size(46.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "+",
                    fontSize = 24.sp,
                    color = Color(0xFF6A6A6A),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "+ tippen & Notfallkontakte\nhinzufügen",
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = Color(0xFF2E2E2E),
        )
    }
}

@Composable
private fun DialPad(onDigit: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#"),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                row.forEach { key -> DialKey(key) { onDigit(key) } }
            }
        }
    }
}

@Composable
private fun DialKey(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(width = 88.dp, height = 70.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                fontSize = 34.sp,
                color = TextPrimary,
            )
            val letters = KEY_LETTERS[label]
            if (letters != null) {
                Text(
                    text = letters,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp,
                )
            }
        }
    }
}

@Composable
private fun BottomActionRow(onCall: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 88.dp, height = 72.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MedInfoRed,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Med. Infos",
                    fontSize = 12.sp,
                    color = MedInfoRed,
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 88.dp, height = 72.dp),
        ) {
            Surface(
                onClick = onCall,
                shape = CircleShape,
                color = CallGreen,
                modifier = Modifier.size(68.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Anrufen",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
        Surface(
            onClick = onDelete,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(width = 88.dp, height = 72.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "⌫",
                    fontSize = 26.sp,
                    color = TextPrimary,
                )
            }
        }
    }
}

private fun placeCall(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_CALL, "tel:$number".toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
