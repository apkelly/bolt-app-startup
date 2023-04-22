package com.github.apkelly.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.apkelly.demo.ui.theme.DemoTheme
import java.time.LocalTime

class DemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("DemoActivity onCreate()")
        Timing.activityOnCreate = LocalTime.now()

        setContent {
            DemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "Content Provider: ${Timing.providerOnCreate}",
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W400
                        )
                        Text(
                            text = "Application     : ${Timing.applicationOnCreate}",
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W400
                        )
                        Text(
                            text = "Activity         : ${Timing.activityOnCreate}",
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W400
                        )
                    }

                }
            }
        }
    }

}