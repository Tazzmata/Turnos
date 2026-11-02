# Quién trabaja (Android)

App y widget para ver qué turno trabaja cada día. Funciona sin internet y sin permiso de acceso a la red.

## Compilar con GitHub (sin instalar nada)
1. Crea un repositorio PRIVADO en GitHub. Contiene la clave de firma de la app.
2. Sube todo el contenido de esta carpeta, incluida `.github`.
3. En la pestaña Actions, abre "Compilar APK" y descarga `QuienTrabaja-apk` al terminar.

## Compilar con Android Studio
Abre esta carpeta y usa Build > Generate App Bundles or APKs > Build APK(s). Si eliges la variante `release`,
el APK queda en `app/build/outputs/apk/release/`.

## Instalar
Pasa el APK al móvil, ábrelo y permite instalar desde esa fuente. Conserva `app/quientrabaja.keystore`:
sin ese archivo, las actualizaciones no se instalarán encima de la versión anterior.
