package tareas.isoft.tarea2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tareas.isoft.tarea2.model.Unidad;

import java.util.List;

/**
 * Compras de una unidad convertidas a pesos. Respuesta de {@code GET /compras/{unidad}}.
 *
 * @param unidad     unidad de las compras
 * @param compras    compras de esa unidad, en el orden del archivo de compras
 * @param totalPesos suma de los montos en pesos de todas las compras
 */
@JsonPropertyOrder({"unidad", "compras", "total_pesos"})
public record ComprasPorUnidadDTO(
        Unidad unidad,
        List<CompraConvertidaDTO> compras,
        @JsonProperty("total_pesos") long totalPesos
) {
}
