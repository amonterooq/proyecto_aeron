# Arquitectura y decisiones

## Contexto

AERON es una simulación local en Java 21 con interfaz Swing. El dominio permite estudiar la contención: varios aviones necesitan recursos escasos y deben esperar autorización para avanzar. La estructura está organizada por responsabilidades; no constituye un MVC estricto, ya que la torre referencia directamente la ventana y las utilidades de persistencia.

| Componente | Responsabilidad |
| --- | --- |
| `AirportDriver` | Recoger parámetros, crear recursos y aviones, esperar su finalización y generar el CSV |
| `Airplane` | Ejecutar el ciclo de vuelo y esperar cada autorización |
| `Request` | Asociar un avión a un tipo de solicitud |
| `ControlTower` | Coordinar colas, operarios y asignación de recursos |
| `Runway`, `Gate` | Representar disponibilidad y ocupación; su coordinación pertenece a la torre |
| `AirportWindow`, `AirportState` | Mostrar eventos y estado de recursos |
| `FlightPanel`, `Logger` | Persistir estado de vuelos y trazas |
| `TestRunner` | Ejecutar escenarios e inspeccionar el resultado final |

## Protocolo de recursos

| Solicitud | Efecto cuando puede procesarse |
| --- | --- |
| `LANDING` | Reservar conjuntamente una pista y una puerta |
| `LANDED` | Liberar la pista tras aterrizar |
| `BOARDING` | Actualizar el estado de embarque |
| `BOARDED` | Liberar la puerta |
| `TAKEOFF` | Reservar una pista |
| `DEPARTED` | Liberar la pista y registrar la salida |

La reserva conjunta evita que un aterrizaje retenga una pista mientras espera una puerta. El avión mantiene la puerta durante el aterrizaje y el embarque; el modelo la libera antes de solicitar despegue.

## Sincronización

1. El avión reinicia su indicador de autorización bajo su propio monitor.
2. Publica la solicitud sin conservar ese monitor, evitando adquirir el monitor de la torre mientras retiene el del avión.
3. Espera en un bucle `while (!resourceGranted)` con `wait()`. La condición protege frente a despertares espurios y autorizaciones recibidas antes de entrar en espera.
4. Los operarios extraen peticiones bajo el monitor de la torre. Simulan 500 ms de trabajo fuera de él, permitiendo solapamiento entre operarios.
5. La comprobación y modificación de recursos se realiza bajo el monitor de la torre; después se autoriza al avión mediante `grantPermission()`.

`requestQueue` contiene solicitudes nuevas y `waitingList` aquellas que necesitan recursos. `LANDED`, `BOARDED` y `DEPARTED` se insertan al principio de la cola por liberar recursos. Al procesarlas se revisa la lista de espera y se atienden las solicitudes compatibles con los recursos disponibles. No es un planificador FIFO estricto ni ofrece una garantía formal de espera acotada.

## Interfaz y persistencia

La torre construye una representación HTML del estado y la ventana aplica actualizaciones mediante `SwingUtilities.invokeLater`. El registro usa escritura sincronizada. El panel mantiene un `ConcurrentHashMap` y serializa las escrituras JSON; `setup` inicia el estado de una nueva simulación y debe llamarse antes de lanzar sus aviones.

Los archivos son salidas de observación. No permiten recuperar una simulación tras reiniciar ni constituyen una base de datos transaccional. Los fallos de escritura del panel se notifican, pero no detienen la simulación.

## Decisiones conservadas

- Se conserva `src/aeronpcd` para facilitar la lectura junto a la memoria y mantener la configuración académica.
- Se utiliza el JDK sin dependencias externas: el ejemplo puede compilarse sin descargar un gestor de construcción.
- Los scripts compilan a `build/classes`; los bytecodes locales de `bin` no se usan como fuente de verdad.
- Las pruebas ejercitan el ciclo real y sus retardos; no reemplazan el núcleo concurrente con simulaciones de prueba.

## Limitaciones y evolución

- Los operarios son hilos daemon y no disponen de cierre ordenado funcional. La batería crea varias torres; los operarios anteriores permanecen esperando hasta terminar la JVM.
- Algunas interrupciones se registran sin cancelar el protocolo de vuelo. Una estrategia de cancelación y liberación de recursos sería una mejora independiente.
- No hay límite implementado para la cola de solicitudes ni rechazo por saturación.
- La creación inicial de Swing se realiza desde `main`; las actualizaciones posteriores sí se envían al EDT. Migrar toda la inicialización al EDT y la simulación a un trabajador requiere una revisión específica de GUI.
- La escritura de archivos dentro de la coordinación añade contención. Un escritor asíncrono permitiría estudiar su impacto, a cambio de gestionar entrega y cierre.
- Las pruebas verifican el estado final, no todos los posibles entrelazados ni la exclusión de recursos en cada instante.
- Los tiempos aleatorios y el retardo administrativo impiden tratar una ejecución aislada como un benchmark. Antes de medir mejoras: introducir una semilla, repetir escenarios, fijar entorno y registrar dispersión.
- El dominio es didáctico: no modela meteorología, prioridades de emergencia, rodaje ni protocolos aeronáuticos reales.
