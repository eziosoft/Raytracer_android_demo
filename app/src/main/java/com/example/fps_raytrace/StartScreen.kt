package com.example.fps_raytrace

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fps_raytrace.ui.theme.FPS_raytraceTheme

@Composable
fun StartScreen(modifier: Modifier = Modifier, onStart: () -> Unit = {}) {
    // Create a RuntimeShader instance
    val runtimeShader = remember { RuntimeShader(glitchShader) }

    // Animatable to control time uniform
    val time = remember { Animatable(0f) }

    // Start animating the time value
    LaunchedEffect(Unit) {
        time.animateTo(
            targetValue = 50000f, // Effectively infinite animation
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing)
            )
        )
    }

    val resolution = LocalDensity.current.run {
        val width = LocalConfiguration.current.screenWidthDp.dp.toPx()
        val height = LocalConfiguration.current.screenHeightDp.dp.toPx()
        floatArrayOf(width, height)
    }

    // Set the uniform values for the shader
    LaunchedEffect(time.value) {
        runtimeShader.setFloatUniform("time", time.value)
        runtimeShader.setFloatUniform("resolution", resolution[0], resolution[1])
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)) // Rounded corners
            .blendMode(BlendMode.Screen) // Set blend mode
            .graphicsLayer { // Use graphicsLayer for RenderEffect
                clip = true
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(runtimeShader, "composable")
                    .asComposeRenderEffect()
            }
            .background(Color.Black) // Set background color
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App title
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 80.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Start button with pulsating effect
            Pulsating {
                Text(
                    modifier = Modifier.clickable { onStart() },
                    text = "-> Start <-",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun GlitchEffect(modifier: Modifier, content: @Composable () -> Unit){
    // Create a RuntimeShader instance
    val runtimeShader = remember { RuntimeShader(glitchShader) }

    // Animatable to control time uniform
    val time = remember { Animatable(0f) }

    // Start animating the time value
    LaunchedEffect(Unit) {
        time.animateTo(
            targetValue = 50000f, // Effectively infinite animation
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing)
            )
        )
    }

    val resolution = LocalDensity.current.run {
        val width = LocalConfiguration.current.screenWidthDp.dp.toPx()
        val height = LocalConfiguration.current.screenHeightDp.dp.toPx()
        floatArrayOf(width, height)
    }

    // Set the uniform values for the shader
    LaunchedEffect(time.value) {
        runtimeShader.setFloatUniform("time", time.value)
        runtimeShader.setFloatUniform("resolution", resolution[0], resolution[1])
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)) // Rounded corners
            .blendMode(BlendMode.Screen) // Set blend mode
            .graphicsLayer { // Use graphicsLayer for RenderEffect
                clip = true
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(runtimeShader, "composable")
                    .asComposeRenderEffect()
            }
            .background(Color.Black) // Set background color
    ) {
        content()
    }
}


@Composable
fun Pulsating(pulseFraction: Float = 1.2f, content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = pulseFraction,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(modifier = Modifier.scale(scale)) {
        content()
    }
}

@Composable
@Preview
private fun StartScreenPreview() {
    FPS_raytraceTheme {
        StartScreen()
    }
}


fun Modifier.blendMode(blendMode: BlendMode): Modifier {
    return this.drawWithCache {
        val graphicsLayer = obtainGraphicsLayer()
        graphicsLayer.apply {
            record {
                drawContent()
            }
            this.blendMode = blendMode
        }
        onDrawWithContent {
            drawLayer(graphicsLayer)
        }
    }
}