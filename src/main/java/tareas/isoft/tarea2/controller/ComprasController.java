package tareas.isoft.tarea2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tareas.isoft.tarea2.dto.ComprasPorUnidadDTO;
import tareas.isoft.tarea2.service.ComprasService;

/**
 * Endpoints de consulta de compras convertidas a pesos chilenos.
 */
@RestController
@RequestMapping("/compras")
public class ComprasController {
    private final ComprasService comprasService;

    /**
     * @param comprasService servicio de conversión de compras
     */
    public ComprasController(ComprasService comprasService) {
        this.comprasService = comprasService;
    }

    /**
     * {@code GET /compras/{unidad}}: compras de una unidad convertidas a pesos, con su total.
     *
     * @param unidad {@code uf} o {@code dolar}
     * @return las compras de la unidad y su total en pesos; 404 si la unidad no existe
     */
    @GetMapping("/{unidad}")
    public ComprasPorUnidadDTO comprasPorUnidad(@PathVariable String unidad) {
        return comprasService.comprasPorUnidad(unidad);
    }
}
