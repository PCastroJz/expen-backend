package com.expen.expenses.controllers;

import com.expen.expenses.dtos.BalanceDTO;
import com.expen.expenses.dtos.TransactionDTO;
import com.expen.expenses.dtos.TransactionRequest;
import com.expen.expenses.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody TransactionRequest transactionRequest) {
        TransactionDTO createdTransaction = transactionService.createTransaction(transactionRequest);
        return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable Long id) {
        TransactionDTO transactionDTO = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transactionDTO);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByAccountId(@PathVariable Long accountId) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDTO> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest transactionRequest) {
        TransactionDTO updatedTransaction = transactionService.updateTransaction(id, transactionRequest);
        return ResponseEntity.ok(updatedTransaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRange(
            @RequestParam Long accountId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByDateRange(accountId, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceDTO> calculateBalance(
            @RequestParam Long accountId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        BalanceDTO balanceDTO = transactionService.calculateBalance(accountId, startDate, endDate);
        return ResponseEntity.ok(balanceDTO);
    }

    @PatchMapping("/{id}/mark-as-paid")
    public ResponseEntity<TransactionDTO> markTransactionAsPaid(@PathVariable Long id) {
        TransactionDTO transactionDTO = transactionService.markTransactionAsPaid(id);
        return ResponseEntity.ok(transactionDTO);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByTypeAndDateRange(
            @RequestParam Long accountId,
            @RequestParam String type,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByTypeAndDateRange(accountId, type, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }
}