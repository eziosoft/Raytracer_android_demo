package com.example.fps_raytrace

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.fps_raytrace.composable.Joystick
import com.example.fps_raytrace.engine.Const.USE_AI
import com.example.fps_raytrace.engine.Moves
import com.example.fps_raytrace.engine.raycaster.RaytracerEngine
import com.example.fps_raytrace.ui.theme.FPS_raytraceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import kotlin.random.Random

private const val WIDTH = 640
private const val HEIGHT = WIDTH * 7 / 16
private const val FPS = 30

private const val noiseIntensity = 0.2f

class MainActivity : ComponentActivity() {

    private lateinit var raytracerEngine: RaytracerEngine
    private val pressedKeys = mutableSetOf<Moves>()

    private var isRunning = true

    private lateinit var aiController: AIController

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemNavigationBar()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        aiController = AIController(this)


        raytracerEngine =
            RaytracerEngine(context = this, screenWidth = WIDTH, screenHeight = HEIGHT)

        setContent {
            var started by remember { mutableStateOf(true) }

            val healthProgress = remember { Animatable(0f) }

            LaunchedEffect(started, isRunning) {
                if (started) {
                    raytracerEngine.playNewMusic(R.raw.game_track, true)
                } else {
                    raytracerEngine.playNewMusic(R.raw.menu_track, true)
                }
            }

            LaunchedEffect(Unit) {
                while (isRunning) {
                    healthProgress.animateTo(
                        targetValue = raytracerEngine.getPlayerHealth() / 100f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
                    )
                    delay(100)
                }
            }

            FPS_raytraceTheme {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            if (event.key == Key.DirectionUp && !pressedKeys.contains(Moves.UP)) pressedKeys.add(Moves.UP)
                            if (event.key == Key.DirectionDown && !pressedKeys.contains(Moves.DOWN)) pressedKeys.add(Moves.DOWN)
                            if (event.key == Key.DirectionLeft && !pressedKeys.contains(Moves.LEFT)) pressedKeys.add(Moves.LEFT)
                            if (event.key == Key.DirectionRight && !pressedKeys.contains(Moves.RIGHT)) pressedKeys.add(Moves.RIGHT)
                            if (event.key == Key.Spacebar && !pressedKeys.contains(Moves.SHOOT)) pressedKeys.add(Moves.SHOOT)
                        }

                        if (event.type == KeyEventType.KeyUp) {
                            if (event.key == Key.DirectionUp) pressedKeys.removeAll { it == Moves.UP }
                            if (event.key == Key.DirectionDown) pressedKeys.removeAll { it == Moves.DOWN }
                            if (event.key == Key.DirectionLeft) pressedKeys.removeAll { it == Moves.LEFT }
                            if (event.key == Key.DirectionRight) pressedKeys.removeAll { it == Moves.RIGHT }
                            if (event.key == Key.Spacebar) pressedKeys.removeAll { it == Moves.SHOOT }
                        }

                        true
                    }
                ) {
                    RayCaster(raytracer = raytracerEngine)

                    var enemiesCount by remember { mutableIntStateOf(raytracerEngine.getAliveEnemiesCount()) }

                    GlitchEffect(modifier = Modifier.fillMaxSize()) {
                        if (started) {
                            LinearProgressIndicator(
                                progress = { healthProgress.value },
                                color = Color.Red,
                                strokeCap = StrokeCap.Square,
                                gapSize = 1.dp,
                                drawStopIndicator = {},
                                modifier = Modifier
                                    .height(10.dp)
                                    .align(Alignment.BottomStart)
                            )

                            Joystick(modifier = Modifier.align(BottomStart)) { x, y ->
                                pressedKeys.clear()
                                raytracerEngine.movePlayer(0f, -y / 3f, x / 5)
                            }

                            LaunchedEffect(Unit, isRunning) {
                                while (isRunning) {
                                    enemiesCount = raytracerEngine.getAliveEnemiesCount()
                                    delay(1000)
                                }
                            }

                            // Display the enemies count
                            Text(
                                text = enemiesCount.toString(),
                                fontSize = 40.sp,
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

                    Button(
                        onClick = {
                            raytracerEngine.shareLogFile()
                        },
                        modifier = Modifier.align(TopEnd)
                    ) {
                        Text("Share logs")
                    }
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
        val runtimeShader = remember { RuntimeShader(emptyShader) }
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
//                            screen.depthMap()
                        },
                        onFrame = { screen ->
                            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(screen.getByteArray()))
                            imageBitmap.value = bitmap.asImageBitmap()
//                            pressedKeys.clear()
                        }
                    )


                    if (USE_AI) ai(raytracer)

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


        var displacement by remember { mutableFloatStateOf(0f) }
        var lastHealth by remember { mutableIntStateOf(raytracer.getPlayerHealth()) }


        LaunchedEffect(Unit) {
            while (isRunning) {
                if (lastHealth > raytracer.getPlayerHealth()) {
                    shaderNoiseIntensity = 1.0f
                    delay(20) // Duration of the noise effect
                    shaderNoiseIntensity = noiseIntensity
                }
                lastHealth = raytracer.getPlayerHealth()
                displacement = 50 * ((100 - raytracer.getPlayerHealth()) / 100f)
                delay(1)
            }
        }

        // Set the uniform values to the shader
        LaunchedEffect(time.value) {
            runtimeShader.setFloatUniform("time", time.value)
            runtimeShader.setFloatUniform("noiseIntensity", shaderNoiseIntensity)
            runtimeShader.setFloatUniform("displacement", displacement)
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
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume() // Consume the gesture
                            raytracerEngine.movePlayer(dragAmount.x / 200f, 0f, 0f)
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
//                            raytracerEngine.shootAndCheckHits()
                            pressedKeys.add(Moves.SHOOT)
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


    private fun ai(raytracer: RaytracerEngine) {

        val output = aiController.predict(raytracer.getDataForAi())

        Log.d("bbb", "ai: ${output.joinToString(",")}")

        val threshold = Random.nextFloat().coerceIn(0.05f, 0.5f)

        if (output[0] > threshold) pressedKeys.add(Moves.UP)
        else pressedKeys.removeAll { it == Moves.UP }

        if (output[1] > threshold) pressedKeys.add(Moves.DOWN)
        else pressedKeys.removeAll { it == Moves.DOWN }

        if (output[2] > threshold) pressedKeys.add(Moves.LEFT)
        else pressedKeys.removeAll { it == Moves.LEFT }

        if (output[3] > threshold) pressedKeys.add(Moves.RIGHT)
        else pressedKeys.removeAll { it == Moves.RIGHT }

        if (output[4] > threshold) pressedKeys.add(Moves.SHOOT)
        else pressedKeys.removeAll { it == Moves.SHOOT }
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

