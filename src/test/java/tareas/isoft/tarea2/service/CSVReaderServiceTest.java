package tareas.isoft.tarea2.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tareas.isoft.tarea2.exceptions.CSVReaderException;
import tareas.isoft.tarea2.model.Compra;
import tareas.isoft.tarea2.model.Unidad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de {@link CSVReaderService}. */
class CSVReaderServiceTest {

    @TempDir
    Path carpeta;

    @Test
    void leeLasComprasEnOrden() {
        List<Compra> compras = new CSVReaderService("src/test/resources/compras-test.csv").leerCompras();

        assertThat(compras).hasSize(10);
        assertThat(compras.getFirst())
                .isEqualTo(new Compra(1, "Notebook", 25.5, Unidad.UF, LocalDate.of(2026, 9, 1)));
        assertThat(compras).extracting(Compra::id).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void toleraBomLineasVaciasYColumnasEnOtroOrden() throws IOException {
        Path csv = escribir(CSVReaderService.BOM + "fecha,unidad,monto,producto,id\n2026-09-02,usd,180,Monitor,2\n\n");

        assertThat(new CSVReaderService(csv.toString()).leerCompras())
                .containsExactly(new Compra(2, "Monitor", 180, Unidad.USD, LocalDate.of(2026, 9, 2)));
    }

    @Test
    void lineaInvalidaIndicaElNumeroDeLinea() throws IOException {
        Path csv = escribir("id,producto,monto,unidad,fecha\n1,Notebook,25.5,EUR,2026-09-01\n");

        assertThatThrownBy(() -> new CSVReaderService(csv.toString()).leerCompras())
                .isInstanceOf(CSVReaderException.class)
                .hasMessageContaining("Línea 2");
    }

    @Test
    void archivoInexistente() {
        assertThatThrownBy(() -> new CSVReaderService(carpeta.resolve("no-existe.csv").toString()).leerCompras())
                .isInstanceOf(CSVReaderException.class)
                .hasMessageContaining("No se encontró");
    }

    private Path escribir(String contenido) throws IOException {
        return Files.writeString(carpeta.resolve("compras.csv"), contenido);
    }
}
