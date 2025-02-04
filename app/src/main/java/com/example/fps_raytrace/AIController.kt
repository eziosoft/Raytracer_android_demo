package com.example.fps_raytrace

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class AIController(context: Context) {

    private var interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context))
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("ai_movement.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun predict(inputData: FloatArray): FloatArray {
        // Create input and output buffers
        val inputBuffer = ByteBuffer.allocateDirect(inputData.size * 4)
            .order(ByteOrder.nativeOrder())

        val outputBuffer = ByteBuffer.allocateDirect(5 * 4)
            .order(ByteOrder.nativeOrder())

        // Fill input buffer
        for (value in inputData) {
            inputBuffer.putFloat(value)
        }

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Convert output buffer to FloatArray
        outputBuffer.rewind()
        val outputData = FloatArray(5)
        for (i in outputData.indices) {
            outputData[i] = outputBuffer.float
        }

        return outputData
    }

    fun close() {
        interpreter.close()
    }
}
