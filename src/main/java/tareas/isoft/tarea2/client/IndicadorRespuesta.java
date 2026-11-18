package tareas.isoft.tarea2.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Respuesta de la consulta {@code GET {url}/{indicador}/{dd-mm-yyyy}} de la API de indicadores. Ejemplo:
 * <pre>
 * {"codigo":"uf","nombre":"Unidad de fomento (UF)","unidad_medida":"Pesos",
 *  "serie":[{"fecha":"2026-09-01T03:00:00.000Z","valor":40875.09}]}
 * </pre>
 *
 * @param codigo código del indicador
 * @param serie  valores publicados en la fecha consultada; vacía si no hay valor publicado
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record IndicadorRespuesta(String codigo, List<Valor> serie) {

    /**
     * Valor publicado de un indicador.
     *
     * @param fecha fecha del valor, como texto ISO 8601, con o sin hora según la fuente
     * @param valor valor en pesos
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Valor(String fecha, double valor) {
    }
}
