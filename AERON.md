
# AERON - Simulador de Aeropuerto (Práctica 1)

**Asignatura:** Programación Concurrente y Distribuida  
**Proyecto:** AERON (Aeropuerto Extremeño de Regulación y Operaciones de Navegación)  
**Entrega:** Práctica 1 - Entendimiento del sistema y primeros pasos

---

## 📋 1. Descripción y Objetivo

El objetivo principal del proyecto AERON es simular el funcionamiento de un aeropuerto que gestiona **20 aviones**, **3 pistas** de aterrizaje/despegue y **5 puertas** de embarque.

En esta **Práctica 1**, el foco se ha centrado en sentar los cimientos del simulador sin concurrencia real.  
Se han implementado las clases base, la estructura del proyecto y, fundamentalmente, el sistema de **Logging** y la interfaz gráfica básica para visualizar los eventos, tal y como se especifica en los requisitos de entrega.

El sistema actual trabaja en **modo secuencial**, lo que permite validar correctamente el flujo lógico de los aviones antes de introducir concurrencia en prácticas posteriores.

---

## 🏗️ 2. Arquitectura y Organización del Proyecto

El proyecto se ha estructurado separando claramente:

- Lógica de negocio  
- Recursos físicos del aeropuerto  
- Control de operaciones  
- Interfaz gráfica  
- Utilidades transversales  

Esto facilita la mantenibilidad y la futura adaptación al modelo concurrente.

### 📁 Estructura de Directorios

```text
AERON_PROJECT/
├── src/
│   └── aeronpcd/
│       ├── driver/
│       │   └── AirportDriver.java
│       ├── gui/
│       │   └── AirportWindow.java
│       ├── model/
│       │   ├── Airplane.java
│       │   ├── Passenger.java
│       │   └── FlightStatus.java
│       ├── resources/
│       │   ├── Gate.java
│       │   └── Runway.java
│       ├── tower/
│       │   └── ControlTower.java
│       └── util/
│           ├── AirportState.java
│           └── Logger.java
├── logs/
│   ├── secuencial/
│   └── concurrent/
└── README.md
```

---

## 🛠️ 3. Detalles de Implementación (Modo Secuencial)

Para esta entrega se ha implementado exclusivamente el **Modo Secuencial**, donde los aviones realizan su ciclo de operaciones uno tras otro, sin solapamiento temporal ni ejecución paralela.

Esto permite:
- Verificar la lógica completa de cada avión.
- Depurar fácilmente errores.
- Asegurar que los recursos se asignan correctamente antes de introducir concurrencia.

### ✈️ Clases Principales

#### AirportDriver
Inicializa los recursos (3 pistas y 5 puertas), crea la torre de control y los aviones, y ejecuta su ciclo secuencialmente.

#### Airplane
Contiene el método `runSequential()` que ejecuta:
Solicitar pista → Aterrizar (100ms) → Ir a puerta → Embarcar → Solicitar despegue → Despegar (100ms).

#### ControlTower
Asigna pistas y puertas verificando disponibilidad con bucles simples.

#### Logger
Crea automáticamente `logs/secuencial/` y genera archivos con formato:
`aeron-MODO-TIMESTAMP.log`

#### AirportWindow
Ventana gráfica con `JFrame` y `JTextArea` que muestra eventos en tiempo real.

---

## 🚀 4. Instrucciones de Ejecución

1. Compilar todo el paquete `aeronpcd`.
2. Ejecutar:
   `aeronpcd.driver.AirportDriver`

Durante la ejecución:
- Se abre ventana gráfica.
- Se muestran eventos por consola.
- Se genera log en `logs/secuencial/`.

---

## 📄 Ejemplo de Salida

```text
=== INICIO SIMULACIÓN AEROPUERTO (MODO SECUENCIAL) ===
Avión [IBE-001 - IN_FLIGHT] Inicia ciclo.
Torre: Recibida solicitud de aterrizaje del avión IBE-001
...
Avión [IBE-001 - DEPARTED] Ciclo completado en 281 ms.
```

---

## ✅ 5. Checklist de Tareas

- [x] Crear repositorio y estructura.
- [x] Preparar rutas de logs.
- [x] Definir clases base.
- [x] Implementar Logger.
- [x] Implementar ventana.
- [x] Simulación secuencial por mensajes.

---

## 🔮 6. Futuro

Preparado para introducir concurrencia en siguientes prácticas.
