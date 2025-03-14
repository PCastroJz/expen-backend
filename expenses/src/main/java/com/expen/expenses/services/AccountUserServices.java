package com.expen.expenses.services;

import com.expen.expenses.dtos.AccountDTO;
import com.expen.expenses.dtos.AccountUserDTO;
import com.expen.expenses.jwt.JwtUtil;
import com.expen.expenses.models.Account;
import com.expen.expenses.models.AccountUser;
import com.expen.expenses.repositories.AccountRepository;
import com.expen.expenses.repositories.AccountUserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class AccountUserServices {

    private static final Logger log = Logger.getLogger(AccountUserServices.class.getName());

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RestTemplate restTemplate;

    public AccountUserDTO createAccountUser(Long accountId, String email) {
        Long authenticatedUserId = extractUserIdFromToken();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.severe("La cuenta con id " + accountId + " no existe");
                    throw new RuntimeException("La cuenta no existe");
                });

        Optional<AccountUser> adminAccountUserOpt = accountUserRepository.findByAccountIdAndUserId(accountId,
                authenticatedUserId);
        if (adminAccountUserOpt.isEmpty() || !"admin".equals(adminAccountUserOpt.get().getRole())) {
            log.warning("El usuario con id " + authenticatedUserId + " no tiene permisos para compartir la cuenta");
            throw new RuntimeException("No tienes permisos para compartir esta cuenta");
        }

        Long userIdToAdd = getUserIdByEmail(email);

        AccountUser newAccountUser = AccountUser.builder()
                .account(account)
                .userId(userIdToAdd)
                .role("member")
                .build();

        accountUserRepository.save(newAccountUser);
        log.info("Nuevo AccountUser creado para el userId: " + userIdToAdd + " con rol member");

        return mapToDTO(newAccountUser);
    }

    public List<AccountDTO> getAccountsWhereUserIsMember() {
        Long userId = extractUserIdFromToken();
        log.info("Buscando cuentas donde el usuario es 'member' con userId: " + userId);

        List<AccountUser> accountUsers = accountUserRepository.findByUserIdAndRole(userId, "member");

        if (accountUsers.isEmpty()) {
            log.info("No se encontraron cuentas para el usuario con rol 'member'");
        } else {
            log.info("Número de cuentas encontradas: " + accountUsers.size());
        }

        return accountUsers.stream()
                .map(AccountUser::getAccount)
                .map(this::mapToAccountDTO)
                .collect(Collectors.toList());
    }

    private AccountDTO mapToAccountDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setUserId(account.getUserId());
        return dto;
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

    private Long getUserIdByEmail(String email) {
        String url = "http://localhost:8081/user/find-by-email?email=" + email;
        final String authorizationHeader = request.getHeader("Authorization");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, entity, Long.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }

    private AccountUserDTO mapToDTO(AccountUser accountUser) {
        AccountUserDTO dto = new AccountUserDTO();
        dto.setId(accountUser.getId());
        dto.setAccountId(accountUser.getAccount().getId());
        dto.setUserId(accountUser.getUserId());
        dto.setRole(accountUser.getRole());
        return dto;
    }

}