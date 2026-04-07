package polete.utaplayer.visual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import polete.utaplayer.dataclass.Song
import polete.utaplayer.bdd.SongDao
import polete.utaplayer.utilitats.agafarEsquemaColors

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
    songDao: SongDao, scope: CoroutineScope, onClose: () -> Unit

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

    //calcular el color per la animacio amb lerp (interpolacio lineal) per barrejar els 2 colors animats
    val animatedPastel = lerp(animatedSurface, animatedContainer, 0.5f)


    var karaokeFullScreen by remember { mutableStateOf(false) }

    val lyricsListState = remember(song.id, karaokeFullScreen) { LazyListState() }

    // pantalla full screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedPastel)
            .navigationBarsPadding()
            .statusBarsPadding()
            .clickable(enabled = false) {}
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            AnimatedVisibility(visible = !karaokeFullScreen) {


                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                if (dragAmount > 65) {
                                    onClose()
                                }
                            }
                        }) {
                    //img album
                    ImgAlbum(song)
                    //Titol i artista
                    TitolArtista(song, colors)
                }
            }

            // 4. Slider i Temps
            Slider(duration, currentPosition, onSeek, isPlaying, colors)
            Box(
                modifier = Modifier.weight(0.9f)
            ) {
                Lyrics(
                    song, currentPosition, onLyricsDownloaded = { lyrics ->
                        scope.launch(Dispatchers.IO) {
                            songDao.updateLyrics(song.id, lyrics)
                        }
                    }, onSeek = { milisegundos ->
                        // moure canço al temps de la lletra
                        onSeek(milisegundos)
                    }, colors = colors, listState = lyricsListState
                )
            }
            //Botons de control
            BarraBotons(onPrevious, onPlayPause, isPlaying, onNext, colors.primary)
        }

        //tancar (flexeta avall)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .padding(8.dp)
                .size(48.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Tancar",
                tint = colors.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

        if(karaokeFullScreen){
            Titol(
                song = song,
                colors = colors,
                modifier = Modifier
                    .padding(top = 16.dp, start = 48.dp, end = 48.dp) // Espai per no xocar amb les icones dels costats
                    .align(Alignment.TopCenter) // Posicionament dins del Box
            )
            IconButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        // Esborrem les lletres de la base de dades
                        songDao.deleteLyrics(song.id)
                        // Opcional: pots forçar una recàrrega aquí si la teva lògica de lletres ho requereix
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd) // El posem a baix a la dreta
                    .navigationBarsPadding() // Perquè no quedi tapat per la barra de navegació
                    .padding(bottom = 85.dp) // Pujem una mica perquè no toqui els botons de control (Play/Next)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete, // O Icons.Rounded.Delete
                    contentDescription = "Esborrar lletres",
                    tint = colors.onSurface.copy(alpha = 0.6f), // Una mica transparent perquè no distregui
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        //obrir lyrics
        IconButton(
            onClick = { karaokeFullScreen = !karaokeFullScreen },
            modifier = Modifier
                .padding(8.dp)
                .size(48.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = "Lyrics",
                tint = colors.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

    }

}
