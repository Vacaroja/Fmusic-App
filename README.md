# 📱 FMusic App - Control Remoto Android

> **Aplicación móvil nativa en Kotlin para el control remoto de FMusic API en red local (LAN).**

FMusic App es una aplicación moderna desarrollada con **Jetpack Compose** y **Material 3** que te permite controlar la reproducción de YouTube Music desde tu teléfono Android, conectándose de forma directa y segura al servidor backend `fmusic-api`.

---

## ✨ Características

- 🎨 **Interfaz Moderna e Intuitiva**: Visualización de carátula en alta calidad, barra de progreso en tiempo real y controles multimedia.
- 📜 **Explorador de Playlists y Álbumes**: Consulta todas las canciones de un álbum o playlist antes de reproducirla.
- ⚡ **Gestos y Acciones Rápidas**:
  - **Clic en la carátula**: Abre directamente la lista de canciones de la playlist o mix en reproducción activa.
  - **Clic normal en playlist**: Explora el listado de canciones en un menú desplegable.
  - **Long Press (Clic prolongado)**: Inicia la reproducción de la playlist completa de inmediato.
- 🔐 **Seguridad Cifrada**: Clave de API almacenada de forma segura en el dispositivo usando `EncryptedSharedPreferences` (cifrado AES-256).
- ⚙️ **Configuración Flexible**: Ajuste de la dirección IP del servidor, puerto y API Key desde la pantalla de ajustes de la app.

---

## 🛠️ Tecnologías y Librerías

- **Lenguaje**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Conectividad HTTP**: [Retrofit 2](https://square.github.io/retrofit/) & [OkHttpClient](https://square.github.io/okhttp/)
- **Seguridad**: [Jetpack Security (`security-crypto`)](https://developer.android.com/topic/security/crypto)
- **Carga de Imágenes**: [Coil Compose](https://coil-kt.github.io/coil/compose/)

---

## 📁 Estructura del Proyecto

```text
fmusic-android/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── app/
    ├── build.gradle.kts
    └── src/main/java/com/ccc/fmusic/
        ├── MainActivity.kt            # Punto de entrada de la aplicación
        ├── MusicControllerScreen.kt   # Pantalla principal del reproductor
        ├── SettingsScreen.kt          # Configuración de IP, Puerto y API Key
        ├── AppPreferences.kt          # Almacenamiento cifrado de credenciales
        └── RetrofitClient.kt          # Cliente HTTP con Interceptor de API Key
