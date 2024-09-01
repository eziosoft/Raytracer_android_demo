package com.example.fps_raytrace

import RaytracerEngine
import android.graphics.Bitmap
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import engine.Screen
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import kotlin.time.DurationUnit
import kotlin.time.measureTime

const val W = 640
const val H = 400
const val FPS = 30

class MainActivity : ComponentActivity() {

    private lateinit var raytracerEngine: RaytracerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        raytracerEngine = RaytracerEngine(context = applicationContext, width = W, height = H)


        setContent {
            RayCaster(raytracer = raytracerEngine)
        }
    }

    @Composable
    fun RayCaster(raytracer: RaytracerEngine) {
        // Create a single Bitmap instance that will be reused
        val bitmap = remember { Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888) }
        // Convert the Bitmap to an ImageBitmap once
        val imageBitmap = remember(bitmap) { mutableStateOf(bitmap.asImageBitmap()) }

        // LaunchedEffect for the game loop
        LaunchedEffect(Unit) {
            while (true) {
                val renderTime = measureTime {
                    raytracer.gameLoop(
                        pressedKeys = emptySet(),
                        onFrame = { newBitmap ->
                            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(newBitmap.getByteArray()))
                            imageBitmap.value = bitmap.asImageBitmap()
                        }
                    )
                }
                // Delay to control FPS
                delay((1000 - renderTime.toLong(DurationUnit.MILLISECONDS)) / FPS.toLong())
            }
        }

        // Display the ImageBitmap
        Image(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            bitmap = imageBitmap.value,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
        )
    }



}

