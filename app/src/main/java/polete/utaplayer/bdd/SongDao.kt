package polete.utaplayer.bdd

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import polete.utaplayer.bdd.Song

@Dao
interface SongDao {
    //Per llegir bdd
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>
    //Per guardar
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<Song>)
    //Per borrar una
    @Delete
    suspend fun deleteSong(song: Song)
    //Per netejar
    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("DELETE FROM songs WHERE id NOT IN (:currentIds)")
    suspend fun deleteOldSongs(currentIds: List<Long>)

    @Query("UPDATE songs SET lyrics = :lyrics WHERE id = :id")
    suspend fun updateLyrics(id: Long, lyrics: String)

    @Query("UPDATE songs SET lyrics = NULL WHERE id = :id")
    suspend fun deleteLyrics(id: Long)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getCount(): Int
    @Transaction
    suspend fun syncSongs(songs: List<Song>) {
        // obtenim el que te el movil ara
        val ids = songs.map { it.id }

        //borrem el que ja no tenim
        deleteOldSongs(ids)

        //insertem el que si tenim
        insertSongs(songs)
    }
}