package koeln.paffrath.notruftrainer

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val EXIT_PIN = "0000"
private const val MAX_PIN_LENGTH = 8

private val PinTextPrimary = Color(0xFF1C1C1C)
private val PinTextSecondary = Color(0xFF3C3C3C)
private val PinKeyCircle = Color.White.copy(alpha = 0.42f)

private val PIN_KEY_LETTERS = mapOf(
    "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
)

@Composable
fun PinScreen(onEmergency: () -> Unit, onExit: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFCBCAC2), Color(0xFFD9C6CA), Color(0xFFB8A3DB))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = PinTextPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(56.dp))
        if (pin.isEmpty()) {
            Text(
                text = "PIN eingeben",
                fontSize = 22.sp,
                color = PinTextPrimary,
            )
        } else {
            PinDots(count = pin.length)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (error) "Falsche PIN" else "Deine PIN enthält mindestens 4 Ziffern.",
            fontSize = 14.sp,
            color = if (error) Color(0xFFB3261E) else PinTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PinKeypad(
            onDigit = { digit ->
                if (pin.length < MAX_PIN_LENGTH) {
                    pin += digit
                    error = false
                }
            },
            onDelete = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                    error = false
                }
            },
            onOk = {
                if (pin == EXIT_PIN) {
                    onExit()
                } else {
                    pin = ""
                    error = true
                }
            },
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            onClick = onEmergency,
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.5f),
        ) {
            Text(
                text = "Notruf",
                fontSize = 15.sp,
                color = PinTextPrimary,
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 11.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PinDots(count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(PinTextPrimary)
            )
        }
    }
}

@Composable
private fun PinKeypad(onDigit: (String) -> Unit, onDelete: () -> Unit, onOk: () -> Unit) {
    val digitRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        digitRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    PinDigitKey(digit = key, letters = PIN_KEY_LETTERS[key]) { onDigit(key) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            PinTextKey(label = "⌫", fontSize = 26, onClick = onDelete)
            PinDigitKey(digit = "0", letters = null) { onDigit("0") }
            PinTextKey(label = "OK", fontSize = 20, onClick = onOk)
        }
    }
}

@Composable
private fun PinDigitKey(digit: String, letters: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = PinKeyCircle,
        modifier = Modifier.size(76.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = digit,
                fontSize = 28.sp,
                color = PinTextPrimary,
            )
            if (letters != null) {
                Text(
                    text = letters,
                    fontSize = 9.sp,
                    color = PinTextSecondary,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
private fun PinTextKey(label: String, fontSize: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(76.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = fontSize.sp,
                color = PinTextPrimary,
            )
        }
    }
}
