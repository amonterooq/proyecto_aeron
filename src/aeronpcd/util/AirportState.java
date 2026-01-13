package aeronpcd.util;

import java.util.List;
import aeronpcd.model.Request;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;

public class AirportState {

    public static String getHTMLState(List<Runway> runways, List<Gate> gates, List<Request> requestQueue) {
        StringBuilder sb = new StringBuilder();

        // Estilo base: Fondo oscuro, texto cian
        sb.append("<html><body style='font-family:Monospaced; background-color:#0A0A28; color:#00FFFF; margin:10px;'>");

        // --- SECCIÓN 1: PISTAS ---
        sb.append("<h2>Estado de Pistas:</h2>");
        sb.append("<hr color='#00FFFF'>"); // Línea separadora cian
        sb.append(generateResourceTable(runways, true));

        // --- SECCIÓN 2: PUERTAS ---
        sb.append("<br><h2>Estado de Puertas:</h2>");
        sb.append("<hr color='#00FFFF'>");
        sb.append(generateResourceTable(gates, false));

        // --- SECCIÓN 3: COLA DE PETICIONES ---
        sb.append("<br><h2>Cola de Peticiones (" + requestQueue.size() + "):</h2>");
        sb.append("<hr color='#00FFFF'>");
        sb.append(generateQueueHTML(requestQueue));

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String generateResourceTable(List<?> resources, boolean isRunway) {
        StringBuilder table = new StringBuilder();
        // Usamos atributos HTML clásicos (width, align) que Swing entiende mejor que
        // CSS
        table.append("<table width='100%'>");

        // Fila 1: Nombres (PIS1, PIS2...)
        table.append("<tr>");
        for (Object obj : resources) {
            String id = isRunway ? ((Runway) obj).getId() : ((Gate) obj).getId();
            // align='center' fuerza el centrado horizontal del texto
            table.append("<th align='center' style='font-size:14px;'>").append(id).append("</th>");
        }
        table.append("</tr>");

        // Fila 2: Iconos (🟢 / 🔴)
        table.append("<tr>");
        for (Object obj : resources) {
            boolean available = isRunway ? ((Runway) obj).isAvailable() : !((Gate) obj).isOccupied();
            // Usamos el carácter ● con tamaño grande (size='6')
            String dot = available ? "<font color='#00FF00' size='6'>●</font>"
                    : "<font color='#FF0000' size='6'>●</font>";

            // align='center' aquí asegura que el punto esté justo en medio de la celda
            table.append("<td align='center'>").append(dot).append("</td>");
        }
        table.append("</tr>");

        table.append("</table>");
        return table.toString();
    }

    private static String generateQueueHTML(List<Request> queue) {
        if (queue.isEmpty()) {
            return "<i>(Cola vacía)</i>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<ul>"); // Lista simple
        int i = 1;
        synchronized (queue) {
            for (Request r : queue) {
                String tipoStr = getRequestIcon(r.getType());
                String planeId = (r.getPlane() != null) ? r.getPlane().getId() : "??";
                sb.append("<li>")
                        .append("<b>").append(i++).append(".</b> ")
                        .append(tipoStr).append(" &rarr; ").append(planeId)
                        .append("</li>");
            }
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private static String getRequestIcon(Request.Type type) {
        if (type == null)
            return "UNK";
        switch (type) {
            // Usamos colores brillantes para que resalte sobre el fondo oscuro
            case LANDING:
                return "<font color='#FFFF00'>Aterrizaje 🛬</font>";
            case LANDED:
                return "<font color='#00FF00'>Aterrizado ✅</font>";
            case BOARDING:
                return "<font color='#FFA500'>Embarcando 🚶</font>";
            case BOARDED:
                return "<font color='#00FF00'>Embarcado 🧳</font>";
            case TAKEOFF:
                return "<font color='#FFFF00'>Despegue 🛫</font>";
            case DEPARTED:
                return "<font color='#FF0000'>Despegado ✈️</font>";
            default:
                return type.toString();
        }
    }

    // Métodos legacy vacíos para evitar errores de compilación si algo los llama
    public static String showRequestQueue(List<Request> q) {
        return "";
    }

    public static String showResourcesStatus(List<Runway> r, List<Gate> g) {
        return "";
    }
}