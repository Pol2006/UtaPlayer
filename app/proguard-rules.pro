# 1. Mantener tus modelos de datos (Crucial para Retrofit y Room)
# Esto evita que R8 cambie el nombre de los campos como 'trackName' o 'plainLyrics'
-keep class polete.utaplayer.api.LyricSearchResult { *; }
-keep class polete.utaplayer.bdd.Song { *; }

# 2. Mantener las interfaces de API y DAO
# Si R8 cambia el nombre de 'getLyricsLlista', Retrofit no sabrá qué URL llamar
-keep interface polete.utaplayer.api.LyricsApi { *; }
-keep interface polete.utaplayer.bdd.SongDao { *; }

# 3. Reglas para Retrofit y GSON
# Estas son estándar para que el motor de red no falle
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn retrofit2.**

# 4. Reglas para Room
# Evita que Room pierda la conexión con la base de datos
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.util.TableInfo$Column { *; }

# 5. Librerías de UI (Coil, SquigglySlider, Accompanist)
# Para que las animaciones y la carga de imágenes de álbum no fallen
-keep class coil.** { *; }
-keep class me.saket.squigglyslider.** { *; }
-keep class com.mocharealm.accompanist.** { *; }

# 6. Media3 / ExoPlayer
-keep class androidx.media3.** { *; }