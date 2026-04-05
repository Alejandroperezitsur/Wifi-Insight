# WiFi Insight - QA Checklist de Hardening

> Checklist de pruebas para validar el hardening profesional de WiFi Insight.

## Pre-requisitos

- [ ] Dispositivo Android físico (emulador tiene limitaciones WiFi)
- [ ] Android Studio con SDK 24-36 instalado
- [ ] Red WiFi de prueba disponible (2.4GHz y/o 5GHz)
- [ ] Permisos de ubicación concedidos

---

## 1. Conexión WiFi REAL (CRÍTICO)

### 1.1 WifiNetworkSpecifier
- [ ] Conectar a red abierta (sin contraseña)
  - Resultado esperado: Conexión exitosa, estado muestra "Internet OK" o estado real
  - Logs deben mostrar: "✓ Conectado exitosamente a [SSID]"
  
- [ ] Conectar a red WPA2 con contraseña correcta
  - Resultado esperado: Conexión exitosa
  - Timeout de 30s debe funcionar
  
- [ ] Conectar a red WPA2 con contraseña incorrecta
  - Resultado esperado: "No se pudo conectar. Verifica la contraseña..."
  - Logs deben mostrar: "✗ Conexión no disponible"
  
- [ ] Cancelar conexión en progreso
  - Resultado esperado: Callback cancelado limpiamente, no leaks

### 1.2 Estados de Conexión
- [ ] Verificar onAvailable() - conexión exitosa
- [ ] Verificar onLost() - perder conexión (apagar router momentáneamente)
- [ ] Verificar onUnavailable() - intento fallido

---

## 2. Network Capabilities (NIVEL SENIOR)

### 2.1 Detección de Internet Real
- [ ] Conectar a red CON internet
  - UI debe mostrar: "Internet OK" (verde)
  - ConnectionState: isValidated=true, hasInternet=true
  
- [ ] Conectar a red SIN internet (router sin uplink)
  - UI debe mostrar: "Conectado (sin internet)" (gris)
  - ConnectionState: hasInternet=false
  
- [ ] Conectar a portal cautivo (hotel/aeropuerto simulado)
  - UI debe mostrar: "Conectado (sin validar)" (naranja)
  - ConnectionState: hasInternet=true, isValidated=false

### 2.2 Capabilities Detectados
- [ ] NET_CAPABILITY_INTERNET presente en red con internet
- [ ] NET_CAPABILITY_VALIDATED presente después de validación
- [ ] TRANSPORT_WIFI siempre presente en conexiones WiFi

---

## 3. Manejo de Limitaciones Android

### 3.1 Throttling de Scan
- [ ] Realizar 5 escaneos rápidos (< 30 segundos entre ellos)
  - Escaneos 1-4: Deben funcionar
  - Escaneo 5+: Debe mostrar cache o mensaje de throttling
  - Logs deben mostrar: "Scan throttling activo. Tiempo restante: Xs"
  
- [ ] Verificar mensaje de throttling visible al usuario

### 3.2 Fallback de Cache
- [ ] Forzar throttling y verificar que se retorna cache
- [ ] Verificar que historial se mantiene entre intentos

### 3.3 Errores de Scan
- [ ] Denegar permiso de ubicación durante escaneo
  - Debe mostrar: "Permisos de ubicación requeridos..."
  
- [ ] Desactivar ubicación del sistema durante escaneo
  - Debe mostrar: "Ubicación desactivada..."

---

## 4. Edge Cases Completos

### 4.1 WiFi Desactivado
- [ ] Intentar escanear con WiFi apagado
  - UI debe mostrar estado vacío con botón "Activar WiFi"
  - Mensaje: "WiFi está desactivado"

### 4.2 Modo Avión
- [ ] Activar modo avión e intentar escanear
  - Debe detectar: "Modo avión activado"
  - No debe crashear

### 4.3 Permisos Denegados
- [ ] Denegar NEARBY_WIFI_DEVICES (Android 13+)
  - Mensaje explicativo debe aparecer
  - Botón para ir a configuración
  
- [ ] Denegar ACCESS_FINE_LOCATION (Android < 13)
  - Mensaje explicativo sobre ubicación

### 4.4 Sin DHCP/IP
- [ ] Conectar a red sin servidor DHCP
  - Debe mostrar IP como null o "Unknown"
  - No debe crashear

---

## 5. Diagnóstico Avanzado Real

### 5.1 SignalCalculator
- [ ] Verificar conversión RSSI a porcentaje no lineal
  - -50 dBm → ~100%
  - -70 dBm → ~50%
  - -90 dBm → ~10%
  
- [ ] Verificar cálculo de distancia
  - 2.4GHz: Distancia razonable estimada
  - 5GHz: Distancia más corta que 2.4GHz a igual RSSI

### 5.2 Análisis de Estabilidad
- [ ] Señal estable: CV < 0.15, clasificación GOOD/EXCELLENT
- [ ] Señal inestable: Variar posición del dispositivo
  - CV > 0.30 debe clasificar como UNSTABLE
  - Recomendación debe mencionar inestabilidad

### 5.3 Recomendaciones Contextuales
- [ ] Señal EXCELLENT + estable → "Ideal para streaming 4K"
- [ ] Señal FAIR + degradándose → "Considera acercarte al router"
- [ ] Señal UNSTABLE → "Posible interferencia"

---

## 6. Monitoreo Continuo

### 6.1 NetworkCallback Lifecycle
- [ ] Iniciar app → Registrar callback
- [ ] Cambiar de red → Detectar cambio en UI
- [ ] Cerrar app → Desregistrar callback (verificar en logs)

### 6.2 Sin Memory Leaks
- [ ] Rotar pantalla 10 veces
- [ ] Verificar que no hay múltiples callbacks activos

---

## 7. Logging Profesional

### 7.1 TAGs Correctos
- [ ] WiFiRepository: Todos los logs usan TAG = "WiFiRepository"
- [ ] SignalCalculator: Logs de análisis de estabilidad

### 7.2 Niveles de Log Apropiados
- [ ] Log.d: Flujo normal (escaneos, conexiones)
- [ ] Log.i: Eventos importantes (conexión exitosa)
- [ ] Log.w: Advertencias (throttling, errores recuperables)
- [ ] Log.e: Errores (excepciones, fallos de conexión)

---

## 8. UI/UX Mejoras

### 8.1 Indicadores de Estado
- [ ] Verde + CheckCircle: Internet validado
- [ ] Naranja + Warning: Conectado sin validar
- [ ] Gris + Error: Conectado sin internet
- [ ] Gris: Desconectado

### 8.2 Mensajes Humanos
- [ ] "Conectado a RedX (Internet OK)"
- [ ] "Conectado a RedX (sin validar) - Posible portal cautivo"
- [ ] "Conectado a RedX (sin internet)"
- [ ] "WiFi desactivado - Activa el WiFi"

---

## Resultados Esperados

### Métricas de Éxito
- **0 crashes** durante todas las pruebas
- **100% detección** de estados de internet real
- **< 2s** tiempo de respuesta para cambios de red
- **0 memory leaks** detectados en profiler

### Logs de Éxito (ejemplo)
```
D/WiFiRepository: Iniciando escaneo de redes...
I/WiFiRepository: Escaneo completado: 8 redes encontradas
D/WiFiRepository: Estado de red: WiFi=true, Internet=true, Validated=true, IP=192.168.1.100
I/WiFiRepository: ✓ Conectado exitosamente a MiRedWiFi
```

---

## Notas para Desarrolladores

### Limitaciones Conocidas de Android
1. **Android 10+**: No permite activar WiFi programáticamente
2. **Android 9+**: Throttling de scan (4 scans / 2 minutos foreground)
3. **Android 8+**: Requiere permiso de ubicación para escanear
4. **Android 12+**: NEARBY_WIFI_DEVICES opcional con neverForLocation

### Workarounds Implementados
- Cache de resultados de escaneo para throttling
- Validación de estado de ubicación del sistema
- Manejo de modo avión
- Graceful degradation cuando APIs fallan
