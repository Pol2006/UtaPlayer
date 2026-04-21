package polete.utaplayer.bdd.playlist

import androidx.room.Entity
import androidx.room.ForeignKey
import polete.utaplayer.bdd.Song

@Entity(
    tableName = "playlist_song_ref",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE // si es borra la llista, tot lo relacionat tambe perque no quedi per alla
        ),
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE // si es borra la canço, tambe desapareix de la llista
        )
    ]
)
data class PlaylistSongRef(
    val playlistId: Long,
    val songId: Long,
    val songOrder: Int
)