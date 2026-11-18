package tareas.isoft.tarea2.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Unidad monetaria en que está expresado el monto de una compra.
 * En JSON se serializa con su nombre: {@code "UF"} o {@code "USD"}.
 */
public enum Unidad {
    /** Unidad de fomento. */
    UF("uf"),
    /** Dólar estadounidense (dólar observado). */
    USD("dolar");

    /** Código del indicador en la API de mindicador.cl, usado también en la ruta {@code /compras/{unidad}}. */
    private final String indicador;

    Unidad(String indicador) {
        this.indicador = indicador;
    }

    /**
     * @return código del indicador en la API de mindicador.cl ({@code "uf"} o {@code "dolar"})
     */
    public String indicador() {
        return indicador;
    }

    /**
     * Obtiene la unidad que corresponde a un código de indicador, sin distinguir mayúsculas de minúsculas.
     *
     * @param valor código recibido en la ruta, por ejemplo {@code "dolar"}
     * @return la unidad, o vacío si el código no corresponde a ninguna
     */
    public static Optional<Unidad> desdeRuta(String valor) {
        return Arrays.stream(values())
                .filter(unidad -> unidad.indicador.equalsIgnoreCase(valor))
                .findFirst();
    }
}
