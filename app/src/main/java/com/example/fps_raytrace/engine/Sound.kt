package engine

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log


class Sound(private val context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(10).build()
    private val soundMap = mutableMapOf<Int, Int>()
    private var mediaPlayer: MediaPlayer? = null

    fun loadSound(resourceId: Int) {
        val soundId = soundPool.load(context, resourceId, 1)
        soundMap[resourceId] = soundId

        soundPool.setOnLoadCompleteListener { soundPool, sampleId, status ->
            Log.d(
                "bbb",
                "onLoadComplete: $sampleId"
            )
        }
    }

    fun playSound(resourceId: Int, volume: Float = 1f) {
        val soundId = soundMap[resourceId] ?: return
        Log.d("bbb", "playSound: $soundId")
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }

    fun playMusic(resId: Int, isLooping:Boolean) {
        mediaPlayer = MediaPlayer.create(context, resId)
        mediaPlayer?.let {
            it.isLooping = isLooping
            it.start()
        }
    }

    fun stopMusic() {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
    }

    fun release() {
        soundPool.release()
    }

}