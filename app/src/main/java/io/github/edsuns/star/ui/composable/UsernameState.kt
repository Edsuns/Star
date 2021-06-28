package io.github.edsuns.star.ui.composable

class UsernameState :
    TextFieldState(validator = ::isEmailValid, errorFor = ::emailValidationError)

/**
 * Returns an error to be displayed or null if no error was found
 */
private fun emailValidationError(email: String): String {
    return "Invalid email: $email"
}

private fun isEmailValid(username: String): Boolean {
    return !username.contains(Regex("\\s+"))
}
