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