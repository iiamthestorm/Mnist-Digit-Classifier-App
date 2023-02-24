package com.tensorflow.lite.digitclassifier

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PostAugDigitClassifier(private val context: Context) {
  // Add a TF Lite interpreter as a field.
  private var interpreter: Interpreter? = null

  var isInitialized = false
    private set

  /** Executor to run inference task in the background. */
  private val executorService: ExecutorService = Executors.newCachedThreadPool()

  private var inputImageWidth: Int = 0 // will be inferred from TF Lite model.
  private var inputImageHeight: Int = 0 // will be inferred from TF Lite model.
  private var modelInputSize: Int = 0 // will be inferred from TF Lite model.

  fun initialize(): Task<Void?> {
    val task = TaskCompletionSource<Void?>()
    executorService.execute {
      try {
        initializeInterpreter()
        task.setResult(null)
      } catch (e: IOException) {
        task.setException(e)
      }
    }
    return task.task
  }

  @Throws(IOException::class)
  private fun initializeInterpreter() {
    // Load the TF Lite model from file and initialize an interpreter.
    val assetManager = context.assets
    val model = loadModelFile(assetManager, "after_data_aug_mnist.tflite")
    val interpreter = Interpreter(model)

    // Read the model input shape from model file.
    val inputShape = interpreter.getInputTensor(0).shape()
    inputImageWidth = inputShape[1]
    inputImageHeight = inputShape[2]
    modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE

    // Finish interpreter initialization
    this.interpreter = interpreter


    isInitialized = true
    Log.d(TAG, "Initialized TFLite interpreter.")
  }

  @Throws(IOException::class)
  private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
    val fileDescriptor = assetManager.openFd(filename)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
  }

  private fun classify(bitmap: Bitmap): Pair<String, String> {
    check(isInitialized) { "TF Lite Interpreter is not initialized yet." }

    // TODO: Add code to run inference with TF Lite.
    // preprocessing: resize the input image to match the modul input shape.
    val resizedImage = Bitmap.createScaledBitmap(
      bitmap,
      inputImageWidth,
      inputImageHeight,
      true
    )

    val byteBuffer = convertBitmapToByteBuffer(resizedImage)

    //define an array to store the model output.
    val output = Array(1) {
      FloatArray(OUTPUT_CLASSES_COUNT)
    }

    //run inference with the input data.
    interpreter?.run(byteBuffer, output)

    //post processing: find the digit that has the highest probability
    // and return it a human-readable string.
    val result = output[0]
    val maxIndex = result.indices.maxBy {
      result[it]
    } ?: -1

    return Pair(
      "Prediction Result: %d\nConfidence: %2f".format(maxIndex, result[maxIndex]),
      "The Result is, : %d".format(maxIndex)
    )
  }

  fun classifyAsync(bitmap: Bitmap): Task<Pair<String, String>> {
    val task = TaskCompletionSource<Pair<String, String>>()
    executorService.execute {
      val result = classify(bitmap)
      task.setResult(result)
    }
    return task.task
  }

  fun close() {
    executorService.execute {
      // TODO: close the TF Lite interpreter here
      interpreter?.close()

      Log.d(TAG, "Closed TFLite interpreter.")
    }
  }

  private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
    byteBuffer.order(ByteOrder.nativeOrder())

    val pixels = IntArray(inputImageWidth * inputImageHeight)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    for (pixelValue in pixels) {
      val r = (pixelValue shr 16 and 0xFF)
      val g = (pixelValue shr 8 and 0xFF)
      val b = (pixelValue and 0xFF)

      // Convert RGB to grayscale and normalize pixel value to [0..1].
      val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
      byteBuffer.putFloat(normalizedPixelValue)
    }

    return byteBuffer
  }

  companion object {
    private const val TAG = "PostAugDigitClassifier"

    private const val FLOAT_TYPE_SIZE = 4
    private const val PIXEL_SIZE = 1

    private const val OUTPUT_CLASSES_COUNT = 10
  }
}