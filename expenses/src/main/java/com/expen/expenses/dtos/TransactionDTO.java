package com.expen.expenses.dtos;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDTO {
    private Long id;
    private String type;
    private String category;
    private Double amount;
    private LocalDate date;
    private String description;
    private Long accountId;
    private boolean schedule;
    private Long idSchedule;
}