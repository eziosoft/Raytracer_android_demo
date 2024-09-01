package engine

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log


class Sound(private val context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(10).build()
    private val soundMap = mutableMapOf<Int, Int>()

    fun loadSound(resourceId: Int) {
        val soundId = soundPool.load(context, resourceId, 1)
        soundMap[resourceId] = soundId
    }

    fun playSound(resourceId: Int, volume: Float = 1f) {
        val soundId = soundMap[resourceId] ?: return
        Log.d("bbb", "playSound: $soundId")
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }

    fun playMp3(resId: Int, isLooping:Boolean): MediaPlayer {
        val mediaPlayer = MediaPlayer.create(context, resId)
        mediaPlayer.isLooping = isLooping
        mediaPlayer.start()
        return mediaPlayer
    }

}