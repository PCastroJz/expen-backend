package com.expen.expenses.services;

import com.expen.expenses.controllers.AccountNotificationController;
import com.expen.expenses.dtos.AccountDTO;
import com.expen.expenses.dtos.AccountRequest;
import com.expen.expenses.jwt.JwtUtil;
import com.expen.expenses.models.Account;
import com.expen.expenses.models.AccountUser;
import com.expen.expenses.models.Transaction;
import com.expen.expenses.repositories.AccountRepository;
import com.expen.expenses.repositories.AccountUserRepository;
import com.expen.expenses.repositories.TransactionRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class AccountServices {

    private static final Logger log = Logger.getLogger(AccountServices.class.getName());

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountNotificationController accountNotificationController;

    public AccountDTO createAccount(AccountRequest accountRequest) {
        Long userId = extractUserIdFromToken();

        Account account = new Account();
        account.setName(accountRequest.getName());
        account.setUserId(userId);

        Account createdAccount = accountRepository.save(account);
        log.info("Cuenta creada con id: " + createdAccount.getId());

        AccountUser accountUser = AccountUser.builder()
                .account(createdAccount)
                .userId(userId)
                .role("admin")
                .build();

        accountUserRepository.save(accountUser);
        log.info("Registro en account_users creado para el userId: " + userId + " con rol admin");

        AccountDTO accountDTO = mapToDTO(createdAccount);

        accountNotificationController.notifyAccountUpdate(accountDTO, "CREATE");

        return accountDTO;
    }

    public List<AccountDTO> getAccounts() {
        Long userId = extractUserIdFromToken();
        List<Account> accounts = accountRepository.findByUserId(userId);

        if (accounts.isEmpty()) {
            log.info("No se encontraron cuentas para el userId: " + userId);
        } else {
            log.info("Número de cuentas encontradas: " + accounts.size());
        }

        return accounts.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public AccountDTO getAccountById(Long id) {
        Long userId = extractUserIdFromToken();
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.severe("Cuenta no encontrada con id: " + id);
                    return new RuntimeException("Cuenta no encontrada con id: " + id);
                });

        if (!account.getUserId().equals(userId)) {
            log.warning("El userId del token no coincide con el userId de la cuenta");
            throw new RuntimeException("No tienes permiso para acceder a esta cuenta");
        }

        log.info("Cuenta encontrada: " + account);
        return mapToDTO(account);
    }

    public AccountDTO updateAccount(Long id, AccountRequest accountRequest) {
        Long userId = extractUserIdFromToken();
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.severe("Cuenta no encontrada con id: " + id);
                    return new RuntimeException("Cuenta no encontrada con id: " + id);
                });

        if (!account.getUserId().equals(userId)) {
            log.warning("El userId del token no coincide con el userId de la cuenta");
            throw new RuntimeException("No tienes permiso para actualizar esta cuenta");
        }

        account.setName(accountRequest.getName());
        log.info("Actualizando cuenta con id: " + id);

        Account updatedAccount = accountRepository.save(account);

        AccountDTO accountDTO = mapToDTO(updatedAccount);

        accountNotificationController.notifyAccountUpdate(accountDTO, "UPDATE");

        return accountDTO;
    }

    @Transactional
    public Map<String, Object> deleteAccount(Long id) {
        Long userId = extractUserIdFromToken();

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada con id: " + id));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para eliminar esta cuenta");
        }

        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
        if (!transactions.isEmpty()) {
            transactionRepository.deleteAll(transactions);
        }

        List<AccountUser> accountUsers = accountUserRepository.findByAccountId(account.getId());
        if (!accountUsers.isEmpty()) {
            accountUserRepository.deleteAll(accountUsers);
        }

        accountRepository.delete(account);

        AccountDTO accountDTO = mapToDTO(account);

        accountNotificationController.notifyAccountUpdate(accountDTO, "DELETE");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cuenta eliminada exitosamente");
        response.put("accountId", account.getId());
        response.put("deletedTransactions", transactions.size());
        response.put("deletedAccountUsers", accountUsers.size());

        return response;
    }

    private Long extractUserIdFromToken() {
        final String authorizationHeader = request.getHeader("Authorization");
        String jwt = null;
        Long userId = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            userId = jwtUtil.extractUserId(jwt);
            log.info("UserId extraído del token: " + userId);
        }

        if (userId == null) {
            log.severe("No se pudo extraer el userId del token JWT");
            throw new RuntimeException("No se pudo extraer el userId del token JWT");
        }

        return userId;
    }

    public AccountDTO mapToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setUserId(account.getUserId());
        return dto;
    }
}