package com.mudahdi.ffixsavebridge

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.FileNotFoundException

private data class SelectedSave(
    val name: String,
    val size: Long,
    val kind: String,
    val note: String
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
    var selected by remember { mutableStateOf<SelectedSave?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isReading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isReading = true
            error = null
            try {
                val name = queryDisplayName(context, uri)
                val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    it.length
                } ?: -1L
                val kind = classifyCandidate(size)
                selected = SelectedSave(
                    name = name,
                    size = size,
                    kind = kind.first,
                    note = kind.second
                )
            } catch (_: FileNotFoundException) {
                error = "File tidak dapat dibuka. Coba pilih file lain."
                selected = null
            } catch (_: Exception) {
                error = "Gagal membaca informasi file."
                selected = null
            } finally {
                isReading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("FFIX Save Bridge") })
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Pindahkan save Final Fantasy IX dari kartu memori PS1 ke FF9Recomp.",
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
                        androidx.compose.material3.Icon(
                            Icons.Outlined.Save,
                            contentDescription = null
                        )
                        Text(
                            "  Sumber save",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        "Pilih file kartu memori DuckStation (.mcd, .mcr, .srm) atau dump save PS1.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReading
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null
                        )
                        Text("  Pilih file")
                    }
                }
            }

            selected?.let { save ->
                Card {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("File terpilih", style = MaterialTheme.typography.titleMedium)
                        Text(save.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (save.size >= 0) "Ukuran: ${save.size} byte" else "Ukuran tidak diketahui",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(save.kind, color = MaterialTheme.colorScheme.primary)
                        Text(save.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Status pengembangan", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Fondasi pemilihan file sudah tersedia. Pemeriksaan struktur save, " +
                            "pemetaan data PS1 ke format PC, dan ekspor SavedData_ww.dat " +
                            "belum diaktifkan. Aplikasi tidak akan mengubah file sumber.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun classifyCandidate(size: Long): Pair<String, String> = when (size) {
    131_072L -> "Kandidat kartu memori PS1 (128 KiB)" to
        "Ukuran cocok dengan image kartu memori standar. Isi dan checksum belum divalidasi."
    8_192L -> "Kandidat blok save PS1 (8 KiB)" to
        "Ukuran cocok dengan blok save mentah. Header dan checksum belum divalidasi."
    8_320L -> "Kandidat wrapper save PS1 (8,320 byte)" to
        "Ukuran cocok dengan wrapper yang dikenal. Struktur internal belum divalidasi."
    2_937_152L -> "Kandidat container save PC/Steam" to
        "Ukuran cocok dengan container rr2016. Dekripsi dan integritas belum diperiksa."
    else -> "Format belum dikenali" to
        "Ukuran file saja tidak cukup untuk menentukan format. File sumber tetap aman."
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) return it.getString(nameIndex)
    }
    return uri.lastPathSegment ?: "save-file"
}
