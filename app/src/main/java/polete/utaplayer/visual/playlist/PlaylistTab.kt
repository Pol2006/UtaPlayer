package polete.utaplayer.visual.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import polete.utaplayer.bdd.Song
import polete.utaplayer.bdd.playlist.PlaylistDao
import polete.utaplayer.utilitats.getAlbumArtUri
import kotlin.collections.take

@Composable
fun PlaylistTab(playlistDao: PlaylistDao, onPlaylistClick: (Long) -> Unit, scope: CoroutineScope){

    val playlists by playlistDao.getAllPlaylistsWithSongs().collectAsState(emptyList())
    var showDialog by remember {(mutableStateOf(false))}
    var currentplaylist by remember { mutableStateOf<Long?>(null) }
    Surface(Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth().fillMaxHeight().padding(top = 8.dp)) {

        if(playlists.isEmpty())
            Box(Modifier.fillMaxWidth().fillMaxHeight())
            {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,


                    )
                    Text("No hi ha cap playlist")

                }
            }
        LazyColumn() { items(playlists, key = { it.playlist.playlistId }) {playlist ->


                Box(Modifier.padding(start = 4.dp, end = 4.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).fillMaxWidth().clickable(onClick = { onPlaylistClick(playlist.playlist.playlistId) })) {
                    Row(Modifier.height(128.dp)) {
                        PlaylistCover(playlist.songs)
                        Text(
                            text = playlist.playlist.name.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2500)
                                .align(Alignment.CenterVertically)
                                .padding(start = 12.dp)
                                .weight(1f)
                        )
                        IconButton(onClick = {showDialog = !showDialog
                                             currentplaylist = playlist.playlist.playlistId}, Modifier.padding(end = 8.dp).align(Alignment.CenterVertically)) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }


                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (showDialog && currentplaylist != null) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    currentplaylist = null
                },
                title = {
                    Text(text = "Confirmar Eliminació")
                },
                text = {
                    Text(text = "Segur que vols eliminar la playlist?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val idToDelete = currentplaylist
                            if(idToDelete != null){
                                scope.launch(Dispatchers.IO){
                                    playlistDao.deletePlaylistById(idToDelete)
                                }
                            }
                            showDialog = false
                            currentplaylist = null
                        }
                    ) {
                        Text("Eliminar", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false
                            currentplaylist = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

    }
}

@Composable
fun PlaylistCover(songs: List<Song>) {
    Box(
        modifier = Modifier
            .size(128.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val coverSongs = songs.take(4)

        // organitza els elements en files i columnes automaticament per fer mosaic de les miniatures
        FlowRow(
            maxItemsInEachRow = 2
        ) {
            coverSongs.forEach { song ->
                AsyncImage(
                    model = getAlbumArtUri(song.albumId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}