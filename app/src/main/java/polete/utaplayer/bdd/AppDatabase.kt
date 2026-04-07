package polete.utaplayer.bdd

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import polete.utaplayer.dataclass.Song

@Database(entities = [Song::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    //Accedir a les comandes sql
    abstract fun songDao(): SongDao
    companion object {
        @Volatile //Es perque ho vegin tots els fils disponibles
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            //si ja existeix retorna la instancia, sino la crea.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "uta_player_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}