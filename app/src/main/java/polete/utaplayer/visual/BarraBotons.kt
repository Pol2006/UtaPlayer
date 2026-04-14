package polete.utaplayer.visual

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOn
import androidx.compose.material.icons.rounded.RepeatOneOn
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BarraBotons(
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    isPlaying: Boolean,
    onNext: () -> Unit,
    color: Color,
    shuffle: () -> Unit,
    shuffleEnabled: Boolean,
    bucle: () -> Unit,
    bucleMode: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(32.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aleatori

            FilledIconButton(onClick = { shuffle() },
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if(shuffleEnabled) color.copy(alpha = 0.2f) else color.copy(alpha = 0f),
                )) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = if(shuffleEnabled) color else color.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Anterior
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Play/Pause
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = color.copy(alpha = 0.2f),
                    contentColor = color.copy(alpha = 0.9f)
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Següent
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Bucle
            IconButton(onClick = { bucle() }) {
                Icon(
                    imageVector = if(bucleMode == 0) Icons.Rounded.Repeat else if (bucleMode == 1) Icons.Rounded.RepeatOneOn else if (bucleMode == 2) Icons.Rounded.RepeatOn else Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

        }
    }
}