import android.content.Context
import com.example.fps_raytrace.R
import engine.textures.readPpmImage

class Walls(context: Context) {
    private val wallTexture1 = readPpmImage(context = context, path = R.raw.wall1)
    private val wallTexture2 = readPpmImage(context = context, path = R.raw.wall2)
    private val wallTexture3 = readPpmImage(context = context, path = R.raw.wall3)
    private val doorTexture = readPpmImage(context = context, path = R.raw.door)
    private val exitTexture = readPpmImage(context = context, path = R.raw.exit)

    val wallTextures = mapOf(
        1 to wallTexture1,
        2 to wallTexture2,
        3 to wallTexture3,
        8 to exitTexture,
        9 to doorTexture
    )
}