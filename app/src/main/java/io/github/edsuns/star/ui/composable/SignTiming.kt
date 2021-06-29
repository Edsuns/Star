package io.github.edsuns.star.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.edsuns.chaoxing.model.Timing
import io.github.edsuns.star.R
import io.github.edsuns.star.Repository
import io.github.edsuns.star.ext.copy
import io.github.edsuns.star.ext.ref
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
    val (clickUiState, refreshPost, clearError) = produceUiState(
        Repository,
        selectedTiming.ref
    ) {
        onTimingClicked(selectedTiming.ref)
    }
    val signed = selectedTiming.ref.state == Timing.State.SUCCESS
    val signButtonText =
        if (signed) stringResource(id = R.string.signed) else stringResource(id = R.string.sign)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                val uiState = clickUiState.value
                if (uiState.data == true) {
                    coroutineScope.launch { sheetState.hide() }
                    selectedTiming.ref =
                        selectedTiming.ref.copy(Timing.State.SUCCESS)
                }
            },
            enabled = !signed
        ) {
            Text(text = signButtonText)
        }
    }
}
