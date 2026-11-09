package tareas.isoft.tarea2.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tareas.isoft.tarea2.exceptions.IndicadorException;
import tareas.isoft.tarea2.model.Unidad;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de {@link IndicadorClient} contra un servidor HTTP local que simula la API de mindicador.cl. */
class IndicadorClientTest {
    private static final String SERIE_VACIA = "{\"codigo\":\"dolar\",\"serie\":[]}";
    private static final Map<String, String> RESPUESTAS = Map.of(
            "/api/uf/01-09-2026", "{\"version\":\"1.7.0\",\"codigo\":\"uf\",\"serie\":[{\"fecha\":\"2026-09-01T03:00:00.000Z\",\"valor\":40875.09}]}",
            "/api/dolar/04-09-2026", "{\"codigo\":\"dolar\",\"serie\":[{\"fecha\":\"2026-09-04\",\"valor\":936.86}]}",
            "/api/dolar/05-09-2026", SERIE_VACIA,
            "/api/dolar/06-09-2026", SERIE_VACIA);

    private HttpServer servidor;
    private final List<String> consultas = new CopyOnWriteArrayList<>();

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            String ruta = intercambio.getRequestURI().getPath();
            consultas.add(ruta);
            String cuerpo = RESPUESTAS.getOrDefault(ruta, SERIE_VACIA);
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            intercambio.sendResponseHeaders(200, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    @Test
    void obtieneElValorDeLaFechaYLoGuarda() {
        IndicadorClient cliente = new IndicadorClient(List.of(urlServidor()));

        assertThat(cliente.valorEn(Unidad.UF, LocalDate.of(2026, 9, 1))).isEqualTo(40875.09);
        assertThat(cliente.valorEn(Unidad.UF, LocalDate.of(2026, 9, 1))).isEqualTo(40875.09);
        assertThat(consultas).containsExactly("/api/uf/01-09-2026");
    }

    @Test
    void enFinDeSemanaUsaElUltimoDiaConValor() {
        IndicadorClient cliente = new IndicadorClient(List.of(urlServidor()));

        assertThat(cliente.valorEn(Unidad.USD, LocalDate.of(2026, 9, 6))).isEqualTo(936.86);
        assertThat(consultas).containsExactly("/api/dolar/06-09-2026", "/api/dolar/05-09-2026", "/api/dolar/04-09-2026");
    }

    @Test
    void siLaPrimeraFuenteNoRespondeUsaLaSiguiente() throws IOException {
        IndicadorClient cliente = new IndicadorClient(List.of(urlSinServidor(), urlServidor()));

        assertThat(cliente.valorEn(Unidad.UF, LocalDate.of(2026, 9, 1))).isEqualTo(40875.09);
    }

    @Test
    void sinNingunaFuenteDisponibleLanzaIndicadorException() throws IOException {
        IndicadorClient cliente = new IndicadorClient(List.of(urlSinServidor()));

        assertThatThrownBy(() -> cliente.valorEn(Unidad.UF, LocalDate.of(2026, 9, 1)))
                .isInstanceOf(IndicadorException.class);
    }

    private String urlServidor() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/api/";
    }

    /** @return URL de un puerto local sin servidor, cuyas conexiones son rechazadas */
    private static String urlSinServidor() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return "http://127.0.0.1:" + socket.getLocalPort() + "/api";
        }
    }
}
