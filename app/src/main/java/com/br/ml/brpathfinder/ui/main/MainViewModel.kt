package com.br.ml.brpathfinder.ui.main

import android.os.AsyncTask
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions

class MainViewModel : ViewModel() {

    private val imageAnalysisConfig = ImageAnalysisConfig.Builder()
        .setTargetResolution(Size(1280, 720))
        .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        .build()
    val imageAnalysis = ImageAnalysis(imageAnalysisConfig)

    // Live detection and tracking
    private val options = FirebaseVisionObjectDetectorOptions.Builder()
        .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .build()

    val objectDetector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

    private val analyzer = BRImageAnalyzer()
    inner class BRImageAnalyzer : ImageAnalysis.Analyzer {
        private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
        }

        override fun analyze(imageProxy: ImageProxy?, degrees: Int) {
            val mediaImage = imageProxy?.image
            val imageRotation = degreesToFirebaseRotation(degrees)
            if (mediaImage != null) {
                val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                // Pass image to an ML Kit Vision API
                objectDetector.processImage(image)
                    .addOnSuccessListener { detectedObjects ->
                        Log.d("CCS", "objects: ${detectedObjects.size}")
                        // haptic
//                        detectedObjects[0].boundingBox.
                        // TODO - MOAR!!!!!
                    }
                    .addOnFailureListener { e ->
                        Log.e("CCS", "FAIL!!!")
                    }
            }
        }
    }



    init {
        imageAnalysis.setAnalyzer(AsyncTask.THREAD_POOL_EXECUTOR, analyzer)
    }



}
