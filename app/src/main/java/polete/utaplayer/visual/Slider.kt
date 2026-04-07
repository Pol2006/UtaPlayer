package polete.utaplayer.visual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.saket.squiggles.SquigglySlider
import polete.utaplayer.utilitats.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slider( //barra de temps
    duration: Long, //duracio
    currentPosition: Long, //posicio actual
    onSeek: (Long) -> Unit, //on cliquem (per moure temps)
    isPlaying: Boolean, //si esta reproduint
    colors: ColorScheme //esquema de colors per seguir la paleta
) {
    //es per calcular el progress i es necesari per si es 0 que no doni error
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

    SquigglySlider(
        value = progress,
        // cuan cliquem anem al nou lloc clicat
        onValueChange = { newProgress ->
            val newTime = (newProgress * duration).toLong()
            onSeek(newTime)
        },
        //colors palette
        colors = SliderDefaults.colors(
            thumbColor = colors.onPrimaryContainer,
            activeTrackColor = colors.onPrimaryContainer,
            inactiveTrackColor = colors.onPrimaryContainer.copy(alpha = 0.2f)
        ),
        modifier = Modifier //per fer mes gran la barra
            .padding(horizontal = 24.dp),
        squigglesSpec = SquigglySlider.SquigglesSpec(
            strokeWidth = 6.dp,
            amplitude = if (isPlaying) 4.dp else 0.dp,
        )

    )
    Row( //Temps de canço
        modifier = Modifier
            .fillMaxWidth(0.8f),
        horizontalArrangement = Arrangement.SpaceBetween // Fica un a cada costat
    ) {
        Text( //temps actual
            formatTime(currentPosition),
            color = colors.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
        Text( //duracio total
            formatTime(duration),
            color = colors.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}
