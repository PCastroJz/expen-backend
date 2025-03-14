package com.expen.expenses.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShareAccountRequest {
    @NotNull(message = "El ID de la cuenta es obligatorio")
    private Long accountId;

    @NotBlank(message = "El email es obligatorio")
    private String email;

}