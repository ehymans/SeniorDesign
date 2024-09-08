package com.sur_tec.helmetiq

import HelmetIQNavigation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.sur_tec.helmetiq.ui.theme.HelmetIQTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            HelmetIQApp {
                HelmetIQNavigation()
            }
        }
    }
}


@Composable
fun HelmetIQApp(content: @Composable () -> Unit) {
    HelmetIQTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {

            content()
        }
    }
}

/*  @Composable
  fun Greeting(name: String, modifier: Modifier = Modifier) {
      Text(
          text = "Hello $name!",
          modifier = modifier
      )
  }

  @Preview(showBackground = true)
  @Composable
  fun GreetingPreview() {
      HelmetIQTheme {
          Greeting("Android")
      }
  }
}

 */