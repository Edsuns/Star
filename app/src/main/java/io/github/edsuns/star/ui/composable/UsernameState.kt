package io.github.edsuns.star.ui.composable

class UsernameState(errorFor: (String) -> String) :
    TextFieldState(validator = ::isUsernameValid, errorFor = errorFor)

private fun isUsernameValid(username: String): Boolean {
    return !username.trim().contains(Regex("\\s+"))
}
