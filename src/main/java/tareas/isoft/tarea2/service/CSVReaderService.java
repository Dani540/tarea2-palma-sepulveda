package tareas.isoft.tarea2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tareas.isoft.tarea2.exceptions.CSVReaderException;
import tareas.isoft.tarea2.model.Compra;
import tareas.isoft.tarea2.model.Unidad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lector del archivo de compras en formato CSV.
 * <p>
 * La ruta del archivo se define en la propiedad {@code compras.csv-path} (variable de entorno {@code CSV_PATH}).
 * El archivo se lee en cada llamada a {@link #leerCompras()}, por lo que sus cambios se reflejan sin reiniciar
 * la aplicación. Las columnas {@code id, producto, monto, unidad, fecha} se identifican por su nombre en el
 * encabezado y pueden aparecer en cualquier orden.
 */
@Service
public class CSVReaderService {
    /** Nombres de las columnas requeridas, en el orden en que se usan para construir cada {@link Compra}. */
    private static final List<String> COLUMNAS = List.of("id", "producto", "monto", "unidad", "fecha");

    /**
     * Marca de orden de bytes (BOM, carácter U+FEFF). Algunos programas, como Excel, la escriben al inicio de
     * los archivos UTF-8; es invisible y no forma parte del nombre de la primera columna.
     */
    static final char BOM = '﻿';

    private final Path ruta;

    /**
     * @param ruta ruta del archivo de compras, absoluta o relativa al directorio de trabajo
     */
    public CSVReaderService(@Value("${compras.csv-path}") String ruta) {
        this.ruta = Path.of(ruta);
    }

    /**
     * Lee todas las compras del archivo. Las líneas vacías se omiten.
     *
     * @return las compras, en el orden en que aparecen en el archivo
     * @throws CSVReaderException si el archivo no existe, no se puede leer, le falta una columna
     *                            o contiene una línea con formato inválido
     */
    public List<Compra> leerCompras() {
        if (!Files.isRegularFile(ruta)) {
            throw new CSVReaderException("No se encontró el archivo de compras en " + ruta.toAbsolutePath());
        }
        List<String> lineas;
        try {
            lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CSVReaderException("No se pudo leer " + ruta.toAbsolutePath() + ": " + e.getMessage(), e);
        }
        if (lineas.isEmpty()) {
            return List.of();
        }

        int[] indice = indicesDeColumnas(lineas.getFirst());
        List<Compra> compras = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).strip();
            if (!linea.isEmpty()) {
                compras.add(parsear(linea, i + 1, indice));
            }
        }
        return compras;
    }

    /**
     * Calcula la posición de cada columna requerida a partir del encabezado.
     *
     * @param encabezado primera línea del archivo; se ignora una {@link #BOM} inicial
     * @return posición de cada columna, en el orden de {@link #COLUMNAS}
     * @throws CSVReaderException si falta alguna columna requerida
     */
    private int[] indicesDeColumnas(String encabezado) {
        String sinBom = encabezado.isEmpty() || encabezado.charAt(0) != BOM ? encabezado : encabezado.substring(1);
        List<String> nombres = Arrays.stream(sinBom.split(","))
                .map(nombre -> nombre.strip().toLowerCase(Locale.ROOT))
                .toList();
        int[] indice = new int[COLUMNAS.size()];
        for (int i = 0; i < COLUMNAS.size(); i++) {
            indice[i] = nombres.indexOf(COLUMNAS.get(i));
            if (indice[i] < 0) {
                throw new CSVReaderException("Al CSV le falta la columna '" + COLUMNAS.get(i) + "'. Encabezado: " + sinBom);
            }
        }
        return indice;
    }

    /**
     * Construye una compra a partir de una línea de datos.
     *
     * @param linea       línea del archivo, sin espacios al inicio ni al final
     * @param numeroLinea número de la línea en el archivo, contando desde 1, para los mensajes de error
     * @param indice      posición de cada columna, según {@link #indicesDeColumnas(String)}
     * @return la compra representada por la línea
     * @throws CSVReaderException si faltan campos o alguno tiene un formato inválido
     */
    private Compra parsear(String linea, int numeroLinea, int[] indice) {
        String[] campos = linea.split(",", -1);
        try {
            return new Compra(
                    Integer.parseInt(campo(campos, indice[0])),
                    campo(campos, indice[1]),
                    Double.parseDouble(campo(campos, indice[2])),
                    Unidad.valueOf(campo(campos, indice[3]).toUpperCase(Locale.ROOT)),
                    LocalDate.parse(campo(campos, indice[4]))
            );
        } catch (IllegalArgumentException | DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            throw new CSVReaderException("Línea " + numeroLinea + " inválida en el CSV (" + linea + "): " + e.getMessage(), e);
        }
    }

    private static String campo(String[] campos, int indice) {
        return campos[indice].strip();
    }
}
