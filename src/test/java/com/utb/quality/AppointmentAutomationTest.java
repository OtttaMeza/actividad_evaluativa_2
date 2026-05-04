package com.utb.quality;

import com.utb.quality.model.Appointment;
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

    @Test
    public void testCP1_RegistroExitoso() {
        Appointment appointment = Appointment.builder()
                .clientName("Juan Perez")
                .petName("Rex")
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(appointment)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(201)
            .body("clientName", equalTo("Juan Perez"))
            .body("petName", equalTo("Rex"));
    }

    @Test
    public void testCP2_CampoVacio() {
        Appointment appointment = Appointment.builder()
                .clientName("") // Vacío
                .petName("Rex")
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(appointment)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(400)
            .body("clientName", equalTo("El nombre es obligatorio"));
    }

    @Test
    public void testCP3_ConflictoHorario() {
        // Truncamos a segundos para evitar problemas de precisión con la BD
        LocalDateTime sameTime = LocalDateTime.now().plusHours(5).withNano(0);
        
        Appointment appointment1 = Appointment.builder()
                .clientName("User 1")
                .petName("Pet 1")
                .appointmentTime(sameTime)
                .build();

        // Primer registro
        given().contentType(ContentType.JSON).body(appointment1).post("/api/appointments");

        // Segundo registro con misma hora
        Appointment appointment2 = Appointment.builder()
                .clientName("User 2")
                .petName("Pet 2")
                .appointmentTime(sameTime)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(appointment2)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(409)
            .body(containsString("Horario no disponible"));
    }

    @Test
    public void testCP9_MascotaNoPertenece() {
        Appointment appointment = Appointment.builder()
                .clientName("Juan")
                .petName("Rex")
                .appointmentTime(LocalDateTime.now().plusDays(2))
                .petId(500L) // Invalido (> 100)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(appointment)
        .when()
            .post("/api/appointments")
        .then()
            .statusCode(400)
            .body(containsString("La mascota seleccionada no pertenece a tu perfil"));
    }
}
