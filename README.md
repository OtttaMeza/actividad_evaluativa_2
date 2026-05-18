# actividad_evaluativa_2 — Cobertura, Análisis Estático y CI/CD

**Universidad Tecnológica de Bolívar (UTB)** · Asignatura: **Métricas de Calidad de Software**

Documentación técnica de la implementación de **JaCoCo** (cobertura de pruebas), su **integración con SonarCloud** (análisis estático y Quality Gate) y la **automatización del pipeline CI/CD con Jenkins** sobre una API REST de gestión de citas veterinarias construida con Spring Boot.

| Recurso | Enlace |
|---|---|
| Repositorio | https://github.com/OtttaMeza/actividad_evaluativa_2 |
| Dashboard SonarCloud | https://sonarcloud.io/dashboard?id=OtttaMeza_actividad_evaluativa_2 |
| Swagger UI (local) | http://localhost:8080/swagger-ui.html |

---

## Tabla de contenido

1. [Descripción del proyecto](#1-descripción-del-proyecto)
2. [Arquitectura y stack tecnológico](#2-arquitectura-y-stack-tecnológico)
3. [Paso 1 — Configuración de JaCoCo](#3-paso-1--configuración-de-jacoco)
4. [Paso 2 — Integración con SonarCloud](#4-paso-2--integración-con-sonarcloud)
5. [Paso 3 — Pipeline CI/CD con Jenkins](#5-paso-3--pipeline-cicd-con-jenkins)
6. [Paso 4 — Refactor del DTO `AppointmentRequest`](#6-paso-4--refactor-del-dto-appointmentrequest)
7. [Problemas encontrados y soluciones](#7-problemas-encontrados-y-soluciones)
8. [Comandos de referencia rápida](#8-comandos-de-referencia-rápida)
9. [Estructura del proyecto](#9-estructura-del-proyecto)

---

## 1. Descripción del proyecto

`actividad_evaluativa_2` es una **API REST** para la **gestión de citas veterinarias**. Expone operaciones para agendar y listar citas, aplicando reglas de negocio y validaciones de integridad de datos.

El objetivo académico de esta actividad **no es la funcionalidad** en sí, sino instrumentar el proyecto con un flujo completo de **aseguramiento de calidad de software**:

- **Medir** la cobertura de las pruebas automatizadas con JaCoCo.
- **Analizar** la calidad del código (bugs, code smells, vulnerabilidades, duplicación) con SonarCloud.
- **Automatizar** el ciclo build → test → análisis → Quality Gate mediante un pipeline declarativo de Jenkins, de forma que ningún cambio que no cumpla los umbrales de calidad pueda promoverse.

### Dominio funcional

| Endpoint | Método | Descripción | Códigos de respuesta |
|---|---|---|---|
| `/api/appointments` | `POST` | Crea una cita validando campos obligatorios, conflicto de horario y pertenencia de mascota | `201`, `400`, `409` |
| `/api/appointments` | `GET` | Lista todas las citas registradas | `200` |

Reglas de negocio implementadas en `AppointmentService`:

- **CP2 — Campos obligatorios:** `clientName` no puede ser nulo ni vacío (también vía `@Valid`).
- **CP3 — Conflicto de horario:** no se permiten dos citas con el mismo `appointmentTime` (`409 Conflict`).
- **CP9 — Pertenencia de mascota:** un `petId > 100` se rechaza como mascota no perteneciente al perfil (`400 Bad Request`).

Los errores se traducen a respuestas HTTP semánticas mediante `GlobalExceptionHandler` (`@RestControllerAdvice`).

---

## 2. Arquitectura y stack tecnológico

### Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 17 |
| Framework | Spring Boot | 3.2.0 |
| Build / dependencias | Maven | 3.x |
| Persistencia | Spring Data JPA | (gestionado por Spring Boot parent) |
| Base de datos | H2 Database (in-memory) | runtime |
| Validación | Spring Validation (Jakarta Bean Validation) | (gestionado) |
| Reducción de boilerplate | Lombok | `optional` |
| Documentación API | Springdoc OpenAPI (Swagger UI) | 2.3.0 |
| Pruebas | Spring Boot Test + JUnit 5 + RestAssured | (gestionado) |
| Cobertura | JaCoCo Maven Plugin | 0.8.11 |
| Análisis estático | SonarCloud + Sonar Maven Plugin | 4.0.0.4121 |
| CI/CD | Jenkins (pipeline declarativo) | — |

### Arquitectura de la aplicación (capas)

```
HTTP Request
     │
     ▼
┌─────────────────────────┐
│  AppointmentController  │  @RestController — recibe AppointmentRequest (DTO)
└─────────────────────────┘
     │
     ▼
┌─────────────────────────┐
│   AppointmentService    │  Reglas de negocio (CP2, CP3, CP9) · mapeo DTO→Entidad
└─────────────────────────┘
     │
     ▼
┌─────────────────────────┐
│  AppointmentRepository  │  Spring Data JPA · findByAppointmentTime
└─────────────────────────┘
     │
     ▼
┌─────────────────────────┐
│      H2 (in-memory)     │
└─────────────────────────┘

   GlobalExceptionHandler  (@RestControllerAdvice) — traduce excepciones a HTTP 400/409
```

### Pipeline CI/CD (flujo de calidad)

```
                            ┌──────────────────────────────────────────────────────┐
                            │                      JENKINS                          │
                            │                                                       │
  git push (rama main)      │  ┌──────────┐   ┌────────────────────────┐            │
 ────────────────────────►  │  │ Checkout │──►│ Build, Test & Analyze  │            │
                            │  └──────────┘   │  mvn -B clean verify   │            │
                            │                 │     sonar:sonar        │            │
                            │                 └───────────┬────────────┘            │
                            │                             │                         │
                            │              ┌──────────────┼───────────────┐         │
                            │              ▼              ▼                ▼         │
                            │        ┌───────────┐  ┌───────────┐   ┌──────────────┐│
                            │        │  JUnit /  │  │  JaCoCo   │   │  Sonar       ││
                            │        │ Surefire  │  │  report   │   │  Scanner     ││
                            │        └───────────┘  └─────┬─────┘   └──────┬───────┘│
                            │                             │ jacoco.xml     │ push   │
                            │                             ▼                ▼        │
                            │                       ┌───────────────────────────┐  │
                            │                       │   jacoco:check (verify)   │  │
                            │                       │  LINE ≥ 0.80 / BR ≥ 0.70  │  │
                            │                       └───────────────────────────┘  │
                            │                             │                         │
                            │                             ▼                         │
                            │                 ┌────────────────────────┐            │
                            │                 │   Quality Gate         │            │       ┌──────────────┐
                            │                 │ waitForQualityGate     │ ◄──webhook─┼──────►│  SonarCloud  │
                            │                 │ abortPipeline: true    │            │       │  (análisis)  │
                            │                 └───────────┬────────────┘            │       └──────────────┘
                            │                  PASS │      │ FAIL                    │
                            │                       ▼      └──────► ❌ build fallido │
                            │                 ┌────────────────────────┐            │
                            │                 │   Publish Reports      │            │
                            │                 │ junit · publishHTML ·  │            │
                            │                 │   archiveArtifacts     │            │
                            │                 └────────────────────────┘            │
                            │                       │                               │
                            │                       ▼  post { cleanup → cleanWs }    │
                            └──────────────────────────────────────────────────────┘
```

---

## 3. Paso 1 — Configuración de JaCoCo

**JaCoCo** (Java Code Coverage) instrumenta la JVM durante la ejecución de los tests para medir qué líneas y ramas del código son ejecutadas. Es el insumo de cobertura que después consume SonarCloud.

### 3.1 Properties parametrizadas

Se parametrizó la versión del plugin y los umbrales de cobertura en `<properties>` para no tener valores "mágicos" dispersos:

```xml
<properties>
    <java.version>17</java.version>
    <jacoco.version>0.8.11</jacoco.version>
    <jacoco.line.coverage>0.80</jacoco.line.coverage>
    <jacoco.branch.coverage>0.70</jacoco.branch.coverage>
    <!-- ... propiedades de Sonar (Paso 2) ... -->
</properties>
```

| Propiedad | Significado |
|---|---|
| `jacoco.version` | Versión del plugin (`0.8.11`, compatible con Java 17). |
| `jacoco.line.coverage` | Umbral mínimo de **cobertura de líneas**: `0.80` (80 %). |
| `jacoco.branch.coverage` | Umbral mínimo de **cobertura de ramas**: `0.70` (70 %). |

### 3.2 Plugin `jacoco-maven-plugin`

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.version}</version>
    <configuration>
        <excludes>
            <exclude>**/ActivityApplication.class</exclude>
            <exclude>**/model/**</exclude>
            <exclude>**/repository/**</exclude>
            <exclude>**/dto/**</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>${jacoco.line.coverage}</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>${jacoco.branch.coverage}</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 3.3 Explicación de cada sección

| Sección | Fase Maven | Función |
|---|---|---|
| **`prepare-agent`** | `initialize` (implícita) | Inyecta el agente Java de JaCoCo (`-javaagent`) en la JVM de pruebas. Sin esto, no se registra ninguna ejecución de código. |
| **`report`** | `test` | Genera el reporte de cobertura: HTML legible en `target/site/jacoco/index.html` y **XML** en `target/site/jacoco/jacoco.xml` (este XML es el que consume SonarCloud). |
| **`check`** | `verify` | **Quality Gate local.** Falla el build (`BUILD FAILURE`) si la cobertura de líneas del *bundle* es < 80 % o la de ramas < 70 %. Garantiza que el umbral se respete incluso antes de llegar a SonarCloud. |

### 3.4 Justificación de las exclusiones

Las clases excluidas **no aportan lógica de negocio comprobable** y, de incluirse, distorsionarían la métrica (especialmente las ramas que Lombok genera automáticamente en `equals`, `hashCode`, `canEqual`, etc.):

| Exclusión | Motivo |
|---|---|
| `**/ActivityApplication.class` | Clase *bootstrap* de Spring Boot (`main`); solo arranca el contexto. |
| `**/model/**` | Entidades JPA (`Appointment`) — POJOs con código generado por Lombok. |
| `**/repository/**` | Interfaces de Spring Data JPA — implementación generada por el framework, sin código propio que probar. |
| `**/dto/**` | DTOs (`AppointmentRequest`) — POJOs con Lombok; sus ramas sintéticas hundían la cobertura de ramas (ver [Paso 4](#6-paso-4--refactor-del-dto-appointmentrequest)). |

> Nota: en JaCoCo las exclusiones usan rutas de **`.class`** (bytecode). En SonarCloud (Paso 2) las mismas exclusiones se expresan con **`.java`** (código fuente).

### 3.5 Verificación

```bash
mvn clean test
```

Resultado verificado:

- `BUILD SUCCESS`
- 4/4 tests ejecutados (`AppointmentAutomationTest`: CP1, CP2, CP3, CP9)
- Reporte generado en `target/site/jacoco/index.html` y `jacoco.xml`
- 3 clases analizadas tras aplicar exclusiones (`controller`, `service`, `exception`)

---

## 4. Paso 2 — Integración con SonarCloud

**SonarCloud** realiza el análisis estático (bugs, vulnerabilidades, *code smells*, duplicación) y consume el `jacoco.xml` para reportar cobertura dentro de su **Quality Gate**.

### 4.1 Properties de Sonar en el `pom.xml`

```xml
<properties>
    <!-- ... properties de JaCoCo (Paso 1) ... -->
    <sonar.maven.plugin.version>4.0.0.4121</sonar.maven.plugin.version>
    <sonar.projectKey>OtttaMeza_actividad_evaluativa_2</sonar.projectKey>
    <sonar.projectName>actividad_evaluativa_2</sonar.projectName>
    <sonar.organization>otttameza</sonar.organization>
    <sonar.host.url>https://sonarcloud.io</sonar.host.url>
    <sonar.java.source>17</sonar.java.source>
    <sonar.coverage.jacoco.xmlReportPaths>${project.build.directory}/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
    <sonar.exclusions>**/ActivityApplication.java,**/model/**,**/repository/**,**/dto/**</sonar.exclusions>
    <sonar.coverage.exclusions>**/ActivityApplication.java,**/model/**,**/repository/**,**/dto/**</sonar.coverage.exclusions>
</properties>
```

| Propiedad | Función |
|---|---|
| `sonar.projectKey` | Identificador único del proyecto en SonarCloud. |
| `sonar.organization` | Organización SonarCloud propietaria del proyecto. |
| `sonar.host.url` | URL del servidor (SonarCloud SaaS). |
| `sonar.java.source` | Nivel de lenguaje Java analizado (17). |
| `sonar.coverage.jacoco.xmlReportPaths` | **Puente JaCoCo → SonarCloud.** Ruta del `jacoco.xml` desde el cual Sonar importa la cobertura. |
| `sonar.exclusions` | Clases excluidas del análisis estático (espejo de las exclusiones JaCoCo, con extensión `.java`). |
| `sonar.coverage.exclusions` | Clases excluidas del **cálculo de cobertura** en Sonar. |

### 4.2 Plugin `sonar-maven-plugin`

```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>${sonar.maven.plugin.version}</version>
</plugin>
```

### 4.3 Análisis manual de verificación

```bash
mvn clean test sonar:sonar -Dsonar.token=<TOKEN>
```

Resultado verificado: `ANALYSIS SUCCESSFUL`, con la cobertura de JaCoCo reflejada en el dashboard de SonarCloud.

> El token es un secreto: **nunca** se versiona en el `pom.xml` ni en el `Jenkinsfile`. En local se pasa por `-Dsonar.token=...`; en Jenkins lo inyecta `withSonarQubeEnv` (Paso 3).

### 4.4 Desactivación del Automatic Analysis

SonarCloud, por defecto, ejecuta **Automatic Analysis**. Esto **entra en conflicto** con el análisis basado en CI (Maven + Jenkins): no se pueden tener ambos a la vez sobre el mismo proyecto.

**Acción:** SonarCloud → *Project* → **Administration → Analysis Method → desactivar Automatic Analysis**, dejando únicamente el análisis vía CI (Maven). Esto es además requisito para que la cobertura de JaCoCo se importe correctamente.

---

## 5. Paso 3 — Pipeline CI/CD con Jenkins

Pipeline **declarativo** de 4 stages que automatiza todo el ciclo de calidad. El principio rector: **una sola invocación de Maven** (`clean verify sonar:sonar`) para que JaCoCo, las pruebas y el escáner Sonar compartan el mismo *build* y el reporte de cobertura sea consistente.

### 5.1 `Jenkinsfile` completo

```groovy
pipeline {
    agent any
    // Recomendado en entornos con varios agentes: agent { label 'maven' }

    tools {
        maven 'Maven'
        jdk 'JDK17'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/OtttaMeza/actividad_evaluativa_2.git'
            }
        }

        stage('Build, Test & Analyze') {
            steps {
                withSonarQubeEnv('SONAR_CLOUD') {
                    sh 'mvn -B clean verify sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Publish Reports') {
            steps {
                junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                publishHTML(target: [
                    allowMissing         : false,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : 'target/site/jacoco',
                    reportFiles          : 'index.html',
                    reportName           : 'JaCoCo Coverage Report'
                ])
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            echo "Resultado: ${currentBuild.currentResult}"
        }
        success {
            echo 'Pipeline OK — Quality Gate aprobado'
        }
        failure {
            echo 'Pipeline FAILED — revisar logs y SonarCloud'
            // mail to: 'otalvaro.jose.meza@gmail.com',
            //      subject: "FALLO ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //      body: "Ver: ${env.BUILD_URL}"
        }
        cleanup {
            cleanWs()
        }
    }
}
```

### 5.2 Configuración global (`agent`, `tools`, `options`)

| Bloque | Valor | Función |
|---|---|---|
| `agent any` | — | Ejecuta en cualquier nodo/agente disponible. |
| `tools.maven` | `'Maven'` | Instalación de Maven configurada en *Manage Jenkins → Tools*. |
| `tools.jdk` | `'JDK17'` | JDK 17 configurado en *Manage Jenkins → Tools*. |
| `options.timeout` | 30 min | Aborta el pipeline completo si excede 30 minutos (protege agentes ante cuelgues). |
| `options.disableConcurrentBuilds()` | — | Evita ejecuciones simultáneas del mismo job (previene corromper el *workspace* y análisis Sonar en paralelo). |
| `options.timestamps()` | — | Antepone marca de tiempo a cada línea del log (diagnóstico de rendimiento). |

### 5.3 Explicación de cada stage

#### Stage 1 — `Checkout`
Clona la rama `main` del repositorio. Es el punto de entrada: garantiza que el análisis se ejecuta sobre el código exacto que se quiere promover.

#### Stage 2 — `Build, Test & Analyze`
El **corazón del pipeline**. Una sola línea de Maven encadena:

- `clean` — elimina `target/` para un build reproducible.
- `verify` — compila, ejecuta los tests (JaCoCo `prepare-agent` activo), genera el reporte (`report` en fase `test`) y ejecuta el **Quality Gate local** (`jacoco:check` en fase `verify`).
- `sonar:sonar` — el escáner Sonar publica resultados y la cobertura del `jacoco.xml` en SonarCloud.

`withSonarQubeEnv('SONAR_CLOUD')` envuelve la ejecución e **inyecta automáticamente** la URL del servidor y el token de autenticación desde la configuración de Jenkins (*Manage Jenkins → System → SonarQube servers*, instalación llamada `SONAR_CLOUD`). El token **no se hardcodea** en ningún archivo versionado.

El flag `-B` (*batch mode*) desactiva la salida interactiva/coloreada, óptima para logs de CI.

#### Stage 3 — `Quality Gate`
`waitForQualityGate abortPipeline: true` espera (vía *webhook* de SonarCloud) el veredicto del Quality Gate del servidor. Si SonarCloud **no aprueba** (bugs, vulnerabilidades, cobertura insuficiente, etc.), `abortPipeline: true` **detiene el pipeline con fallo**. El `timeout(5 min)` evita un bloqueo indefinido si el webhook no llega.

Este es el control de calidad **bloqueante**: ningún cambio que no cumpla la barra de calidad puede continuar.

#### Stage 4 — `Publish Reports`
Solo se alcanza si el Quality Gate aprobó. Publica los resultados para trazabilidad:

| Paso | Función |
|---|---|
| `junit` | Publica resultados de pruebas (`target/surefire-reports/*.xml`) en la UI de Jenkins. `allowEmptyResults: false` falla si no hay tests. |
| `publishHTML` | Publica el reporte HTML de JaCoCo como artefacto navegable ("JaCoCo Coverage Report"). |
| `archiveArtifacts` | Archiva el JAR generado (`target/*.jar`) con *fingerprint* para trazabilidad de binarios. |

### 5.4 Bloque `post`

| Condición | Acción |
|---|---|
| `always` | Registra el resultado final (`currentBuild.currentResult`) — se ejecuta siempre. |
| `success` | Mensaje de confirmación (Quality Gate aprobado). |
| `failure` | Mensaje de alerta; plantilla de notificación por correo comentada y lista para activar. |
| `cleanup` | `cleanWs()` — limpia el *workspace* del agente tras cada ejecución (evita arrastrar estado entre builds). |

---

## 6. Paso 4 — Refactor del DTO `AppointmentRequest`

### 6.1 Hallazgo de SonarCloud — regla `java:S4684`

SonarCloud marcó como **vulnerabilidad** que `AppointmentController` usaba la **entidad JPA `Appointment` directamente como `@RequestBody`**.

> **Regla `java:S4684` — "Persistent entities should not be used as arguments of `@RequestMapping` methods".**
> Vincular el cuerpo de la petición directamente a una entidad persistente expone **todos** los campos de la entidad al *binding* automático, habilitando un ataque de **mass assignment**: un atacante podría enviar campos no previstos (por ejemplo `id`) y manipular el estado persistido.

### 6.2 Solución — patrón DTO de entrada

Se introdujo un POJO de transferencia **`AppointmentRequest`** en `com.utb.quality.dto`, desacoplando el contrato de entrada de la entidad de persistencia.

**`src/main/java/com/utb/quality/dto/AppointmentRequest.java`** (creado):

```java
package com.utb.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String clientName;

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    private String petName;

    @NotNull(message = "La fecha y hora son obligatorias")
    private LocalDateTime appointmentTime;

    private Long petId;
}
```

Características clave:

- **Mismos campos que la entidad excepto `id`.** El cliente ya no puede inyectar el identificador → se cierra el vector de mass-assignment.
- **Mismas anotaciones de validación** (`@NotBlank`, `@NotNull`) con **mensajes idénticos**, de modo que el contrato de errores HTTP no cambia.

**`AppointmentController.java`** (modificado) — recibe el DTO, sigue devolviendo la entidad (con `id`) en la respuesta:

```java
@PostMapping
@Operation(summary = "Crear una nueva cita", description = "Valida campos obligatorios y traslape de horario")
public ResponseEntity<Appointment> create(@Valid @RequestBody AppointmentRequest request) {
    return new ResponseEntity<>(service.createAppointment(request), HttpStatus.CREATED);
}
```

**`AppointmentService.java`** (modificado) — recibe el DTO y lo mapea a la entidad mediante `Appointment.builder()`:

```java
public Appointment createAppointment(AppointmentRequest request) {
    // CP2: refuerzo de validación de campos
    if (request.getClientName() == null || request.getClientName().isBlank()) {
        throw new IllegalArgumentException("Este campo es obligatorio");
    }
    // CP3: conflicto de horario
    if (repository.findByAppointmentTime(request.getAppointmentTime()).isPresent()) {
        throw new IllegalStateException("Horario no disponible");
    }
    // CP9: pertenencia de mascota
    if (request.getPetId() != null && request.getPetId() > 100) {
        throw new IllegalArgumentException("La mascota seleccionada no pertenece a tu perfil");
    }

    Appointment appointment = Appointment.builder()
            .clientName(request.getClientName())
            .petName(request.getPetName())
            .appointmentTime(request.getAppointmentTime())
            .petId(request.getPetId())
            .build();

    return repository.save(appointment);
}
```

### 6.3 Impacto sobre las pruebas

Los **4 tests de integración** (`AppointmentAutomationTest`: CP1, CP2, CP3, CP9) **pasaron sin modificación**: el JSON de entrada y de salida tiene exactamente los mismos campos, por lo que el contrato observable de la API no cambió. El paquete `dto` quedó **excluido** de JaCoCo y SonarCloud (POJO con Lombok), evitando que sus ramas sintéticas distorsionaran la cobertura.

### 6.4 Archivos modificados/creados

| Archivo | Acción |
|---|---|
| `pom.xml` | Modificado: plugins y properties de JaCoCo + Sonar |
| `Jenkinsfile` | Creado/modificado: pipeline CI/CD completo |
| `src/main/java/com/utb/quality/dto/AppointmentRequest.java` | **Creado:** DTO de entrada |
| `src/main/java/com/utb/quality/controller/AppointmentController.java` | Modificado: recibe `AppointmentRequest` |
| `src/main/java/com/utb/quality/service/AppointmentService.java` | Modificado: recibe DTO y mapea a entidad |

---

## 7. Problemas encontrados y soluciones

| # | Problema | Causa raíz | Solución |
|---|---|---|---|
| 1 | `SonarQube installation does not match` | El nombre del servidor SonarQube en Jenkins era `SONAR_CLOUD`, pero el `Jenkinsfile` referenciaba `sonarcloud`. | Alinear el nombre: `withSonarQubeEnv('SONAR_CLOUD')` coincidiendo exactamente con *Manage Jenkins → System → SonarQube servers*. |
| 2 | `Automatic Analysis conflict` | SonarCloud tenía el Automatic Analysis activo, incompatible con el análisis vía CI/Maven. | Desactivar en SonarCloud → *Administration → Analysis Method*; dejar solo análisis por CI. |
| 3 | Quality Gate en `ERROR` por regla `java:S4684` | La entidad JPA `Appointment` se usaba como `@RequestBody` (vulnerabilidad de mass-assignment). | Refactor: introducir el DTO `AppointmentRequest` (sin `id`) como cuerpo de la petición ([Paso 4](#6-paso-4--refactor-del-dto-appointmentrequest)). |
| 4 | `branch coverage 0.14` (build fallido por `jacoco:check`) | Lombok genera ramas sintéticas (`equals`, `hashCode`, `canEqual`) en `AppointmentRequest`; al contarse, hundían la cobertura de ramas muy por debajo del 70 %. | Excluir `**/dto/**` en JaCoCo y en `sonar.(coverage.)exclusions` (los DTOs no contienen lógica comprobable). |
| 5 | `recordCoverage` no disponible en Jenkins | El plugin *Coverage* no estaba instalado en la instancia de Jenkins. | Reemplazar `recordCoverage` por `publishHTML`, que publica el reporte HTML de JaCoCo sin dependencias adicionales. |

---

## 8. Comandos de referencia rápida

```bash
# Ejecutar la aplicación localmente (http://localhost:8080)
mvn spring-boot:run

# Ejecutar pruebas + generar reporte JaCoCo (HTML + XML)
mvn clean test
#   → target/site/jacoco/index.html   (reporte navegable)
#   → target/site/jacoco/jacoco.xml   (insumo para SonarCloud)

# Build completo + Quality Gate local de cobertura (jacoco:check)
mvn clean verify

# Análisis manual contra SonarCloud (token NO se versiona)
mvn clean test sonar:sonar -Dsonar.token=<TOKEN>

# Equivalente exacto a lo que ejecuta el pipeline de Jenkins
mvn -B clean verify sonar:sonar
```

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Dashboard SonarCloud | https://sonarcloud.io/dashboard?id=OtttaMeza_actividad_evaluativa_2 |
| Reporte JaCoCo (local) | `target/site/jacoco/index.html` |
| Reporte JaCoCo (Jenkins) | Build → "JaCoCo Coverage Report" |

### Umbrales de calidad

| Métrica | Umbral mínimo | Dónde se aplica |
|---|---|---|
| Cobertura de líneas (LINE) | 80 % (`0.80`) | `jacoco:check` (fase `verify`) + SonarCloud |
| Cobertura de ramas (BRANCH) | 70 % (`0.70`) | `jacoco:check` (fase `verify`) |
| Quality Gate SonarCloud | Definido en SonarCloud | Stage `Quality Gate` (bloqueante) |

---

## 9. Estructura del proyecto

```text
actividad_evaluativa_2/
├── pom.xml                                  # Maven: deps, JaCoCo 0.8.11, Sonar 4.0.0.4121, properties
├── Jenkinsfile                              # Pipeline declarativo CI/CD (4 stages)
├── README.md                                # Esta documentación técnica
├── src/
│   ├── main/
│   │   └── java/com/utb/quality/
│   │       ├── ActivityApplication.java     # Bootstrap Spring Boot (excluido de cobertura)
│   │       ├── controller/
│   │       │   └── AppointmentController.java   # REST · recibe AppointmentRequest
│   │       ├── service/
│   │       │   └── AppointmentService.java      # Reglas de negocio (CP2/CP3/CP9) · mapeo DTO→Entidad
│   │       ├── repository/
│   │       │   └── AppointmentRepository.java   # Spring Data JPA (excluido de cobertura)
│   │       ├── model/
│   │       │   └── Appointment.java             # Entidad JPA (excluida de cobertura)
│   │       ├── dto/
│   │       │   └── AppointmentRequest.java      # DTO de entrada — fix S4684 (excluido de cobertura)
│   │       └── exception/
│   │           └── GlobalExceptionHandler.java  # @RestControllerAdvice → HTTP 400/409
│   └── test/
│       └── java/com/utb/quality/
│           └── AppointmentAutomationTest.java   # RestAssured + JUnit 5 (CP1, CP2, CP3, CP9)
└── target/
    └── site/jacoco/
        ├── index.html                       # Reporte de cobertura navegable
        └── jacoco.xml                       # Reporte XML consumido por SonarCloud
```

> Las clases en `model/`, `repository/`, `dto/` y `ActivityApplication` se **excluyen del análisis de cobertura** tanto en JaCoCo (rutas `.class`) como en SonarCloud (rutas `.java`), por ser código *bootstrap*, generado por el framework o POJOs con Lombok sin lógica comprobable. La cobertura efectiva se mide sobre `controller/`, `service/` y `exception/`.

---

**Autor:** Otalvaro Meza · **Asignatura:** Métricas de Calidad de Software · **Institución:** Universidad Tecnológica de Bolívar (UTB)
