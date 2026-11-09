package tareas.isoft.tarea2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;

/**
 * Compra convertida a pesos, como elemento de la lista de {@link ComprasPorUnidadDTO}.
 * No incluye la unidad, que se informa una sola vez en la respuesta que la contiene.
 *
 * @param id          identificador de la compra
 * @param producto    nombre del producto
 * @param monto       monto en la unidad de la compra
 * @param fecha       fecha de la compra
 * @param valorUnidad valor en pesos de una unidad en la fecha de la compra
 * @param montoPesos  monto convertido a pesos, redondeado al entero más cercano
 */
@JsonPropertyOrder({"id", "producto", "monto", "fecha", "valor_unidad", "monto_pesos"})
public record CompraConvertidaDTO(
        int id,
        String producto,
        double monto,
        LocalDate fecha,
        @JsonProperty("valor_unidad") double valorUnidad,
        @JsonProperty("monto_pesos") long montoPesos
) {
}
