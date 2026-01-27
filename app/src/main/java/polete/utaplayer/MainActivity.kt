package polete.utaplayer

import android.R
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource

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

    //estats app per guardar cançons i que esta sonant i estat de play pause
    val llistaCancons = remember { fetchSongs(context) }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    //reproductor
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

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
    Scaffold(
        bottomBar = {
            // mostrar playpause menu
            currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    onPlayPause = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }
                )
            }
        }
    ) { padding ->
        // llistar cançons
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(llistaCancons) { canco ->
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
    }
}

@Composable
fun SongRow(song: Song, onSongClick: (Song) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick(song) } // tornem canço
            .padding(16.dp)
    ) {
        Text(text = song.title, style = MaterialTheme.typography.titleMedium)
        Text(text = song.artist, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
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

    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
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