package polete.utaplayer.visual

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import polete.utaplayer.dataclass.Song
import polete.utaplayer.utilitats.getAlbumArtUri

@Composable
fun ImgAlbum(song: Song) { //imatge del album
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
                contentDescription = "album img", //descripcio
                modifier = Modifier.fillMaxSize(), //perque ocupi tot el que pugi
                contentScale = Crop, // fem que agafi tot el lloc
                error = rememberVectorPainter(Icons.Rounded.MusicNote), // si falla o no te imatge
                placeholder = rememberVectorPainter(Icons.Rounded.MusicNote) //mentre carrega
            )
        }
    }
}
