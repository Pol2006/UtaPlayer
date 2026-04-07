package polete.utaplayer

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle.Companion.light
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import polete.utaplayer.bdd.AppDatabase
import polete.utaplayer.dataclass.Song
import polete.utaplayer.visual.MiniPlayer
import polete.utaplayer.ui.theme.UtaplayerTheme
import polete.utaplayer.utilitats.AudioPlayerService
import polete.utaplayer.utilitats.fetchSongs
import polete.utaplayer.utilitats.scanMusic
import polete.utaplayer.utilitats.toMediaItem
import polete.utaplayer.visual.PantallaPermisos
import polete.utaplayer.visual.PlayerFullScreen
import polete.utaplayer.visual.album.AlbumsTab
import polete.utaplayer.visual.song.SongsTab

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = light(
                android.graphics.Color.TRANSPARENT, // Color de fons
                android.graphics.Color.TRANSPARENT  // Color de control de contrast
            )
        )
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
                    }
                    )
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UtaPlayerApp() {
    val context = LocalContext.current

    //declarem base de dades
    val database = remember { AppDatabase.getDatabase(context) }
    //declarem el dao
    val songDao = remember { database.songDao() }
    //Serveix per mirar la base de dades en temps real
    val songList by songDao.getAllSongs().collectAsState(emptyList())
    //canço actual
    var currentSong by remember { mutableStateOf<Song?>(null) }
    //saber si esta sonant o no
    var isPlaying by remember { mutableStateOf(false) }
    //posicio actual canço
    var currentPosition by remember { mutableLongStateOf(0L) }
    //duracio canço
    var duration by remember { mutableLongStateOf(0L) }
    //pantalla completa
    var isFullScreen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var mediaItemsLoaded by remember { mutableStateOf(false) }
    // Creem un token d'identificacio per conectarnos a AudioPlayerService que fa que el controlador sapiga les ordres de la notificacio i reproduccio
    val sessionToken = remember { SessionToken(context, ComponentName(context, AudioPlayerService::class.java)) }
    //serveix per comunicar la interficie de la notificacio, es null pero al ser mutable, cuan canvii automaticament s'actualitzara
    var controller by remember { mutableStateOf<MediaController?>(null) }
    //controla la conexio amb el reproductor en segon pla
    val controllerPeticio = remember { MediaController.Builder(context, sessionToken).buildAsync() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Albums")
    var selectedAlbumId by remember { mutableStateOf<Long?>(null) }

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
                songList.map { it.toMediaItem() }
            }
            currentController.setMediaItems(mediaItems)
        } else {
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
                currentPosition = controller?.currentPosition ?: 0L
                duration = controller?.duration?.coerceAtLeast(0L) ?: 0L // fer que minim sigui 0
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

            // detecta canvi de canço
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
        containerColor = Color.Transparent, bottomBar = {
            /// nomes si no estem en pantalla completa i hi ha una cançó
            if (!isFullScreen && currentSong != null) {

                Surface(
                    modifier = Modifier
                        // afegim el gest de lliscar
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                // dragAmount es per detectar que el dit puja cap adalt
                                if (dragAmount < -15) {
                                    isFullScreen = true
                                }
                            }
                        }
                        .clickable { isFullScreen = true }, color = Color.Transparent
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
        }) { padding ->

        // barra superior per triar entre albums i cançons
        Surface {
            Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.titleMedium) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> SongsTab(
                        songList = songList,
                        currentSong = currentSong,
                        padding = padding,
                        onSongClick = { cancoClicada ->
                            currentSong = cancoClicada
                            controller?.setMediaItems(songList.map { it.toMediaItem() })
                            val index = songList.indexOf(cancoClicada)
                            if (index != -1) {
                                controller?.seekTo(index, 0L)
                                controller?.prepare()
                                controller?.play()
                            }
                        }
                    )

                    1 -> {
                        if (selectedAlbumId != null) {
                            val albumSongs = songList.filter { it.albumId == selectedAlbumId }

                            BackHandler {
                                selectedAlbumId = null
                            }

                            SongsTab(
                                songList = albumSongs,
                                currentSong = currentSong,
                                padding = padding,
                                onSongClick = { cancoClicada ->
                                    currentSong = cancoClicada
                                    controller?.setMediaItems(albumSongs.map { it.toMediaItem() })

                                    val index = albumSongs.indexOf(cancoClicada)
                                    if (index != -1) {
                                        controller?.seekTo(index, 0L)
                                        controller?.prepare()
                                        controller?.play()
                                    }
                                }
                            )
                        } else {
                            AlbumsTab(
                                songList = songList,
                                padding = padding,
                                onAlbumClick = { albumId ->
                                    selectedAlbumId = albumId
                                }
                            )
                        }
                    }
                }

            }
        }



        //fullscreen
        AnimatedVisibility(
            visible = isFullScreen && currentSong != null, enter = slideInVertically(
                initialOffsetY = { it }, //  comença desde abaix de tot
                animationSpec = tween(durationMillis = 400) // duracio de pujada
            ) + fadeIn(), exit = slideOutVertically(
                targetOffsetY = { it }, // per baixar
                animationSpec = tween(durationMillis = 400) // duracio baixada
            ) + fadeOut()
        ) {
            BackHandler(enabled = isFullScreen) {
                isFullScreen = false
            }
            PlayerFullScreen(
                song = currentSong!!,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPause = { if (isPlaying) controller?.pause() else controller?.play() },
                onSeek = {
                    currentPosition = it
                    controller?.seekTo(it) /*per anar al ms que toquin*/
                },
                onNext = { controller?.seekToNext() },
                onPrevious = { controller?.seekToPrevious() },
                songDao = songDao,
                scope = scope,
                onClose = { isFullScreen = false }

            )
        }
    }
}
