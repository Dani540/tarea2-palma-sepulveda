package tareas.isoft.tarea2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tareas.isoft.tarea2.exceptions.CSVReaderException;
import tareas.isoft.tarea2.exceptions.IndicadorException;
import tareas.isoft.tarea2.exceptions.UnidadNoValidaException;

/**
 * Traduce las excepciones de la aplicación a respuestas HTTP con cuerpo {@code application/problem+json}
 * (RFC 9457), que incluye los campos {@code status}, {@code title} y {@code detail}.
 */
@RestControllerAdvice
public class ManejadorErrores {
    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Responde 404 Not Found cuando la unidad solicitada no existe. */
    @ExceptionHandler(UnidadNoValidaException.class)
    public ProblemDetail unidadNoValida(UnidadNoValidaException e) {
        return problema(HttpStatus.NOT_FOUND, "Unidad no encontrada", e.getMessage());
    }

    /** Responde 502 Bad Gateway cuando no se puede obtener el valor de un indicador. */
    @ExceptionHandler(IndicadorException.class)
    public ProblemDetail indicadorNoDisponible(IndicadorException e) {
        log.error("Error al obtener indicador: {}", e.getMessage());
        return problema(HttpStatus.BAD_GATEWAY, "Indicador no disponible", e.getMessage());
    }

    /** Responde 500 Internal Server Error cuando no se puede leer el archivo de compras. */
    @ExceptionHandler(CSVReaderException.class)
    public ProblemDetail errorCsv(CSVReaderException e) {
        log.error("Error al leer el CSV de compras: {}", e.getMessage());
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el archivo de compras", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
