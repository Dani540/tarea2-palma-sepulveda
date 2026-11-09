package tareas.isoft.tarea2.exceptions;

/**
 * Se lanza cuando no es posible obtener el valor de un indicador económico desde ninguna de las fuentes
 * configuradas, o cuando no hay un valor publicado para la fecha solicitada.
 */
public class IndicadorException extends RuntimeException {
    public IndicadorException(String message) {
        super(message);
    }
}
