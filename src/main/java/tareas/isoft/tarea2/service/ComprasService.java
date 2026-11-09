package tareas.isoft.tarea2.service;

import org.springframework.stereotype.Service;
import tareas.isoft.tarea2.client.IndicadorClient;
import tareas.isoft.tarea2.dto.CompraConvertidaDTO;
import tareas.isoft.tarea2.dto.CompraDetalleDTO;
import tareas.isoft.tarea2.dto.ComprasPorUnidadDTO;
import tareas.isoft.tarea2.exceptions.CompraNoEncontradaException;
import tareas.isoft.tarea2.exceptions.UnidadNoValidaException;
import tareas.isoft.tarea2.model.Compra;
import tareas.isoft.tarea2.model.Unidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Conversión de las compras a pesos chilenos, según el valor de su unidad en la fecha de cada compra.
 */
@Service
public class ComprasService {
    private final CSVReaderService lectorCsv;
    private final IndicadorClient indicadores;

    /**
     * @param lectorCsv   lector del archivo de compras
     * @param indicadores cliente de la API de indicadores
     */
    public ComprasService(CSVReaderService lectorCsv, IndicadorClient indicadores) {
        this.lectorCsv = lectorCsv;
        this.indicadores = indicadores;
    }

    /**
     * Obtiene las compras de una unidad convertidas a pesos, junto con su total. El total es la suma de los
     * montos en pesos ya redondeados de cada compra.
     *
     * @param unidadSolicitada código de la unidad ({@code uf} o {@code dolar}), sin distinguir mayúsculas
     * @return las compras de esa unidad, en el orden del archivo, y su total en pesos
     * @throws UnidadNoValidaException si la unidad no existe
     */
    public ComprasPorUnidadDTO comprasPorUnidad(String unidadSolicitada) {
        Unidad unidad = Unidad.desdeRuta(unidadSolicitada)
                .orElseThrow(() -> new UnidadNoValidaException(unidadSolicitada));

        List<CompraConvertidaDTO> compras = lectorCsv.leerCompras().stream()
                .filter(compra -> compra.unidad() == unidad)
                .map(compra -> {
                    double valorUnidad = indicadores.valorEn(unidad, compra.fecha());
                    return new CompraConvertidaDTO(compra.id(), compra.producto(), compra.monto(), compra.fecha(),
                            valorUnidad, aPesos(compra.monto(), valorUnidad));
                })
                .toList();

        long totalPesos = compras.stream().mapToLong(CompraConvertidaDTO::montoPesos).sum();
        return new ComprasPorUnidadDTO(unidad, compras, totalPesos);
    }

    /**
     * Obtiene una compra convertida a pesos según su unidad.
     *
     * @param id identificador de la compra
     * @return la compra con el valor de su unidad y su monto en pesos
     * @throws CompraNoEncontradaException si no existe una compra con ese identificador
     */
    public CompraDetalleDTO detalle(int id) {
        Compra compra = lectorCsv.leerCompras().stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .orElseThrow(() -> new CompraNoEncontradaException(id));

        double valorUnidad = indicadores.valorEn(compra.unidad(), compra.fecha());
        return new CompraDetalleDTO(compra.id(), compra.producto(), compra.monto(), compra.unidad(), compra.fecha(),
                valorUnidad, aPesos(compra.monto(), valorUnidad));
    }

    /**
     * Convierte un monto a pesos. El producto se calcula con aritmética decimal exacta y se redondea al entero
     * más cercano; las mitades se redondean hacia arriba.
     *
     * @param monto       monto en la unidad de la compra
     * @param valorUnidad valor en pesos de una unidad
     * @return monto en pesos
     */
    static long aPesos(double monto, double valorUnidad) {
        return BigDecimal.valueOf(monto)
                .multiply(BigDecimal.valueOf(valorUnidad))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
