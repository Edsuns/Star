package io.github.edsuns.star.ui.composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LocationState {
    var address: String by mutableStateOf("")
    var latitude: String by mutableStateOf("")
    var longitude: String by mutableStateOf("")
}