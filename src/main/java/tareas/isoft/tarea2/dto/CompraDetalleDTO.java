package tareas.isoft.tarea2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tareas.isoft.tarea2.model.Unidad;

import java.time.LocalDate;

/**
 * Compra convertida a pesos según su unidad. Respuesta de {@code GET /compras/detalle/{id}}.
 *
 * @param id          identificador de la compra
 * @param producto    nombre del producto
 * @param monto       monto en la unidad de la compra
 * @param unidad      unidad del monto
 * @param fecha       fecha de la compra
 * @param valorUnidad valor en pesos de una unidad en la fecha de la compra
 * @param montoPesos  monto convertido a pesos, redondeado al entero más cercano
 */
@JsonPropertyOrder({"id", "producto", "monto", "unidad", "fecha", "valor_unidad", "monto_pesos"})
public record CompraDetalleDTO(
        int id,
        String producto,
        double monto,
        Unidad unidad,
        LocalDate fecha,
        @JsonProperty("valor_unidad") double valorUnidad,
        @JsonProperty("monto_pesos") long montoPesos
) {
}
