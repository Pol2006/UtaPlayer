package polete.utaplayer.utilitats

import android.content.Context
import android.provider.MediaStore
import polete.utaplayer.bdd.Song

//extreure dades
fun fetchSongs(context: Context): List<Song> {
    val tempSongs = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION
    )
    val selection =
        "${MediaStore.Audio.Media.IS_MUSIC} = 1" //Ara nomes ens donara musica, no audios de whatsapp i altres coses


    context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val idAlbumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext()) {
            tempSongs.add(
                Song(
                    id = cursor.getLong(idCol),
                    albumId = cursor.getLong(idAlbumCol),
                    album = cursor.getString(albumCol) ?: "",
                    title = cursor.getString(titleCol),
                    artist = cursor.getString(artistCol).let {
                        if (it == "<unknown>") "" else it
                    },
                    data = cursor.getString(dataCol),
                    duration = cursor.getInt(durationCol)
                )
            )
        }
    }
    return tempSongs
}
