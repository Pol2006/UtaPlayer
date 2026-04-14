package polete.utaplayer.visual

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import polete.utaplayer.dataclass.Song

@Composable
fun QueueDialog(
    queue: List<Song>,           // La lista de canciones en cola
    currentSong: Song,           // La que suena ahora
    onSongClick: (Song) -> Unit, // Qué pasa al elegir una
    onDismiss: () -> Unit   ,     // Función para cerrar el diálogo
    shuffleEnabled: Boolean,
    colors: ColorScheme
) {
    AlertDialog(
        containerColor = colors.primaryContainer,
        titleContentColor = colors.onPrimaryContainer,
        onDismissRequest = onDismiss,
        confirmButton = { if (shuffleEnabled)
            Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Es realment aleatori!",
                modifier = Modifier.align(Alignment.Center),
                color = colors.onPrimaryContainer
            )
        }
        },
        title = { Text("Cua de reproducció") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(queue) { songInQueue ->
                    QueueMusic(
                        song = songInQueue,
                        currentSong = currentSong,
                        onSongClick = {
                            onSongClick(it)
                            onDismiss()

                        },
                        colors = colors

                    )
                }
            }
        }
    )
}