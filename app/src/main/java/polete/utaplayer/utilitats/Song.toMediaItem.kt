package polete.utaplayer.utilitats

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import polete.utaplayer.bdd.Song

//converteix el objecte canço a un mediaitem reproduible per exoplayer
//Song.toMediaItem es per no modificar song pero afegir-ho
fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(data.toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(getAlbumArtUri(albumId))
                .build()
        ).build()
}
