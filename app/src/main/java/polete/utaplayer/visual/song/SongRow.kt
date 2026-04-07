package polete.utaplayer.visual.song

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import polete.utaplayer.dataclass.Song
import polete.utaplayer.utilitats.agafarEsquemaColors
import polete.utaplayer.utilitats.getAlbumArtUri

@Composable
fun SongRow(song: Song, onSongClick: (Song) -> Unit, currentSong: Song?) {
    val colors = agafarEsquemaColors(song.albumId)

    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onSongClick(song) } // tornem canço
        .padding(4.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(color = if (song == currentSong) colors.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically

    ) {
        AsyncImage(
            model = getAlbumArtUri(song.albumId), // funcio per agafar img
            contentDescription = "album img",
            contentScale = Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            error = rememberVectorPainter(Icons.Rounded.MusicNote), // si falla o no te imatge
            placeholder = rememberVectorPainter(Icons.Rounded.MusicNote) //mentre carrega
        )
        Spacer(Modifier.padding(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2500)
            )
            if (song.artist != "") Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2000)
            )

        }

    }
}