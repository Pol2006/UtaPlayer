package polete.utaplayer.visual.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import polete.utaplayer.dataclass.Album
import polete.utaplayer.bdd.Song
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun AlbumsTab(songList: List<Song>, padding: PaddingValues, onAlbumClick: (Long) -> Unit) {
    val albums = remember(songList) {
        songList.groupBy { it.albumId }.map { (albumId, songs) ->
            Album(
                albumId = albumId,
                name = songs.first().album,
                artist = songs.first().artist,
                songCount = songs.size
            )
        }.sortedBy { it.artist }
    }
    //es per mostrar els albums de 2 en 2
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 8.dp, end = 8.dp,
            bottom = padding.calculateBottomPadding() + 80.dp //perque el ultim album no el tapi miniplayer
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),//separacio albums
        verticalArrangement = Arrangement.spacedBy(8.dp), //separacio albums
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { it.albumId }) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album.albumId) }
            )
        }
    }
}
