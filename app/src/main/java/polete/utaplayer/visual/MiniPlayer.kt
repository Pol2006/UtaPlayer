package polete.utaplayer.visual

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import polete.utaplayer.dataclass.Song
import polete.utaplayer.utilitats.agafarEsquemaColors
import polete.utaplayer.utilitats.getAlbumArtUri

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    currentPosition: Long,
    duration: Long
) {
    val colors = agafarEsquemaColors(song.albumId)
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .background(colors.primary.copy(alpha = 0.5f))
            .navigationBarsPadding()
    ) {
        // barra de progres al fons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(colors.onPrimaryContainer.copy(alpha = 1f))
                .align(Alignment.TopStart)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .background(colors.primary)
                .align(Alignment.TopStart)
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = song.title,
                    maxLines = 1,
                    color = Color.White,
                    modifier = Modifier.basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2500)
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
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
                    .clickable { onPlayPause() }, contentAlignment = Alignment.Center
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
