package tareas.isoft.tarea2.model;

import java.time.LocalDate;

/**
 * Compra registrada en el archivo de compras.
 *
 * @param id       identificador de la compra
 * @param producto nombre del producto
 * @param monto    monto en la unidad indicada
 * @param unidad   unidad del monto
 * @param fecha    fecha de la compra
 */
public record Compra(
        int id,
        String producto,
        double monto,
        Unidad unidad,
        LocalDate fecha
) {
}
