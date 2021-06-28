package io.github.edsuns.star.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.accompanist.insets.ProvideWindowInsets
import io.github.edsuns.star.Screen
import io.github.edsuns.star.ui.composable.Login
import io.github.edsuns.star.ui.composable.LoginEvent
import io.github.edsuns.star.ui.theme.ApplicationTheme

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApplicationTheme {
                ProvideWindowInsets {
//                    val systemUiController = rememberSystemUiController()
//                    SideEffect {
//                        systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = true)
//                    }
                    Login(
                        onNavigationEvent = { event ->
                            when (event) {
                                is LoginEvent.Login -> {
                                    viewModel.signIn(event.username, event.password)
                                }
                                LoginEvent.NavigateBack -> {
                                    onBackPressed()
                                }
                            }
                        }
                    )
                }
            }
        }

        viewModel.navigateTo.observe(this) {
            if (it.getContentIfNotHandled() == Screen.Timing) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
