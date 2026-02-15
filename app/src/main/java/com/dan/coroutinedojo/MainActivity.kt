package com.dan.coroutinedojo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dan.coroutinedojo.ui.theme.CoroutineDojoTheme

private data class Level(
    val title: String,
    val subtitle: String,
    val destination: Class<out Activity>,
)

private val levels = listOf(
    Level("Level 1", "Dispatchers & withContext", Level1Activity::class.java),
    Level("Level 2", "Lifecycle & Scopes", Level2Activity::class.java),
    Level("Level 3", "StateFlow & SharedFlow", Level3Activity::class.java),
    Level("Level 4", "Exception Handling", Level4Activity::class.java),
    Level("Level 5", "Parallel Decomposition", Level5Activity::class.java),
    Level("Level 6", "ViewModel & Lifecycle", Level6Activity::class.java),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            CoroutineDojoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text("Coroutine Dojo") })
                    }
                ) { innerPadding ->
                    LauncherScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun LauncherScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            itemsIndexed(levels) { _, level ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(context, level.destination))
                        },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = level.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = level.subtitle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
