package com.example.fps_raytrace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun Joystick(modifier: Modifier = Modifier, out: (Float, Float) -> Unit) {
    var joystickCenter by remember { mutableStateOf(Offset.Zero) }
    var handlePosition by remember { mutableStateOf(Offset.Zero) }
    var isTouched by remember { mutableStateOf(false) }
    val radius = 100f

    Box(
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isTouched = true },
                    onDragEnd = { isTouched = false },
                    onDragCancel = { isTouched = false }
                ) { change, dragAmount ->
                    val newOffset = handlePosition + dragAmount
                    val distance = sqrt(
                        (newOffset.x - joystickCenter.x).pow(2) + (newOffset.y - joystickCenter.y).pow(
                            2
                        )
                    )
                    if (distance <= radius) {
                        handlePosition = newOffset
                    } else {
                        val angle =
                            atan2(newOffset.y - joystickCenter.y, newOffset.x - joystickCenter.x)
                        handlePosition = Offset(
                            joystickCenter.x + radius * cos(angle),
                            joystickCenter.y + radius * sin(angle)
                        )
                    }
                    out(
                        (handlePosition.x - joystickCenter.x) / radius,
                        (handlePosition.y - joystickCenter.y) / radius
                    )
                    change.consume()
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            joystickCenter = center
            drawCircle(
                color = Color.Gray,
                radius = radius,
                center = joystickCenter,
                style = Stroke(width = 5f)
            )
            drawCircle(
                color = Color.LightGray,
                radius = 30f,
                center = handlePosition
            )
        }
    }

    LaunchedEffect(isTouched) {
        if (!isTouched) {
            handlePosition = joystickCenter
            out(0f, 0f)
        }
    }
}
@Composable
@Preview
private fun JoystickPreview() {
    Joystick { x, y ->
        println("x: $x, y: $y")
    }
}