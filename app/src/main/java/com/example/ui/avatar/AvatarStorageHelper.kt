package com.example.ui.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Avatar fotoğraflarını sistem fotoğraf seçicisinden alıp
 * 512x512 piksele ölçekleyerek uygulama içi özel alanda saklayan yardımcı sınıf.
 */
object AvatarStorageHelper {

  private const val AVATAR_DIR = "user_avatars"
  private const val TARGET_SIZE = 512

  /**
   * Seçilen fotoğrafı 512x512 kare olarak kırpıp kaydeder.
   * Geriye kaydedilen dosyanın yolunu ("file:///...") döndürür.
   */
  suspend fun saveAvatarFromUri(context: Context, sourceUri: Uri, profileId: Int): String? =
    withContext(Dispatchers.IO) {
      try {
        val resolver = context.contentResolver

        // 1. Orijinal boyutları öğren
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(sourceUri)?.use { inputStream ->
          BitmapFactory.decodeStream(inputStream, null, options)
        }

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        if (originalWidth <= 0 || originalHeight <= 0) return@withContext null

        // 2. Sample size hesapla (Bellek tasarrufu için)
        var inSampleSize = 1
        val minDim = min(originalWidth, originalHeight)
        if (minDim > TARGET_SIZE) {
          inSampleSize = max(1, minDim / TARGET_SIZE)
        }

        // 3. Gerçek bitmap'i oku
        val decodeOptions = BitmapFactory.Options().apply {
          this.inSampleSize = inSampleSize
          this.inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var decodedBitmap: Bitmap? = resolver.openInputStream(sourceUri)?.use { inputStream ->
          BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } ?: return@withContext null

        // 4. EXIF rotasyonunu düzelt
        try {
          resolver.openInputStream(sourceUri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
              ExifInterface.TAG_ORIENTATION,
              ExifInterface.ORIENTATION_NORMAL
            )
            val rotationAngle = when (orientation) {
              ExifInterface.ORIENTATION_ROTATE_90 -> 90f
              ExifInterface.ORIENTATION_ROTATE_180 -> 180f
              ExifInterface.ORIENTATION_ROTATE_270 -> 270f
              else -> 0f
            }
            if (rotationAngle != 0f) {
              val matrix = Matrix().apply { postRotate(rotationAngle) }
              val rotated = Bitmap.createBitmap(
                decodedBitmap!!, 0, 0,
                decodedBitmap!!.width, decodedBitmap!!.height,
                matrix, true
              )
              if (rotated != decodedBitmap) {
                decodedBitmap!!.recycle()
                decodedBitmap = rotated
              }
            }
          }
        } catch (_: Exception) {}

        val currentBmp = decodedBitmap ?: return@withContext null

        // 5. Ortadan kare kırpma (Center crop)
        val cropSize = min(currentBmp.width, currentBmp.height)
        val cropX = (currentBmp.width - cropSize) / 2
        val cropY = (currentBmp.height - cropSize) / 2

        val squareBitmap = Bitmap.createBitmap(currentBmp, cropX, cropY, cropSize, cropSize)
        if (squareBitmap != currentBmp) {
          currentBmp.recycle()
        }

        // 6. 512x512 boyutuna ölçekle
        val finalBitmap = if (squareBitmap.width != TARGET_SIZE || squareBitmap.height != TARGET_SIZE) {
          val scaled = Bitmap.createScaledBitmap(squareBitmap, TARGET_SIZE, TARGET_SIZE, true)
          if (scaled != squareBitmap) {
            squareBitmap.recycle()
          }
          scaled
        } else {
          squareBitmap
        }

        // 7. Uygulama içi depolama dizinine yaz
        val dir = File(context.filesDir, AVATAR_DIR).apply { mkdirs() }

        // Eski dosyaları temizle
        dir.listFiles { f -> f.name.startsWith("avatar_p${profileId}_") }?.forEach {
          it.delete()
        }

        val destFile = File(dir, "avatar_p${profileId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(destFile).use { out ->
          finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        finalBitmap.recycle()

        "file://${destFile.absolutePath}"
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }
    }

  /**
   * Profilin kayıtlı özel avatar fotoğrafını siler.
   */
  fun deleteAvatarForProfile(context: Context, profileId: Int) {
    try {
      val dir = File(context.filesDir, AVATAR_DIR)
      if (dir.exists()) {
        dir.listFiles { f -> f.name.startsWith("avatar_p${profileId}_") }?.forEach {
          it.delete()
        }
      }
    } catch (_: Exception) {}
  }
}
