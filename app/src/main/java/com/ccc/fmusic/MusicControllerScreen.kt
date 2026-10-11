package com.ccc.fmusic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MusicControllerScreen(onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(context) { RetrofitClient.getInstance(context) }

    var searchQuery by remember { mutableStateOf("") }
    var songList by remember { mutableStateOf(listOf<Song>()) }

    var currentVolume by remember { mutableStateOf<Float?>(null) }
    var nowPlaying by remember { mutableStateOf<NowPlayingData?>(null) }

    var isServerConnected by remember { mutableStateOf(true) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var selectedPlaylistInfo by remember { mutableStateOf<PlaylistInfoResponse?>(null) }
    var isLoadingPlaylistTracks by remember { mutableStateOf(false) }

    // Función para buscar canciones en la API
    val search = {
        scope.launch {
            try {
                val response = api.searchSongs(searchQuery)
                songList = response.songs
                selectedPlaylistInfo = null
                showBottomSheet = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Función para abrir la playlist de la canción en reproducción al tocar la portada
    // Agrega este estado junto a los demás mutableStateOf arriba en MusicControllerScreen
    var lastActivePlaylistId by remember { mutableStateOf<String?>(null) }

// Modifica la función openCurrentPlaylist
    val openCurrentPlaylist = {
        scope.launch {
            // Priorizar el playlistId recibido del servidor; si viene vacío, usar el último registrado
            val targetPlaylistId = if (!nowPlaying?.playlistId.isNullOrEmpty()) {
                nowPlaying?.playlistId
            } else {
                lastActivePlaylistId
            }

            val activeVideoId = nowPlaying?.videoId

            try {
                isLoadingPlaylistTracks = true
                showBottomSheet = true

                if (!targetPlaylistId.isNullOrEmpty()) {
                    // 1. Cargar datos de la playlist si tenemos un ID guardado o activo
                    val details = api.getPlaylistInfo(targetPlaylistId)
                    selectedPlaylistInfo = if (details.tracks.isNotEmpty()) {
                        details
                    } else {
                        null
                    }
                } else if (!activeVideoId.isNullOrEmpty()) {
                    // 2. Si es una canción suelta sin playlist asociada, intentar cargar su radio (RDAMVM)
                    val radioPlaylistId = "RDAMVM$activeVideoId"
                    selectedPlaylistInfo = try {
                        val details = api.getPlaylistInfo(radioPlaylistId)
                        if (details.tracks.isNotEmpty()) {
                            details
                        } else {
                            null
                        }
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    // 3. Solo si no hay playlist ni track cargado, mostrar buscador limpio
                    selectedPlaylistInfo = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                selectedPlaylistInfo = null
            } finally {
                isLoadingPlaylistTracks = false
            }
        }
    }

    LaunchedEffect(refreshTrigger) {
        isLoadingInitial = true
        isServerConnected = true

        try {
            val volRes = api.getVolume()
            currentVolume = volRes.volume.toFloat()
            isServerConnected = true
        } catch (e: Exception) {
            e.printStackTrace()
            isServerConnected = false
        } finally {
            isLoadingInitial = false
        }

        while (isActive && isServerConnected) {
            try {
                val res = api.getNowPlaying()
                if (res.status == "success") {
                    nowPlaying = res.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isServerConnected = false
            }
            delay(2000.milliseconds)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reproductor FMUSIC",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar datos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isServerConnected && !isLoadingInitial) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Sin conexión con el servidor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verifica la IP, Puerto y API Key en Configuración.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reintentar conexión")
                    }
                }
            } else if (isLoadingInitial) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isSongLoaded = nowPlaying?.title?.isNotEmpty() == true

                    // CARÁTULA PRINCIPAL: Al tocarla abre la lista de reproducción actual
                    if (isSongLoaded && !nowPlaying?.thumbnail.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(nowPlaying?.thumbnail)
                                .crossfade(true)
                                .diskCacheKey(nowPlaying?.thumbnail)
                                .build(),
                            contentDescription = "Carátula de la canción actual",
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { openCurrentPlaylist() },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { openCurrentPlaylist() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isSongLoaded) nowPlaying!!.title else "No se está reproduciendo nada",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = if (isSongLoaded) nowPlaying!!.artist else "Toca la imagen o buscar para seleccionar música",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val currentTime = nowPlaying?.currentTime ?: 0
                    val duration = nowPlaying?.duration ?: 0
                    val progressRatio = if (duration > 0) (currentTime.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

                    if (isSongLoaded && duration > 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatSeconds(currentTime),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatSeconds(duration),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Controles de medios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            modifier = Modifier.size(56.dp),
                            onClick = {
                                scope.launch {
                                    try { api.mediaControl("previous") } catch (e: Exception) { e.printStackTrace() }
                                }
                            }
                        ) {
                            Text("⏮", style = MaterialTheme.typography.headlineMedium)
                        }

                        Button(
                            modifier = Modifier.height(56.dp),
                            onClick = {
                                scope.launch {
                                    try { api.mediaControl("play_pause") } catch (e: Exception) { e.printStackTrace() }
                                }
                            },
                            shape = RoundedCornerShape(100)
                        ) {
                            Text(
                                text = if (nowPlaying?.isPlaying == true) "⏸ Pausa" else "▶ Reproducir",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(56.dp),
                            onClick = {
                                scope.launch {
                                    try { api.mediaControl("next") } catch (e: Exception) { e.printStackTrace() }
                                }
                            }
                        ) {
                            Text("⏭", style = MaterialTheme.typography.headlineMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Slider de Volumen
                    currentVolume?.let { vol ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Volumen del Local",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${vol.toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = vol,
                                onValueChange = { currentVolume = it },
                                onValueChangeFinished = {
                                    scope.launch {
                                        try {
                                            api.setVolume(VolumeRequest(vol.toInt()))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // FAB Flotante
        if (isServerConnected) {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedPlaylistInfo = null
                    showBottomSheet = true
                },
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                text = { Text("Buscar Música") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }

        // Bottom Sheet para Búsqueda / Ver Playlist
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    selectedPlaylistInfo = null
                },
                sheetState = sheetState,
                modifier = Modifier.fillMaxHeight(),


            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    val playlist = selectedPlaylistInfo

                    if (playlist != null) {
                        // VISTA DE CANCIONES DE LA PLAYLIST
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            IconButton(onClick = { selectedPlaylistInfo = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlist.author} • ${playlist.trackCount} canciones",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // BOTÓN SUPERIOR PARA REPRODUCIR LA PLAYLIST COMPLETA
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        api.playSong(PlayRequest(playlist.playlistId))
                                        showBottomSheet = false
                                        selectedPlaylistInfo = null
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reproducir Playlist Completa")
                        }

                        val tracksList = playlist.tracks

                        LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                            items(tracksList) { track ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            scope.launch {
                                                try {
                                                    api.playSong(PlayRequest(track.videoId))
                                                    showBottomSheet = false
                                                    selectedPlaylistInfo = null
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (track.thumbnail.isNotEmpty()) {
                                            AsyncImage(
                                                model = track.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Column(
                                            modifier = Modifier
                                                .padding(start = 12.dp)
                                                .weight(1f)
                                        ) {
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = track.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        if (track.duration.isNotEmpty()) {
                                            Text(
                                                text = track.duration,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    } else if (isLoadingPlaylistTracks) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // VISTA DE BÚSQUEDA GENERAL
                        Text(
                            text = "Buscar Música / Mixes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            maxLines = 1,
                            singleLine = true,
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("¿Qué quieres escuchar?") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { search() },
                                onDone = { search() }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { search() }) {
                                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            items(songList) { song ->
                                val isPlaylistOrAlbum = song.type == "Playlist" || song.type == "Álbum"

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {
                                                scope.launch {
                                                    if (isPlaylistOrAlbum) {
                                                        try {
                                                            isLoadingPlaylistTracks = true
                                                            val details = api.getPlaylistInfo(song.videoId)
                                                            selectedPlaylistInfo = details
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        } finally {
                                                            isLoadingPlaylistTracks = false
                                                        }
                                                    } else {
                                                        try {
                                                            api.playSong(PlayRequest(song.videoId))
                                                            showBottomSheet = false
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (isPlaylistOrAlbum) {
                                                    scope.launch {
                                                        try {
                                                            api.playSong(PlayRequest(song.videoId))
                                                            showBottomSheet = false
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                }
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (song.thumbnail.isNotEmpty()) {
                                            AsyncImage(
                                                model = song.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Column(
                                            modifier = Modifier
                                                .padding(start = 12.dp)
                                                .weight(1f)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = song.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )

                                                if (song.type.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = song.type,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(
                                                                horizontal = 4.dp,
                                                                vertical = 2.dp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (isPlaylistOrAlbum) "${song.artist} • Mantén presionado para reproducir todo" else song.artist,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}