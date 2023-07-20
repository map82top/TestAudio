package com.example.testaudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.testaudio.ui.theme.AudioTranslatorScreen
import com.example.testaudio.ui.theme.TestAudioTheme
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TestAudioTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AudioTranslatorScreen()
                }
            }
        }
    }
}