package com.example.fps_raytrace

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class AIController(context: Context) {

    private var interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null

    init {
        gpuDelegate = GpuDelegate()
        val options = Interpreter.Options().apply {
            addDelegate(gpuDelegate)  // Enable GPU acceleration
            setNumThreads(4)          // Adjust based on device capability
        }

        interpreter = Interpreter(loadModelFile(context), options)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("ai_movement.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun predict(inputData: FloatArray): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(inputData.size * 4)
            .order(ByteOrder.nativeOrder())

        val outputBuffer = ByteBuffer.allocateDirect(5 * 4)
            .order(ByteOrder.nativeOrder())

        for (value in inputData) {
            inputBuffer.putFloat(value)
        }

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val outputData = FloatArray(5)
        for (i in outputData.indices) {
            outputData[i] = outputBuffer.float
        }

        return outputData
    }

    fun close() {
        interpreter.close()
        gpuDelegate?.close()  // Properly release GPU resources
    }
}
