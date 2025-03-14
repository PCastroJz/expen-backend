package com.expen.expenses.controllers;

import com.expen.expenses.dtos.AccountDTO;
import com.expen.expenses.dtos.AccountRequest;
import com.expen.expenses.dtos.AccountUserDTO;
import com.expen.expenses.dtos.ShareAccountRequest;
import com.expen.expenses.services.AccountServices;
import com.expen.expenses.services.AccountUserServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountServices accountServices;
 
    @Autowired
    private AccountUserServices accountUserServices;

    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        AccountDTO createdAccount = accountServices.createAccount(accountRequest);
        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        List<AccountDTO> accounts = accountServices.getAccounts();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
        AccountDTO accountDTO = accountServices.getAccountById(id);
        return ResponseEntity.ok(accountDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDTO> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest accountRequest) {
        AccountDTO updatedAccount = accountServices.updateAccount(id, accountRequest);
        return ResponseEntity.ok(updatedAccount);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountServices.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/share-account")
    public ResponseEntity<AccountUserDTO> shareAccount(@Valid @RequestBody ShareAccountRequest shareAccountRequest) {
        AccountUserDTO accountUserDTO = accountUserServices.createAccountUser(
                shareAccountRequest.getAccountId(),
                shareAccountRequest.getEmail()
        );
        return ResponseEntity.ok(accountUserDTO);
    }

    @GetMapping("/member-accounts")
    public ResponseEntity<List<AccountDTO>> getMemberAccounts() {
        List<AccountDTO> accounts = accountUserServices.getAccountsWhereUserIsMember();
        return ResponseEntity.ok(accounts);
    }
}