package tareas.isoft.tarea2.exceptions;

/**
 * Se lanza cuando no existe una compra con el identificador solicitado.
 */
public class CompraNoEncontradaException extends RuntimeException {
    /**
     * @param id identificador solicitado
     */
    public CompraNoEncontradaException(int id) {
        super("No existe una compra con id " + id);
    }
}
