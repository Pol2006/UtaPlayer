package polete.utaplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import polete.utaplayer.ui.theme.UtaplayerTheme
import androidx.compose.material.icons.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.common.util.concurrent.MoreExecutors
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UtaplayerTheme {
                // Demanem permisos (Android 13+)
                val permissionsState = rememberMultiplePermissionsState(
                    permissions = listOf(
                        android.Manifest.permission.READ_MEDIA_AUDIO,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                )

                // Si tots els permisos estan acceptats entrem a la app
                if (permissionsState.allPermissionsGranted) {
                    UtaPlayerApp()
                } else {
                    // Si falta algun permis el demanem
                    PantallaPermisos(onGrantClick = {
                        permissionsState.launchMultiplePermissionRequest()
                    })
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UtaPlayerApp() {
    val context = LocalContext.current

    val database = remember { AppDatabase.getDatabase(context) } //declarem base de dades
    val songDao = remember { database.songDao() } //declarem el dao
    val songList by songDao.getAllSongs().collectAsState(emptyList()) //Serveix per mirar la base de dades en temps real
    var currentSong by remember { mutableStateOf<Song?>(null) } //canço actual
    var isPlaying by remember { mutableStateOf(false) } //saber si esta sonant o no
    var currentPosition by remember { mutableLongStateOf(0L) } //posicio actual canço
    var duration by remember { mutableLongStateOf(0L) } //duracio canço
    var isFullScreen by remember { mutableStateOf(false) } //pantalla completa
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) //per animacio de lliscar cap abaix
    var mediaItemsLoaded by remember { mutableStateOf(false) }

    // Creem un token d'identificacio per conectarnos a AudioPlayerService que fa que el controlador sapiga les ordres de la notificacio i reproduccio
    val sessionToken = remember {
        SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
    }

    //serveix per comunicar la interficie de la notificacio, es null pero al ser mutable, cuan canvii automaticament s'actualitzara
    var controller by remember { mutableStateOf<MediaController?>(null) }

    //controla la conexio amb el reproductor en segon pla
    val controllerPeticio = remember { MediaController.Builder(context, sessionToken).buildAsync() }

    //es com un LaunchedEffect pero al tencar la app anira al ondispose
    DisposableEffect(Unit) {
        controllerPeticio.addListener({
            val c = controllerPeticio.get()
            controller = c

            // Si s'esta reproduint algo cuan obrim la app actualitzem la ui.
            val mediaId = c.currentMediaItem?.mediaId?.toLongOrNull()
            if (mediaId != null && c.isPlaying) {
                currentSong = songList.find { it.id == mediaId }
                isPlaying = c.isPlaying
            }
                    // Indica que el codi anterior s'ha d'executar immediatament en el mateix fil. (optimitzacio)
        }, MoreExecutors.directExecutor())
        //tanca conexio al tencar app
        onDispose {
            MediaController.releaseFuture(controllerPeticio)
        }
    }
    // converteix la llista de la bdd a mediaitem i la carrega al reproductor.
    LaunchedEffect(songList, controller) {
        val currentController = controller ?: return@LaunchedEffect
        if (songList.isEmpty() || mediaItemsLoaded) return@LaunchedEffect

        if (currentController.mediaItemCount == 0) {
        val mediaItems = withContext(Dispatchers.Default) {
                songList.map { song ->
                    MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(song.data.toUri())
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist)
                                .setArtworkUri(getAlbumArtUri(song.albumId))
                                .build()
                        )
                        .build()
                }
            }
            currentController.setMediaItems(mediaItems)
        }
        else {
            //si venim de tancar la app i tenim musica busquem quina es perque surti el miniplayer
            val idSonant = currentController.currentMediaItem?.mediaId?.toLongOrNull()
            if (idSonant != null && currentController.isPlaying) {
                currentSong = songList.find { it.id == idSonant }
                isPlaying = currentController.isPlaying
            }
        }
            mediaItemsLoaded = true
    }
    // Escanegem el disc per si hi ha fitxers nous
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // nomes mira el disc si la bd esta buida
            val musicFromSystem = fetchSongs(context)

            if (songDao.getCount() != musicFromSystem.size) {
                // si no es igual el disc del que tenim mirem
                songDao.syncSongs(musicFromSystem)
            }

            // scanMusic solo para detectar archivos MUY nuevos
            scanMusic(context) {
                scope.launch(Dispatchers.IO) {
                    val updatedMusic = fetchSongs(context)
                    if (updatedMusic.size != musicFromSystem.size) {
                        songDao.syncSongs(updatedMusic)
                    }
                }
            }
        }
    }
    // Serveix per actualitzar dades canço (lyrics) quan canvia la bdd
    LaunchedEffect(songList) {
        val id = currentSong?.id ?: return@LaunchedEffect
        currentSong = songList.find { it.id == id }
    }

    //Agafar temps i duracio
    LaunchedEffect(isPlaying, isFullScreen) {
        if (isPlaying) {
            while (true) {
                currentPosition = controller?.currentPosition?: 0L
                duration = controller?.duration?.coerceAtLeast(0L)?:0L // fer que minim sigui 0
                kotlinx.coroutines.delay(if (isFullScreen) 32L else 500L)
            }
        }
    }

    LaunchedEffect(controller, currentSong) {
        val c = controller ?: return@LaunchedEffect // Si es null no fem res

        // serveix per el canvi de canço que no es quedi penjada la barra
        currentPosition = c.currentPosition
        duration = c.duration.coerceAtLeast(0L)
    }
    //listener de exoplayer (necesari) per saber si la musica sona o no
    LaunchedEffect(controller) {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }


            // Detecta cuando cambia la canción automáticamente
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idSonant = mediaItem?.mediaId?.toLongOrNull()
                if (idSonant != null) {
                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                        currentSong = songList.find { it.id == idSonant }
                    }
                }
            }
        })
    }
    //visual

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                /// Només si NO estem en pantalla completa i hi ha una cançó
                if (!isFullScreen && currentSong != null) {

                    Surface(
                        modifier = Modifier
                            // afegim el gest de lliscar
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    // dragAmount es per detectar que el dit puja cap adalt
                                    if (dragAmount < -15) { isFullScreen = true } } }
                            .clickable { isFullScreen = true },
                        color = Color.Transparent
                    ) {
                        MiniPlayer(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            onPlayPause = { if (isPlaying) controller?.pause() else controller?.play() },
                            onNext = { controller?.seekToNext() },
                            onPrevious = { controller?.seekToPrevious() },
                            currentPosition = currentPosition,
                            duration = duration
                            )
                    }
                }
            }
        ) { padding ->

            // llistar cançons
                Surface {
                    if(songList.isNotEmpty())
                        LazyColumn(modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .background(color = Color.Transparent)) {
                            items(songList,key = { it.id }) { canco ->
                                SongRow(
                                    song = canco,
                                    onSongClick = { cancoClicada ->
                                        currentSong = cancoClicada
                                        val index = songList.indexOf(cancoClicada)
                                        if (index != -1) {
                                            controller?.seekTo(index, 0L) // per anar a la canço triada
                                            controller?.prepare()
                                            controller?.play()
                                        }
                                    },
                                    currentSong = currentSong,
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
                if (isFullScreen && currentSong != null) {
                    //animacio per tancar el player
                    ModalBottomSheet(
                        onDismissRequest = { isFullScreen = false }, // Es tanca si toques fora o llisques baix
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp,
                        dragHandle = { BottomSheetDefaults.DragHandle()  } // La ratlleta de dalt per estirar
                    ) {
                        PlayerFullScreen(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            onPlayPause = { if (isPlaying) controller?.pause() else controller?.play() },
                            onSeek = {  currentPosition = it
                                controller?.seekTo(it) /*per anar al ms que toquin*/ },
                            onNext = { controller?.seekToNext() },
                            onPrevious = { controller?.seekToPrevious() },
                            songDao = songDao,
                            scope = scope,

                        )
                    }
                }
        }
    }

@Composable
fun SongRow(song: Song, onSongClick: (Song) -> Unit, currentSong: Song?) {
    val colors = agafarEsquemaColors(song.albumId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick(song) } // tornem canço
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = if(song == currentSong) colors.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically

    ) {
            AsyncImage(
                model = getAlbumArtUri(song.albumId), // funcio per agafar img
                contentDescription = "album img",
                contentScale = Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                error = rememberVectorPainter(Icons.Rounded.MusicNote) , // si falla o no te imatge
                placeholder = rememberVectorPainter(Icons.Rounded.MusicNote) //mentre carrega
            )
        Spacer(Modifier.padding(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2500))
            if(song.artist != "")Text(text = song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,modifier = Modifier.basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2000))

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
    onNext: () -> Unit,          //seguent canço
    onPrevious: () -> Unit,       //anterior canço
    songDao: SongDao,
    scope: CoroutineScope

) {
    // colors
    val colors = agafarEsquemaColors(song.albumId)

    //animem els colors perque no es vegi un canvi directe
    val animatedSurface by animateColorAsState(
        targetValue = colors.surfaceVariant,
        animationSpec = tween(1000),
    )
    val animatedContainer by animateColorAsState(
        targetValue = colors.primaryContainer,
        animationSpec = tween(1000),
    )
    val animatedPrimary by animateColorAsState(
        targetValue = colors.primary,
        animationSpec = tween(1000),
    )

    //calcular el color per la animacio amb lerp (interpolacio lineal) per barrejar els 2 colors animats
    val animatedPastel = lerp(animatedSurface, animatedContainer, 0.5f)

    // pantalla full screen
    Box(modifier = Modifier.fillMaxSize().background(animatedPastel)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), //per que no es quedi pillada l'animacio afegim scroll
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //img album
            ImgAlbum(song)
            //Titol i artista
            TitolArtista(song, colors)

            // 4. Slider i Temps
            Slider(duration,currentPosition,onSeek,isPlaying,colors)
            Box(
                modifier = Modifier
                    .weight(0.9f)
            ){
                Lyrics(song,currentPosition, onLyricsDownloaded = {lyrics ->
                    scope.launch(Dispatchers.IO) {
                        songDao.updateLyrics(song.id, lyrics)
                    }},
                    onSeek = { milisegundos ->
                    // moure canço al temps de la lletra
                    onSeek(milisegundos)}, colors = colors)
            }
            //Botons de control
            BarraBotons(onPrevious,onPlayPause,isPlaying,onNext,colors.primary)
        }
    }
}
@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onPlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, currentPosition: Long, duration: Long) {
    val colors = agafarEsquemaColors(song.albumId)
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(colors.primary.copy(alpha = 0.15f))
    ) {
        // barra de progres al fons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(colors.primary.copy(alpha = 0.2f))
                .align(Alignment.BottomStart)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .background(colors.primary)
                .align(Alignment.BottomStart)
        )

        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = getAlbumArtUri(song.albumId), // funcio per agafar img
                contentDescription = "album img",
                contentScale = Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                error = rememberVectorPainter(Icons.Rounded.MusicNote), // si falla o no te imatge
                placeholder = rememberVectorPainter(Icons.Rounded.MusicNote) //mentre carrega
            )

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = song.title, maxLines = 1, color = Color.White,
                    modifier = Modifier.basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2500))
                Text(text = song.artist, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f))
            }

            IconButton(onClick = onPrevious) {
                //icona boto
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            // boto play/pause destacat amb el color de l'album
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    //icona boto
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
            }

            IconButton(onClick = onNext) {
                //icona boto
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
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
                    artist = cursor.getString(artistCol).let {if (it == "<unknown>") "" else it
                    },
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

//fiquem Supress pq no plori
@SuppressLint("DefaultLocale")
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun agafarEsquemaColors(albumId: Long): ColorScheme {
    val context = LocalContext.current
    val albumArtUri = getAlbumArtUri(albumId)

    //color que volem, gris per si falles
    var targetColor by remember { mutableStateOf(Color.Gray) }

    LaunchedEffect(albumId) {
        val loader = coil.Coil.imageLoader(context)
        val request = coil.request.ImageRequest.Builder(context)
            .data(albumArtUri)
            .allowHardware(false) //necesari per que palette pugui llegir els pixels del bitmap
            .build()

        val result = loader.execute(request)
        if (result is coil.request.SuccessResult) {
            val bitmap = (result.drawable as BitmapDrawable).bitmap
            //palette analitza la imatge
            Palette.from(bitmap).generate { palette ->
                // busquem el color vibrant, sino el dominant i sino el gris
                val rgb = palette?.getVibrantColor(
                    palette.getDominantColor(Color.Gray.toArgb())
                ) ?: Color.Gray.toArgb()
                targetColor = Color(rgb) // li assignem el color a target color
            }
        }
    }

    //retornem el color de manera que torna tot, surface, primary... amb la llibreria de Material Kolor (aixis ens fa la paleta completa)
    return rememberDynamicColorScheme(
        seedColor = targetColor,
        isDark = false,
        style = PaletteStyle.TonalSpot
    )
}