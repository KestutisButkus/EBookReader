package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EpubReaderApp()
                }
            }
        }
    }
}

@Composable
fun EpubReaderApp(viewModel: MainViewModel = viewModel()) {
    val activeBook by viewModel.activeBook.collectAsStateWithLifecycle()

    Crossfade(
        targetState = activeBook,
        label = "reader_screen_crossfade"
    ) { book ->
        if (book != null) {
            ReaderScreen(viewModel = viewModel, book = book)
        } else {
            LibraryScreen(viewModel = viewModel)
        }
    }
}
