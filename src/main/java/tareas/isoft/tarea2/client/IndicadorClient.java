package tareas.isoft.tarea2.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tareas.isoft.tarea2.exceptions.IndicadorException;
import tareas.isoft.tarea2.model.Unidad;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente de la API de indicadores económicos de mindicador.cl.
 * <p>
 * Obtiene el valor en pesos de la UF o del dólar en una fecha mediante la consulta
 * {@code GET {url}/{indicador}/{dd-mm-yyyy}}. Las URLs base se definen en la propiedad {@code indicadores.urls}
 * (variable de entorno {@code INDICADORES_URLS}) en orden de preferencia; por defecto, mindicador.cl y, como
 * respaldo, findic.cl, que expone la misma API. Cada consulta se envía a la primera fuente disponible y, si
 * esta falla, a la siguiente. Los valores obtenidos se almacenan en caché.
 */
@Component
public class IndicadorClient {
    private static final Logger log = LoggerFactory.getLogger(IndicadorClient.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ZoneId ZONA_CHILE = ZoneId.of("America/Santiago");
    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(4);
    private static final Duration TIEMPO_LECTURA = Duration.ofSeconds(8);

    /**
     * Cantidad máxima de días anteriores a la fecha solicitada en que se busca un valor publicado, para las fechas
     * sin valor (en el caso del dólar, fines de semana y feriados).
     */
    static final int DIAS_HACIA_ATRAS = 7;

    /** Tiempo durante el cual una fuente que falló se consulta solo después de las demás. */
    static final Duration PAUSA_TRAS_FALLA = Duration.ofMinutes(1);

    private final List<Fuente> fuentes;

    /** Resultados ya consultados, por ruta de consulta. Una entrada vacía indica una fecha sin valor publicado. */
    private final Map<String, Optional<Double>> cache = new ConcurrentHashMap<>();

    /**
     * @param urls URLs base de las fuentes de indicadores, en orden de preferencia
     * @throws IllegalArgumentException si la lista está vacía
     */
    public IndicadorClient(@Value("${indicadores.urls}") List<String> urls) {
        if (urls.isEmpty()) {
            throw new IllegalArgumentException("Falta configurar indicadores.urls");
        }
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(TIEMPO_CONEXION)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http);
        requestFactory.setReadTimeout(TIEMPO_LECTURA);

        this.fuentes = urls.stream()
                .map(String::strip)
                .map(url -> url.replaceAll("/+$", ""))
                .map(url -> new Fuente(url, RestClient.builder().baseUrl(url).requestFactory(requestFactory).build()))
                .toList();
    }

    /**
     * Obtiene el valor en pesos de una unidad en una fecha. Si la fecha no tiene valor publicado, se usa el del
     * día anterior más cercano que lo tenga, hasta {@value #DIAS_HACIA_ATRAS} días antes.
     *
     * @param unidad unidad cuyo valor se consulta
     * @param fecha  fecha de la consulta
     * @return valor en pesos de una unidad
     * @throws IndicadorException si ninguna fuente responde o no hay valor publicado en ese rango de días
     */
    public double valorEn(Unidad unidad, LocalDate fecha) {
        for (int dias = 0; dias <= DIAS_HACIA_ATRAS; dias++) {
            Optional<Double> valor = consultar(unidad, fecha.minusDays(dias));
            if (valor.isPresent()) {
                return valor.get();
            }
        }
        throw new IndicadorException("No hay valor de " + unidad + " para el " + fecha
                + " ni para los " + DIAS_HACIA_ATRAS + " días anteriores");
    }

    /**
     * Obtiene el valor publicado exactamente en una fecha, desde la caché o desde las fuentes. Los resultados
     * vacíos de la fecha actual o de fechas futuras no se almacenan en caché, porque el valor puede publicarse
     * más adelante.
     *
     * @return el valor, o vacío si no hay valor publicado en esa fecha
     * @throws IndicadorException si ninguna fuente responde
     */
    private Optional<Double> consultar(Unidad unidad, LocalDate fecha) {
        String ruta = "/" + unidad.indicador() + "/" + fecha.format(FORMATO_FECHA);
        Optional<Double> guardado = cache.get(ruta);
        if (guardado != null) {
            return guardado;
        }
        Optional<Double> valor = consultarFuentes(ruta);
        if (valor.isPresent() || fecha.isBefore(LocalDate.now(ZONA_CHILE))) {
            cache.put(ruta, valor);
        }
        return valor;
    }

    /**
     * Envía una consulta a las fuentes, comenzando por las que no están en pausa, hasta obtener una respuesta.
     * Una fuente que responde con error o no responde queda en pausa durante {@link #PAUSA_TRAS_FALLA}.
     *
     * @param ruta ruta de la consulta relativa a la URL base, por ejemplo {@code /uf/01-09-2026}
     * @return el primer valor de la serie, o vacío si la serie está vacía
     * @throws IndicadorException si ninguna fuente responde
     */
    private Optional<Double> consultarFuentes(String ruta) {
        List<Fuente> enOrden = fuentes.stream()
                .sorted(Comparator.comparing(Fuente::enPausa))
                .toList();
        for (Fuente fuente : enOrden) {
            try {
                IndicadorRespuesta respuesta = fuente.cliente().get().uri(ruta).retrieve().body(IndicadorRespuesta.class);
                if (respuesta == null || respuesta.serie() == null || respuesta.serie().isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(respuesta.serie().getFirst().valor());
            } catch (RestClientException e) {
                fuente.pausar();
                log.warn("No se pudo consultar {}{}: {}", fuente.url(), ruta, e.getMessage());
            }
        }
        throw new IndicadorException("No se pudo consultar " + ruta + " en ninguna fuente de indicadores");
    }

    /** Fuente de indicadores: URL base, cliente HTTP asociado y estado de pausa. */
    private static final class Fuente {
        private final String url;
        private final RestClient cliente;
        private volatile Instant pausadaHasta = Instant.MIN;

        Fuente(String url, RestClient cliente) {
            this.url = url;
            this.cliente = cliente;
        }

        String url() {
            return url;
        }

        RestClient cliente() {
            return cliente;
        }

        /** @return {@code true} si la fuente falló hace menos de {@link #PAUSA_TRAS_FALLA} */
        boolean enPausa() {
            return Instant.now().isBefore(pausadaHasta);
        }

        /** Pone la fuente en pausa durante {@link #PAUSA_TRAS_FALLA} a partir de este momento. */
        void pausar() {
            pausadaHasta = Instant.now().plus(PAUSA_TRAS_FALLA);
        }
    }
}
