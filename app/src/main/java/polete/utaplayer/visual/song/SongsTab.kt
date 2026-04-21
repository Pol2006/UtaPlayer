package polete.utaplayer.visual.song

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import polete.utaplayer.bdd.SongDao
import polete.utaplayer.bdd.Song
import polete.utaplayer.bdd.playlist.Playlist
import polete.utaplayer.bdd.playlist.PlaylistDao
import polete.utaplayer.bdd.playlist.PlaylistWithSongs

@Composable
fun SongsTab(songList: List<Song>, currentSong: Song?, padding: PaddingValues, onSongClick: (Song) -> Unit, songDao: SongDao, scope: CoroutineScope, playlistDao: PlaylistDao, playlists: List<PlaylistWithSongs>){
    if (songList.isNotEmpty()) LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(
            top = 8.dp,
            bottom = padding.calculateBottomPadding() + 80.dp
        )
    ) {
        items(songList, key = { it.id }) { canco ->
            SongRow(
                song = canco,
                onSongClick = onSongClick,
                currentSong = currentSong,
                scope = scope,
                songDao = songDao,
                playlistDao = playlistDao,
                playlists = playlists
            )
        }
    }
    else {
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