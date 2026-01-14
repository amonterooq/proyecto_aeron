package aeronpcd.util;

import java.util.List;
import aeronpcd.model.Request;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;

public class AirportState {

    // PRÁCTICA 4/7: Genera el código HTML para mostrar el estado en la ventana
    public static String getHTMLState(List<Runway> runways, List<Gate> gates, List<Request> requestQueue) {
        StringBuilder sb = new StringBuilder();
        // Estilos CSS inline para compatibilidad con Swing
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
        sb.append("<br><h2>Cola de Peticiones (" + requestQueue.size() + "):</h2>");
        sb.append("<hr color='#00FFFF'>");
        sb.append(generateQueueHTML(requestQueue));

        sb.append("</body></html>");
        return sb.toString();
    }

    // Genera tabla HTML para recursos (puntos verdes/rojos)
    private static String generateResourceTable(List<?> resources, boolean isRunway) {
        StringBuilder table = new StringBuilder();
        table.append("<table width='100%'>");

        // Cabeceras (IDs)
        table.append("<tr>");
        for (Object obj : resources) {
            String id = isRunway ? ((Runway) obj).getId() : ((Gate) obj).getId();
            table.append("<th align='center' style='font-size:14px;'>").append(id).append("</th>");
        }
        table.append("</tr>");

        // Estado (Puntos)
        table.append("<tr>");
        for (Object obj : resources) {
            boolean available = isRunway ? ((Runway) obj).isAvailable() : !((Gate) obj).isOccupied();
            String dot = available ? "<font color='#00FF00' size='6'>●</font>"
                    : "<font color='#FF0000' size='6'>●</font>";
            table.append("<td align='center'>").append(dot).append("</td>");
        }
        table.append("</tr></table>");
        return table.toString();
    }

    // Genera lista HTML para la cola
    private static String generateQueueHTML(List<Request> queue) {
        if (queue.isEmpty())
            return "<i>(Cola vacía)</i>";

        StringBuilder sb = new StringBuilder("<ul>");
        int i = 1;

        // Sincronizamos para leer la lista sin errores concurrentes
        synchronized (queue) {
            for (Request r : queue) {
                // IMPORTANTE: Usamos getFlightId()
                String planeId = (r.getPlane() != null) ? r.getPlane().getFlightId() : "??";
                sb.append("<li><b>").append(i++).append(".</b> ")
                        .append(r.getType()).append(" &rarr; ").append(planeId).append("</li>");
            }
        }
        sb.append("</ul>");
        return sb.toString();
    }

    // Métodos legacy vacíos (para compatibilidad)
    public static String showRequestQueue(List<Request> q) {
        return "";
    }

    public static String showResourcesStatus(List<Runway> r, List<Gate> g) {
        return "";
    }
}