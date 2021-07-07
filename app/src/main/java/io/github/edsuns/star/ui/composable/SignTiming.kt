package io.github.edsuns.star.ui.composable

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.zxing.*
import io.github.edsuns.chaoxing.model.Timing
import io.github.edsuns.star.R
import io.github.edsuns.star.Repository
import io.github.edsuns.star.ext.*
import io.github.edsuns.star.local.SettingsStorage
import io.github.edsuns.star.util.Result
import io.github.edsuns.star.util.produceUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * Created by Edsuns@qq.com on 2021/6/30.
 */

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

@ExperimentalMaterialApi
@Composable
fun SignTimingSheetContent(
    selectedTiming: MutableState<MutableState<Timing>>,
    sheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope
) {
    val context = LocalContext.current
    val locationState = remember {
        val state = LocationState()
        state.address = SettingsStorage.address ?: "-1"
        state.longitude = SettingsStorage.longitude ?: "-1"
        state.latitude = SettingsStorage.latitude ?: "-1"
        state
    }
    val imageUri = remember { mutableStateOf<Uri?>(null) }
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
                    val qrcode = try {
                        decodeQRCode(context.contentResolver, uri)
                    } catch (err: NotFoundException) {
                        Toast.makeText(context, R.string.qrcode_not_found, Toast.LENGTH_SHORT)
                            .show()
                        null
                    }
                    if (inputStream != null && qrcode != null) {
                        val enc = qrcode.substring(qrcode.lastIndexOf("enc=") + 4)
                        result = onTimingClicked(selectedTiming.ref, Repository.TimingConfig(enc))
                    }
                }
            }
            Timing.Type.LOCATION -> {
                val latitude = locationState.latitude.toFloatOrNull() ?: -1.0f
                val longitude = locationState.longitude.toFloatOrNull() ?: -1.0f
                val config = Repository.TimingConfig(
                    address = locationState.address,
                    latitude = latitude,
                    longitude = longitude
                )
                SettingsStorage.address = locationState.address
                SettingsStorage.latitude = locationState.latitude
                SettingsStorage.longitude = locationState.longitude
                result = onTimingClicked(selectedTiming.ref, config)
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
        var noBlank = true
        if (!signed) {
            if (selectedTiming.ref.needImage) {
                noBlank = imageUri.value != null
                ImageRequestBox(imageUri)
                Spacer(modifier = Modifier.height(20.dp))
            }
            if (selectedTiming.ref.type == Timing.Type.LOCATION) {
                noBlank =
                    (locationState.address.isNotBlank()
                            && locationState.longitude.isNotBlank()
                            && locationState.latitude.isNotBlank())
                LocationConfigBox(locationState)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        if (clickUiState.value.loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    sendSign()
                },
                enabled = !signed && noBlank
            ) {
                Text(text = signButtonText)
            }
        }
    }
}

@Composable
fun ImageRequestBox(imageUri: MutableState<Uri?>) {
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
        bitmap.value = context.contentResolver.getBitmap(it)
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
            } else {
                Text(text = stringResource(id = R.string.pick_image))
            }
        }
    }
}

@Composable
fun LocationConfigBox(locationState: LocationState) {
    val focusRequesters = List(3) { FocusRequester() }
    Column {
        DefaultTextField(
            value = locationState.address,
            placeholder = { Text(stringResource(R.string.address)) },
            onValueChange = { locationState.address = it },
            focusRequester = focusRequesters[0],
            nextFocusRequester = focusRequesters[1],
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.address),
                    modifier = Modifier.padding(4.dp)
                )
            }
        )
        DefaultTextField(
            value = locationState.longitude,
            placeholder = { Text(stringResource(R.string.longitude)) },
            onValueChange = { locationState.longitude = it },
            imeAction = ImeAction.Go,
            focusRequester = focusRequesters[1],
            nextFocusRequester = focusRequesters[2],
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.EditLocation,
                    contentDescription = stringResource(R.string.longitude),
                    modifier = Modifier.padding(4.dp)
                )
            }
        )
        DefaultTextField(
            value = locationState.latitude,
            placeholder = { Text(stringResource(R.string.latitude)) },
            onValueChange = { locationState.latitude = it },
            focusRequester = focusRequesters[2],
            nextFocusRequester = focusRequesters[2],
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.EditLocation,
                    contentDescription = stringResource(R.string.latitude),
                    modifier = Modifier.padding(4.dp)
                )
            }
        )
    }
}

@Composable
fun DefaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .focusOrder(focusRequester) {
                nextFocusRequester.requestFocus()
            }
            .padding(bottom = 8.dp)
            .fillMaxWidth(),
        isError = value.isBlank(),
        maxLines = 1,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        )
    )
}
