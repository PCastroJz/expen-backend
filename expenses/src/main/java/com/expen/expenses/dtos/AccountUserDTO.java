package com.expen.expenses.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountUserDTO {
    private Long id;
    private Long accountId;
    private Long userId;
    private String role;
}
