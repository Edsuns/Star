package io.github.edsuns.star.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import com.google.accompanist.insets.ProvideWindowInsets
import io.github.edsuns.star.R
import io.github.edsuns.star.Repository
import io.github.edsuns.star.Screen
import io.github.edsuns.star.ext.ioScope
import io.github.edsuns.star.ui.composable.TimingScreen
import io.github.edsuns.star.ui.theme.ApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Edsuns@qq.com on 2021/6/23.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ioScope.launch {
            if (Repository.init()) {
                if (!Repository.validateLogin()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.login_out_of_date,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    navigateToLogin()
                }
            } else {
                navigateToLogin()
            }
        }
        setContent {
            ApplicationTheme {
                ProvideWindowInsets {
                    TimingScreen(onLogoutClicked = {
                        viewModel.onLogout()
                    })
                }
            }
        }
        viewModel.navigateTo.observe(this) {
            if (it.getContentIfNotHandled() == Screen.SignIn) {
                navigateToLogin()
            }
        }
        viewModel.command.observe(this) {
            if (it is MainViewModel.Command.OpenLink) {
                val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it.url))
                startActivity(openUrlIntent)
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        finish()
    }
}