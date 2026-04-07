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
import polete.utaplayer.dataclass.Song

@Composable
fun SongsTab(songList: List<Song>, currentSong: Song?, padding: PaddingValues, onSongClick: (Song) -> Unit){
    if (songList.isNotEmpty()) LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 80.dp
        )
    ) {
        items(songList, key = { it.id }) { canco ->
            SongRow(
                song = canco,
                onSongClick = onSongClick,
                currentSong = currentSong,
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