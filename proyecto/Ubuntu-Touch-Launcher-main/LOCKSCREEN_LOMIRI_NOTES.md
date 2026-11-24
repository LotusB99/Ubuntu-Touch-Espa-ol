# Pantalla visual estilo Lomiri (Kotlin)

Esta etapa reemplaza la vista previa simple por una pantalla nativa Android/Kotlin con:
- composición centrada, tipografía ligera y reloj/fecha en español;
- fondo oscuro degradado y capa de oscurecimiento inspirada en `qml/Components/Lockscreen.qml`;
- gesto de deslizar hacia arriba para solicitar el desbloqueo del sistema;
- Android Keyguard como único responsable de PIN, patrón, contraseña y biometría.

## Diferencia importante con Lomiri original

El componente original `Lockscreen.qml` carga `PinLockscreen.qml` o `PassphraseLockscreen.qml`, y su teclado introduce una frase/PIN que el shell valida. Una aplicación launcher normal de Android no puede reemplazar de forma segura ese mecanismo. Por eso esta implementación **no dibuja un teclado PIN falso ni almacena códigos**: al deslizar, solicita a Android que muestre su autenticación real.

La actividad es una vista previa manual desde Ajustes. No se garantiza su lanzamiento automático al encender la pantalla: Android restringe que aplicaciones comunes abran actividades desde segundo plano y no permite que un launcher sustituya Keyguard.

## Automatic wake display (stage 3)
- The NavigationGestureService dynamically listens for `ACTION_SCREEN_ON` while the accessibility service is connected.
- When the screen turns on and Android reports Keyguard locked, it launches `LomiriLockScreenActivity` after a short debounce.
- This can be enabled/disabled in Launcher Settings with “Mostrar pantalla Lomiri automáticamente al encender la pantalla”. It is enabled by default.
- The feature requires the launcher's Accessibility Service to be enabled. Android/OEM background-activity-launch restrictions may prevent automatic presentation on some devices; this is best-effort, not a replacement for Keyguard.
- The native Android PIN, pattern, password, and biometric flow remains authoritative. No credentials are collected or stored by the launcher.
