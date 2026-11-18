package tareas.isoft.tarea2.exceptions;

/**
 * Se lanza cuando la unidad solicitada no corresponde a ninguna unidad soportada ({@code uf} o {@code dolar}).
 */
public class UnidadNoValidaException extends RuntimeException {
    /**
     * @param unidad unidad solicitada
     */
    public UnidadNoValidaException(String unidad) {
        super("La unidad '" + unidad + "' no existe. Unidades válidas: uf, dolar");
    }
}
