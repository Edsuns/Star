package io.github.edsuns.star.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.edsuns.star.R
import io.github.edsuns.star.Repository
import io.github.edsuns.star.local.SettingsStorage
import io.github.edsuns.star.ui.LoginViewModel
import io.github.edsuns.star.ui.theme.ApplicationTheme
import io.github.edsuns.star.ui.theme.snackbarAction
import io.github.edsuns.star.util.produceUiState
import io.github.edsuns.star.util.supportWideScreen

sealed class LoginEvent {
    data class Login(val username: String, val password: String) : LoginEvent()
    object NavigateBack : LoginEvent()
}

@Composable
fun Login(onNavigationEvent: (LoginEvent) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            SignInSignUpTopAppBar(
                topAppBarText = stringResource(id = R.string.app_name),
                onBackPressed = { onNavigationEvent(LoginEvent.NavigateBack) }
            )
        },
        content = {
            SignInSignUpScreen(
                modifier = Modifier.supportWideScreen(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LoginContent(
                        onLoginSubmitted = { email, password ->
                            onNavigationEvent(LoginEvent.Login(email, password))
                        }
                    )
                }
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ErrorSnackbar(
            snackbarHostState = snackbarHostState,
            onDismiss = { snackbarHostState.currentSnackbarData?.dismiss() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun LoginContent(
    onLoginSubmitted: (email: String, password: String) -> Unit
) {
    val viewModel = viewModel(LoginViewModel::class.java)

    val (loginUiState, refreshPost, clearError) = produceUiState(Repository) {
        login(viewModel.username, viewModel.password)
    }

    if (loginUiState.value.data == true) {
        viewModel.onLoginSuccess()
        return
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val focusRequester = remember { FocusRequester() }
        val usernameState = remember { UsernameState() }
        if (!usernameState.isFocusedDirty) {
            usernameState.text = SettingsStorage.username ?: ""
        }
        val invalidPasswordStr = stringResource(R.string.invalid_password)
        val passwordState = remember { PasswordState { invalidPasswordStr } }
        if (passwordState.hasSubmit
            && !loginUiState.value.loading
            && !loginUiState.value.hasError
            && loginUiState.value.data == false
        ) {
            passwordState.text = ""
            passwordState.hasSubmit = false
        }

        Username(usernameState, onImeAction = { focusRequester.requestFocus() })

        Spacer(modifier = Modifier.height(16.dp))

        Password(
            label = stringResource(id = R.string.password),
            passwordState = passwordState,
            modifier = Modifier.focusRequester(focusRequester),
            onImeAction = { onLoginSubmitted(usernameState.text, passwordState.text) }
        )
        if (loginUiState.value.hasError) {
            TextFieldError(textError = stringResource(id = R.string.network_error))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                onLoginSubmitted(usernameState.text, passwordState.text)
                refreshPost()
                passwordState.hasSubmit = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = usernameState.isValid && passwordState.isValid && !loginUiState.value.loading
        ) {
            Text(
                text = stringResource(id = R.string.login),
                modifier = Modifier.padding(6.dp)
            )
        }
        if (loginUiState.value.loading) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun ErrorSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { }
) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                content = {
                    Text(
                        text = data.message,
                        style = MaterialTheme.typography.body2
                    )
                },
                action = {
                    data.actionLabel?.let {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(id = R.string.dismiss),
                                color = MaterialTheme.colors.snackbarAction
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
    )
}

@Preview(name = "Login light theme")
@Composable
fun LoginPreview() {
    ApplicationTheme {
        Login {}
    }
}

@Preview(name = "Login dark theme")
@Composable
fun LoginPreviewDark() {
    ApplicationTheme(darkTheme = true) {
        Login {}
    }
}
