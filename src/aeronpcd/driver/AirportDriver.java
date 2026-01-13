package aeronpcd.driver;

import aeronpcd.gui.AirportWindow;
import aeronpcd.model.Airplane;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class AirportDriver {

    public static void main(String[] args) {
        // 1. INICIALIZACIÓN DE INTERFAZ Y LOGS
        AirportWindow window = new AirportWindow();

        Logger.setup("secuencial");
        String startMsg = "=== INICIO SIMULACIÓN AEROPUERTO (MODO SECUENCIAL - PRÁCTICA 2) ===";
        Logger.log(startMsg);
        window.appendLog(startMsg);

        // 2. CREACIÓN DE RECURSOS
        // 3 Pistas
        List<Runway> runways = new ArrayList<>();
        runways.add(new Runway("PIS1"));
        runways.add(new Runway("PIS2"));
        runways.add(new Runway("PIS3"));

        // 5 Puertas
        List<Gate> gates = new ArrayList<>();
        gates.add(new Gate("PUE1"));
        gates.add(new Gate("PUE2"));
        gates.add(new Gate("PUE3"));
        gates.add(new Gate("PUE4"));
        gates.add(new Gate("PUE5"));

        // 3. INICIALIZAR TORRE DE CONTROL
        ControlTower tower = new ControlTower(runways, gates);
        tower.setWindow(window);

        // 4. CREAR AVIONES
        List<Airplane> airplanes = new ArrayList<>();
        airplanes.add(new Airplane("IBE-001", tower));
        airplanes.add(new Airplane("RYR-002", tower));

        // 5. EJECUCIÓN DEL CICLO SECUENCIAL
        for (Airplane plane : airplanes) {
            String sep = "\n--- Iniciando secuencia para " + plane.getId() + " ---";
            Logger.log(sep);
            window.appendLog(sep);

            plane.runSequential();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        String endMsg = "\n=== FIN DE LA SIMULACIÓN ===";
        Logger.log(endMsg);
        window.appendLog(endMsg);

        Logger.close();
    }
}