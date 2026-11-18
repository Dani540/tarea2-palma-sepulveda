package tareas.isoft.tarea2.exceptions;

/**
 * Se lanza cuando el archivo de compras no existe, no se puede leer o contiene datos con formato inválido.
 */
public class CSVReaderException extends RuntimeException {
    public CSVReaderException(String message) {
        super(message);
    }

    public CSVReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
