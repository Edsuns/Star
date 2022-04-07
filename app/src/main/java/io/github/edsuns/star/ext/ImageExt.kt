package io.github.edsuns.star.ext

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions

/**
 * Created by Edsuns@qq.com on 2021/07/07.
 */

fun ContentResolver.getBitmap(uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        BitmapFactory.decodeStream(openInputStream(uri))
    } else {
        val source = ImageDecoder.createSource(this, uri)
        ImageDecoder.decodeBitmap(source)
    }
}

fun decodeQRCodeByScanKit(context: Context, uri: Uri): String? {

    /**
     * Scan Kit 无法识别通过 ImageDecoder 获取的 Bitmap 中的二维码，
     * 这里只好暂时使用传统的 BitmapFactory 了
     */
    val inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)

    val options = HmsScanAnalyzerOptions
        .Creator()
        .setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE)
        .setPhotoMode(true)
        .create()

    val hmsScans = ScanUtil.decodeWithBitmap(context, bitmap, options)

    return if (hmsScans.isNotEmpty()) hmsScans[0].originalValue else null
}