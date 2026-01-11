package aeronpcd.driver;

import aeronpcd.model.Airplane;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.Logger;
import aeronpcd.gui.AirportWindow;

import java.util.ArrayList;
import java.util.List;

public class AirportDriver {

    public static void main(String[] args) {
        // 1. Configuración inicial
        
        // --- INICIALIZAMOS LA VENTANA ---
        AirportWindow window = new AirportWindow(); 
        
        // Elegimos modo "secuencial" para el log
        Logger.setup("secuencial");
        
        String startMsg = "=== INICIO SIMULACIÓN AEROPUERTO (MODO SECUENCIAL) ===";
        Logger.log(startMsg);
        window.appendLog(startMsg); // <--- Escribimos también en la ventana

        // 2. Crear Recursos (3 Pistas, 5 Puertas)
        List<Runway> runways = new ArrayList<>();
        runways.add(new Runway("Pista 1"));
        runways.add(new Runway("Pista 2"));
        runways.add(new Runway("Pista 3"));

        List<Gate> gates = new ArrayList<>();
        gates.add(new Gate("Gate 1"));
        gates.add(new Gate("Gate 2"));
        gates.add(new Gate("Gate 3"));
        gates.add(new Gate("Gate 4"));
        gates.add(new Gate("Gate 5"));

        // 3. Inicializar Torre de Control
        ControlTower tower = new ControlTower(runways, gates);

        // 4. Crear Aviones (Probamos con 2 aviones primero)
        List<Airplane> airplanes = new ArrayList<>();
        airplanes.add(new Airplane("IBE-001", tower));
        airplanes.add(new Airplane("RYR-002", tower));
        // Puedes añadir más aquí para probar con 20

        // 5. Ejecución Secuencial
        // En este modo, un avión hace todo su ciclo antes de que empiece el siguiente
        for (Airplane plane : airplanes) {
            
            // Avisamos a la ventana qué avión va a procesarse
            window.appendLog("\n--- Procesando avión: " + plane.getId() + " ---");
            
            plane.runSequential();

            // Simulación de pequeña pausa entre aviones para legibilidad
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        String endMsg = "=== FIN DE LA SIMULACIÓN ===";
        Logger.log(endMsg);
        window.appendLog(endMsg); // <--- Finalizamos en la ventana
        
        Logger.close();
    }
}