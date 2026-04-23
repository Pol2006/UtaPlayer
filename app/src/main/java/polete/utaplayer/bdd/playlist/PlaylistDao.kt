package polete.utaplayer.bdd.playlist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    //CREAR PLAYLIST
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    //ELIMINAR PLAYLIST
    @Query("DELETE FROM playlists WHERE playlistId = :id")
    suspend fun deletePlaylistById(id: Long)

    //AFEGIR CAÇO PLAYLIST
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(songRef: PlaylistSongRef)

    //OBTENIR PLAYLISTS
    @Transaction
    @Query("SELECT * FROM playlists")
    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<List<PlaylistWithSongs>>

    //ELIMINAR CANÇO DE PLAYLIST
    @Query("DELETE FROM playlist_song_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
}