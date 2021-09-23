package io.github.edsuns.star.ui.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.edsuns.star.R

@Composable
fun CheckboxIconButton(
    modifier: Modifier = Modifier,
    isTimeout: Boolean = false,
    isChecked: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            // TODO: think about animating these icons
            imageVector = when {
                isTimeout -> Icons.Default.ErrorOutline
                isChecked -> Icons.Default.Check
                else -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = when {
                isChecked -> stringResource(R.string.checked)
                else -> stringResource(R.string.unchecked)
            },
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Preview
@Composable
fun CheckboxIconButtonPreview() {
    CheckboxIconButton(isTimeout = false, isChecked = true) {}
}

@Preview
@Composable
fun CheckboxIconButtonPreviewUnchecked() {
    CheckboxIconButton(isTimeout = false, isChecked = false) {}
}

@Preview
@Composable
fun CheckboxIconButtonPreviewExpired() {
    CheckboxIconButton(isTimeout = true, isChecked = true) {}
}
