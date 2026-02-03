package polete.utaplayer

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import polete.utaplayer.ui.theme.UtaplayerTheme
import androidx.compose.material.icons.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import coil.compose.AsyncImage
import androidx.core.net.toUri
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UtaplayerTheme {
                // Tema permisos
                val permissionState = rememberPermissionState(android.Manifest.permission.READ_MEDIA_AUDIO)

                if (permissionState.status.isGranted) {
                    UtaPlayerApp()
                } else {
                    PantallaPermisos(onGrantClick = { permissionState.launchPermissionRequest() })
                }
            }
        }
    }
}

@Composable
fun UtaPlayerApp() {
    val context = LocalContext.current

    val database = remember { AppDatabase.getDatabase(context) } //declarem base de dades
    val songDao = remember { database.songDao() } //declarem el dao
    //Serveix per mirar la base de dades en temps real
    val songList by songDao.getAllSongs().collectAsState(emptyList())

    var currentSong by remember { mutableStateOf<Song?>(null) } //canço actual
    var isPlaying by remember { mutableStateOf(false) } //saber si esta sonant o no
    var currentPosition by remember { mutableLongStateOf(0L) } //posicio actual canço
    var duration by remember { mutableLongStateOf(0L) } //duracio canço
    var isFullScreen by remember { mutableStateOf(false) } //pantalla completa
    //reproductor
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(Unit) {
// Escanegem el disc per si hi ha fitxers nous
        scanMusic(context) {
            // Obtenim la música del sistema (MediaStore)
            val musicFromSystem = fetchSongs(context)

            // La guardem a la nostra base de dades de Room
            // Fem servir una corrutina perquè insertSongs és 'suspend'
            kotlinx.coroutines.GlobalScope.launch {
                songDao.insertSongs(musicFromSystem)
            }
        }    }
    //Agafar temps i duracio
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L) // fer que minim sigui 0
                kotlinx.coroutines.delay(1000) // ho fem cada segon
            }
        }
    }
    //listener de exoplayer (necesari) per saber si la musica sona o no
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        })
    }

    //tencar exoplayer al tencar app per no consumir
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    //visual
    if (isFullScreen && currentSong != null) {
        PlayerFullScreen(
            song = currentSong!!,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onClose = { isFullScreen = false },
            onPlayPause = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
            onSeek = { exoPlayer.seekTo(it) /*per anar al ms que toquin*/ },
        )
    }
    else{
        Scaffold(
            bottomBar = {
                // mostrar playpause menu
                currentSong?.let { song ->
                    Surface(modifier = Modifier.clickable { isFullScreen = true }) {
                    MiniPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        onPlayPause = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }
                        )
                    }
                }
            }
        ) { padding ->
            // llistar cançons
            if(songList.isNotEmpty())
            LazyColumn(modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)) {
                items(songList) { canco ->
                    SongRow(
                        song = canco,
                        onSongClick = { cancoClicada ->
                            currentSong = cancoClicada // ho fem servir per la ui per saber quina canço esta sonant

                            // reproduccio exoplayer
                            val mediaItem = MediaItem.fromUri(cancoClicada.data)
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        }
                    )
                }
            }
            else{
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No s'ha trobat cap cançó")
                }
            }
        }
    }

}

@Composable
fun SongRow(song: Song, onSongClick: (Song) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick(song) } // tornem canço
            .padding(16.dp)
    ) {
            AsyncImage(
                model = getAlbumArtUri(song.albumId), // funcio per agafar img
                contentDescription = "album img",
                contentScale = Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                error = rememberVectorPainter(Icons.Rounded.MusicNote) , // si falla o no te imatge
                placeholder = rememberVectorPainter(Icons.Rounded.MusicNote) //mentre carrega
            )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)

        }

    }
}
@Composable
fun PlayerFullScreen(
    song: Song,                  // info de la canço
    isPlaying: Boolean,          // esta en play o no
    currentPosition: Long,       // temps actual
    duration: Long,              // total duracio
    onPlayPause: () -> Unit,     // pausar / reanudar
    onSeek: (Long) -> Unit,      // barra de temps (avisa on ha tocat)
    onClose: () -> Unit          // tancar
) {
    // pantalla full screen
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // TODO: fer que canvii el color de fons per el del album
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), // Marges perquè res toqui les vores
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AQUÍ COMENÇA EL TEU DISSENY
            //boto tancar
            IconButton(onClick = onClose) {
                //icona boto
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Tencar",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary                )
            }
            //img album
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = getAlbumArtUri(song.albumId), // funcio per agafar img
                        contentDescription = "album img",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = Crop, // fem que agafi tot el lloc
                        error = rememberVectorPainter(Icons.Rounded.MusicNote) , // si falla o no te imatge
                        placeholder = rememberVectorPainter(Icons.Rounded.MusicNote) //mentre carrega
                    )

                }
            }
            // 3. Títol i Artista
            // 4. Slider i Temps
            // 5. Botons de control

        }
    }
}
@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onPlayPause: () -> Unit) {
    Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, maxLines = 1)
                Text(text = song.artist, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPlayPause) {
                //icona boto
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary                )
            }
        }
    }
}

@Composable
fun PantallaPermisos(onGrantClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onGrantClick) { Text("Donar permís per veure la música") }
    }
}

//extreure dades
fun fetchSongs(context: Context): List<Song> {
    val tempSongs = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1" //Ara nomes ens donara musica, no audios de whatsapp i altres coses


    context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val idAlbumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext()) {
            tempSongs.add(
                Song(
                    id = cursor.getLong(idCol),
                    albumId = cursor.getLong(idAlbumCol),
                    title = cursor.getString(titleCol),
                    artist = cursor.getString(artistCol),
                    data = cursor.getString(dataCol),
                    duration = cursor.getInt(durationCol)
                )
            )
        }
    }
    return tempSongs
}
fun getAlbumArtUri(albumId: Long): android.net.Uri {
    return android.content.ContentUris.withAppendedId(
        "content://media/external/audio/albumart".toUri(),
        albumId
    )
}
//serveix per si afegeixes arxius nous i el mobil encara no ho ha procesat a la base de dades per forçar-ho
fun scanMusic(context: Context, onFinish: () -> Unit) {
    MediaScannerConnection.scanFile(
        context,
        arrayOf("/storage/emulated/0/Music"), //directori que mirarem
        null //es el tipus de fitxer, null fa que ho detecti el sistema sol
    ) { path, uri -> // aixo es per avisar cuan acabi de escanejar
        onFinish()
    }
}