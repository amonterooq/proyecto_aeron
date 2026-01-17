# MEMORIA DEL PROYECTO AERON
## Simulador Aeroportuario Concurrente
### Asignatura: Programación Concurrente y Distribuida

---

## 1. Introducción y Objetivos
El proyecto **AERON** (Aeropuerto Exremeño de Regulación y Operaciones de Navegación) tiene como objetivo la simulación completa del tráfico aéreo de un aeropuerto con recursos limitados. El sistema gestiona las operaciones de **20 aviones**, **3 pistas** de aterrizaje/despegue y **5 puertas de embarque**.

El núcleo del proyecto es la implementación de soluciones a problemas clásicos de concurrencia, utilizando mecanismos de sincronización en Java (Monitores, Wait/Notify) para garantizar la integridad de los datos y el correcto funcionamiento bajo alta demanda.

---

## 2. Arquitectura y Constitución del Proyecto
El proyecto sigue una arquitectura modular separada en paquetes funcionales (MVC).

### 2.1. Organización de Carpetas
*   `src/`: Código fuente Java organizado en paquetes.
*   `bin/`: Bytecode compilado (.class).
*   `logs/`: Archivos de registro generados automáticamente (`secuencial/` o `concurrente/`).
*   `lib/`: Librerías externas.

### 2.2. Clases Principales y Responsabilidades

| Paquete | Descripción | Clases Principales |
| :--- | :--- | :--- |
| `aeronpcd.driver` | Punto de entrada y configuración. | `AirportDriver` (Main) |
| `aeronpcd.model` | Entidades lógicas del dominio. | `Airplane` (Hilo), `Request`, `FlightStatus` |
| `aeronpcd.tower` | Cerebro del sistema (Monitor). | `ControlTower`, `Operator` (hilo interno) |
| `aeronpcd.resources` | Recursos físicos compartidos. | `Runway`, `Gate` |
| `aeronpcd.gui` | Interfaz gráfica de usuario. | `AirportWindow` |
| `aeronpcd.util` | Utilidades transversales. | `Logger`, `FlightPanel`, `AirportState` |

---

## 3. Implementación de la Concurrencia (Semáforos y Monitores)

El proyecto resuelve la concurrencia utilizando exclusivamente **Monitores Java** (`synchronized`, `wait`, `notify/notifyAll`) para garantizar la seguridad y vivacidad.

### 3.1. Patrón Productor-Consumidor (Torre de Control)
Se ha implementado el patrón **Productor-Consumidor** para gestionar las solicitudes:
*   **Productor (Avión):** Genera peticiones (`LANDING`, `TAKEOFF`) y las deposita en la cola (`tower.addRequest`).
*   **Consumidor (Operario):** La `ControlTower` contiene un pool de **5 hilos operarios** que consumen peticiones.

**Mecanismo de Sincronización:**
*   **Monitor en Cola:** `synchronized(requestQueue)` (u objeto monitor equivalente) protege el acceso.
*   **Wait/Notify:** Si la cola está vacía, los operarios hacen `wait()`. Al entrar peticiones, se hace `notifyAll()`.

### 3.2. Sincronización de Aviones (Guarded Blocks)
Los aviones son procesos independientes (Hilos) que no pueden avanzar sin autorización.
*   **Dónde se aplica:** Clase `Airplane`, método `sendRequestAndWait`.
*   **Implementación:** Se usa un flag `volatile boolean resourceGranted` dentro de un bucle `synchronized`.
    ```java
    while (!resourceGranted) { wait(); }
    ```
    Esto evita el problema de las notificaciones perdidas.

### 3.3. Exclusión Mutua en Recursos
*   **Dónde se aplica:** Clase `ControlTower`, método `processRequest`.
*   **Implementación:** `synchronized(this)` garantiza que la comprobación de disponibilidad y la ocupación de pistas/puertas sea atómica.

### 3.4. Resolución de Problemas Clásicos
*   **Deadlock (Interbloqueo):** Resuelto mediante **Asignación Atómica**. El aterrizaje requiere 1 Pista Y 1 Puerta. Si no están ambas, no se coge ninguna y se pasa a la lista de espera.
*   **Nested Monitor (Inversión):** Resuelto extrayendo la llamada a la torre fuera del bloque sincronizado del avión.
*   **Starvation (Inanición):** Resuelto mediante una `waitingList` que se revisa prioritariamente cada vez que se liberan recursos.

---

## 4. Gestión de Recursos y Estados

### Protocolo de Asignación
*   **Aterrizaje:** Requiere **1 Pista** libre Y **1 Puerta** libre.
*   **Despegue:** Requiere **1 Pista** libre.

### Persistencia de Estado (JSON)
Para desacoplar la visualización de la lógica, se ha implementado `FlightPanel`. Un mapa concurrente (`ConcurrentHashMap`) mantiene el estado de cada vuelo y lo vuelca a disco (`flight_board.json`) en tiempo real.

---

## 5. Gestión de Errores y Robustez
El sistema implementa captura de excepciones específicas para garantizar estabilidad:
*   **Saturación:** Detección de cola llena (`MAX_QUEUE_SIZE`).
*   **Fallo de I/O:** Control de errores en escritura de Logs y JSON.
*   **Recursos:** Mensajes específicos cuando no se pueden asignar pistas o puertas.

---

## 6. Manual de Ejecución

### Requisitos Previos
*   JDK 17 o superior (Compilado con `--release 21`).

### Comandos de Compilación y Ejecución
Para ejecutar el proyecto desde la raíz (`path/to/proyecto_aeron`):

1.  **Compilar:**
    ```powershell
    javac --release 21 -d bin -cp src src/aeronpcd/driver/AirportDriver.java src/aeronpcd/gui/AirportWindow.java src/aeronpcd/model/Airplane.java src/aeronpcd/model/FlightStatus.java src/aeronpcd/model/Request.java src/aeronpcd/tower/ControlTower.java src/aeronpcd/resources/Gate.java src/aeronpcd/resources/Runway.java src/aeronpcd/util/*.java
    ```

2.  **Ejecutar:**
    ```powershell
    java -cp "bin;lib/*" aeronpcd.driver.AirportDriver
    ```

3.  **Configuración en Tiempo de Ejecución:**
    *   Seleccionar modo `SECUENCIAL` o `CONCURRENTE`.
    *   Indicar nº de Aviones, Pistas, Puertas y Operarios.
    *   Al finalizar, pulsar **OK** en el diálogo para cerrar la aplicación.

### 6.3. Modo de Pruebas Automático (TestRunner)
Se incluye una clase especial `aeronpcd.test.TestRunner` diseñada para validar rápidamente el funcionamiento del sistema sin interfaz gráfica.

Este modo ejecuta secuencialmente **6 casos de prueba** predefinidos que cubren escenarios críticos (pocos recursos, muchos recursos, secuencial vs concurrente).

*   **Ubicación:** `src/aeronpcd/test/TestRunner.java`
*   **Ejecución:**
    ```powershell
    java -cp bin aeronpcd.test.TestRunner
    ```
*   **Resultados:** Los logs y CSVs generados por este modo se almacenan en una carpeta separada `test/` (subcarpetas `secuencial` y `concurrente`) para no mezclarse con las ejecuciones normales del usuario.

---

## 7. Conclusiones
El proyecto AERON demuestra cómo la concurrencia bien aplicada permite gestionar eficientemente recursos limitados. La transición de un modelo secuencial a uno concurrente ha permitido:
1.  Aumentar el realismo de la simulación.
2.  Optimizar el tiempo total de operación.
3.  Garantizar la integridad mediante monitores y sincronización correcta.

---
**Autor:** Ana Montero Preciado  
**Fecha:** Enero 2026
