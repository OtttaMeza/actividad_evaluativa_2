# Gestión de Citas Veterinarias - Suite de Pruebas Automatizadas (UTB)

Este proyecto ha sido desarrollado para la **Actividad Evaluativa 2** de la asignatura **Métricas de Calidad de Software**. Consiste en una API REST robusta para la gestión de citas, diseñada bajo principios de Clean Code y validaciones de integridad de datos, acompañada de una suite de pruebas automatizadas.

## 🛠️ Stack Tecnológico
*   **Lenguaje**: Java 17
*   **Framework**: Spring Boot 3.2.0
*   **Base de Datos**: H2 Database (In-Memory)
*   **Documentación**: SpringDoc OpenAPI 3 (Swagger)
*   **Pruebas Automatizadas**: RestAssured + JUnit 5
*   **Herramienta de Construcción**: Maven

## 📂 Estructura del Proyecto
```text
actividad_evaluativa_2/
├── src/
│   ├── main/
│   │   ├── java/com/utb/quality/
│   │   │   ├── controller/      # Endpoints REST (Swagger Annotations)
│   │   │   ├── model/           # Entidades y validaciones
│   │   │   ├── repository/      # Persistencia de datos (JPA)
│   │   │   ├── service/         # Lógica de negocio y Reglas de Calidad
│   │   │   └── exception/       # Manejo global de errores técnicos
│   └── test/
│       └── java/com/utb/quality/# Scripts de automatización RestAssured
└── pom.xml                      # Dependencias y configuración de Maven
```

## 🚀 Instalación y Ejecución

### Requisitos Previos
*   JDK 17 instalado.
*   Maven 3.x instalado.

### Pasos para ejecutar
1. Clonar o descargar el proyecto.
2. Abrir una terminal en la raíz del proyecto.
3. Ejecutar el servidor:
   ```bash
   mvn spring-boot:run
   ```
4. El servidor iniciará en `http://localhost:8080`.

## 📖 Documentación de la API (Swagger)
Una vez el servidor esté corriendo, puedes acceder a la interfaz interactiva de Swagger en:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

Desde aquí puedes probar manualmente los endpoints `POST` y `GET` de citas.

## 🧪 Automatización de Pruebas (Métricas de Calidad)
La suite de pruebas automatizadas se encuentra en `AppointmentAutomationTest.java`. Estas pruebas validan los siguientes escenarios críticos definidos en la planeación:

### Casos de Prueba Automatizados:
1.  **CP1 (Registro Exitoso)**: Valida que los datos correctos generen un registro con estado `201 Created`.
2.  **CP2 (Campos Vacíos)**: Valida la integridad de datos impidiendo registros sin nombre de cliente (Estado `400`).
3.  **CP3 (Conflicto Horario)**: Implementa una validación de negocio para evitar que dos citas se crucen en el mismo minuto (Estado `409`). *Nota: Se aplica truncado de nanosegundos para precisión en la comparación*.
4.  **CP9 (Pertenencia de Mascota)**: Simula una regla de seguridad donde IDs de mascota superiores a 100 son rechazados por no pertenecer al perfil del usuario (Estado `400`).

### Ejecución de Pruebas:
Para ejecutar todos los scripts de prueba y obtener el reporte de calidad:
```bash
mvn test
```

## ⚠️ Manejo de Excepciones
El proyecto cuenta con un `GlobalExceptionHandler` que traduce errores internos de Java en respuestas HTTP semánticas, asegurando que el cliente de la API (o el script de prueba) reciba mensajes claros como:
*   `409 Conflict`: "Horario no disponible"
*   `400 Bad Request`: "El nombre es obligatorio"

---
**Autor**: [Otalvaro Meza]
**Asignatura**: Métricas de Calidad de Software
**Institución**: Universidad Tecnológica de Bolívar (UTB)
