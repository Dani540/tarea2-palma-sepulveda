# Tarea 2 - Ingeniería de Software

API REST en Spring Boot que lee un archivo de compras en formato CSV y entrega cada compra convertida a
pesos chilenos, según el valor de la UF o del dólar en la fecha de la compra. Los valores se obtienen de la
API de [mindicador.cl](https://mindicador.cl) o del fallback de [findic.cl] (https://findic.cl).

## Integrantes

| Nombre | Usuario de GitHub | Desafíos |
|---|---|---|
| `Daniel Palma` | `Dani540` | 1. Compras por unidad · 2. Detalle de una compra · 3. Docker · 4. Pipeline |

## Endpoints

| Método | URL | Respuesta |
|---|---|---|
| GET | `/compras/uf` | Compras en UF convertidas a pesos y su total |
| GET | `/compras/dolar` | Compras en USD convertidas a pesos y su total |
| GET | `/compras/detalle/{id}` | Compra con el id indicado, convertida a pesos según su unidad |

Ejemplo de `GET /compras/detalle/3`:

```json
{
  "id": 3,
  "producto": "Escritorio",
  "monto": 8.0,
  "unidad": "UF",
  "fecha": "2026-09-04",
  "valor_unidad": 40879.04,
  "monto_pesos": 327032
}
```

`monto_pesos` y `total_pesos` son enteros redondeados; `total_pesos` es la suma de los `monto_pesos`.
Las compras aparecen en el mismo orden que en el CSV.

### Errores

Los errores se responden con cuerpo `application/problem+json` (RFC 9457):

| Código | Caso |
|---|---|
| 404 | La unidad no es `uf` ni `dolar`, o no existe una compra con el id indicado |
| 400 | El id no es un número entero |
| 502 | No se pudo obtener el valor del indicador |
| 500 | No se pudo leer el archivo de compras |

### Valores de los indicadores

El valor de cada fecha se obtiene con `GET https://mindicador.cl/api/{uf|dolar}/{dd-mm-yyyy}`. Si la fecha
no tiene valor publicado (el dólar no se publica fines de semana ni feriados), se usa el último valor
publicado dentro de los 7 días anteriores. Si mindicador.cl no responde, la consulta se envía a findic.cl,
que expone la misma API. Los valores consultados se mantienen en caché.

## Ejecución

Con Docker, desde la raíz del repositorio:

```bash
docker compose up --build
```

La API queda disponible en <http://localhost:3001/compras/uf>. El archivo `compras.csv` de la raíz del
repositorio se monta en el contenedor y se lee en cada consulta, por lo que se puede modificar sin
reconstruir la imagen. Si un editor reemplaza el archivo en lugar de modificarlo, el contenedor conserva la
versión anterior hasta ejecutar `docker compose restart`.

Sin Docker (Java 21):

```bash
./mvnw spring-boot:run
```

Tests:

```bash
./mvnw test
```

## Variables de entorno

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `APP_PORT` | `3001` | Puerto HTTP |
| `CSV_PATH` | `compras.csv`; en la imagen, `/data/compras.csv` | Ruta del archivo de compras |
| `APP_ENV` | `production` en la imagen | Entorno de ejecución |
| `INDICADORES_URLS` | `https://mindicador.cl/api,https://findic.cl/api` | URLs base de las fuentes de indicadores, en orden de preferencia |

## Imagen Docker

- Construcción en dos etapas: Maven compila el JAR dentro de `docker build` y `jlink` genera un JRE con
  solo los módulos que usa la aplicación. La imagen final contiene Alpine, ese JRE y el JAR (alrededor de
  155 MB).
- Se ejecuta con el usuario sin privilegios `app`.
- No contiene `compras.csv`.

## Pipeline

`.github/workflows/ci.yml` se ejecuta en GitHub Actions con tres jobs encadenados; si uno falla, los
siguientes no se ejecutan:

1. `test`: ejecuta los tests con Maven.
2. `build`: genera el JAR y construye la imagen Docker.
3. `publish`: solo en push a `main`. Publica la imagen en `ghcr.io/<usuario>/<repositorio>` con los tags
   `latest` y el SHA corto del commit (7 caracteres). Se autentica con el `GITHUB_TOKEN` del workflow.
