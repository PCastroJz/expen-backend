package com.expen.expenses.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BalanceDTO {
    private double totalIncome;
    private double totalExpenses;
    private double balance;
    private List<TransactionDTO> transactions;

}