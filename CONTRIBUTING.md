# Contribuir a AERON

Usa JDK 21 y ejecuta `./scripts/aeron.ps1 test` o `bash scripts/aeron.sh test` antes de proponer cambios. Mantén los paquetes actuales y guarda el código Java en UTF-8.

Una propuesta debe describir el problema, el comportamiento resultante y cómo se ha comprobado. Para cambios de concurrencia, explica qué monitor protege cada estado y qué ocurre al esperar, notificar, interrumpir y liberar recursos. Añade una prueba cuando el cambio introduzca comportamiento que pueda regresionar.

No incluyas bytecodes, archivos de entorno ni resultados de ejecuciones en el commit. Adjunta evidencia relevante a la propuesta o utiliza los artefactos de CI. Las mejoras que alteren el modelo académico deben documentar sus diferencias respecto a la memoria original.
