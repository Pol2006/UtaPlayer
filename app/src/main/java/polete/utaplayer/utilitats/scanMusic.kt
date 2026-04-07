package polete.utaplayer.utilitats

import android.content.Context
import android.media.MediaScannerConnection

//serveix per si afegeixes arxius nous i el mobil encara no ho ha procesat a la base de dades per forçar-ho
fun scanMusic(context: Context, onFinish: () -> Unit) {
    MediaScannerConnection.scanFile(
        context, arrayOf("/storage/emulated/0/Music"), //directori que mirarem
        null //es el tipus de fitxer, null fa que ho detecti el sistema sol
    ) { path, uri -> // aixo es per avisar cuan acabi de escanejar
        onFinish()
    }
}
