package polete.utaplayer.visual

import android.graphics.fonts.Font
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaPermisos(onGrantClick: () -> Unit) {
    Surface(modifier = Modifier.background(MaterialTheme.colorScheme.surface).navigationBarsPadding().statusBarsPadding().padding(24.dp)){
        Column {
            Text("Benvingut a", style = MaterialTheme.typography.titleLarge, fontSize = 52.sp, color = MaterialTheme.colorScheme.primary)
            Text("UTA Player", style = MaterialTheme.typography.titleLarge, fontSize = 52.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(top = 56.dp))
            Text("Tip: Recorda tenir la teva musica guardada a la carpeta per defecte del dispositiu ( /Music )", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                        Button(onClick = onGrantClick, Modifier.align(Alignment.Center)) { Text("Donar permís per veure la música", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                    }

            }

        }

    }
