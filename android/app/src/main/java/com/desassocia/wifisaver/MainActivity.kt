package com.desassocia.wifisaver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF050806)
private val Panel = Color(0xFF101A14)
private val Panel2 = Color(0xFF15251B)
private val Green = Color(0xFF55F29A)
private val Muted = Color(0xFF9BAFA2)
private val Border = Color(0xFF243B2C)
data class SavedItem(val id: String, val type: String, val title: String, val value: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 11, 9)
        window.navigationBarColor = android.graphics.Color.rgb(7, 11, 9)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Green, secondary = Color(0xFF9AF7BC), background = Bg, surface = Panel, onPrimary = Color.Black, onBackground = Color.White, onSurface = Color.White)) {
                WiFiSaverApp()
            }
        }
    }
}

@Composable
private fun WiFiSaverApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wifi_saver", 0) }
    val saved = remember { mutableStateListOf<SavedItem>() }
    var tab by remember { mutableStateOf("Início") }
    var showAdd by remember { mutableStateOf(false) }
    var addType by remember { mutableStateOf("Nota") }
    var browserUrl by remember { mutableStateOf("https://www.google.com") }
    var openNote by remember { mutableStateOf<SavedItem?>(null) }

    fun persist() {
        val array = JSONArray()
        saved.forEach { item -> array.put(JSONObject().put("id", item.id).put("type", item.type).put("title", item.title).put("value", item.value)) }
        prefs.edit().putString("items", array.toString()).apply()
    }
    LaunchedEffect(Unit) {
        runCatching {
            val array = JSONArray(prefs.getString("items", "[]"))
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                saved.add(SavedItem(o.getString("id"), o.getString("type"), o.getString("title"), o.getString("value")))
            }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            saved.add(0, SavedItem(System.currentTimeMillis().toString(), "Vídeo", uri.lastPathSegment ?: "Vídeo salvo", uri.toString()))
            persist()
        }
    }

    Scaffold(containerColor = Bg, bottomBar = {
        NavigationBar(containerColor = Color(0xFF09100C), contentColor = Green) {
            listOf("Início", "Navegar", "YouTube", "Biblioteca", "Ajustes").forEach { name ->
                NavigationBarItem(
                    selected = tab == name, onClick = { tab = name },
                    icon = { Text(when(name) { "Início" -> "⌂"; "Navegar" -> "◎"; "YouTube" -> "▶"; "Biblioteca" -> "▤"; else -> "⚙" }, fontSize = 19.sp) },
                    label = { Text(name, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = Color(0xFF1B3A28), unselectedTextColor = Muted)
                )
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                "Início" -> HomeScreen(saved.size, { addType = "Nota"; showAdd = true }, { addType = "Link"; showAdd = true }, { tab = "Navegar" }, { tab = "YouTube" }, { tab = "Biblioteca" }, { videoPicker.launch(arrayOf("video/*")) })
                "Navegar" -> BrowserScreen(browserUrl, { title, url -> saved.add(0, SavedItem(System.currentTimeMillis().toString(), "Link", title, url)); persist() }, { browserUrl = it })
                "YouTube" -> YouTubeScreen()
                "Biblioteca" -> LibraryScreen(saved, { item -> saved.remove(item); persist() }, { item ->
                    when (item.type) {
                        "Link" -> { browserUrl = item.value; tab = "Navegar" }
                        "Vídeo" -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(item.value), "video/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                        else -> openNote = item
                    }
                }, { addType = "Nota"; showAdd = true }, { videoPicker.launch(arrayOf("video/*")) })
                else -> SettingsScreen()
            }
        }
    }
    if (showAdd) AddDialog(addType, { showAdd = false }) { title, value ->
        saved.add(0, SavedItem(System.currentTimeMillis().toString(), addType, title, value)); persist(); showAdd = false
    }
    openNote?.let { item ->
        AlertDialog(onDismissRequest = { openNote = null }, containerColor = Panel, title = { Text(item.title, color = Green) },
            text = { Text(item.value, color = Color.White) }, confirmButton = { TextButton(onClick = { openNote = null }) { Text("FECHAR") } })
    }
}

@Composable
private fun HomeScreen(count: Int, onAddNote: () -> Unit, onAddLink: () -> Unit, onBrowse: () -> Unit, onYoutube: () -> Unit, onLibrary: () -> Unit, onVideo: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1710), Bg, Bg))).padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(34.dp).background(Color(0xFF173B27), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                    Text("W", color = Green, fontWeight = FontWeight.Black, fontSize = 19.sp)
                }
                Column {
                    Text("WIFI-SAVER", color = Green, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 13.sp)
                    Text("SEU ESPAÇO DIGITAL", color = Muted, fontSize = 8.sp, letterSpacing = 1.5.sp)
                }
                Spacer(Modifier.weight(1f))
                Surface(color = Color(0xFF12271A), shape = RoundedCornerShape(50)) {
                    Text("● OFFLINE READY", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Seu mundo.\nDo seu jeito.", fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Salve conteúdos e leve sua biblioteca com você.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 22.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF14291C)), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SEU ESPAÇO", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        Spacer(Modifier.weight(1f))
                        Text("● SALVO NO APARELHO", color = Color(0xFFB5EBC8), fontSize = 8.sp)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text("SUA BIBLIOTECA", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("$count itens guardados", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 7.dp))
                    Text("Notas, links e vídeos que você importou.", color = Muted, fontSize = 12.sp)
                    TextButton(onClick = onLibrary) { Text("ABRIR BIBLIOTECA →", color = Green) }
                }
            }
            Text("ATALHOS", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 26.dp, bottom = 12.dp))
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeTile("◎", "Navegador", "Visite sites", Modifier.weight(1f), onBrowse)
            HomeTile("▶", "YouTube", "Assistir online", Modifier.weight(1f), onYoutube)
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
            HomeTile("✎", "Nova nota", "Guardar texto", Modifier.weight(1f), onAddNote)
            HomeTile("↗", "Salvar link", "Favoritos", Modifier.weight(1f), onAddLink)
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
            HomeTile("▣", "Vídeo offline", "Importar do aparelho", Modifier.weight(1f), onVideo)
            HomeTile("▤", "Coleção", "Ver conteúdos", Modifier.weight(1f), onLibrary)
        } }
        item { Text("DICA: páginas e vídeos precisam ser salvos/importados antes para ficarem disponíveis sem internet.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 22.dp)) }
    }
}

@Composable
private fun HomeTile(icon: String, title: String, subtitle: String, modifier: Modifier, action: () -> Unit) {
    Card(modifier = modifier.height(122.dp).clickable { action() }, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(icon, color = Green, fontSize = 23.sp)
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun BrowserScreen(initialUrl: String, onSaveLink: (String, String) -> Unit, onUrlChanged: (String) -> Unit) {
    var address by remember { mutableStateOf(initialUrl) }
    var current by remember { mutableStateOf(initialUrl) }
    var title by remember { mutableStateOf("Navegador privado") }
    val context = LocalContext.current
    val web = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.setGeolocationEnabled(false)
            settings.allowFileAccess = false; settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false; settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            if (android.os.Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled = true
            webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) { callback?.invoke(origin, false, false) }
                override fun onPermissionRequest(request: PermissionRequest?) { request?.deny() }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val scheme = request?.url?.scheme; return scheme != "https" && scheme != "http"
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url != null) { current = url; address = url; onUrlChanged(url); title = view?.title ?: "Página" }
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { web.stopLoading(); web.destroy() } }
    Column(Modifier.fillMaxSize().background(Bg).padding(14.dp)) {
        Text("NAVEGADOR PRIVADO", color = Green, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp, fontSize = 13.sp)
        Text("Acesse a web com controles de privacidade.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(address, { address = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Endereço") })
            Spacer(Modifier.width(6.dp))
            Button(onClick = {
                val raw = address.trim()
                val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
                current = url; onUrlChanged(url); web.loadUrl(url)
            }) { Text("IR") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            OutlinedButton(onClick = { if (web.canGoBack()) web.goBack() }) { Text("←") }
            OutlinedButton(onClick = { web.reload() }) { Text("↻") }
            OutlinedButton(onClick = { onSaveLink(title, current) }) { Text("♡ SALVAR LINK") }
        }
        Text(title, color = Muted, fontSize = 11.sp, maxLines = 1)
        AndroidView(factory = { web }, modifier = Modifier.fillMaxSize().padding(top = 7.dp))
    }
}

@Composable
private fun YouTubeScreen() {
    val context = LocalContext.current
    val web = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true; settings.setGeolocationEnabled(false)
            settings.allowFileAccess = false; settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            if (android.os.Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled = true
            webChromeClient = WebChromeClient(); webViewClient = WebViewClient()
            loadUrl("https://m.youtube.com")
        }
    }
    DisposableEffect(Unit) { onDispose { web.stopLoading(); web.destroy() } }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("YOUTUBE", color = Green, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("Vídeos online • conexão necessária", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
        }
        AndroidView(factory = { web }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun LibraryScreen(items: List<SavedItem>, onDelete: (SavedItem) -> Unit, onOpen: (SavedItem) -> Unit, onAdd: () -> Unit, onImportVideo: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1710), Bg))).padding(18.dp)) {
        Text("MINHA BIBLIOTECA", color = Green, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp, fontSize = 14.sp)
        Text("${items.size} conteúdos guardados", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd) { Text("+ NOTA") }
            OutlinedButton(onClick = onImportVideo) { Text("+ VÍDEO") }
        }
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sua biblioteca ainda está vazia.", color = Muted) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(items, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(when(item.type) { "Nota" -> "✎"; "Vídeo" -> "▶"; else -> "↗" }, color = Green, fontSize = 22.sp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp).clickable { onOpen(item) }) {
                            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(item.type.uppercase(), color = Muted, fontSize = 10.sp)
                        }
                        TextButton(onClick = { onDelete(item) }) { Text("EXCLUIR", color = Color(0xFFFF8888), fontSize = 10.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDialog(type: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Panel,
        title = { Text(if (type == "Nota") "Nova anotação" else "Salvar link", color = Green) },
        text = { Column {
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value, { value = it }, label = { Text(if (type == "Nota") "Seu texto" else "https://...") }, minLines = if (type == "Nota") 4 else 1)
        } },
        confirmButton = { TextButton(onClick = {
            val v = value.trim()
            val normalized = if (type == "Link" && v.isNotBlank() && !v.startsWith("http")) "https://$v" else v
            if (title.isNotBlank() && normalized.isNotBlank()) onSave(title.trim(), normalized)
        }) { Text("SALVAR", color = Green) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().background(Bg).padding(20.dp)) {
        Text("AJUSTES E PRIVACIDADE", color = Green, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Text("Privacidade", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("O navegador bloqueia solicitações de localização GPS, câmera e microfone. Isso não esconde seu endereço IP: sites ainda podem estimar sua região pela conexão.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
        HorizontalDivider(color = Color(0xFF263A2D), modifier = Modifier.padding(vertical = 18.dp))
        Text("Uso offline", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Notas ficam salvas no aparelho. Links e YouTube precisam de internet. Vídeos offline são importados de arquivos que você já tem no dispositivo.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
        HorizontalDivider(color = Color(0xFF263A2D), modifier = Modifier.padding(vertical = 18.dp))
        Text("WiFi-Saver • Android", color = Green, fontWeight = FontWeight.Bold)
        Text("Interface nativa em Kotlin + Jetpack Compose.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
    }
}
