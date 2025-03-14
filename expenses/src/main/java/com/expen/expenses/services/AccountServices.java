package com.expen.expenses.services;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        return mapToDTO(createdAccount);
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

        return mapToDTO(updatedAccount);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Long userId = extractUserIdFromToken();

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.severe("Cuenta no encontrada con id: " + id);
                    return new RuntimeException("Cuenta no encontrada con id: " + id);
                });

        if (!account.getUserId().equals(userId)) {
            log.warning("El userId del token no coincide con el userId de la cuenta");
            throw new RuntimeException("No tienes permiso para eliminar esta cuenta");
        }

        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
        if (!transactions.isEmpty()) {
            log.info("Eliminando " + transactions.size() + " transacciones asociadas a la cuenta con id: " + id);
            transactionRepository.deleteAll(transactions);
        }

        List<AccountUser> accountUsers = accountUserRepository.findByAccountId(account.getId());
        if (!accountUsers.isEmpty()) {
            log.info("Eliminando " + accountUsers.size() + " AccountUser asociados a la cuenta con id: " + id);
            accountUserRepository.deleteAll(accountUsers);
        }

        log.info("Eliminando cuenta con id: " + id);
        accountRepository.delete(account);
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

    private AccountDTO mapToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setUserId(account.getUserId());
        return dto;
    }
}