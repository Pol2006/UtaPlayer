package polete.utaplayer.bdd.playlist

import androidx.room.*
import polete.utaplayer.bdd.Song


//serveix per fer "multiconsulta" unint ho a un sol objecte de kotlin i li permet consultar la seva info i la llista song que te dintre
data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<Song>
)