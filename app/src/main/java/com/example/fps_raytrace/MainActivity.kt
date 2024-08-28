package com.example.fps_raytrace

import RaytracerEngine
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import engine.Screen
import kotlinx.coroutines.delay
import kotlin.time.DurationUnit
import kotlin.time.measureTime

const val W = 640
const val H = 480
const val FPS = 30

class MainActivity : ComponentActivity() {

    private lateinit var raytracerEngine: RaytracerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        raytracerEngine = RaytracerEngine(context = this, width = W, height = H)


        setContent {
            RayCaster(raytracer = raytracerEngine)
        }
    }

    @Composable
    fun RayCaster(raytracer: RaytracerEngine) {

        var screen by remember { mutableStateOf(Screen(W, H)) }

        Image(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            bitmap = screen.getBitmap(W, H).asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None,
        )


        LaunchedEffect(Unit) {
            while (true) {
                val renderTime = measureTime {
                    raytracer.gameLoop(
                        pressedKeys = emptySet(),//mapKeysToMoves(pressedKeys),
                        onFrame = {
                            screen = it
                        }
                    )
                }

                delay((1000 - renderTime.toLong(DurationUnit.MILLISECONDS)) / FPS.toLong())

                println("render time: $renderTime, maxFPS = ${1000 / renderTime.toInt(DurationUnit.MILLISECONDS)}")
            }
        }
    }


}

