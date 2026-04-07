package polete.utaplayer.utilitats

import androidx.core.net.toUri

fun getAlbumArtUri(albumId: Long): android.net.Uri {
    return android.content.ContentUris.withAppendedId(
        "content://media/external/audio/albumart".toUri(), albumId
    )
}