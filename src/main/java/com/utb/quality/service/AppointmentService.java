package com.utb.quality.service;

import com.utb.quality.model.Appointment;
import com.utb.quality.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository repository;

    public Appointment createAppointment(Appointment appointment) {
        // CP2: Validación de campos (Se hace vía @Valid en Controller, pero aquí reforzamos)
        if (appointment.getClientName() == null || appointment.getClientName().isBlank()) {
            throw new IllegalArgumentException("Este campo es obligatorio");
        }

        // CP3: Conflicto horario
        if (repository.findByAppointmentTime(appointment.getAppointmentTime()).isPresent()) {
            throw new IllegalStateException("Horario no disponible");
        }

        // CP9: Mascota no pertenece (Simulación de regla de negocio)
        if (appointment.getPetId() != null && appointment.getPetId() > 100) {
            throw new IllegalArgumentException("La mascota seleccionada no pertenece a tu perfil");
        }

        return repository.save(appointment);
    }

    public List<Appointment> getAllAppointments() {
        return repository.findAll();
    }
}
