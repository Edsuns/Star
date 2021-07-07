package io.github.edsuns.star.ui.composable

class PasswordState(errorFor: (String) -> String) :
    TextFieldState(validator = ::isPasswordValid, errorFor = errorFor)

private fun isPasswordValid(password: String): Boolean {
    return password.length > 3
}
