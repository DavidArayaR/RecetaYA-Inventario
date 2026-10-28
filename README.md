# Inventario — Receta Ya

Microservicio REST para administrar medicamentos, sucursales y existencias. Usa H2 local, migraciones Flyway y JWT emitidos por Auth. En desarrollo escucha en `127.0.0.1:8080`.

## Relación con la evaluación

| Criterio | Evidencia en este servicio |
|---|---|
| IE1 e IE2 | Rutas REST en `ControladorInventario` y separación entre controlador, servicio, repositorios y entidades. |
| IE3 | Solicitudes correctas y errores `400`, `403`, `404` y `409` en Postman. |
| IE4 e IE6 | H2, Flyway y JPA; relaciones de existencias con medicamento y sucursal. |
| IE5 | CRUD completo de un medicamento temporal y consultas de sus filas en H2. |
| IE7 e IE8 | `pom.xml`, Maven Wrapper, pruebas y JAR ejecutable. |
| IE9 | Ramas `main`, `develop` y `feature/inventario-service` en el repositorio actual. |
| IE10 | Clonación, configuración, compilación y ejecución reproducibles. |

## Requisitos y obtención del código

- JDK 21, Git y acceso a Maven Central para descargar dependencias la primera vez.
- Puerto `8080` libre. H2 viene incluido; no se instala un servidor de base de datos.
- Para escribir, Auth debe estar iniciado en `http://localhost:8081` y debe existir un usuario local con rol `FARMACEUTICO`.
- En Windows usar PowerShell; en macOS y Linux usar Bash. Maven Wrapper evita instalar Maven aparte.

Mientras los cuatro servicios estén en el repositorio actual, clonar y entrar en `inventario`:

### Windows

```powershell
git clone https://github.com/DavidArayaR/RecetaYA-DavidAraya-TomasHoyt.git receta-ya
Set-Location .\receta-ya
git switch develop
Set-Location .\inventario
java -version
```

### macOS y Linux

```bash
git clone https://github.com/DavidArayaR/RecetaYA-DavidAraya-TomasHoyt.git receta-ya
cd receta-ya
git switch develop
cd inventario
java -version
```

Si Inventario se publica luego en un repositorio independiente, clonar su URL y ejecutar los comandos siguientes desde la raíz de ese repositorio; no habrá que entrar en `inventario/`.

## Compilar, probar y empaquetar

Ejecutar desde la carpeta que contiene este README y `pom.xml`. Detener el JAR si está corriendo antes de `clean`, especialmente en Windows.

### Windows

```powershell
.\mvnw.cmd clean verify
Get-Item .\target\inventario-0.0.1-SNAPSHOT.jar
```

### macOS y Linux

```bash
bash ./mvnw clean verify
ls -lh target/inventario-0.0.1-SNAPSHOT.jar
```

`verify` compila, ejecuta pruebas y empaqueta. Para mostrar la fase `package` de Maven en el video también puede usarse `clean package`. Comprobar `BUILD SUCCESS` y el JAR antes de arrancarlo. El `pom.xml` declara Spring Boot 4.1.1, Java 21, Web MVC, Security, servidor de recursos OAuth2, JPA, Validation, Flyway, H2, Actuator, cliente SQS y dependencias de prueba.

## Configurar y ejecutar localmente

Auth e Inventario deben usar **el mismo** `RECETAYA_JWT_EMISOR` y `RECETAYA_JWT_SECRETO`. El secreto debe ser aleatorio y tener al menos 32 caracteres UTF-8; generarlo fuera de cámara y no guardarlo en Git. Para la demostración con H2 local, mantener SQS deshabilitado.

El README de Auth incluye comandos para generar un secreto local. Generarlo una sola vez y copiar el mismo valor a ambas terminales; no generar otro secreto independiente para Inventario.

### Windows: en la terminal de Inventario

```powershell
$env:RECETAYA_JWT_EMISOR = 'http://localhost:8081'
$env:RECETAYA_JWT_SECRETO = Read-Host 'El mismo secreto JWT usado en Auth'
$env:RECETAYA_SQS_HABILITADA = 'false'
java -jar .\target\inventario-0.0.1-SNAPSHOT.jar --spring.profiles.active=desarrollo
```

### macOS y Linux: en la terminal de Inventario

```bash
export RECETAYA_JWT_EMISOR='http://localhost:8081'
read -r -s -p 'El mismo secreto JWT usado en Auth: ' RECETAYA_JWT_SECRETO; printf '\n'
export RECETAYA_JWT_SECRETO
export RECETAYA_SQS_HABILITADA=false
java -jar target/inventario-0.0.1-SNAPSHOT.jar --spring.profiles.active=desarrollo
```

Ejecutar desde esta carpeta, porque `jdbc:h2:file:./data/inventario-recetaya` es relativa al directorio actual. El perfil `desarrollo` usa H2 de archivo, habilita la consola y deja las lecturas GET públicas; las escrituras requieren JWT con rol `FARMACEUTICO`.

Comprobar `GET http://localhost:8080/actuator/health`: debe responder `200` y `status: UP`. En Windows puede usarse `Invoke-RestMethod http://localhost:8080/actuator/health`; en macOS y Linux, `curl -fsS http://localhost:8080/actuator/health`.

## Obtener el token en Auth

Iniciar sesión desde Postman con `POST http://localhost:8081/api/v1/autenticacion/iniciar-sesion`, usando el correo y la contraseña **locales** del farmacéutico configurado en Auth:

```json
{
  "correoElectronico": "farmaceutico@local.test",
  "contrasena": "<contraseña local>"
}
```

La respuesta `200` incluye `tokenAcceso`. En las solicitudes de escritura de Inventario usar `Authorization: Bearer <tokenAcceso>`. No mostrar ni exportar el token o la contraseña en el video.

## Rutas REST principales

| Método | Ruta | Resultado esperado |
|---|---|---|
| `GET` | `/api/v1/inventario/medicamentos` | `200`, lista de medicamentos. |
| `POST` | `/api/v1/inventario/medicamentos` | `201`, medicamento creado con `id`. |
| `GET` | `/api/v1/inventario/medicamentos/{id}` | `200` o `404`. |
| `PUT` | `/api/v1/inventario/medicamentos/{id}` | `200`, medicamento actualizado. |
| `DELETE` | `/api/v1/inventario/medicamentos/{id}` | `204`; puede dar `409` si tiene existencias o historial de reservas. |
| `GET` / `POST` / `PUT` / `DELETE` | `/api/v1/inventario/sucursales` y `/{id}` | Lista, crea, actualiza o elimina sucursales. |
| `GET` / `PUT` | `/api/v1/inventario/sucursales/{id}/existencias/{medicamentoId}` | Consulta o establece la cantidad física. |

Los `POST`, `PUT` y `DELETE` requieren el rol `FARMACEUTICO`. Las lecturas GET son públicas en el perfil `desarrollo`. El cuerpo para establecer existencias es `{"cantidadFisica":25}`.

## Demostración CRUD y persistencia (IE3, IE4 e IE5)

Usar un medicamento **temporal sin existencias**; no usar el medicamento de una receta. Cambiar el código en cada repetición, por ejemplo `VIDEO-20261028-01`.

1. En Postman, enviar `POST http://localhost:8080/api/v1/inventario/medicamentos` con JWT farmacéutico:

   ```json
   {"codigo":"VIDEO-20261028-01","nombre":"Medicamento de demostración","presentacion":"Caja"}
   ```

   Mostrar `201` y copiar el `id` devuelto.
2. Enviar `GET /api/v1/inventario/medicamentos/{id}`. Mostrar `200` y los mismos datos.
3. Enviar `PUT /api/v1/inventario/medicamentos/{id}` con el **mismo código** y el nombre `Medicamento actualizado`. Mostrar `200`.
4. Enviar `DELETE /api/v1/inventario/medicamentos/{id}`. Mostrar `204` sin cuerpo.
5. Repetir `GET` por `id`. Mostrar `404` y el mensaje `Medicamento no encontrado.`

La consola H2 del perfil `desarrollo` está en `http://localhost:8080/consola-h2`. Usar la URL, usuario y contraseña de la configuración local, sin incluirlos en capturas. Para comprobar cada cambio:

```sql
SELECT codigo, nombre FROM medicamento WHERE codigo = 'VIDEO-20261028-01';
```

La consulta debe mostrar una fila tras el `POST`, el nombre nuevo tras el `PUT` y ninguna fila tras el `DELETE`. No abrir directamente el archivo `.mv.db` mientras el servicio lo utiliza.

Mostrar también errores controlados: código vacío (`400`), escritura con JWT de médico (`403`), recurso eliminado (`404`) y código duplicado (`409`). En las respuestas de la API observar `codigoEstado`, `mensaje` y `ruta` cuando se entreguen.

## Base de datos y estructura

- `src/main/resources/application.yaml`: perfil predeterminado `desarrollo`, configuración JPA/Flyway, JWT y SQS.
- `src/main/resources/application-desarrollo.yaml`: H2 de archivo y consola local. No mostrar sus credenciales durante la presentación.
- `src/main/resources/bd/migracion/`: migraciones de catálogo, existencias, reservas y bandeja de salida.
- `entidad/Medicamento.java`, `Sucursal.java`, `ExistenciaSucursal.java` y entidades de reserva: mapeo JPA. `ExistenciaSucursal` une sucursal y medicamento con dos relaciones `@ManyToOne` y clave compuesta.
- `repositorio/`, `servicio/`, `controlador/` y `transferencia/`: acceso a datos, reglas de negocio, rutas y cuerpos JSON.

La reserva asíncrona mediante SQS queda deshabilitada en esta demostración local. No presentar una receta pendiente como reserva confirmada ni una dispensación como entrega completada.

## Ramas y entrega

En el repositorio actual, `main` representa la versión estable, `develop` integra los servicios y `feature/inventario-service` identifica el trabajo de Inventario. Para IE9, mostrar las ramas y commits descriptivos con `git branch -a` y `git log --oneline`; publicar únicamente la versión revisada en el repositorio que se entregue al docente.

La evaluación solicita enlace(s) de GitHub y un video de **3 a 8 minutos**. En el video deben escucharse las voces de ambos estudiantes con audio claro; mostrar comandos y resultados ejecutados, respuestas de Postman y filas reales de H2. Entregar los enlaces en AVA y enviar copia al correo del docente según la pauta.
