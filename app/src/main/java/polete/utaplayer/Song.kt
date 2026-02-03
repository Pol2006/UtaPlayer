package polete.utaplayer

import androidx.room.*

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long, //ID
    val albumId: Long, //Per obtenir portada
    val title: String, //Titol
    val artist: String, //Nom Artista
    val data: String, //Ruta fitxer
    val duration: Int //Duracio en ms
)
