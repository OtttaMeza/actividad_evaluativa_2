package com.utb.quality;

import com.utb.quality.dto.AppointmentRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AppointmentAutomationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setup() {
        RestAssured.port = port;
    }

    // CP1: Registro exitoso — verifica id generado, todos los campos y Content-Type
    @Test
    public void testCP1_RegistroExitoso() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withNano(0);

        AppointmentRequest request = AppointmentRequest.builder()
                .clientName("Juan Perez")
                .petName("Rex")
                .appointmentTime(appointmentTime)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("id", notNullValue())
            .body("id", greaterThan(0))
            .body("clientName", equalTo("Juan Perez"))
            .body("petName", equalTo("Rex"))
            .body("appointmentTime", notNullValue());
    }

    // CP2: Campo clientName vacío — verifica 400 y mensaje específico de validación
    @Test
    public void testCP2_ClientNameVacio() {
        AppointmentRequest request = AppointmentRequest.builder()
                .clientName("")
                .petName("Rex")
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("clientName", equalTo("El nombre es obligatorio"));
    }

    // CP5: Campo petName vacío — verifica 400 y mensaje específico de validación
    @Test
    public void testCP5_PetNameVacio() {
        AppointmentRequest request = AppointmentRequest.builder()
                .clientName("Juan Perez")
                .petName("")
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("petName", equalTo("El nombre de la mascota es obligatorio"));
    }

    // CP6: Múltiples campos inválidos — verifica 400 con ambos mensajes en la misma respuesta
    @Test
    public void testCP6_MultiplesCamposVacios() {
        AppointmentRequest request = AppointmentRequest.builder()
                .clientName("")
                .petName("")
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("clientName", equalTo("El nombre es obligatorio"))
            .body("petName", equalTo("El nombre de la mascota es obligatorio"));
    }

    // CP3: Conflicto de horario — verifica que el primero se guardó (201) y el segundo falla (409)
    @Test
    public void testCP3_ConflictoHorario() {
        LocalDateTime sameTime = LocalDateTime.now().plusHours(5).withNano(0);

        AppointmentRequest request1 = AppointmentRequest.builder()
                .clientName("User 1")
                .petName("Pet 1")
                .appointmentTime(sameTime)
                .build();

        // Primer registro — debe guardarse con éxito
        given()
            .contentType(ContentType.JSON)
            .body(request1)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(201)
            .body("clientName", equalTo("User 1"));

        // Segundo registro con la misma hora — debe fallar
        AppointmentRequest request2 = AppointmentRequest.builder()
                .clientName("User 2")
                .petName("Pet 2")
                .appointmentTime(sameTime)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(request2)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(409)
            .body(containsString("Horario no disponible"));
    }

    // CP9: Mascota con id > 100 no pertenece al perfil
    @Test
    public void testCP9_MascotaNoPertenece() {
        AppointmentRequest request = AppointmentRequest.builder()
                .clientName("Juan")
                .petName("Rex")
                .appointmentTime(LocalDateTime.now().plusDays(2))
                .petId(500L)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(400)
            .body(containsString("La mascota seleccionada no pertenece a tu perfil"));
    }

    // CP7: petId válido (≤ 100) — cubre la rama false del if(petId > 100)
    @Test
    public void testCP7_MascotaValida() {
        AppointmentRequest request = AppointmentRequest.builder()
                .clientName("Maria Lopez")
                .petName("Luna")
                .appointmentTime(LocalDateTime.now().plusDays(3))
                .petId(50L)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("clientName", equalTo("Maria Lopez"))
            .body("petName", equalTo("Luna"))
            .body("petId", equalTo(50));
    }

    // CP4: GET todas las citas — verifica 200, lista no vacía y datos del primer registro
//    @Test
//    public void testCP4_ListarCitas() {
//        // Seed: crear una cita conocida
//        AppointmentRequest request = AppointmentRequest.builder()
//                .clientName("Carlos Ruiz")
//                .petName("Toby")
//                .appointmentTime(LocalDateTime.now().plusDays(10).withNano(0))
//                .build();
//
//        given()
//            .contentType(ContentType.JSON)
//            .body(request)
//        .when()
//            .post("/api/appointments");
//
//        // Verificar que GET retorna al menos esa cita con datos correctos
//        given()
//        .when()
//            .get("/api/appointments")
//        .then()
//            .statusCode(200)
//            .contentType(ContentType.JSON)
//            .body("$", not(empty()))
//            .body("size()", greaterThanOrEqualTo(1))
//            .body("clientName", hasItem("Carlos Ruiz"))
//            .body("petName", hasItem("Toby"));
//    }
}
