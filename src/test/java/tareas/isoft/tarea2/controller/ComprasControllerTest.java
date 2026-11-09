package tareas.isoft.tarea2.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tareas.isoft.tarea2.client.IndicadorClient;
import tareas.isoft.tarea2.exceptions.IndicadorException;
import tareas.isoft.tarea2.model.Unidad;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints de compras con el archivo {@code src/test/resources/compras-test.csv} y valores
 * simulados de la UF y del dólar.
 */
@SpringBootTest(properties = "compras.csv-path=src/test/resources/compras-test.csv")
@AutoConfigureMockMvc
class ComprasControllerTest {
    private static final double VALOR_UF = 40000.0;
    private static final double VALOR_DOLAR = 950.5;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndicadorClient indicadores;

    @BeforeEach
    void valoresFijos() {
        when(indicadores.valorEn(eq(Unidad.UF), any())).thenReturn(VALOR_UF);
        when(indicadores.valorEn(eq(Unidad.USD), any())).thenReturn(VALOR_DOLAR);
    }

    @Test
    void comprasEnUfConvertidasYEnOrden() throws Exception {
        mockMvc.perform(get("/compras/uf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidad").value("UF"))
                .andExpect(jsonPath("$.compras[*].id", contains(1, 3, 5, 7, 9)))
                .andExpect(jsonPath("$.compras[0].producto").value("Notebook"))
                .andExpect(jsonPath("$.compras[0].monto").value(25.5))
                .andExpect(jsonPath("$.compras[0].fecha").value("2026-09-01"))
                .andExpect(jsonPath("$.compras[0].valor_unidad").value(VALOR_UF))
                .andExpect(jsonPath("$.compras[0].monto_pesos").value(1_020_000))
                .andExpect(jsonPath("$.compras[0].unidad").doesNotExist())
                .andExpect(jsonPath("$.total_pesos").value(4_018_000));
    }

    @Test
    void comprasEnDolarRedondeadasAlEntero() throws Exception {
        // Montos esperados: 95.9 × 950.5 = 91152.95 → 91153; 349.99 × 950.5 = 332665.495 → 332665; 75 × 950.5 = 71287.5 → 71288
        mockMvc.perform(get("/compras/dolar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidad").value("USD"))
                .andExpect(jsonPath("$.compras[*].id", contains(2, 4, 6, 8, 10)))
                .andExpect(jsonPath("$.compras[*].monto_pesos", contains(171_090, 91_153, 332_665, 114_535, 71_288)))
                .andExpect(jsonPath("$.total_pesos").value(780_731));
    }

    @Test
    void unidadInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/compras/euro"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString("euro")));
    }

    @Test
    void apiDeIndicadoresCaidaResponde502() throws Exception {
        when(indicadores.valorEn(eq(Unidad.UF), any())).thenThrow(new IndicadorException("sin conexión"));
        mockMvc.perform(get("/compras/uf"))
                .andExpect(status().isBadGateway());
    }
}
