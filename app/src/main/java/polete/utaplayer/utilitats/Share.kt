package polete.utaplayer.utilitats

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import polete.utaplayer.bdd.Song

//FUNCIO PER COMPARTIR MUSICA AMB NEARBY SHARE
fun compartirMusica(context: Context, song: Song) {
    val musicUri: Uri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        song.id
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        //stream d'audio
        putExtra(Intent.EXTRA_STREAM, musicUri)
        //nom canço
        val clip = ClipData.newRawUri(song.title, musicUri)
        clipData = clip
        //sino no es veu el nom de el fitxer
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(shareIntent, song.title)
    context.startActivity(chooser)
}