package polete.utaplayer.visual

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeBreathingDotsDefaults
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import polete.utaplayer.api.LyricSearchResult
import polete.utaplayer.api.RetrofitClient
import polete.utaplayer.dataclass.Song
import kotlin.collections.filter
import kotlin.text.lines

@Composable
//lyrics
fun Lyrics(
    song: Song,
    currentPosition: Long,
    onLyricsDownloaded: (String) -> Unit,
    onSeek: (Long) -> Unit,
    colors: ColorScheme,
    listState: LazyListState
) {
    //definim un fil
    val scope = rememberCoroutineScope()
    //li donem contexte
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

    // comprovem si les lyrics guardades son synced (format LRC comença per "[")
    // aixi evitem passar lyrics planes al parser de la llibreria i que peti
    val isSynced = remember(currentLyrics) {
        currentLyrics?.trimStart()?.startsWith("[") == true
    }


    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (currentLyrics == null) {
            //boto buscar lyrics
            IconButton(
                modifier = Modifier
                    .size(96.dp),
                onClick = {
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
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT)
                                    .show()
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
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 4.dp,
                        color = colors.onPrimaryContainer
                    )
                } else {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            if (llistaResultat != null) {
                //alert dialog per obrir finestra emergent
                AlertDialog(
                    //per si toquem fora
                    containerColor = colors.primaryContainer,
                    textContentColor = colors.onPrimaryContainer,
                    onDismissRequest = { llistaResultat = null },
                    title = {
                        //camp de buscar
                        OutlinedTextField(
                            value = manualQuery,
                            onValueChange = { manualQuery = it },
                            label = { Text("Cercar lletra") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.onPrimaryContainer,        // color text
                                unfocusedTextColor = colors.onPrimaryContainer,      // color text
                                focusedBorderColor = colors.onPrimaryContainer,      // color vora
                                unfocusedBorderColor = colors.onPrimaryContainer,    // color vora
                                unfocusedLabelColor = colors.onPrimaryContainer,       // color text d'adalt
                                focusedLabelColor = colors.onPrimaryContainer,       // color text d'adalt
                                cursorColor = colors.onPrimaryContainer              // color senyalador
                            ),
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
                                items(resultat.filter { it.syncedLyrics != null }) { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { seleccioPreview = item } // Obre preview
                                            .padding(vertical = 12.dp, horizontal = 8.dp)
                                    ) {
                                        Text(text = item.trackName, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = item.artistName,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(top = 8.dp),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { llistaResultat = null }) { Text("Tancar", color = colors.onPrimaryContainer) }
                        TextButton(onClick = {
                            //consulta fora del main thread un altre cop
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val response =
                                        RetrofitClient.instance.getLyricsLlista(manualQuery)
                                    //retornem la llista al main
                                    withContext(Dispatchers.Main) {
                                        llistaResultat = response
                                    }
                                    //ficat _ en canvi de e (quick fix de android studio) perque no mostri warning
                                } catch (_: Exception) {
                                    withContext(Dispatchers.Main) {
                                        //per controlar si falles el internet o el servidor
                                        Toast.makeText(
                                            context,
                                            "Error de connexió",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }) { Text("Buscar", color = colors.onPrimaryContainer) }
                    }
                )
            }

            if (seleccioPreview != null) {
                AlertDialog(
                    //si cliquem fora o enrere
                        containerColor = colors.primaryContainer,
                    textContentColor = colors.onPrimaryContainer,
                    onDismissRequest = { seleccioPreview = null },
                    title = { Text("Previsualitzacio", color = colors.onPrimaryContainer)},
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
                                    text = seleccioPreview!!.syncedLyrics
                                        ?: seleccioPreview!!.plainLyrics ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            //agafem nomes el text sincronitzat, mai el pla, per evitar passar lyrics sense timestamps al parser i que peti
                            val lletra = seleccioPreview?.syncedLyrics
                            if (lletra != null) {
                                //la guardem a la bdd
                                onLyricsDownloaded(lletra)
                                //actualitzem la variable local perque aixis refresca la pantalla
                                currentLyrics = lletra
                                //fiquem null els estats de busqueda aixis es tanquen
                                llistaResultat = null
                                seleccioPreview = null
                            }
                        },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryContainer,
                                contentColor = colors.onPrimaryContainer
                            )
                            ) {
                            Text("Importar", color = colors.onPrimaryContainer)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { seleccioPreview = null }) { Text("Tornar", color = colors.onPrimaryContainer) }
                    }

                )
            }
        } else {
            if (isSynced) {
                //Karaoke (llibreria accompanist-lyrics-ui)
                // netegem les lyrics abans de parsear
                //treiem linies  nomes tenen timestamp sense text (ex: "[00:08.57]")

                val cleanedLyrics = remember(currentLyrics) {
                    currentLyrics!!
                        .lines()
                        .filter { line ->
                            val stripped =
                                line.replace(Regex("\\[\\d{2}:\\d{2}\\.\\d{2}]"), "").trim()
                            stripped.isNotEmpty() && stripped != "♪"
                        }
                        .distinctBy { line ->
                            Regex("\\[\\d{2}:\\d{2}\\.\\d{2}]").find(line)?.value ?: line
                        }
                        .joinToString("\n")
                }

                val lyricsParser = remember { AutoParser.Builder().build() } //dependencia llibreria
                val lyrics = remember(currentLyrics) {
                    lyricsParser.parse(cleanedLyrics)
                }
                val timeProvider by rememberUpdatedState(currentPosition.toInt())
                //llibreria acommpanist-ui
                KaraokeLyricsView(
                    listState = listState,
                    lyrics = lyrics,
                    currentPosition = { timeProvider },
                    onLineClicked = { line -> onSeek(line.start.toLong()) },
                    onLinePressed = { line -> onSeek(line.start.toLong()) },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    textColor = colors.onPrimaryContainer,
                    blendMode = BlendMode.SrcOver,
                    breathingDotsDefaults = KaraokeBreathingDotsDefaults(
                        breathingDotsColor = colors.onPrimaryContainer
                    )

                )
            }
        }
    }
}