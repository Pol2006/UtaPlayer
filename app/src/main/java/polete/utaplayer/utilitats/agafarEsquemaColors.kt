package polete.utaplayer.utilitats

import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun agafarEsquemaColors(albumId: Long): ColorScheme {
    val context = LocalContext.current
    val albumArtUri = getAlbumArtUri(albumId)

    //color que volem, gris per si falles
    var targetColor by remember { mutableStateOf(Color.Gray) }

    LaunchedEffect(albumId) {
        val loader = coil.Coil.imageLoader(context)
        val request = coil.request.ImageRequest.Builder(context).data(albumArtUri)
            .allowHardware(false) //necesari per que palette pugui llegir els pixels del bitmap
            .build()

        val result = loader.execute(request)
        if (result is coil.request.SuccessResult) {
            val bitmap = (result.drawable as BitmapDrawable).bitmap
            //palette analitza la imatge
            Palette.from(bitmap).generate { palette ->
                // busquem el color vibrant, sino el dominant i sino el gris
                val rgb = palette?.getVibrantColor(
                    palette.getDominantColor(Color.Gray.toArgb())
                ) ?: Color.Gray.toArgb()
                targetColor = Color(rgb) // li assignem el color a target color
            }
        }
    }

    //retornem el color de manera que torna tot, surface, primary... amb la llibreria de Material Kolor (aixis ens fa la paleta completa)
    return rememberDynamicColorScheme(
        seedColor = targetColor, isDark = false, style = PaletteStyle.TonalSpot
    )
}