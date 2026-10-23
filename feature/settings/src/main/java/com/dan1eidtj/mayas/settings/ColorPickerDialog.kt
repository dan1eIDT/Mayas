package com.dan1eidtj.mayas.core.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.dan1eidtj.mayas.settings.R
import com.dan1eidtj.mayas.core.ui.theme.ThemeConverter.hexToColor
import com.dan1eidtj.mayas.core.ui.theme.ThemeConverter.toHexArgb
import kotlin.math.roundToInt


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val hsv = remember {
        val argb = android.graphics.Color.valueOf(
            initialColor.red, initialColor.green, initialColor.blue, initialColor.alpha
        ).toArgb()
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, out)
        out
    }

    var hue by remember { mutableStateOf(hsv[0]) }
    var saturation by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }
    var alpha by remember { mutableStateOf(initialColor.alpha) }
    var hexInput by remember { mutableStateOf(initialColor.toHexArgb()) }

    val currentColor = Color.hsv(hue, saturation, value, alpha)



    LaunchedEffectSafe(hue, saturation, value, alpha) {
        hexInput = currentColor.toHexArgb()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(currentColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledSlider(stringResource(R.string.hue_label), hue, 0f, 360f) { hue = it }
                LabeledSlider(stringResource(R.string.saturation_label), saturation, 0f, 1f) { saturation = it }
                LabeledSlider(stringResource(R.string.brightness_label), value, 0f, 1f) { value = it }
                LabeledSlider(stringResource(R.string.opacity_label), alpha, 0f, 1f) { alpha = it }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    label = { Text(stringResource(R.string.hex_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    runCatching { hexInput.hexToColor() }.onSuccess { parsed ->
                        val out = FloatArray(3)
                        android.graphics.Color.colorToHSV(parsed.toArgb(), out)
                        hue = out[0]; saturation = out[1]; value = out[2]
                        alpha = parsed.alpha
                    }
                }) {
                    Text(stringResource(R.string.apply_hex))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor) }) { Text(stringResource(R.string.done_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel)) }
        }
    )
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label)
            Spacer(modifier = Modifier.height(0.dp))
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
        )
    }
}



@Composable
private fun LaunchedEffectSafe(vararg keys: Any?, block: suspend () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(*keys) { block() }
}
