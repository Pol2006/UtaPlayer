package polete.utaplayer.dataclass

data class SongOption(
    val text: String, //per menu de songrow de musica individual, nom de la opcio
    val onClick: (Song) -> Unit //accio
)