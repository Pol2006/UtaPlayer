package polete.utaplayer.utilitats

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import polete.utaplayer.dataclass.Song

//FUNCIO PER COMPARTIR MUSICA AMB NEARBY SHARE
fun compartirMusica(context: Context, song: Song) {
    val musicUri: Uri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        song.id
    )

    //intent per enviar la canço amb nearby share
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        val clip = ClipData.newRawUri(song.title, musicUri)
        clipData = clip
    }

    val chooser = Intent.createChooser(shareIntent, song.title)
    context.startActivity(chooser)
}