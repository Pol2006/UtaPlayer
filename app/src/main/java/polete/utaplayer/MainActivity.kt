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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.core.net.toUri
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import me.saket.squiggles.SquigglySlider

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

@OptIn(DelicateCoroutinesApi::class)
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

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Escanegem el disc per si hi ha fitxers nous
        scanMusic(context) {
            val musicFromSystem = fetchSongs(context)
            // fem servir l'scope per guardar les cançons sense que es quedi penjat el dispositiu
            scope.launch {
                songDao.insertSongs(musicFromSystem)
            }
        }
    }
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
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
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
@OptIn(ExperimentalMaterial3Api::class)
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
                .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //boto tancar
            IconButton(onClick = onClose, Modifier.padding(top = 24.dp)) {
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
                    .padding(24.dp),
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
            //Titol i artista
            Text(song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.9f).padding(start = 24.dp))
            Text(song.artist, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.9f).padding(start = 24.dp))

            //TODO: Posar lyrics aqui entre el titol i els controls

            // 4. Slider i Temps
            //es per calcular el progress i es necesari per si es 0 que no doni error
            val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

                SquigglySlider(
                    value = progress,
                    onValueChange = { newTime ->
                        // cuan cliquem anem al nou lloc clicat
                        onSeek((newTime * duration).toLong())
                    },
                    colors = SliderDefaults.colors( //TODO: Ficar colors de album
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier //per fer mes gran el en si la barra
                        .padding(horizontal = 30.dp)
                        .padding(vertical = 12.dp),
                    squigglesSpec = SquigglySlider.SquigglesSpec(
                        strokeWidth = 8.dp,
                        amplitude = if (isPlaying) 8.dp else 0.dp,
                    )

                )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .weight(1f) // per ocupar tot l'espai possible per que es vegin les lyrics
                    .padding(start = 24.dp, end = 24.dp)

            ) {
                // prova
                Text("lyricssssssssssss", color = MaterialTheme.colorScheme.onBackground)
            }
            //Botons de control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, //ajustar tot per igual
                verticalAlignment = Alignment.CenterVertically
            ) {
                //aleatori
                IconButton(onClick = {/*TODO*/}) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                //anterior
                IconButton(onClick = {/*TODO*/}) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = null, modifier = Modifier.size(36.dp))
                }

                //playpause
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }

                //seguent
                IconButton(onClick = {/*TODO*/}) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(36.dp))
                }

                //bucle TODO:Un cop feta la logica canviar icona un cop seleccionat
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }

        }
    }
}
@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onPlayPause: () -> Unit) {
    Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
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