package com.nbradbury.memorymap.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.nbradbury.memorymap.domain.Photo
import com.nbradbury.memorymap.domain.PinGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * For repo screenshots, only compiled into `-PhidePeople` builds: rearranges a pin's stack so the featured (top, centered) photo has no people in
 * it. Candidates are the original featured photo, then the stack's other photos, then the rest of the group's
 * photos. A photo is rejected if ML Kit finds a face (close-ups) or MediaPipe's EfficientDet finds a COCO "person"
 * (people seen from behind or far away).
 */
object NoFaceFeature {
    private const val MAX_CHECKS = 40
    private const val DECODE_PX = 640

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(0.05f)
                .build(),
        )
    }
    private var personDetector: ObjectDetector? = null
    private val hasPersonCache = ConcurrentHashMap<String, Boolean>()

    private fun personDetector(context: Context): ObjectDetector = synchronized(this) {
        personDetector ?: ObjectDetector.createFromOptions(
            context.applicationContext,
            ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("efficientdet_lite0.tflite").build())
                .setRunningMode(RunningMode.IMAGE)
                .setScoreThreshold(0.3f)
                .setCategoryAllowlist(listOf("person"))
                .build(),
        ).also { personDetector = it }
    }

    suspend fun arrange(context: Context, g: PinGroup): List<Photo> {
        val candidates = (listOf(g.thumbs.last()) + g.thumbs + g.members.flatMap { it.photos }).distinct().take(MAX_CHECKS)
        val featured = candidates.firstOrNull { !hasPerson(context, it.uri) } ?: return g.thumbs
        // The stack draws its last photo on top, in the middle.
        return g.thumbs.filter { it != featured }.take(g.thumbs.size - 1) + featured
    }

    private suspend fun hasPerson(context: Context, uri: String): Boolean = hasPersonCache[uri] ?: withContext(Dispatchers.IO) {
        val bitmap = try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))) { decoder, info, _ ->
                decoder.setTargetSampleSize(maxOf(1, maxOf(info.size.width, info.size.height) / DECODE_PX))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: Exception) {
            return@withContext true // Can't check it, so don't feature it.
        }
        val argb = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val person = synchronized(this@NoFaceFeature) {
            personDetector(context).detect(BitmapImageBuilder(argb).build()).detections().isNotEmpty()
        }
        person || suspendCancellableCoroutine { c ->
            detector.process(InputImage.fromBitmap(argb, 0))
                .addOnSuccessListener { c.resume(it.isNotEmpty()) }
                .addOnFailureListener { c.resume(true) }
        }
    }.also { hasPersonCache[uri] = it }
}
