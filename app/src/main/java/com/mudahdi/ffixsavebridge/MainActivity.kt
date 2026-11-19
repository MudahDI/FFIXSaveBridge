package com.mudahdi.ffixsavebridge

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

private data class SaveEntryUi(
    val block: Int,
    val filename: String,
    val region: String,
    val leader: String,
    val level: Int,
    val location: String,
    val gil: Long,
    val playtime: Double,
    val crcOk: Boolean,
    val directoryOk: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = androidx.compose.foundation.isSystemInDarkTheme()
            val colors = if (android.os.Build.VERSION.SDK_INT >= 31) {
                if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            } else if (dark) {
                androidx.compose.material3.darkColorScheme()
            } else {
                androidx.compose.material3.lightColorScheme()
            }
            MaterialTheme(colorScheme = colors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SaveBridgeScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveBridgeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourcePath by remember { mutableStateOf<String?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<SaveEntryUi>>(emptyList()) }
    var selectedBlock by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Pilih memory card DuckStation untuk mulai.") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                error = null
                status = "Membaca dan memeriksa save…"
                try {
                    val result = withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw FileNotFoundException("File tidak dapat dibuka.")
                        val file = File(context.cacheDir, "source_memory_card.bin")
                        file.writeBytes(bytes)
                        if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
                        val json = Python.getInstance().getModule("android_api")
                            .callAttr("inspect_file", file.absolutePath).toString()
                        file.absolutePath to json
                    }
                    val root = JSONObject(result.second)
                    val array = root.getJSONArray("entries")
                    val parsed = buildList {
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            add(
                                SaveEntryUi(
                                    block = item.getInt("block"),
                                    filename = item.optString("filename"),
                                    region = item.optString("region"),
                                    leader = item.optString("leader", "?"),
                                    level = item.optInt("level"),
                                    location = item.optString("location", "Unknown"),
                                    gil = item.optLong("gil"),
                                    playtime = item.optDouble("playtime_seconds"),
                                    crcOk = item.optBoolean("crc_ok"),
                                    directoryOk = item.optBoolean("directory_ok", true)
                                )
                            )
                        }
                    }
                    sourcePath = result.first
                    sourceName = queryDisplayName(context, uri)
                    entries = parsed
                    selectedBlock = parsed.firstOrNull()?.block
                    status = if (parsed.isEmpty()) {
                        "File terbaca, tetapi tidak ditemukan save FFIX yang dikenali."
                    } else {
                        "${parsed.size} save FFIX ditemukan pada ${root.optString("container")}."
                    }
                } catch (e: Exception) {
                    sourcePath = null
                    sourceName = null
                    entries = emptyList()
                    selectedBlock = null
                    error = e.message ?: "Gagal membaca file."
                    status = "Pemeriksaan gagal."
                } finally {
                    busy = false
                }
            }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        val input = sourcePath
        val block = selectedBlock
        if (uri != null && input != null && block != null) {
            scope.launch {
                busy = true
                error = null
                status = "Mengonversi save dan memverifikasi hasil…"
                try {
                    val output = File(context.cacheDir, "FFIXSaveBridge_export.zip")
                    val resultJson = withContext(Dispatchers.IO) {
                        if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
                        Python.getInstance().getModule("android_api")
                            .callAttr("convert_file", input, output.absolutePath, block).toString()
                    }
                    val result = JSONObject(resultJson)
                    withContext(Dispatchers.IO) {
                        val bytes = output.readBytes()
                        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
                            ?: throw FileNotFoundException("Lokasi ekspor tidak dapat ditulis.")
                        output.delete()
                    }
                    status = "Berhasil: ${result.optInt("converted")} save dikonversi. Container dan slot terenkripsi berhasil dibaca ulang."
                } catch (e: Exception) {
                    error = e.message ?: "Konversi gagal."
                    status = "Konversi dibatalkan. File sumber tidak diubah."
                } finally {
                    busy = false
                }
            }
        }
    }

    val chosen = entries.firstOrNull { it.block == selectedBlock }

    Scaffold(topBar = { TopAppBar(title = { Text("FFIX Save Bridge") }) }) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Konversi save Final Fantasy IX dari PlayStation ke format PC/Steam untuk FF9Recomp.",
                style = MaterialTheme.typography.bodyLarge
            )

            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(Icons.Outlined.Save, contentDescription = null)
                        Text("  Sumber save PS1", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Pilih file .mcd, .mcr, atau .srm dari DuckStation. Aplikasi membaca file secara lokal dan tidak mengubah sumber.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    ) {
                        androidx.compose.material3.Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Text("  Pilih memory card")
                    }
                    sourceName?.let { Text("Dipilih: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }

            if (entries.isNotEmpty()) {
                Text("Save yang ditemukan", style = MaterialTheme.typography.titleMedium)
                entries.forEach { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy) { selectedBlock = entry.block },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedBlock == entry.block)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBlock == entry.block,
                                onClick = { selectedBlock = entry.block },
                                enabled = !busy
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("${entry.leader.ifBlank { "Save FFIX" }} · Lv ${entry.level}", style = MaterialTheme.typography.titleSmall)
                                Text("Blok ${entry.block} · ${entry.region}", style = MaterialTheme.typography.bodySmall)
                                Text(entry.location.ifBlank { "Lokasi tidak diketahui" }, style = MaterialTheme.typography.bodySmall)
                                Text("Gil ${entry.gil} · ${entry.playtime} detik", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "CRC: ${if (entry.crcOk) "OK" else "GAGAL"} · Direktori: ${if (entry.directoryOk) "OK" else "GAGAL"}",
                                    color = if (entry.crcOk && entry.directoryOk) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            chosen?.let { entry ->
                val canConvert = entry.crcOk && entry.directoryOk
                Button(
                    onClick = { exportPicker.launch("FFIX_SaveBridge_Export.zip") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && canConvert
                ) {
                    Text("Konversi save terpilih → ZIP FF9Recomp")
                }
                if (!canConvert) {
                    Text(
                        "Save ini gagal pemeriksaan integritas. Konversi dinonaktifkan agar data tidak rusak.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (busy) {
                androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Output berisi SavedData_ww.dat dalam ZIP. Pilih file ZIP tersebut melalui fitur Import di FF9Recomp.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) return it.getString(index)
    }
    return uri.lastPathSegment ?: "memory-card"
}
