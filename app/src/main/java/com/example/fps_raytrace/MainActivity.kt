package com.example.fps_raytrace

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.fps_raytrace.composable.Joystick
import com.example.fps_raytrace.engine.Moves
import com.example.fps_raytrace.engine.RaytracerEngine
import com.example.fps_raytrace.ui.theme.FPS_raytraceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

private const val WIDTH = 640
private const val HEIGHT = WIDTH * 7 / 16
private const val FPS = 30

private const val noiseIntensity = 0.2f

class MainActivity : ComponentActivity() {

    private lateinit var raytracerEngine: RaytracerEngine
    private val pressedKeys = mutableSetOf<Moves>()

    private var isRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemNavigationBar()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        raytracerEngine =
            RaytracerEngine(context = applicationContext, width = WIDTH, height = HEIGHT)

        setContent {
            var started by remember { mutableStateOf(false) }

            LaunchedEffect(started, isRunning) {
                if (started) {
                    raytracerEngine.playNewMusic(R.raw.game_track, true)
                } else {
                    raytracerEngine.playNewMusic(R.raw.menu_track, true)
                }
            }

            FPS_raytraceTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    RayCaster(raytracer = raytracerEngine)


                    if (started) {
                        Joystick(modifier = Modifier.align(BottomEnd)) { x, y ->
                            pressedKeys.clear()
                            raytracerEngine.movePlayer(x / 20f, -y / 3f)
                        }

                        var enemies by remember { mutableIntStateOf(raytracerEngine.getAliveEnemiesCount()) }

                        LaunchedEffect(Unit, isRunning) {
                            while (isRunning) {
                                enemies = raytracerEngine.getAliveEnemiesCount()
                                delay(1000)
                            }
                        }

                        Text(
                            text = enemies.toString(),
                            fontSize = 40.sp,
                            fontFamily = FontFamily.Serif,
                            color = Color.White,
                            modifier = Modifier
                                .align(TopCenter)
                                .padding(16.dp)
                                .blendMode(BlendMode.Difference)
                        )
                    } else
                        StartScreen(modifier = Modifier.align(Center),
                            onStart = {
                                started = true
                            }
                        )
                }
            }
        }
    }

    @Composable
    fun RayCaster(raytracer: RaytracerEngine) {
        val scope = rememberCoroutineScope()

        // Create a single Bitmap instance that will be reused
        val bitmap = remember { Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888) }
        val imageBitmap = remember(bitmap) { mutableStateOf(bitmap.asImageBitmap()) }

        var fpsTimer = remember { System.currentTimeMillis() }

        // Create a RuntimeShader instance
        val runtimeShader = remember { RuntimeShader(analogShader) }
        // Noise intensity (you can make this a parameter if you want to control it dynamically)
        var shaderNoiseIntensity by remember { mutableFloatStateOf(noiseIntensity) }

        val resolution = LocalDensity.current.run {
            val width = LocalConfiguration.current.screenWidthDp.dp.toPx()
            val height = LocalConfiguration.current.screenHeightDp.dp.toPx()
            floatArrayOf(width, height)
        }


        // LaunchedEffect for the game loop
        LaunchedEffect(isRunning) {
            while (isRunning) {
                if (System.currentTimeMillis() > fpsTimer) {
                    fpsTimer = (System.currentTimeMillis() + (1000 / FPS).toLong())

                    raytracer.gameLoop(
                        pressedKeys = pressedKeys,
                        effects = { screen ->
                            screen
                        },
                        onFrame = { screen ->
                            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(screen.getByteArray()))
                            imageBitmap.value = bitmap.asImageBitmap()
                        }
                    )

                    delay(1) // delay to allow compose to draw the frame
                }
            }
        }


        // Create an Animatable for the time uniform
        val time = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            time.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 10000, easing = LinearEasing)
                )
            )
        }

        // Set the uniform values to the shader
        LaunchedEffect(time.value, shaderNoiseIntensity) {
            runtimeShader.setFloatUniform("time", time.value)
            runtimeShader.setFloatUniform("noiseIntensity", shaderNoiseIntensity)
            runtimeShader.setFloatUniform("displacement", 0f)
            runtimeShader.setFloatUniform("brightness", shaderNoiseIntensity + 1.5f)
            runtimeShader.setFloatUniform("resolution", resolution[0], resolution[1])
        }

        // Display the ImageBitmap
        Image(
            modifier = Modifier
                .graphicsLayer {
                    clip = true
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(
                            runtimeShader, "composable"
                        )
                        .asComposeRenderEffect()
                }
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            raytracerEngine.shootAndCheckHits()
                            scope.launch {
                                shaderNoiseIntensity = 1.0f
                                delay(200) // Duration of the noise effect
                                shaderNoiseIntensity = noiseIntensity
                            }
                        }
                    )
                },
            bitmap = imageBitmap.value,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.High,
        )
    }

    override fun onResume() {
        super.onResume()
        isRunning = true
        raytracerEngine.init()
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

