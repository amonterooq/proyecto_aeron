# MEMORIA DEL PROYECTO AERON
## Simulador Aeroportuario Concurrente
### Asignatura: Programación Concurrente y Distribuida

---

## 1. Introducción y Objetivos
El proyecto **AERON** (Aeropuerto Exremeño de Regulación y Operaciones de Navegación) tiene como objetivo la simulación completa del tráfico aéreo de un aeropuerto con recursos limitados. El sistema gestiona las operaciones de **20 aviones**, **3 pistas** de aterrizaje/despegue y **5 puertas de embarque**.

El núcleo del proyecto es la implementación de soluciones a problemas clásicos de concurrencia, utilizando mecanismos de sincronización en Java (Monitores, Wait/Notify) para garantizar la integridad de los datos y el correcto funcionamiento bajo alta demanda.

---

## 2. Arquitectura del Sistema
El proyecto sigue una arquitectura modular separada en paquetes funcionales:

| Paquete | Descripción | Clases Principales |
| :--- | :--- | :--- |
| `aeronpcd.driver` | Punto de entrada y configuración de la simulación. | `AirportDriver` |
| `aeronpcd.model` | Entidades lógicas del dominio. | `Airplane` (Hilo), `Request`, `FlightStatus` |
| `aeronpcd.tower` | Cerebro del sistema. Gestiona la concurrencia. | `ControlTower`, `Operator` (hilo interno) |
| `aeronpcd.resources` | Recursos físicos compartidos. | `Runway`, `Gate` |
| `aeronpcd.gui` | Interfaz gráfica de usuario. | `AirportWindow` |
| `aeronpcd.util` | Utilidades transversales. | `Logger`, `FlightPanel`, `AirportState` |

---

## 3. Implementación de la Concurrencia (Prácticas 3, 4 y 7)

### 3.1. Patrón Productor-Consumidor (Torre de Control)
Se ha implementado el patrón **Productor-Consumidor** para gestionar las solicitudes de los aviones.
*   **Productor (Avión):** Genera peticiones (`LANDING`, `TAKEOFF`, etc.) y las deposita en una cola compartida.
*   **Consumidor (Operario):** La `ControlTower` contiene un pool de **5 hilos operarios** que consumen peticiones de la cola y las procesan.

**Mecanismo de Sincronización:**
*   Se utiliza un bloque `synchronized(requestQueue)` para proteger el acceso a la cola.
*   Si la cola está vacía, los operarios ejecutan `wait()` para liberar la CPU.
*   Cuando entra una petición, se ejecuta `notifyAll()` para despertarlo a los operarios.

### 3.2. Sincronización de Aviones (Wait-Notify)
Los aviones son procesos independientes (Hilos) que no pueden avanzar sin autorización.
1.  El avión envía una petición (`sendRequestAndWait`).
2.  Inmediatamente entra en estado de espera pasiva (`this.wait()`).
3.  Solo cuando un operario procesa su petición y asigna recursos, ejecuta `plane.notify()` para despertarlo.

### 3.3. Exclusión Mutua en Recursos (Monitores)
Para evitar condiciones de carrera (ej. dos aviones aterrizando en la misma pista):
*   La lógica de asignación (`processRequestLogic`) está protegida por `synchronized(this)` en la Torre.
*   Esto garantiza que las operaciones de *Consultar Disponibilidad -> Ocupar Recurso* sean atómicas.

---

## 4. Gestión de Recursos y Estados

### Protocolo de Asignación
*   **Aterrizaje:** Requiere **1 Pista** libre Y **1 Puerta** libre. Si no hay ambos disponibles, la petición se re-encola (evitando bloqueo indefinido del operario).
*   **Despegue:** Requiere **1 Pista** libre.

### Persistencia de Estado (JSON) - Práctica 5
Para desacoplar la visualización de la lógica, se ha implementado `FlightPanel`. Un mapa concurrente (`ConcurrentHashMap`) mantiene el estado de cada vuelo y lo vuelca a disco (`flight_board.json`) en tiempo real, simulando un panel de información al pasajero.

---

## 5. Gestión de Errores y Robustez (Práctica 6)
El sistema implementa captura de excepciones específicas para garantizar estabilidad:
*   **Saturación:** Detección de cola llena (`MAX_QUEUE_SIZE`) con mensaje de advertencia.
*   **Fallo de I/O:** Control de errores en escritura de Logs y JSON.
*   **Recursos:** Mensajes específicos cuando no se pueden asignar pistas o puertas.

---

## 6. Manual de Ejecución

### Requisitos Previos
*   JDK 17 o superior.
*   Entorno capaz de compilar proyectos Java estándar.

### Pasos
1.  Compilar el proyecto.
2.  Ejecutar la clase principal: `aeronpcd.driver.AirportDriver`.
3.  Seguir el asistente en consola:
    *   **Modo:** Seleccionar `SECUENCIAL` (1) o `CONCURRENTE` (2).
    *   **Configuración:** Introducir número de Aviones, Pistas, Puertas y Operarios.

### Resultados
*   **Ventana Gráfica:** Muestra el log de eventos en tiempo real y el estado visual de pistas/puertas (Verde/Rojo).
*   **Consola:** Trazas de ejecución.
*   **Archivos Generados:**
    *   `logs/`: Carpeta con ficheros de log detallados (Timestamp y Hilo).
    *   `flight_board.json`: Estado actual de los vuelos.
    *   `aeropuerto_resumen.csv`: Informe final con tiempos y ranking.

---

## 7. Conclusiones
El proyecto AERON demuestra cómo la concurrencia bien aplicada permite gestionar eficientemente recursos limitados. La transición de un modelo secuencial a uno concurrente ha permitido:
1.  Aumentar el realismo de la simulación.
2.  Optimizar el tiempo total de operación (múltiples aviones operan en paralelo).
3.  Garantizar la integridad mediante monitores y sincronización correcta.

---
**Autor:** Ana Montero Preciado  
**Fecha:** Enero 2026
