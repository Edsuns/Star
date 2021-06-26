package io.github.edsuns.star

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by Edsuns@qq.com on 2021/6/23.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!Repository.initialized()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}