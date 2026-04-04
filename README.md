# WiFi Insight

Aplicación Android profesional para análisis y gestión de redes WiFi con arquitectura MVVM, Material 3 y todas las capacidades modernas de acceso WiFi en Android 10+.

## Características

### Core Features
- **Escaneo de redes WiFi** en tiempo real con lista dinámica
- **Visualización de señal** con indicadores coloridos (RSSI, porcentaje, nivel)
- **Información de red actual**: SSID, IP, velocidad de enlace, RSSI en tiempo real
- **Monitor de conectividad** usando ConnectivityManager
- **Diagnóstico avanzado**: clasificación de calidad de red y recomendaciones
- **Conexión a redes** usando WifiNetworkSpecifier/Suggestion API

### UI/UX Premium
- **Material 3 Design** con tema personalizado profesional
- **Dark/Light mode** automático y manual
- **Animaciones suaves**: radar de escaneo, transiciones entre pantallas
- **Estados vacíos** diseñados con feedback claro
- **Loading states** con indicadores visuales

### Extras
- **Gráfica de señal** en tiempo real con historial de 50 puntos
- **Animación de radar** durante el escaneo
- **Tarjetas dashboard** con información clara
- **Historial de redes** detectadas durante la sesión

## Arquitectura

```
com.example.wifiinsight/
├── data/
│   ├── model/           # WifiNetwork, ConnectionState, SignalLevel, NetworkQuality
│   ├── repository/      # WifiRepository, WifiRepositoryImpl
│   └── local/           # (Room opcional para persistencia)
├── domain/
│   ├── usecase/         # Scan, Connect, GetCurrentConnection, MonitorSignal
│   └── util/            # PermissionHelper, SignalCalculator
├── presentation/
│   ├── common/          # Componentes reutilizables, tema
│   ├── screens/         # Home, Scan, Detail screens con ViewModels
│   └── navigation/      # AppNavigation, Screen routes
└── MainActivity.kt
```

### Patrones utilizados
- **MVVM**: ViewModels con StateFlow para estados UI
- **Repository Pattern**: Abstracción de fuentes de datos
- **Clean Architecture**: Separación de concerns en capas
- **StateFlow**: Para streams reactivos de datos
- **Coroutines**: Operaciones asíncronas sin bloquear UI

## Tecnologías

- **Jetpack Compose**: UI declarativa moderna
- **Material 3**: Componentes de diseño actualizados
- **Navigation Compose**: Navegación entre pantallas
- **Coroutines & Flow**: Programación asíncrona reactiva
- **Accompanist Permissions**: Manejo de permisos runtime
- **Android WiFi APIs**: WifiManager, ConnectivityManager

## Permisos

```xml
<!-- Android 13+ -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />

<!-- Android 9-12 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- WiFi State -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```

## Pantallas

### Home (Dashboard)
- Red WiFi conectada actual
- IP, velocidad de enlace, RSSI
- Gráfica de señal en tiempo real
- Botón rápido para escanear

### Scan (Escanear)
- Animación de radar mientras escanea
- Lista de redes con SSID, RSSI, seguridad
- Ordenamiento por intensidad de señal
- Tap para ver detalles

### Detail (Detalles)
- Información completa de la red
- Indicador de calidad visual grande
- BSSID, frecuencia, canal, seguridad
- Input de contraseña para redes protegidas
- Botón de conexión

## Compatibilidad

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 16)
- **Compatibilidad WiFi**: Android 10+ APIs modernas con fallback

## Configuración del Proyecto

### 1. Clonar y abrir
```bash
# Abrir en Android Studio y sincronizar Gradle
```

### 2. Compilar
```bash
./gradlew :app:assembleDebug
```

### 3. Ejecutar
- Conectar dispositivo Android o usar emulador
- Click en "Run" en Android Studio

## Estructura de Archivos Principales

| Archivo | Descripción |
|---------|-------------|
| `MainActivity.kt` | Entry point, manejo de permisos |
| `data/model/*.kt` | Modelos de datos |
| `data/repository/WifiRepositoryImpl.kt` | Lógica WiFi principal |
| `domain/usecase/*.kt` | Casos de uso |
| `presentation/screens/*` | UI con Compose |
| `presentation/navigation/*.kt` | Navegación |

## Manejo de Errores

- **Permisos denegados**: UI explicativa con botón para configuración
- **WiFi desactivado**: Estado vacío con botón para activar
- **Sin redes**: Mensaje informativo con opción de reescanear
- **Errores de conexión**: Snackbars con mensajes claros

## Seguridad

- No se almacenan contraseñas en la app
- Uso de sistema de sugerencias de WiFi nativo
- Permisos just-in-time con explicaciones

## Próximas Mejoras

- [ ] Persistencia de historial con Room
- [ ] Gráficas más detalladas (velocidad, latencia)
- [ ] Mapa de calor de señal
- [ ] Exportar datos de escaneo
- [ ] Widget de escritorio

## Licencia

MIT License - Libre uso para proyectos personales y comerciales.
