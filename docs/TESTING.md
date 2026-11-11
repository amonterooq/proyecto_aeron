# Validación y reproducción

## Ejecución

Desde la raíz, con JDK 21:

```powershell
./scripts/aeron.ps1 test
```

En Bash: `bash scripts/aeron.sh test`. Ambos compilan todo el código y ejecutan `aeronpcd.test.TestRunner` en modo headless. No requieren dependencias externas.

## Escenarios

| Caso | Modo | Aviones | Pistas | Puertas | Operarios |
| --- | --- | ---: | ---: | ---: | ---: |
| 1 | Secuencial | 2 | 1 | 1 | 1 |
| 2 | Secuencial | 20 | 3 | 5 | 1 |
| 3 | Concurrente | 2 | 1 | 1 | 1 |
| 4 | Concurrente | 2 | 1 | 1 | 5 |
| 5 | Concurrente | 20 | 3 | 5 | 1 |
| 6 | Concurrente | 20 | 3 | 5 | 5 |

Se comprueba en cada escenario:

- Todos los aviones terminan en `DEPARTED` con un tiempo positivo.
- Todas las pistas y puertas quedan libres.
- El CSV tiene cabecera y una fila por avión.
- El panel JSON incluye cada vuelo en `DEPARTED` y no contiene vuelos de un escenario anterior.
- El log existe y contiene datos.

Una comprobación fallida produce `AssertionError` y un código de salida distinto de cero. Un watchdog termina con error si el proceso supera 300 segundos. No es necesario activar `-ea`: las comprobaciones son explícitas. Los archivos quedan en `test/secuencial/` y `test/concurrente/` y no se versionan.

## Alcance

Son pruebas de integración del ciclo de simulación. El timeout detecta que una ejecución no termina a tiempo; por sí solo no diagnostica un interbloqueo. Las comprobaciones del JSON validan el formato esperado por este productor, no sustituyen un parser JSON general. No hay una prueba formal de ausencia de inanición ni cobertura exhaustiva de carreras. Tampoco se automatiza la interacción con Swing.

## Comprobación manual de la interfaz

1. Abrir la aplicación y cancelar la configuración: debe finalizar.
2. Ejecutar ambos modos y comprobar que los operarios solo son configurables en concurrente.
3. Introducir un valor fuera del intervalo: debe mostrarse un error sin iniciar la simulación.
4. Ejecutar cinco aviones con una pista y una puerta y observar la cola y liberación de recursos.
5. Verificar los archivos en `logs/` y cerrar el diálogo final.

Esta lista describe comprobaciones manuales; no implica que se hayan ejecutado en cada commit. Los resultados automáticos de cada publicación deben consultarse en [GitHub Actions](https://github.com/amonterooq/proyecto_aeron/actions).

## Comparación de rendimiento

Comparar la duración global indicada en el log para idénticas configuraciones, repetir cada caso y registrar JDK, sistema operativo y recursos de la máquina. El ranking del CSV compara tiempos por avión dentro de una ejecución; no mide por sí mismo la aceleración de la concurrencia. Los registros históricos carecen de un protocolo estadístico y no se usan para afirmar porcentajes de mejora.
