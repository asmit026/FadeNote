package com.example.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secondbrain.ui.theme.*
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
    return when (option) {
        "24h"     -> now + 86400000L
        "7d"      -> now + 7 * 86400000L
        "forever" -> Long.MAX_VALUE
        else      -> now
    }
}

@Composable
fun ThoughtEntryScreen(db: AppDatabase) {
    val isDark = isSystemInDarkTheme()
    val scope  = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            db.thoughtDao().deleteExpired(System.currentTimeMillis())
        }
    }

    var text           by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("24h") }
    val maxChars = 200

    val thoughts by db.thoughtDao()
        .getActiveThoughts(System.currentTimeMillis())
        .collectAsState(initial = emptyList())

    val bg       = MaterialTheme.colorScheme.background
    val surface  = MaterialTheme.colorScheme.surface
    val accent   = MaterialTheme.colorScheme.primary
    val textMain = MaterialTheme.colorScheme.onBackground
    val textDim  = MaterialTheme.colorScheme.outline
    val border   = if (isDark) NavyBorder else SkyBorder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 18.dp)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "FadeNote",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) NavyText else SkyAccent,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Notes that fade, so you don't forget.",
                    fontSize = 11.sp,
                    color = if (isDark) NavyTextFaint else SkyTextFaint
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings",
                        tint = if (isDark) NavyTextFaint else SkyTextFaint)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) UrgentRedDim else UrgentRedLightDim),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        CoroutineScope(Dispatchers.IO).launch { db.thoughtDao().deleteAll() }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all",
                            tint = if (isDark) UrgentRed else UrgentRedLight,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(surface)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= maxChars) text = it },
                placeholder = { Text("Write a quick note...", color = textDim, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = accent,
                    unfocusedBorderColor = border,
                    focusedTextColor     = textMain,
                    unfocusedTextColor   = textMain,
                    cursorColor          = accent
                )
            )
            Text(
                "${text.length}/$maxChars",
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                fontSize = 10.sp,
                color = textDim
            )
        }

        Spacer(Modifier.height(14.dp))

        Text("Set time to fade", fontSize = 11.sp, color = textDim)
        Spacer(Modifier.height(6.dp))

        // Time selector
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            data class Opt(val key: String, val label: String, val sub: String)
            listOf(Opt("24h","24h","1 day"), Opt("7d","7d","7 days"), Opt("forever","Forever","keep")).forEach { opt ->
                val selected = selectedOption == opt.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected)
                                if (isDark) NavyAccentDim else SkyAccent
                            else surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { selectedOption = opt.key },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                opt.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    selected && isDark  -> NavyText
                                    selected && !isDark -> Color.White
                                    else                -> textDim
                                }
                            )
                            Text(
                                opt.sub,
                                fontSize = 9.sp,
                                color = when {
                                    selected && isDark  -> NavyTextDim
                                    selected && !isDark -> Color.White.copy(alpha = 0.6f)
                                    else                -> textDim.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Save button
        Button(
            onClick = {
                val t = text
                if (t.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        db.thoughtDao().insert(ThoughtEntity(
                            content   = t,
                            summary   = "",
                            expiresAt = calculateExpiry(selectedOption)
                        ))
                    }
                    text = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) NavyAccentDim else SkyAccent
            )
        ) {
            Text(
                "Save Note",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) NavyText else Color.White
            )
        }

        Spacer(Modifier.height(22.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("My Notes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textDim)
            if (thoughts.isNotEmpty())
                Text("${thoughts.size} notes", fontSize = 11.sp, color = textDim.copy(alpha = 0.5f))
        }

        Spacer(Modifier.height(10.dp))

        if (thoughts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No notes yet", color = textDim, fontSize = 14.sp)
                    Text("Write something above", color = textDim.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        } else {
            ThoughtListView(thoughts = thoughts, db = db, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ThoughtListView(thoughts: List<ThoughtEntity>, db: AppDatabase, modifier: Modifier = Modifier) {
    val isDark     = isSystemInDarkTheme()
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val surface    = MaterialTheme.colorScheme.surface
    val textMain   = MaterialTheme.colorScheme.onSurface
    val textDim    = MaterialTheme.colorScheme.outline

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(thoughts) { item ->
            val now       = System.currentTimeMillis()
            val isForever = item.expiresAt == Long.MAX_VALUE
            val remaining = if (isForever) Long.MAX_VALUE else item.expiresAt - now
            val isUrgent  = !isForever && remaining < 3_600_000L * 3
            val progress  = if (isForever) 1f else {
                val total = (item.expiresAt - item.createdAt).toFloat()
                (remaining.toFloat() / total).coerceIn(0f, 1f)
            }
            val expiryText = when {
                isForever      -> "Forever"
                remaining <= 0 -> "Expired"
                remaining < 3_600_000L -> "${remaining / 60000}m left"
                remaining < 86_400_000L -> "${remaining / 3_600_000}h left"
                else -> "${remaining / 86_400_000}d left"
            }

            val cardAlpha = if (isUrgent) 0.5f else 1f

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.content,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMain.copy(alpha = cardAlpha)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                dateFormat.format(Date(item.createdAt)),
                                fontSize = 10.sp,
                                color = textDim.copy(alpha = cardAlpha)
                            )
                        }
                        IconButton(
                            onClick = { CoroutineScope(Dispatchers.IO).launch { db.thoughtDao().delete(item) } },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                tint = textDim.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp))
                        }
                    }

                    if (!isForever) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                            color    = if (isUrgent)
                                if (isDark) UrgentRed else UrgentRedLight
                            else
                                if (isDark) NavyAccent else SkyAccent,
                            trackColor = if (isDark) NavyBorder else SkyBorder
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Fades ${dateFormat.format(Date(item.expiresAt))}",
                                fontSize = 10.sp,
                                color = textDim.copy(alpha = 0.5f)
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isUrgent)
                                    if (isDark) UrgentRedDim else UrgentRedLightDim
                                else
                                    if (isDark) NavyPill else SkyPill
                            ) {
                                Text(
                                    expiryText,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isUrgent)
                                        if (isDark) UrgentRed else UrgentRedLight
                                    else
                                        if (isDark) NavyText else SkyAccent
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Surface(shape = RoundedCornerShape(20.dp), color = if (isDark) NavyPill else SkyPill) {
                                Text(
                                    "Forever",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) NavyText else SkyAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
