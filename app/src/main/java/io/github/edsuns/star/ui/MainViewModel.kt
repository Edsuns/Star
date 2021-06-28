package io.github.edsuns.star.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.edsuns.star.Repository
import io.github.edsuns.star.Screen
import io.github.edsuns.star.ext.ioScope
import io.github.edsuns.star.util.Event
import kotlinx.coroutines.launch

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
class MainViewModel : ViewModel() {
    sealed class Command {
        data class OpenLink(val url: String) : Command()
    }

    private val _navigateTo = MutableLiveData<Event<Screen>>()
    val navigateTo: LiveData<Event<Screen>>
        get() = _navigateTo

    private val _command: MutableLiveData<Command> = MutableLiveData()
    val command: LiveData<Command>
        get() = _command

    fun onInfoClicked() {
        _command.value = Command.OpenLink(INFO_URL)
    }

    fun onLogout() {
        ioScope.launch { Repository.logout() }
        _navigateTo.value = Event(Screen.SignIn)
    }

    companion object {
        private const val INFO_URL = "https://github.com/Edsuns/Star"
    }
}