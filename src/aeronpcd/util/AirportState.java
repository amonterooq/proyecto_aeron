package aeronpcd.util;

import java.util.List;
import aeronpcd.model.Request;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;

/**
 * Clase de utilidad encargada de generar la representación visual del estado
 * del aeropuerto.
 * Utiliza HTML para renderizar paneles informativos enriquecidos en la interfaz
 * Swing.
 */
public class AirportState {

    /**
     * Genera un documento HTML completo con el estado actual de todas las pistas,
     * puertas y la cola de peticiones.
     * 
     * @param runways      Lista de pistas del aeropuerto.
     * @param gates        Lista de puertas de embarque.
     * @param requestQueue Cola de peticiones pendientes en la Torre.
     * @return String con el código HTML listo para ser renderizado en un JTextPane.
     */
    public static String getHTMLState(List<Runway> runways, List<Gate> gates, List<Request> requestQueue) {
        StringBuilder sb = new StringBuilder();
        // Estilos CSS inline para compatibilidad con el motor de renderizado HTML3.2 de
        // Swing
        sb.append("<html><body style='font-family:Monospaced; background-color:#0A0A28; color:#00FFFF; margin:10px;'>");

        // SECCIÓN PISTAS
        sb.append("<h2>Estado de Pistas:</h2>");
        sb.append("<hr color='#00FFFF'>");
        sb.append(generateResourceTable(runways, true));

        // SECCIÓN PUERTAS
        sb.append("<br><h2>Estado de Puertas:</h2>");
        sb.append("<hr color='#00FFFF'>");
        sb.append(generateResourceTable(gates, false));

        // SECCIÓN COLA
        // Mostramos el tamaño actual de la cola para identificar saturación
        sb.append("<br><h2>Cola de Peticiones (" + requestQueue.size() + "):</h2>");
        sb.append("<hr color='#00FFFF'>");
        sb.append(generateQueueHTML(requestQueue));

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Helper que crea una tabla HTML para visualizar recursos como indicadores LED
     * (Puntos).
     * 
     * @param resources Lista de objetos Runway o Gate.
     * @param isRunway  Flag para hacer casting correcto.
     * @return Tabla HTML.
     */
    private static String generateResourceTable(List<?> resources, boolean isRunway) {
        StringBuilder table = new StringBuilder();
        table.append("<table width='100%'>");

        // Títulos de columnas (Ej: PIS1, PIS2...)
        table.append("<tr>");
        for (Object obj : resources) {
            String id = isRunway ? ((Runway) obj).getId() : ((Gate) obj).getId();
            table.append("<th align='center' style='font-size:14px;'>").append(id).append("</th>");
        }
        table.append("</tr>");

        // Indicadores de estado (Verde=Libre, Rojo=Ocupado)
        table.append("<tr>");
        for (Object obj : resources) {
            boolean available = isRunway ? ((Runway) obj).isAvailable() : !((Gate) obj).isOccupied();
            // Inserción de carácter UNICODE '●' con color
            String dot = available ? "<font color='#00FF00' size='6'>●</font>"
                    : "<font color='#FF0000' size='6'>●</font>";
            table.append("<td align='center'>").append(dot).append("</td>");
        }
        table.append("</tr></table>");
        return table.toString();
    }

    /**
     * Helper que transforma la cola de peticiones en una lista HTML ordenada.
     * Sincroniza sobre la cola para evitar ConcurrentModificationException mientras
     * se lee.
     * Clorifica el tipo de petición para facilitar la lectura del operador.
     * 
     * @param queue Cola de peticiones.
     * @return Lista HTML
     */
    private static String generateQueueHTML(List<Request> queue) {
        if (queue.isEmpty())
            return "<i>(Cola vacía)</i>";

        StringBuilder sb = new StringBuilder("<ul>");
        int i = 1;

        // SECCIÓN CRÍTICA: La cola puede cambiar mientras se lee
        synchronized (queue) {
            for (Request r : queue) {
                // Obtenemos el ID del avión asociado a la petición
                String planeId = (r.getPlane() != null) ? r.getPlane().getFlightId() : "??";
                
                // Color coding según tipo de petición
                String colorStyle = getRequestColor(r.getType());
                
                sb.append("<li><b>").append(i++).append(".</b> ")
                  .append("<span style='color:").append(colorStyle).append(";'>")
                  .append(r.getType()).append("</span>")
                  .append(" &rarr; ").append(planeId).append("</li>");
            }
        }
        sb.append("</ul>");
        return sb.toString();
    }

    /**
     * Define el color para cada tipo de petición.
     */
    private static String getRequestColor(Request.Type type) {
        switch (type) {
            case LANDING: return "#FF4444"; // Rojo (Crítico)
            case TAKEOFF: return "#FFAA00"; // Naranja
            case LANDED: 
            case BOARDED: 
            case DEPARTED: return "#00FF00"; // Verde (Liberación)
            default: return "#FFFFFF";
        }
    }

    // Métodos legacy vacíos (para compatibilidad)
    public static String showRequestQueue(List<Request> q) {
        return "";
    }

    public static String showResourcesStatus(List<Runway> r, List<Gate> g) {
        return "";
    }
}