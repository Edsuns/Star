package io.github.edsuns.star.ext

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Created by Edsuns@qq.com on 2021/07/07.
 */

/**
 * Compatible with API < 28
 */
fun Bitmap.getPixels(contentResolver: ContentResolver, uri: Uri): IntArray {
    val array = IntArray(width * height)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // to avoid java.lang.IllegalStateException: unable to getPixel(), pixel access is not supported on Config#HARDWARE bitmaps
        val copy = ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri)
        ) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
        copy.getPixels(array, 0, width, 0, 0, width, height)
    } else {
        for (y in 0 until height) {
            for (x in 0 until width) {
                array[y * x] = getPixel(x, y)
            }
        }
    }
    return array
}

fun ContentResolver.getBitmap(uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        BitmapFactory.decodeStream(openInputStream(uri))
    } else {
        val source = ImageDecoder.createSource(this, uri)
        ImageDecoder.decodeBitmap(source)
    }
}

@Throws(NotFoundException::class)
fun decodeQRCode(contentResolver: ContentResolver, uri: Uri): String? {
    val bm = contentResolver.getBitmap(uri)
    var decoded: String? = null
    val intArray = bm.getPixels(contentResolver, uri)
    val source: LuminanceSource = RGBLuminanceSource(bm.width, bm.height, intArray)
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    val reader: Reader = QRCodeReader()
    try {
        val result = reader.decode(bitmap)
        decoded = result.text
    } catch (e: ChecksumException) {
        e.printStackTrace()
    } catch (e: FormatException) {
        e.printStackTrace()
    }
    return decoded
}
