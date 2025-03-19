package com.expen.expenses.services;

import com.expen.expenses.dtos.BalanceDTO;
import com.expen.expenses.dtos.TransactionDTO;
import com.expen.expenses.dtos.TransactionRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = Logger.getLogger(TransactionService.class.getName());

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    public TransactionDTO createTransaction(TransactionRequest transactionRequest) {
        log.info("Iniciando creacion transacción: ");
        Long userId = extractUserIdFromToken();
        Long accountId = transactionRequest.getAccountId();

        log.info("Id de la cuenta: " + accountId);

        Account accounts = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta no existe"));

        log.info("Cuenta: " + accounts);

        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning("El usuario con id " + userId
                    + " no tiene permisos para crear transacciones en la cuenta con id " + accountId);
            throw new IllegalStateException("No tienes permisos para crear transacciones en esta cuenta");
        }

        Transaction transaction = new Transaction();
        transaction.setType(transactionRequest.getType());
        transaction.setCategory(transactionRequest.getCategory());
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setDate(transactionRequest.getDate());
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setAccount(accounts);
        transaction.setUserId(userId);
        transaction.setSchedule(transactionRequest.isSchedule());
        transaction.setIdSchedule(transactionRequest.getIdSchedule());

        log.info("Creando transacción: " + transaction);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToDTO(savedTransaction);
    }

    public TransactionDTO getTransactionById(Long id) {
        log.info("Buscando transacción con id: " + id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));

        Long userId = extractUserIdFromToken();
        Long accountId = transaction.getAccount().getId();

        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning("El usuario con id " + userId + " no tiene permisos para ver la transacción con id " + id);
            throw new IllegalStateException("No tienes permisos para ver esta transacción");
        }

        return mapToDTO(transaction);
    }

    public List<TransactionDTO> getTransactionsByAccountId(Long accountId) {
        log.info("Buscando transacciones para la cuenta con id: " + accountId);
        Long userId = extractUserIdFromToken();

        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning("El usuario con id " + userId + " no tiene permisos para ver transacciones de la cuenta con id "
                    + accountId);
            throw new IllegalStateException("No tienes permisos para ver transacciones de esta cuenta");
        }

        return transactionRepository.findByAccountId(accountId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public TransactionDTO updateTransaction(Long id, TransactionRequest transactionRequest) {
        log.info("Actualizando transacción con id: " + id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));

        Long userId = extractUserIdFromToken();
        Long accountId = transaction.getAccount().getId();

        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning(
                    "El usuario con id " + userId + " no tiene permisos para actualizar la transacción con id " + id);
            throw new IllegalStateException("No tienes permisos para actualizar esta transacción");
        }

        transaction.setType(transactionRequest.getType());
        transaction.setCategory(transactionRequest.getCategory());
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setDate(transactionRequest.getDate());
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setAccount(accountRepository.findById(transactionRequest.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("La cuenta no existe")));
        transaction.setSchedule(transactionRequest.isSchedule());
        transaction.setIdSchedule(transactionRequest.getIdSchedule());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return mapToDTO(updatedTransaction);
    }

    public void deleteTransaction(Long id) {
        log.info("Eliminando transacción con id: " + id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));

        Long userId = extractUserIdFromToken();
        Long accountId = transaction.getAccount().getId();

        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning("El usuario con id " + userId + " no tiene permisos para eliminar la transacción con id " + id);
            throw new IllegalStateException("No tienes permisos para eliminar esta transacción");
        }

        transactionRepository.deleteById(id);
    }

    public List<TransactionDTO> getTransactionsByDateRange(Long accountId, LocalDate startDate, LocalDate endDate) {
        log.info(
                "Buscando transacciones entre " + startDate + " y " + endDate + " para la cuenta con id: " + accountId);
        Long userId = extractUserIdFromToken();

        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning("El usuario con id " + userId + " no tiene permisos para ver transacciones de la cuenta con id "
                    + accountId);
            throw new IllegalStateException("No tienes permisos para ver transacciones de esta cuenta");
        }

        return transactionRepository.findByAccountIdAndDateBetween(accountId, startDate, endDate).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setType(transaction.getType());
        dto.setCategory(transaction.getCategory());
        dto.setAmount(transaction.getAmount());
        dto.setDate(transaction.getDate());
        dto.setDescription(transaction.getDescription());
        dto.setAccountId(transaction.getAccount().getId());
        dto.setSchedule(transaction.isSchedule());
        dto.setIdSchedule(transaction.getIdSchedule());
        return dto;
    }

    private boolean isUserOwnerOrMemberOfAccount(Long userId, Long accountId) {

        Optional<AccountUser> accountUserOpt = accountUserRepository.findByAccountIdAndUserId(accountId, userId);

        return accountUserOpt.isPresent();
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

    public BalanceDTO calculateBalance(Long accountId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculando balance para la cuenta con id: " + accountId + " entre " + startDate + " y " + endDate);

        Long userId = extractUserIdFromToken();

        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning("El usuario con id " + userId + " no tiene permisos para ver transacciones de la cuenta con id "
                    + accountId);
            throw new IllegalStateException("No tienes permisos para ver transacciones de esta cuenta");
        }

        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateBetween(accountId, startDate,
                endDate);

        List<Transaction> filteredTransactions = transactions.stream()
                .filter(t -> !t.isSchedule() || t.isPay())
                .collect(Collectors.toList());

        double totalIncome = filteredTransactions.stream()
                .filter(t -> "Ingreso".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalExpenses = filteredTransactions.stream()
                .filter(t -> "Gasto".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double balance = totalIncome - totalExpenses;

        BalanceDTO balanceDTO = new BalanceDTO();
        balanceDTO.setTotalIncome(totalIncome);
        balanceDTO.setTotalExpenses(totalExpenses);
        balanceDTO.setBalance(balance);
        balanceDTO.setTransactions(filteredTransactions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));

        return balanceDTO;
    }

    public TransactionDTO markTransactionAsPaid(Long transactionId) {
        log.info("Marcando transacción con id: " + transactionId + " como pagada");
    
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));
    
        Long userId = extractUserIdFromToken();
        Long accountId = transaction.getAccount().getId();
    
        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning("El usuario con id " + userId + " no tiene permisos para modificar la transacción con id "
                    + transactionId);
            throw new IllegalStateException("No tienes permisos para modificar esta transacción");
        }
    
        if (!transaction.isSchedule()) {
            log.warning("La transacción con id " + transactionId + " no es programada");
            throw new IllegalStateException("Solo las transacciones programadas pueden marcarse como pagadas");
        }
    
        if (transaction.isPay()) {
            log.warning("La transacción con id " + transactionId + " ya está marcada como pagada");
            throw new IllegalStateException("La transacción ya está marcada como pagada");
        }
    
        transaction.setPay(true);
        Transaction updatedTransaction = transactionRepository.save(transaction);
    
        return mapToDTO(updatedTransaction);
    }

    public List<TransactionDTO> getTransactionsByTypeAndDateRange(Long accountId, String type, LocalDate startDate, LocalDate endDate) {
        log.info("Buscando transacciones de tipo " + type + " entre " + startDate + " y " + endDate
                + " para la cuenta con id: " + accountId);
    
        Long userId = extractUserIdFromToken();
    
        if (!isUserOwnerOrMemberOfAccount(userId, accountId)) {
            log.warning("El usuario con id " + userId + " no tiene permisos para ver transacciones de la cuenta con id "
                    + accountId);
            throw new IllegalStateException("No tienes permisos para ver transacciones de esta cuenta");
        }
    
        List<Transaction> transactions = transactionRepository.findByAccountIdAndTypeAndDateBetween(accountId, type, startDate, endDate);
    
        return transactions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

}