package com.utb.quality.controller;

import com.utb.quality.dto.AppointmentRequest;
import com.utb.quality.model.Appointment;
import com.utb.quality.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Gestión de Citas", description = "Operaciones para agendar citas veterinarias")
public class AppointmentController {

    @Autowired
    private AppointmentService service;

    @PostMapping
    @Operation(summary = "Crear una nueva cita", description = "Valida campos obligatorios y traslape de horario")
    public ResponseEntity<Appointment> create(@Valid @RequestBody AppointmentRequest request) {
        return new ResponseEntity<>(service.createAppointment(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar todas las citas")
    public List<Appointment> getAll() {
        return service.getAllAppointments();
    }
}
