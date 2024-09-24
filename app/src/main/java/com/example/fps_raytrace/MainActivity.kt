package com.example.fps_raytrace

import Moves
import RaytracerEngine
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

const val W = 640
const val H = W * 8 / 16
const val FPS = 30

class MainActivity : ComponentActivity() {

    private lateinit var raytracerEngine: RaytracerEngine
    private val pressedKeys = mutableSetOf<Moves>()

    private var isRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemNavigationBar()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        raytracerEngine = RaytracerEngine(context = applicationContext, width = W, height = H)


        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                RayCaster(raytracer = raytracerEngine)

                Joystick(
                    modifier = Modifier.align(BottomEnd)
                ) { x, y ->
                    Log.d("aaa", "onCreate: x = $x, y = $y")

                    pressedKeys.clear()
//                    when {
//                        x > 0.5 -> pressedKeys.add(Moves.RIGHT)
//                        x < -0.5 -> pressedKeys.add(Moves.LEFT)
//                    }
//                    when {
//                        y > 0.5 -> pressedKeys.add(Moves.DOWN)
//                        y < -0.5 -> pressedKeys.add(Moves.UP)
//                    }

                    raytracerEngine.movePlayer(x / 30f, -y / 5f)
                }
            }
        }
    }


    @Composable
    fun RayCaster(raytracer: RaytracerEngine) {
        // Create a single Bitmap instance that will be reused
        val bitmap = remember { Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888) }
        // Convert the Bitmap to an ImageBitmap once
        val imageBitmap = remember(bitmap) { mutableStateOf(bitmap.asImageBitmap()) }

        val scope = rememberCoroutineScope()

        var timer = 0L

        // LaunchedEffect for the game loop
        LaunchedEffect(isRunning) {
            while (isRunning) {
                if (System.currentTimeMillis() > timer) {
                    timer = (System.currentTimeMillis() + (1000 / FPS).toLong())

                    raytracer.gameLoop(
                        pressedKeys = pressedKeys,
                        onFrame = { newBitmap ->
                            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(newBitmap.getByteArray()))
                            imageBitmap.value = bitmap.asImageBitmap()
                        }
                    )

                    delay(1) // delay to allow compose to draw the frame
                }
            }
        }

        // Display the ImageBitmap
        Image(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable {
                    scope.launch {
                        repeat(8) {
                            raytracerEngine.shootAndCheckHits()
                            delay(10)
                        }
//                        pressedKeys.add(Moves.SHOOT)
//                        delay(200)
//                        pressedKeys.clear()
                    }
                },
            bitmap = imageBitmap.value,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
        )
    }

    override fun onResume() {
        super.onResume()
        isRunning = true
        raytracerEngine.start()
    }

    override fun onPause() {
        super.onPause()
        isRunning = false
        raytracerEngine.dispose()
    }

    private fun hideSystemNavigationBar() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

