# Ubuntu Touch Launcher

Bienvenido a **Ubuntu Touch Launcher**.

Este proyecto permite cambiar el launcher predeterminado del sistema por una interfaz inspirada en **Ubuntu Touch**, ofreciendo una experiencia alternativa a la interfaz estándar de Android.

## Cómo usarlo

1. Instala la aplicación.
2. Sigue las recomendaciones de la configuración inicial.
3. Concede los permisos necesarios.

## Permisos obligatorios

Necesarios para que el launcher funcione correctamente:

- Establecerlo como launcher predeterminado.
- Acceso a ajustes restringidos (en algunos dispositivos).
- Mostrar sobre otras aplicaciones.
- Modificar los ajustes del sistema.
- Acceso a las notificaciones.

## Permisos opcionales

Se utilizan únicamente si el sistema no permite la navegación por gestos al cambiar el launcher predeterminado:

- Servicio de accesibilidad.

## Ocultar la barra de navegación

Para dispositivos que **no permiten la navegación por gestos** al cambiar el launcher predeterminado.

**ATENCIÓN:** En dispositivos **sin root**, este proceso **no es persistente** y puede volver al estado predeterminado del sistema en cualquier momento.

### Requisitos

- Termux instalado y configurado.
- Shizuku instalado y configurado.
- Creación de un script para ocultar la barra de navegación.

### Script para ocultar la barra de navegación

Guarda el archivo con extensión `.sh` y ejecútalo utilizando Shizuku mediante Termux o ADB.

```sh
#!/system/bin/sh

TARGET_MODE=2
SLEEP_TIME=1

apply_nav() {
    settings put secure navigation_mode $TARGET_MODE
    settings put global policy_control immersive.navigation=*
}

while true; do
    CURRENT_MODE=$(settings get secure navigation_mode)

    if [ "$CURRENT_MODE" != "$TARGET_MODE" ]; then
        apply_nav
    fi

    sleep $SLEEP_TIME
done
```

## Funciones del launcher

- Dock lateral que abre el cajón de aplicaciones.
- Cajón de aplicaciones.
- Dock secundario que se abre sobre cualquier pantalla.
- Cajón secundario que se abre sobre cualquier pantalla.
- Compatibilidad con navegación por gestos.
- Opciones para cambiar el fondo de pantalla.
- Widgets personalizados.
- Compatibilidad con iconos personalizados.
- Opciones para cambiar la forma de los iconos.
- Opción para ocultar aplicaciones del cajón.

## Contribuir al proyecto

Si este proyecto te ha resultado útil, considera apoyar su desarrollo para garantizar su mantenimiento y futuras actualizaciones.

### Donaciones mediante PayPal

Próximamente.

### Donaciones mediante Bitcoin

**Dirección BTC:**

```text
dapperparent02@walletofsatoshi.com
```

## Licencia

Este proyecto está publicado bajo la **licencia MIT**.

## Compilar el APK

El proyecto utiliza Android Gradle Plugin 8.2.2, Kotlin 1.9.22 y Java 17.

Para compilarlo desde Android Studio, abre esta carpeta como proyecto Gradle y ejecuta la tarea `:app:assembleDebug`.

También se incluye un flujo de GitHub Actions (`.github/workflows/build-apk.yml`) que compila el APK de depuración y lo publica como artefacto al ejecutar el flujo o subir cambios a las ramas `main` o `master`.
