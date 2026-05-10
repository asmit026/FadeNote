package com.example.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = androidx.room.Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "thought_db"
        )
        .addMigrations(MIGRATION_1_2)
        .build()

        setContent {
            com.example.secondbrain.ui.theme.SecondBrainTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ThoughtEntryScreen(db)
                }
            }
        }
    }
}

fun calculateExpiry(option: String): Long {
    val now = System.currentTimeMillis()
    return when(option) {
        "24h" -> now + 86400000
        "7d" -> now + 7 * 86400000
        "forever" -> Long.MAX_VALUE
        else -> now
    }
}

@Composable
fun ThoughtEntryScreen(db: AppDatabase) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            db.thoughtDao().deleteExpired(System.currentTimeMillis())
        }
    }

    var text by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("24h") }

    val thoughts by db.thoughtDao()
    .getActiveThoughts(System.currentTimeMillis())
    .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FadeNote",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            
            IconButton(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    db.thoughtDao().deleteAll()
                }
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Field
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Quick note...") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("24h", "7d", "forever").forEach { option ->
                FilterChip(
                    selected = selectedOption == option,
                    onClick = { selectedOption = option },
                    label = { Text(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val capturedText = text
                if (capturedText.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        db.thoughtDao().insert(ThoughtEntity(
                            content = capturedText,
                            summary = "",
                            expiresAt = calculateExpiry(selectedOption)
                        ))
                    }
                    text = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Note")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("My Notes", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

        Spacer(modifier = Modifier.height(12.dp))

        if (thoughts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No notes yet.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            ThoughtListView(thoughts = thoughts, db = db, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ThoughtListView(thoughts: List<ThoughtEntity>, db: AppDatabase, modifier: Modifier = Modifier) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(thoughts) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { CoroutineScope(Dispatchers.IO).launch { db.thoughtDao().delete(item) } }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dateFormat.format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        val expiryText = if (item.expiresAt == Long.MAX_VALUE) "Forever" else {
                            val remaining = item.expiresAt - System.currentTimeMillis()
                            if (remaining <= 0) "Expired" else {
                                val hours = remaining / 3600000
                                if (hours >= 24) "${hours/24}d left" else "${hours}h left"
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = expiryText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}