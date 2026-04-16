package polete.utaplayer.bdd

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long, //ID
    val albumId: Long, //Per obtenir portada
    val album: String = "", //Nom album
    val title: String, //Titol
    val artist: String, //Nom Artista
    val data: String, //Ruta fitxer
    val duration: Int, //Duracio en ms
    val lyrics: String? = null //lyrics
)