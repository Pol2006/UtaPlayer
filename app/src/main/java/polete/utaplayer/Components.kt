package polete.utaplayer

import android.widget.Button
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.saket.squiggles.SquigglySlider

@Composable
fun ImgAlbum(song: Song){
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f)
            .padding(24.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 12.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = getAlbumArtUri(song.albumId), // funcio per agafar img
                contentDescription = "album img",
                modifier = Modifier.fillMaxSize(),
                contentScale = Crop, // fem que agafi tot el lloc
                error = rememberVectorPainter(Icons.Rounded.MusicNote) , // si falla o no te imatge
                placeholder = rememberVectorPainter(Icons.Rounded.MusicNote) //mentre carrega
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slider(duration: Long, currentPosition: Long,onSeek: (Long) -> Unit,isPlaying: Boolean){
    //es per calcular el progress i es necesari per si es 0 que no doni error
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

    SquigglySlider(
        value = progress,
        // cuan cliquem anem al nou lloc clicat
        onValueChange = { newProgress ->
            val newTime = (newProgress * duration).toLong()
            onSeek(newTime)
        },
        colors = SliderDefaults.colors( //TODO: Ficar colors de album
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier //per fer mes gran el en si la barra
            .padding(horizontal = 24.dp),
        squigglesSpec = SquigglySlider.SquigglesSpec(
            strokeWidth = 6.dp,
            amplitude = if (isPlaying) 4.dp else 0.dp,
        )

    )
    Row(
        modifier = Modifier
            .fillMaxWidth(0.8f),
        horizontalArrangement = Arrangement.SpaceBetween // Esto empuja uno a cada lado
    ) {
        Text(formatTime(currentPosition))
        Text(formatTime(duration))
    }
}

@Composable
fun TitolArtista(song: Song){
    //basicmarquee fa que es mogui el text si no quep en una linea, util per cuan el nom es molt llarg i no tenir que expandir amb maxLines ja que podria fer que les lyrics despres no es veiesin
    Text(song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.9f).padding(start = 24.dp).basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2000))
    Text(song.artist, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.9f).padding(start = 24.dp))

}

@Composable
fun Lyrics(lyrics: Boolean){
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(start = 24.dp, end = 24.dp)

    ) {
        if (!lyrics)
            Button(onClick = TODO("Buscar lyrics")){
                Text("Buscar Lyrics")
            }
    }
}

@Composable
fun BarraBotons(onPrevious: () -> Unit, onPlayPause: () -> Unit, isPlaying: Boolean, onNext: () -> Unit){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, //ajustar tot per igual
        verticalAlignment = Alignment.CenterVertically
    ) {
        //aleatori
        IconButton(onClick = {/*TODO*/}) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        //anterior
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = null, modifier = Modifier.size(36.dp))
        }

        //playpause
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }

        //seguent
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(36.dp))
        }

        //bucle TODO:Un cop feta la logica canviar icona un cop seleccionat
        IconButton(onClick = {}) {
            Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }

}