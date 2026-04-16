package polete.utaplayer.visual

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import polete.utaplayer.bdd.Song

@Composable
fun TitolArtista(song: Song, colors: ColorScheme) {
    Titol(
        song = song,
        colors = colors,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(start = 24.dp)
    )
    Artista(
        song = song,
        colors = colors,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(start = 24.dp)
    )
}

@Composable
//Titol
fun Titol(song: Song, colors: ColorScheme, modifier: Modifier = Modifier) {
    Text(
        song.title, //nom canço
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = colors.onPrimaryContainer,
        modifier = modifier
            .basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2000)     //basicmarquee fa que es mogui el text si no quep en una linea
            .fillMaxWidth()
    )
}

@Composable
//Artista
fun Artista(song: Song, colors: ColorScheme, modifier: Modifier = Modifier) {
    Text(
        song.artist, //nom artista
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.onPrimaryContainer,
        modifier = modifier
    )
}