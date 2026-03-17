package polete.utaplayer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
fun Slider(duration: Long, currentPosition: Long,onSeek: (Long) -> Unit,isPlaying: Boolean, color: Color){
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
            thumbColor = color,
            activeTrackColor = color,
            inactiveTrackColor = color.copy(alpha = 0.2f)),
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
        horizontalArrangement = Arrangement.SpaceBetween // Fica un a cada costat
    ) {
        Text(formatTime(currentPosition))
        Text(formatTime(duration))
    }
}

@Composable
fun TitolArtista(song: Song){
    //basicmarquee fa que es mogui el text si no quep en una linea, util per cuan el nom es molt llarg i no tenir que expandir amb maxLines ja que podria fer que les lyrics despres no es veiesin
    Text(song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier
        .fillMaxWidth(0.9f)
        .padding(start = 24.dp)
        .basicMarquee(Int.MAX_VALUE, repeatDelayMillis = 2000))
    Text(song.artist, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier
        .fillMaxWidth(0.9f)
        .padding(start = 24.dp))

}

@Composable
fun Lyrics(
    song: Song,
    currentPosition: Long,
    onLyricsDownloaded: (String) -> Unit,
    onSeek: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    //lyrics acutals //fem servir keys per si canvia la canço que s'actualitzin i no surti l'anterior
    var currentLyrics by remember(song.id, song.lyrics) { mutableStateOf(song.lyrics) }
    //busqueda manual
    var manualQuery by remember(song.id) { mutableStateOf("${song.title} ${song.artist}") }

    //resultat lyrics
    var llistaResultat by remember { mutableStateOf<List<LyricSearchResult>?>(null) }
    //per ficar simbol de carrega
    var isSearching by remember { mutableStateOf(false) }

    // estat per la previsualitzacio
    var seleccioPreview by remember { mutableStateOf<LyricSearchResult?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (currentLyrics == null) {
            //boto buscar lyrics
            Button(onClick = {
                isSearching = true
                //busquem en un fil secundari a .io (per no anar al main i saturar)
                scope.launch(Dispatchers.IO) {
                    try {
                        //obtenim la llista de lyrics
                        val response = RetrofitClient.instance.getLyricsLlista(manualQuery)
                        //tornem al main per dibuixarla
                        withContext(Dispatchers.Main) {
                            llistaResultat = response
                        }
                    } catch (e: Exception) {
                        //tornem al main
                        withContext(Dispatchers.Main) {
                            //diguem l'error
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        //treiem el simbol de carregant
                        withContext(Dispatchers.Main) { isSearching = false }
                    }
                }
            }) {
                if (isSearching) {
                    //cercle de carrega
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Buscar Lyrics")
                }
            }

            if (llistaResultat != null) {
                //alert dialog per obrir finestra emergent
                AlertDialog(
                    //per si toquem fora
                    onDismissRequest = { llistaResultat = null },
                    title = {
                        //camp de buscar
                        OutlinedTextField(
                            value = manualQuery,
                            onValueChange = { manualQuery = it },
                            label = { Text("Cercar lletra") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    //consulta fora del main thread un altre cop
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val response = RetrofitClient.instance.getLyricsLlista(manualQuery)
                                            //retornem la llista al main
                                            withContext(Dispatchers.Main) { llistaResultat = response }
                                            //ficat _ en canvi de e (quick fix de android studio) perque no mostri warning
                                        } catch (_: Exception) {
                                            withContext(Dispatchers.Main) {
                                                //per controlar si falles el internet o el servidor
                                                Toast.makeText(context, "Error de connexió", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }) {
                                    //icona per buscar
                                    Icon(Icons.Rounded.Search, contentDescription = "Buscar")
                                }
                            }
                        )
                    },
                    //mostrem la llista
                    text = {
                        val resultat = llistaResultat
                        //si no tenim resultats
                        if (resultat == null || resultat.isEmpty()) {
                            Text("No s'han trobat lyrics.", modifier = Modifier.padding(16.dp))
                        } else {
                            //els mostrem en una columna bonics amb un limit de mida de llista (visible, es pot fer scroll pero perque no ocupi tota la pantalla si hi han molts resultats)
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(resultat) { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { seleccioPreview = item } // Obre preview
                                            .padding(vertical = 12.dp, horizontal = 8.dp)
                                    ) {
                                        Text(text = item.trackName, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${item.artistName} • ${if (item.syncedLyrics != null) "Sincronitzada" else "No Sincronitzada"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (item.syncedLyrics != null) Color.Green else Color.Red
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { llistaResultat = null }) { Text("Tancar") }
                    }
                )
            }

            if (seleccioPreview != null) {
                AlertDialog(
                    //si cliquem fora o enrere
                    onDismissRequest = { seleccioPreview = null },
                    title = { Text("Previsualitzacio") },
                    text = {
                        Column {
                            Text(text = seleccioPreview!!.trackName, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    //provem de agafar primer les lletres sincronitzades, si falla agafem les planes, i si no un text buit per evitar errors
                                    text = seleccioPreview!!.syncedLyrics ?: seleccioPreview!!.plainLyrics ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            //agafem el text, primer si pot ser sincronitzat
                            val lletra = seleccioPreview?.syncedLyrics ?: seleccioPreview?.plainLyrics
                            if (lletra != null) {
                                //la guardem a la bdd
                                onLyricsDownloaded(lletra)
                                //actualitzem la variable local perque aixis refresca la pantalla
                                currentLyrics = lletra
                                //fiquem null els estats de busqueda aixis es tanquen
                                llistaResultat = null
                                seleccioPreview = null
                            }
                        }) {
                            Text("Importar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { seleccioPreview = null }) { Text("Tornar") }
                    }
                )
            }
        } else {
            //Karaoke (llibreria accompanist-lyrics-ui)
            val lyricsParser = remember { AutoParser.Builder().build() }
            val lyrics = remember(currentLyrics) {
                lyricsParser.parse(currentLyrics!!)
            }
            val listState = remember(song.id) { LazyListState() }
            val timeProvider by rememberUpdatedState(currentPosition.toInt())

            KaraokeLyricsView(
                listState = listState,
                lyrics = lyrics,
                currentPosition = { timeProvider },
                onLineClicked = { line -> onSeek(line.start.toLong()) },
                onLinePressed = { line -> onSeek(line.start.toLong()) },
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}
@Composable
fun BarraBotons(
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    isPlaying: Boolean,
    onNext: () -> Unit,
    color: Color
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
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.7f),
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

            // Play/Pause (sin borde propio, ya lo da el contenedor)
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
            IconButton(onClick = { }) {
                Icon(
                    Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}