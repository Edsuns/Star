package io.github.edsuns.star.ui.composable

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.github.edsuns.chaoxing.model.Timing
import io.github.edsuns.star.R
import io.github.edsuns.star.Repository
import io.github.edsuns.star.ext.copy
import io.github.edsuns.star.ext.needImage
import io.github.edsuns.star.ext.ref
import io.github.edsuns.star.util.Result
import io.github.edsuns.star.util.produceUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * Created by Edsuns@qq.com on 2021/6/30.
 */

@RequiresApi(Build.VERSION_CODES.P)
@ExperimentalMaterialApi
@Composable
fun SignTimingBottomSheet(
    selectedTiming: MutableState<MutableState<Timing>>,
    sheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        content = content,
        sheetContent = {
            Column(
                Modifier
                    .padding(16.dp)
                    .height(360.dp)
                    .fillMaxWidth()
            ) {
                SignTimingSheetHeader(selectedTiming = selectedTiming)
                SignTimingSheetContent(
                    selectedTiming = selectedTiming,
                    sheetState = sheetState,
                    coroutineScope = coroutineScope
                )
            }
        }
    )
}

@Composable
fun SignTimingSheetHeader(selectedTiming: MutableState<MutableState<Timing>>) {
    Text(
        text = selectedTiming.ref.type.description,
        color = MaterialTheme.colors.primary,
        style = MaterialTheme.typography.h6
    )
    Text(
        text = selectedTiming.ref.course.name,
        style = MaterialTheme.typography.subtitle2
    )
}

@RequiresApi(Build.VERSION_CODES.P)
fun decodeQRImage(source: ImageDecoder.Source): String? {
    val bMap = ImageDecoder.decodeBitmap(
        source
    ) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = true
    }
    var decoded: String? = null
    val intArray = IntArray(bMap.width * bMap.height)
    bMap.getPixels(
        intArray, 0, bMap.width, 0, 0, bMap.width,
        bMap.height
    )
    val luminanceSource: LuminanceSource = RGBLuminanceSource(
        bMap.width,
        bMap.height, intArray
    )
    val bitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
    val reader: Reader = QRCodeReader()
    try {
        val result = reader.decode(bitmap)
        decoded = result.text
    } catch (e: NotFoundException) {
        e.printStackTrace()
    } catch (e: ChecksumException) {
        e.printStackTrace()
    } catch (e: FormatException) {
        e.printStackTrace()
    }
    return decoded
}

@RequiresApi(Build.VERSION_CODES.P)
@ExperimentalMaterialApi
@Composable
fun SignTimingSheetContent(
    selectedTiming: MutableState<MutableState<Timing>>,
    sheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope
) {
    val context = LocalContext.current
    val imageUri = remember {
        mutableStateOf<Uri?>(null)
    }
    val (clickUiState, sendSign, clearError) = produceUiState(
        Repository,
        selectedTiming.ref,
        false
    ) {
        var result: Result<Boolean>? = null
        when (selectedTiming.ref.type) {
            Timing.Type.NORMAL_OR_PHOTO -> {
                val uri = imageUri.value
                if (uri != null) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    result = onTimingClicked(
                        selectedTiming.ref,
                        Repository.TimingConfig(imageInput = inputStream)
                    )
                }
            }
            Timing.Type.QRCODE -> {
                val uri = imageUri.value
                if (uri != null) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val qrcode =
                        decodeQRImage(ImageDecoder.createSource(context.contentResolver, uri))
                    if (inputStream != null && qrcode != null) {
                        val enc = qrcode.substring(qrcode.lastIndexOf("enc=") + 4)
                        result = onTimingClicked(selectedTiming.ref, Repository.TimingConfig(enc))
                    }
                }
            }
            Timing.Type.GESTURE -> {
                result = onTimingClicked(selectedTiming.ref)
            }
            else -> {
                // do nothing
            }
        }
        // checking sheetState.isVisible is necessary
        if (sheetState.isVisible && result is Result.Success && result.data) {
            coroutineScope.launch {
                sheetState.hide()
                selectedTiming.ref =
                    selectedTiming.ref.copy(Timing.State.SUCCESS)
            }
        }
        return@produceUiState result ?: Result.Success(false)
    }
    val signed = selectedTiming.ref.state == Timing.State.SUCCESS
    val signButtonText =
        if (signed) stringResource(id = R.string.signed) else stringResource(id = R.string.sign)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!signed) {
            if (selectedTiming.ref.needImage) {
                RequestImageSection(imageUri)
            }
        }
        if (clickUiState.value.loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    sendSign()
                },
                enabled = !signed && (!selectedTiming.ref.needImage || imageUri.value != null)
            ) {
                Text(text = signButtonText)
            }
        }
    }
}

@Composable
fun RequestImageSection(imageUri: MutableState<Uri?>) {
    val bitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri.value = it }// only remember success ActivityResult
    }
    val pickImageHandler = {
        launcher.launch("image/*")
    }

    imageUri.value?.let {
        val context = LocalContext.current
        if (Build.VERSION.SDK_INT < 28) {
            bitmap.value = MediaStore.Images
                .Media.getBitmap(context.contentResolver, it)

        } else {
            val source = ImageDecoder.createSource(context.contentResolver, it)
            bitmap.value = ImageDecoder.decodeBitmap(source)
        }
    }

    Column {
        Button(onClick = pickImageHandler) {
            val image = bitmap.value
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                )
                bitmap.value?.let {
                }
            } else {
                Text(text = stringResource(id = R.string.pick_image))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
