# MegaSoccerPlusPro TV

App Android TV — navegador de streaming optimizado para pelotaalibre.su

---

## ¿Cómo obtener el APK? (sin instalar nada)

### Método 1: GitHub Actions (recomendado)

1. Ve a **github.com** → crea cuenta gratis si no tienes
2. Crea un **nuevo repositorio público** llamado `MegaSoccerPlusPro`
3. Sube todos estos archivos manteniendo la estructura de carpetas
4. Ve a la pestaña **Actions** de tu repo
5. Ejecuta el workflow **"Build MegaSoccerPlusPro APK"**
6. Cuando termine (aprox. 5 min), descarga el APK desde **Artifacts**

### Método 2: Android Studio local

1. Instala [Android Studio](https://developer.android.com/studio)
2. Abre este proyecto (File → Open)
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. El APK estará en `app/build/outputs/apk/`

---

## Instalar en Android TV

1. Activa **"Fuentes desconocidas"** en Ajustes → Seguridad del TV
2. Copia el APK a USB o usa apps como **Send Files to TV**
3. Instala con un explorador de archivos

---

## Características

- ✅ Modo incógnito (sin historial, sin cookies persistentes)
- ✅ Pantalla completa con modo inmersivo
- ✅ Soporte de video HLS/fullscreen
- ✅ Navegación por control remoto (D-pad)
- ✅ Aceleración por hardware
- ✅ Pantalla encendida durante reproducción
- ✅ Botón MENU del control remoto = recargar página
- ✅ Compatible Android TV 7.0+

---

## Controles remotos

| Botón | Acción |
|-------|--------|
| D-pad | Navegar por la página |
| OK/Enter | Seleccionar / Click |
| Back | Salir de fullscreen / Navegar atrás |
| Menu | Recargar página |
