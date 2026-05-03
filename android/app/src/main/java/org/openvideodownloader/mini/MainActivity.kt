package org.openvideodownloader.mini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: MainViewModel = viewModel()
                    val ui by vm.state.collectAsStateWithLifecycle()
                    MiniScreen(
                        ui = ui,
                        onDownload = { url -> vm.download(this@MainActivity, url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniScreen(
    ui: MiniUiState,
    onDownload: (String) -> Unit,
) {
    var url by remember { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Paste a video URL (YouTube and other NewPipe sites).",
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL") },
            singleLine = false,
            minLines = 2,
        )
        Button(
            onClick = { onDownload(url) },
            enabled = !ui.busy && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Download to Downloads")
        }
        if (ui.busy) {
            CircularProgressIndicator()
        }
        ui.status?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        ui.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                "Minimal build: progressive streams only. No playlists, subtitles, " +
                    "or DASH/HLS merge.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
