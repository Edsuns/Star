package io.github.edsuns.star.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.edsuns.star.Screen
import io.github.edsuns.star.util.Event

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
class LoginViewModel : ViewModel() {

    private val _navigateTo = MutableLiveData<Event<Screen>>()
    val navigateTo: LiveData<Event<Screen>>
        get() = _navigateTo

    var username: String = ""
    var password: String = ""

    fun signIn(username: String, password: String) {
        this.username = username
        this.password = password
    }

    fun onLoginSuccess() {
        _navigateTo.value = Event(Screen.Timing)
    }
}