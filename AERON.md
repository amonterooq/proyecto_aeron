
# AERON - Simulador de Aeropuerto

**Asignatura:** Programación Concurrente y Distribuida  
**Proyecto:** AERON (Aeropuerto Extremeño de Regulación y Operaciones de Navegación)  
**Entrega:** Práctica 1 - Entendimiento del sistema y primeros pasos

---

# ✈️ Práctica 1: Iniciación del proyecto

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

# ✈️ Práctica 2: Organizando el Aeropuerto

**Entrega:** Práctica 2 - Organización, Estados y Protocolo de Torre
**Objetivo:** Establecer un protocolo estricto de comunicación entre los aviones y la torre de control, introduciendo colas de espera y gestión centralizada de estados, manteniendo la ejecución secuencial.

---

## 📋 1. Cambios Principales y Nuevos Requisitos

En esta fase, el aeropuerto deja de ser una gestión directa de recursos para convertirse en un sistema organizado basado en **Peticiones (Requests)**.

### Novedades implementadas:
1.  **Estados de Vuelo Detallados:** Se ha ampliado el ciclo de vida del avión con estados específicos (`LANDING_REQUEST`, `LANDING_ASSIGNED`, `BOARDING`, etc.) para un control exhaustivo.
2.  **Cola de Peticiones (FIFO):** La torre ya no atiende "al vuelo". Las peticiones se almacenan en una cola y son procesadas ordenadamente.
3.  **El "Operario":** Se simula la figura de un operario (`OP-001`) dentro de la torre que desencola las peticiones y asigna los recursos.
4.  **Centralización:** El avión ya no cambia su propio estado; solicita acciones a la torre y esta es quien autoriza y actualiza el estado del avión y el panel de vuelos.

---

## 🛠️ 2. Actualización de Arquitectura

Se han modificado las clases existentes y añadido nuevas estructuras para soportar el protocolo de comunicación.

### 🆕 Nueva Clase: `Request.java`
Representa una solicitud o notificación enviada por un avión a la torre.
* **Atributos:** Avión emisor y Tipo de petición (`LANDING`, `TAKEOFF`, `BOARDING`, etc.).

### 🔄 Modificaciones en `ControlTower`
* **Cola:** Implementa una `Queue<Request>` para almacenar solicitudes entrantes.
* **Lógica del Operario:** Método `processNextRequest()` que simula al operario recogiendo la siguiente tarea y ejecutando la lógica de asignación de pistas/puertas.
* **Gestión de Estados:** Es la responsable exclusiva de actualizar el `FlightStatus` de los aviones.

### 🔄 Modificaciones en `Airplane`
* **Simplificación:** El avión pierde autonomía de decisión. Su ciclo `runSequential()` ahora consiste en enviar objetos `Request` a la torre y esperar (simuladamente) a que se le asignen recursos.

### 📋 Nuevos Estados (`FlightStatus`)
El ciclo de vida ahora cubre estrictamente el flujo:
`IN_FLIGHT` → `LANDING_REQUEST` → `LANDING_ASSIGNED` → `LANDING` → `LANDED` → `BOARDING` → `BOARDED` → `TAKEOFF_REQUESTED` → `TAKEOFF_ASSIGNED` → `DEPARTING` → `DEPARTED`.

---

## 🚀 3. Ejecución y Logs (Práctica 2)

El sistema de logs se ha actualizado para reflejar el comportamiento del operario y el panel de vuelos, tal y como se especifica en el anexo de formatos.

### Flujo de Log Típico
1.  **Avión:** "Solicita aterrizaje..."
2.  **Torre:** "Recibida solicitud..." (Entra en cola)
3.  **Torre:** "Operario [OP-001] ha cogido una petición..." (Sale de cola)
4.  **Torre:** "Procesando... Asignando Pista X..."
5.  **Panel:** Actualización de estado visual.

### Instrucciones
Ejecutar la misma clase principal: `aeronpcd.driver.AirportDriver`.

**Salida Esperada:**
```text
Torre de control
  Recibida la solicitud de aterrizaje de Avión [IBE-001 - IN_FLIGHT]
Torre de control
  Operario [OP-001] ha cogido una petición de tipo LANDING...
Torre de control
  Avión [IBE-001] autorizado para aterrizar en Pista [PIS1]
Panel de vuelos
  Flight: IBE-001 Status: LANDING_ASSIGNED
```

---

## ✅ 4. Estado del Proyecto

- [x] Protocolo de comunicación robusto (Request/Response).
- [x] Gestión de recursos (Pistas/Puertas) sin conflictos lógicos.
- [x] Definir clases base.
- [x] Sistema de logs detallado cumpliendo normativa.
- [x] Estructura preparada para la fase final: Concurrencia con Hilos.

---

# 👷 Práctica 3: Torre de Control Concurrente (Multi-Operario)

**Entrega:** Práctica 3 - Concurrencia en la Torre y Sincronización
**Objetivo:** Maximizar la eficiencia de la torre de control implementando paralelismo mediante un pool de operarios (Hilos) que procesan las peticiones de los aviones de forma concurrente.

---

## 📋 1. Cambios Principales

El cambio radical en esta práctica es la **introducción de hilos (Threads)** dentro de la `ControlTower`.
Anteriormente, la torre procesaba las peticiones en el mismo hilo de ejecución que el avión. Ahora, la torre y el avión son entidades desacopladas que se sincronizan mediante señales.

### Novedades implementadas:
1.  **Pool de Operarios:** La torre dispone ahora de **5 Operarios (Hilos)** que trabajan en paralelo.
2.  **Productor-Consumidor:** Se ha implementado este patrón clásico.
    * **Productor:** El Avión (genera peticiones).
    * **Consumidor:** Los Operarios (procesan peticiones).
    * **Buffer:** La `requestQueue`.
3.  **Sincronización `wait()` / `notify()`:** El avión se bloquea (se duerme) tras enviar una petición y solo despierta cuando un operario específico le notifica que su solicitud ha sido procesada.

---

## 🛠️ 2. Actualización de Arquitectura

### 🔄 Modificaciones en `ControlTower`
* **Clase Interna `Operator`:** Extiende de `Thread`. Representa a un trabajador que cicla infinitamente buscando trabajo en la cola.
* **Sincronización:**
    * Uso de bloques `synchronized(requestQueue)` para proteger el acceso a la cola compartida.
    * Uso de `requestQueue.wait()` para que los operarios duerman si no hay trabajo.
    * Uso de `requestQueue.notifyAll()` cuando entra una nueva petición.

### 🔄 Modificaciones en `Airplane`
* **Método `sendRequestAndWait()`:** Sustituye a las llamadas directas. Envía la petición y ejecuta inmediatamente `this.wait()`, deteniendo la ejecución del avión hasta recibir respuesta.
* **Eliminación de esperas activas:** El avión ya no hace "polling" (preguntar todo el rato), sino que espera pasivamente una señal.

---

## 🚀 3. Ejecución y Logs (Práctica 3)

Se observa en los logs cómo diferentes operarios (`OP-1`, `OP-5`, etc.) atienden a los aviones, demostrando la concurrencia en el lado del servidor (Torre).

**Nota:** Aunque la torre es concurrente, en esta práctica los aviones se siguen lanzando secuencialmente desde el `main` para verificar que la sincronización no rompe el flujo lógico.

### Ejemplo de Salida
```text
Aviones
  Avión [IBE-001] Solicita aterrizaje y ESPERA...  <-- El avión se bloquea
Torre de control
  Operario [OP-1] ha cogido petición LANDING...    <-- Un hilo operario trabaja
Torre de control
  Avión [IBE-001] autorizado aterrizar...
Aviones
  Avión [IBE-001] Recibe luz verde...              <-- El avión se desbloquea
```

---

## ✅ 4. Estado del Proyecto
Con la Práctica 3 completada, disponemos de:
- [x] Torre de control Multihilo (5 Operarios).
- [x] Acceso seguro a recursos compartidos (Pistas/Puertas/Cola) mediante synchronized.
- [x] Comunicación eficiente entre hilos (sin esperas activas).

---

---

# 🎮 Práctica 4: Asignación de Recursos y Estado Global

**Entrega:** Práctica 4 - Gestión de Recursos y Visualización de Estado
**Objetivo:** Implementar la lógica de asignación y liberación de recursos (Pistas y Puertas) en la Torre de Control, garantizando que ninguna petición se pierda y visualizando el estado del aeropuerto en tiempo real.

---

## 📋 1. Lógica de Asignación (Torre de Control)

Se ha completado la lógica del método `processRequestLogic` en la clase `ControlTower`. Ahora, los operarios verifican la disponibilidad física de los recursos antes de conceder permisos.

### Reglas Implementadas:
1.  **LANDING (Aterrizaje):** Requiere **1 Pista** Y **1 Puerta** libres simultáneamente.
    * *Si hay recursos:* Se marcan como ocupados y se notifica al avión.
    * *Si NO hay recursos:* La petición **vuelve a la cola** (`re-encolado`). Esto evita el bloqueo del operario y permite que procese otras peticiones (como liberaciones) que podrían generar los recursos necesarios.
2.  **TAKEOFF (Despegue):** Requiere **1 Pista** libre.
3.  **Liberación:** Los eventos `LANDED`, `BOARDED` y `DEPARTED` liberan los recursos correspondientes inmediatamente.

---

## 🎨 2. Visualización (Monitor de Estado)

[cite_start]Se ha integrado la clase `AirportState` con la interfaz gráfica para cumplir con el requisito de mostrar el estado de las pistas, puertas y cola[cite: 50, 53].

### Mejoras en la GUI (`AirportWindow`):
* **División de Pantalla:** Se ha separado la ventana en dos paneles: "Registro de Eventos" (Logs) y "Estado del Aeropuerto" (Gráfico).
* **Soporte HTML:** Se utiliza un `JTextPane` para renderizar colores reales, permitiendo distinguir visualmente los estados:
    * <span style="color:green">●</span> (Verde): Recurso Libre.
    * <span style="color:red">●</span> (Rojo): Recurso Ocupado.

---

## ✅ 3. Estado del Proyecto

Con la Práctica 4 completada:
- [x] Lógica de asignación de recursos segura (evita interbloqueos por reencolado).
- [x] Visualización gráfica del estado de los 20 aviones y recursos.
- [x] Gestión completa del ciclo de vida de los aviones.

---

---

# 📊 Práctica 5: Panel de Vuelos Persistente (JSON)

**Entrega:** Práctica 5 - Persistencia de Datos y Panel de Vuelos Real
**Objetivo:** Modernizar el sistema de información al pasajero, sustituyendo los logs efímeros por un sistema de persistencia en archivo (`JSON`) que refleje el estado de los aviones en tiempo real.

---

## 📋 1. Nueva Funcionalidad: Panel de Vuelos IT

Se ha implementado un mecanismo de persistencia que simula una base de datos en tiempo real. Ahora, cada cambio de estado en un avión se vuelca inmediatamente a un fichero `flight_board.json`.

### Componentes:
1.  **Clase `FlightPanel`:**
    * Actúa como un gestor de datos estático y thread-safe (seguro para hilos).
    * Mantiene un mapa en memoria (`ConcurrentHashMap`) con el último estado conocido de cada avión.
    * Método `saveToJson()`: Cada vez que hay una actualización, reescribe el archivo `flight_board.json` con el formato estándar clave-valor.

2.  **Integración en `ControlTower`:**
    * Se han inyectado llamadas a `FlightPanel.updateStatus(id, estado)` en todos los puntos críticos del flujo (asignación de aterrizaje, despegue, etc.).

---

## 🚀 2. Resultado y Verificación

Al ejecutar el simulador, se genera dinámicamente el archivo `flight_board.json` en la raíz del proyecto. Este archivo puede ser consumido por sistemas externos (como una supuesta pantalla en la terminal o una app móvil).

### Ejemplo de contenido generado (`flight_board.json`):
```json
{
  "IBE-001": "DEPARTED",
  "RYR-002": "LANDING_ASSIGNED"
}
```
---
## ✅ 3. Estado del Proyecto

Con la Práctica 5 completada:
- [x] Persistencia de datos en tiempo real.
- [x] Desacoplamiento entre la lógica de control y la visualización de datos.
- [x] Preparación para la integración final y estadísticas.

---