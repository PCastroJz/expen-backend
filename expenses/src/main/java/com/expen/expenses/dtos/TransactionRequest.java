package com.expen.expenses.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequest {

    @NotBlank(message = "El tipo de transacción es obligatorio")
    private String type;

    @NotBlank(message = "La categoría es obligatoria")
    private String category;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser un número positivo")
    private Double amount;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    @NotNull(message = "El ID de la cuenta es obligatorio")
    private Long accountId;

    private boolean schedule = false;

    private boolean pay = false;

    @NotNull(message = "El metodo es obligatorio")
    private String paymentMethod;
}