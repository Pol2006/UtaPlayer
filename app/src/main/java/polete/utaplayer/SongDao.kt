package polete.utaplayer

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    //Per llegir bdd
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>
    //Per guardar
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)
    //Per netejar
    @Query("DELETE FROM songs")
    suspend fun deleteAll()
}