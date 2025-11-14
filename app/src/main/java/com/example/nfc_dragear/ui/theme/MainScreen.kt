package com.example.nfc.dragear.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// This file contains all the Jetpack Compose UI code

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    urlState: MutableState<String>,
    logState: List<String>,
    isWriteMode: MutableState<Boolean>
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spotify NFC Tool") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "Spotify Share Link (URL)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = urlState.value,
                    onValueChange = { urlState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://open.spotify.com/...") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (urlState.value.isBlank()) {
                                Toast.makeText(context, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                            } else {
                                isWriteMode.value = true
                                Toast.makeText(context, "Ready to write. Tap a tag.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)) // Spotify Green
                    ) {
                        Text("Prepare to Write")
                    }

                    Button(
                        onClick = {
                            isWriteMode.value = false
                            Toast.makeText(context, "Ready to read. Tap a tag.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text("Prepare to Read")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Status Log",
                    style = MaterialTheme. typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    border = BorderStroke(1.dp, Color.Gray),
                    shape = MaterialTheme.shapes.medium
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        state = listState,
                        reverseLayout = true // Shows latest logs at the top
                    ) {
                        items(logState) { logEntry ->
                            Text(
                                text = logEntry,
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (logEntry.startsWith("❌")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Scroll to top when log changes
                        scope.launch {
                            if(logState.isNotEmpty()) listState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }
    }
}
